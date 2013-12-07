/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.marcnuri.mnimapsync;

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

//**************************************************************************************************
//  Fields
//**************************************************************************************************
    public static final int THREADS = 5;
    public static final int BATCH_SIZE = 200;
    public static final String HEADER_SUBJECT = "Subject";
    private final SyncOptions syncOptions;
    private final Date startDate;
    private StoreCopier sourceCopier;
    private StoreDeleter targetDeleter;
    //Used for deleting tasks unnecessary if not deleting
    private final StoreIndex sourceIndex;
    private final StoreIndex targetIndex;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
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

//**************************************************************************************************
//  Abstract Methods
//**************************************************************************************************
//**************************************************************************************************
//  Overridden Methods
//**************************************************************************************************
    public final long getElapsedTime() {
        return System.currentTimeMillis() - startDate.getTime();
    }

    public final long getElapsedTimeInSeconds() {
        return getElapsedTime() / 1000l;
    }

//**************************************************************************************************
//  Other Methods
//**************************************************************************************************
    public final void sync() {
        IMAPStore targetStore = null;
        IMAPStore sourceStore = null;
        try {
            targetStore = openStore(syncOptions.getHost2(), syncOptions.getThreads());
            StoreIndex.populateFromStore(targetIndex, targetStore, syncOptions.getThreads());
            sourceStore = openStore(syncOptions.getHost1(), syncOptions.getThreads());
            sourceCopier = new StoreCopier(sourceStore, sourceIndex, targetStore, targetIndex,
                    syncOptions.getThreads());
            sourceCopier.copy();
            //Better to disconnect and reconnect. Avoids inactivity disconnections
            targetStore.close();
            sourceStore.close();
            //Delete only if source store was completely indexed (this happens if no exceptions where raised)
            if (syncOptions.getDelete() && sourceIndex != null && !sourceCopier.hasCopyException()) {
                //Reconnect stores (They can timeout for inactivity.
                sourceStore = openStore(syncOptions.getHost1(), syncOptions.getThreads());
                targetStore = openStore(syncOptions.getHost2(), syncOptions.getThreads());
                targetDeleter = new StoreDeleter(sourceStore, sourceIndex, targetStore, syncOptions.
                        getThreads());
                targetDeleter.delete();
            }
        } catch (MessagingException ex) {
            Logger.getLogger(MNIMAPSync.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(MNIMAPSync.class.getName()).log(Level.SEVERE, null, ex);
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

//**************************************************************************************************
//  Getter/Setter Methods
//**************************************************************************************************
//**************************************************************************************************
//  Static Methods
//**************************************************************************************************
    public static final String translateFolderName(char originalSeparator, char newSeparator,
            String url) {
        return url.replace(originalSeparator, newSeparator);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            final MNIMAPSync sync = new MNIMAPSync(parseArgs(args, new SyncOptions(), 0));
            final Timer timer = new Timer(true);
            timer.schedule(new SyncMonitor(sync), 1000l, 1000l);
            sync.sync();
            timer.cancel();

            System.out.println(
                    "\n===============================================================");
            System.out.println("Sync Process Finished.");
            System.out.println(
                    "===============================================================");
            if (sync.sourceCopier != null) {
                System.out.printf("\nFolders copied: %s/%s",
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
     * Parse command line arguments to build a {@link SyncOptions} object
     *
     * @param args
     * @param options
     * @param current
     * @return
     */
    private static SyncOptions parseArgs(String[] args, SyncOptions options, int current) {
        while (current < args.length) {
            final String arg = args[current++];
            //Host 1
            if (arg.equals("--host1")) {
                options.getHost1().setHost(args[current++]);
            } else if (arg.equals("--port1")) {
                try {
                    options.getHost1().setPort(Integer.parseInt(args[current++]));
                } catch (NumberFormatException numberFormatException) {
                    throw new IllegalArgumentException("Port1 should be an integer");
                }
            } else if (arg.equals("--user1")) {
                options.getHost1().setUser(args[current++]);
            } else if (arg.equals("--password1")) {
                options.getHost1().setPassword(args[current++]);
            } else if (arg.equals("--ssl1")) {
                options.getHost1().setSsl(true);
            } //Host 2
            else if (arg.equals("--host2")) {
                options.getHost2().setHost(args[current++]);
            } else if (arg.equals("--port2")) {
                try {
                    options.getHost2().setPort(Integer.parseInt(args[current++]));
                } catch (NumberFormatException numberFormatException) {
                    throw new IllegalArgumentException("Port2 should be an integer");
                }
            } else if (arg.equals("--user2")) {
                options.getHost2().setUser(args[current++]);
            } else if (arg.equals("--password2")) {
                options.getHost2().setPassword(args[current++]);
            } else if (arg.equals("--ssl2")) {
                options.getHost2().setSsl(true);
            }//Global options 
            else if (arg.equals("--delete")) {
                options.setDelete(true);
            } else if (arg.equals("--threads")) {
                try {
                    options.setThreads(Integer.parseInt(args[current++]));
                } catch (NumberFormatException numberFormatException) {
                    throw new IllegalArgumentException("Threads should be an integer");
                }
            } else {
                throw new IllegalArgumentException("Unrecognized argument: " + arg);
            }
        }
        return options;
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

//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
    private static final class SyncMonitor extends TimerTask {

        private final MNIMAPSync sync;

        public SyncMonitor(MNIMAPSync sync) {
            this.sync = sync;
        }

        @Override
        public void run() {
            System.out.print(String.format(
                    "\rIndexed (target): %,d/%,d  Copied: %,d/%,d "
                    + "Deleted: %,d/%,d Speed: %.2f m/s",
                    (sync.targetIndex != null ? sync.targetIndex.getIndexedMessageCount() : 0l),
                    (sync.targetIndex != null ? sync.targetIndex.getIndexedMessageCount()
                    + sync.targetIndex.getSkippedMessageCount() : 0l),
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
