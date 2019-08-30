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

import com.marcnuri.mnimapsync.store.StoreIndex;
import com.marcnuri.mnimapsync.ssl.AllowAllSSLSocketFactory;
import com.marcnuri.mnimapsync.store.StoreCopier;
import com.marcnuri.mnimapsync.store.StoreDeleter;
import com.sun.mail.imap.IMAPSSLStore;
import com.sun.mail.imap.IMAPStore;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.Session;

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

    private long getElapsedTimeInSeconds() {
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
        } catch (MessagingException ex) {
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
            timer.schedule(new SyncMonitor(sync), 1000L, 1000L);
            sync.sync();
            timer.cancel();

            System.out.println(
                    "\n===============================================================");
            System.out.println("Sync Process Finished.");
            System.out.println(
                    "===============================================================");
            if (sync.sourceCopier != null) {
                System.out.printf("Folders copied: %s/%s",
                        sync.sourceCopier.getFoldersCopiedCount(),
                        (sync.sourceCopier.getFoldersCopiedCount()
                        + sync.sourceCopier.getFoldersSkippedCount()));
                System.out.printf("\nMessages copied: %s/%s",
                        sync.sourceCopier.getMessagesCopiedCount(),
                        (sync.sourceCopier.getMessagesCopiedCount()
                        + sync.sourceCopier.getMessagesSkippedCount()));
                System.out.printf("\nSpeed: %.2f m/s",
                        ((sync.sourceCopier.getMessagesCopiedCount()
                        + sync.sourceCopier.getMessagesSkippedCount())
                        / ((double) sync.getElapsedTimeInSeconds())));
                System.out.printf("\nExceptions: %s", sync.sourceCopier.hasCopyException());
            }
            if (sync.targetDeleter != null) {
                System.out.printf("\nFolders deleted: %s/%s",
                        sync.targetDeleter.getFoldersDeletedCount(),
                        (sync.targetDeleter.getFoldersDeletedCount()
                        + sync.targetDeleter.getFoldersSkippedCount()));
                System.out.printf("\nMessages deleted: %s/%s",
                        sync.targetDeleter.getMessagesDeletedCount(),
                        (sync.targetDeleter.getMessagesDeletedCount()
                        + sync.targetDeleter.getMessagesSkippedCount()));
            }
            System.out.println(
                    "\nElapsed time: " + sync.getElapsedTimeInSeconds() + " s");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Open an {@link IMAPStore} for the provided {@link HostDefinition}
     *
     * @param host
     * @return
     * @throws MessagingException
     */
    private static IMAPStore openStore(HostDefinition host, int threads) throws MessagingException {
        final Properties properties = new Properties();
        properties.put("mail.debug", "false");
        properties.put("mail.imap.starttls.enable", true);
        properties.setProperty("mail.imap.connectionpoolsize", String.valueOf(threads));
        if (host.isSsl()) {
            properties.put("mail.imap.ssl.enable", host.isSsl());
            properties.setProperty("mail.imaps.connectionpoolsize", String.valueOf(threads));
            properties.put("mail.imaps.socketFactory.port", host.getPort());
            properties.put("mail.imaps.socketFactory.class", AllowAllSSLSocketFactory.class.
                    getName());
            properties.put("mail.imaps.socketFactory.fallback", false);
        }
        final Session session = Session.getInstance(properties, null);
        final IMAPStore ret;
        if (host.isSsl()) {
            ret = (IMAPSSLStore) session.getStore("imaps");
        } else {
            ret = (IMAPStore) session.getStore("imap");
        }
        ret.connect(host.getHost(), host.getPort(), host.getUser(), host.getPassword());
        return ret;
    }

    private static final class SyncMonitor extends TimerTask {

        private final MNIMAPSync sync;

        SyncMonitor(MNIMAPSync sync) {
            this.sync = sync;
        }

        @Override
        public void run() {
            System.out.print(String.format(
                    "\rIndexed (target): %,d/%,d  Copied: %,d/%,d "
                    + "Deleted: %,d/%,d Speed: %.2f m/s",
                sync.targetIndex.getIndexedMessageCount(),
                sync.targetIndex.getIndexedMessageCount() + sync.targetIndex
                    .getSkippedMessageCount(),
                    (sync.sourceCopier != null ? sync.sourceCopier.getMessagesCopiedCount() : 0),
                    (sync.sourceCopier != null ? sync.sourceCopier.getMessagesCopiedCount()
                    + sync.sourceCopier.getMessagesSkippedCount() : 0),
                    (sync.targetDeleter != null ? sync.targetDeleter.getMessagesDeletedCount() : 0),
                    (sync.targetDeleter != null ? sync.targetDeleter.getMessagesDeletedCount()
                    + sync.targetDeleter.getMessagesSkippedCount() : 0),
                    (sync.sourceCopier != null ? (double) (sync.sourceCopier.
                    getMessagesCopiedCount()
                    + sync.sourceCopier.getMessagesSkippedCount()) / (double) sync.
                    getElapsedTimeInSeconds() : 0)
            ));
        }

    }

}
