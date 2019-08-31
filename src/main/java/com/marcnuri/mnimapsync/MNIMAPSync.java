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

import com.marcnuri.mnimapsync.cli.SyncMonitor;
import com.marcnuri.mnimapsync.store.StoreCopier;
import com.marcnuri.mnimapsync.store.StoreDeleter;
import com.marcnuri.mnimapsync.store.StoreIndex;
import com.sun.mail.imap.IMAPStore;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;

/**
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public class MNIMAPSync {

    static final int THREADS = 5;
    public static final int BATCH_SIZE = 200;
    private final SyncOptions syncOptions;
    private final Date startDate;
    private StoreCopier sourceCopier;
    private StoreDeleter targetDeleter;
    //Used for deleting tasks unnecessary if not deleting
    private final StoreIndex sourceIndex;
    private final StoreIndex targetIndex;

    public MNIMAPSync(SyncOptions syncOptions) {
        this.syncOptions = syncOptions;
        startDate = new Date();
        sourceCopier = null;
        if (syncOptions.getDelete()) {
            sourceIndex = new StoreIndex();
        } else {
            sourceIndex = null;
        }
        targetIndex = new StoreIndex();

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

    public StoreIndex getTargetIndex() {
        return targetIndex;
    }

    public long getElapsedTimeInSeconds() {
        return getElapsedTime() / 1000L;
    }

    private void sync() {
        IMAPStore targetStore = null;
        IMAPStore sourceStore = null;
        try {
            targetStore = openStore(syncOptions.getTargetHost(), syncOptions.getThreads());
            StoreIndex.populateFromStore(targetIndex, targetStore, syncOptions.getThreads());
            sourceStore = openStore(syncOptions.getSourceHost(), syncOptions.getThreads());
            sourceCopier = new StoreCopier(sourceStore, sourceIndex, targetStore, targetIndex,
                    syncOptions.getThreads());
            sourceCopier.copy();
            //Better to disconnect and reconnect. Avoids inactivity disconnections
            targetStore.close();
            sourceStore.close();
            //Delete only if source store was completely indexed (this happens if no exceptions where raised)
            if (syncOptions.getDelete() && sourceIndex != null && !sourceCopier.hasCopyException()) {
                //Reconnect stores (They can timeout for inactivity.
                sourceStore = openStore(syncOptions.getSourceHost(), syncOptions.getThreads());
                targetStore = openStore(syncOptions.getTargetHost(), syncOptions.getThreads());
                targetDeleter = new StoreDeleter(sourceStore, sourceIndex, targetStore, syncOptions.
                        getThreads());
                targetDeleter.delete();
            }
        } catch (MessagingException | GeneralSecurityException ex) {
            Logger.getLogger(MNIMAPSync.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(MNIMAPSync.class.getName()).log(Level.SEVERE, null, ex);
            Thread.currentThread().interrupt();
        } finally {
            if (targetStore != null && targetStore.isConnected()) {
                try {
                    targetStore.close();
                } catch (MessagingException ex) {
                }
            }
            if (sourceStore != null && sourceStore.isConnected()) {
                try {
                    sourceStore.close();
                } catch (MessagingException ex) {
                }
            }

        }
    }

    public static String translateFolderName(char originalSeparator, char newSeparator,
            String url) {
        return url.replace(originalSeparator, newSeparator);
    }

    /**
     * @param args the command line arguments
     */
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
