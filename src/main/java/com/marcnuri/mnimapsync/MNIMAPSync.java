/*
 * Copyright 2013 Marc Nuri San Felix
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.marcnuri.mnimapsync;

import static com.marcnuri.mnimapsync.cli.ArgumentParser.parseCliArguments;
import static com.marcnuri.mnimapsync.cli.CliSummaryReport.getSummaryReportAsText;
import static com.marcnuri.mnimapsync.imap.IMAPUtils.openStore;
import static com.marcnuri.mnimapsync.index.StoreCrawler.populateFromStore;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;

import com.marcnuri.mnimapsync.cli.SyncMonitor;
import com.marcnuri.mnimapsync.index.Index;
import com.marcnuri.mnimapsync.store.StoreCopier;
import com.marcnuri.mnimapsync.store.StoreDeleter;
import com.sun.mail.imap.IMAPStore;

/**
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
@SuppressWarnings("WeakerAccess")
public class MNIMAPSync {

    static final int THREADS = 5;
    public static final int BATCH_SIZE = 200;
    private final SyncOptions syncOptions;
    private final Date startDate;
    private StoreCopier sourceCopier;
    private StoreDeleter targetDeleter;
    //Used for deleting tasks unnecessary if not deleting
    private final Index sourceIndex;
    private final Index targetIndex;

    public MNIMAPSync(SyncOptions syncOptions) {
        this.syncOptions = syncOptions;
        startDate = new Date();
        sourceCopier = null;
        sourceIndex = new Index();
        targetIndex = new Index();
    }

    private long getElapsedTime() {
        return System.currentTimeMillis() - startDate.getTime();
    }

    public StoreCopier getSourceCopier() {
        return sourceCopier;
    }

    public StoreDeleter getTargetDeleter() {
        return targetDeleter;
    }

    public Index getTargetIndex() {
        return targetIndex;
    }

    public long getElapsedTimeInSeconds() {
        return getElapsedTime() / 1000L;
    }

    private void indexTargetStore()
        throws MessagingException, GeneralSecurityException, InterruptedException {

        try (final IMAPStore targetStore = openStore(syncOptions.getTargetHost(),
            syncOptions.getThreads())) {
            populateFromStore(targetIndex, targetStore, syncOptions.getThreads());
        }
    }

    private void copySourceToTarget()
        throws MessagingException, GeneralSecurityException, InterruptedException {

        try (
            final IMAPStore targetStore = openStore(syncOptions.getTargetHost(),
                syncOptions.getThreads());
            final IMAPStore sourceStore = openStore(syncOptions.getSourceHost(),
                syncOptions.getThreads())
        ) {
            sourceCopier = new StoreCopier(sourceStore, sourceIndex, targetStore, targetIndex,
                syncOptions.getThreads());
            sourceCopier.copy();
        }
    }

    private void deleteFromTarget()
        throws MessagingException, GeneralSecurityException, InterruptedException {

        try (
            final IMAPStore targetStore = openStore(syncOptions.getTargetHost(),
                syncOptions.getThreads())
        ) {
            targetDeleter = new StoreDeleter(sourceIndex, targetIndex, targetStore,
                syncOptions.getThreads());
            targetDeleter.delete();
        }
    }

    public void sync() {
        try {
            indexTargetStore();
            copySourceToTarget();
            //Delete only if source store was completely indexed (this happens if no exceptions where raised)
            if (syncOptions.getDelete() && !sourceCopier.hasCopyException()) {
                deleteFromTarget();
            }
        } catch (MessagingException | GeneralSecurityException ex) {
            Logger.getLogger(MNIMAPSync.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(MNIMAPSync.class.getName()).log(Level.SEVERE, null, ex);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @param args the command line arguments
     */
    @SuppressWarnings({"squid:S106", "UseOfSystemOutOrSystemErr"})
    public static void main(String[] args) {
        try {
            final MNIMAPSync sync = new MNIMAPSync(parseCliArguments(args));
            final Timer timer = new Timer(true);
            timer.schedule(
                new SyncMonitor(sync),
                1000L, 1000L);
            sync.sync();
            timer.cancel();
            System.out.println(String.format("\r%s", getSummaryReportAsText(sync)));
        } catch (IllegalArgumentException | IOException ex) {
            System.err.println(ex.getMessage());
        }
    }


}
