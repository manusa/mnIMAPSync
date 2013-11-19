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
import com.sun.mail.imap.IMAPSSLStore;
import com.sun.mail.imap.IMAPStore;
import java.io.Serializable;
import java.util.Date;
import java.util.Properties;
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
    public static final int THREADS = 8;
    public static final int BATCH_SIZE = 200;
    public static final String HEADER_SUBJECT = "Subject";
    private final SyncOptions syncOptions;
    private final Date startDate;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    public MNIMAPSync(SyncOptions syncOptions) {
        this.syncOptions = syncOptions;
        startDate = new Date();
    }

//**************************************************************************************************
//  Abstract Methods
//**************************************************************************************************
//**************************************************************************************************
//  Overridden Methods
//**************************************************************************************************
//**************************************************************************************************
//  Other Methods
//**************************************************************************************************
    public final void sync() {
        IMAPStore targetStore = null;
        IMAPStore sourceStore = null;
        try {
            targetStore = openStore(syncOptions.host2);
            final StoreIndex targetIndex = StoreIndex.getInstance(targetStore);
            sourceStore = openStore(syncOptions.host1);
            final StoreCopier sourceCopier = new StoreCopier(sourceStore, targetStore, targetIndex);
            sourceCopier.copy();
            System.out.println("===============================================================\n"
                    + "Process finished.\n"
                    + "===============================================================\n"
                    + "Folders copied: " + sourceCopier.getFoldersCopiedCount()+"\n"
                    + "Folders skipped: " + sourceCopier.getFoldersSkippedCount()+"\n"
                    + "Messages copied: " + sourceCopier.getMessagesCopiedCount()+"\n"
                    + "Messages skipped: " + sourceCopier.getMessagesSkippedCount()+"\n"
                    + "Elapsed time: "
                    + ((System.currentTimeMillis() - startDate.getTime()) / 1000l) + "s");
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

    /**
     * Open an {@link IMAPStore} for the provided {@link HostDefinition}
     *
     * @param host
     * @return
     * @throws MessagingException
     */
    private IMAPStore openStore(HostDefinition host) throws MessagingException {
        final Properties properties = new Properties();
        properties.put("mail.imap.starttls.enable", true);
        properties.setProperty("mail.imap.connectionpoolsize", String.valueOf(THREADS));
        if (host.isSsl()) {
            properties.put("mail.imap.ssl.enable", host.isSsl());
            properties.setProperty("mail.imaps.connectionpoolsize", String.valueOf(THREADS));
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
//  Getter/Setter Methods
//**************************************************************************************************
//**************************************************************************************************
//  Static Methods
//**************************************************************************************************
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            final MNIMAPSync sync = new MNIMAPSync(parseArgs(args, new SyncOptions(), 0));
            sync.sync();
            System.out.println("Finished");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private static SyncOptions parseArgs(String[] args, SyncOptions options, int current) {
        while (current < args.length) {
            final String arg = args[current++];
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
            } else if (arg.equals("--host2")) {
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
            } else {
                throw new IllegalArgumentException("Unrecognized argument: " + arg);
            }
        }
        return options;
    }

//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
    public static final class SyncOptions implements Serializable {

        private static final long serialVersionUID = 1L;
        private final HostDefinition host1;
        private final HostDefinition host2;

        public SyncOptions() {
            this.host1 = new HostDefinition();
            this.host2 = new HostDefinition();
        }

        public HostDefinition getHost1() {
            return host1;
        }

        public HostDefinition getHost2() {
            return host2;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 79 * hash + (this.host1 != null ? this.host1.hashCode() : 0);
            hash = 79 * hash + (this.host2 != null ? this.host2.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SyncOptions other = (SyncOptions) obj;
            if (this.host1 != other.host1 && (this.host1 == null || !this.host1.equals(other.host1))) {
                return false;
            }
            if (this.host2 != other.host2 && (this.host2 == null || !this.host2.equals(other.host2))) {
                return false;
            }
            return true;
        }

    }

}
