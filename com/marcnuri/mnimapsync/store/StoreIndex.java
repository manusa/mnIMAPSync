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
package com.marcnuri.mnimapsync.store;

import com.marcnuri.mnimapsync.HostDefinition;
import com.marcnuri.mnimapsync.MNIMAPSync;
import com.marcnuri.mnimapsync.store.MessageId;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public class StoreIndex {

//**************************************************************************************************
//  Fields
//**************************************************************************************************
    private final HostDefinition hostDefinition;
    private final List<String> folders;
    private final Map<String, List<Message>> folderMessages;
    private final Map<MessageId, Message> messageIndex;
    private long messageCount;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    private StoreIndex(HostDefinition hostDefinition) {
        this.hostDefinition = hostDefinition;
        this.folders = Collections.synchronizedList(new ArrayList<String>());
        this.folderMessages = Collections.synchronizedMap(new HashMap<String, List<Message>>());
        this.messageIndex = Collections.synchronizedMap(new HashMap<MessageId, Message>());
        this.messageCount = 0;
    }

//**************************************************************************************************
//  Abstract Methods
//**************************************************************************************************
//**************************************************************************************************
//  Overridden Methods
//**************************************************************************************************
    public final synchronized List<String> getFolders() {
        return folders;
    }

    public final synchronized long getMessageCount() {
        return messageCount;
    }

    public final synchronized void setMessageCount(long messageCount) {
        this.messageCount = messageCount;
    }

    public final synchronized Map<String, List<Message>> getFolderMessages() {
        return folderMessages;
    }

    public final synchronized Map<MessageId, Message> getMessageIndex() {
        return messageIndex;
    }

//**************************************************************************************************
//  Other Methods
//**************************************************************************************************
//**************************************************************************************************
//  Getter/Setter Methods
//**************************************************************************************************
//**************************************************************************************************
//  Static Methods
//**************************************************************************************************
    public static final StoreIndex getInstance(IMAPStore store, HostDefinition hostDefinition)
            throws MessagingException {
        final StoreIndex ret = new StoreIndex(hostDefinition);
        //Crawl
        synchronized (ret.getFolders()) {
            try {
                //Test must modify a lot
                final ExecutorService service = Executors.newFixedThreadPool(MNIMAPSync.THREADS);
                crawlFolders(ret, store.getDefaultFolder(), service);
                service.shutdown();
                service.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException ex) {
                Logger.getLogger(StoreIndex.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ret;
    }

    //Hacerlo por mensajes y no por carpetas.
    private static final class Handler implements Runnable {

        private final StoreIndex storeIndex;
        private final Folder folder;
        private final List<Message> messages;

        public Handler(StoreIndex storeIndex, Folder folder, List<Message> messages) {
            this.storeIndex = storeIndex;
            this.folder = folder;
            this.messages = messages;
        }

        public void run() {
            try {
                FetchProfile fetchProfile = new FetchProfile();
                fetchProfile.add(FetchProfile.Item.ENVELOPE);
                folder.fetch(messages.toArray(new Message[messages.size()]), fetchProfile);
                for (Message message : messages) {
                    storeIndex.getMessageIndex().put(
                            new MessageId(((IMAPMessage) message).getMessageID(),
                                    ((IMAPMessage) message).getFrom(),
                                    ((IMAPMessage) message).getRecipients(Message.RecipientType.TO),
                                    ((IMAPMessage) message).getSubject()), message);
                    System.out.println("Reading: " + message.getMessageNumber() + " / "
                            + ((IMAPMessage) message).getMessageID() + " / " + message.getSubject());
                }
            } catch (MessagingException messagingException) {
                Logger.getLogger(StoreIndex.class.getName()).log(Level.SEVERE, null,
                        messagingException);
            }
        }

    }

    private static StoreIndex crawlFolders(StoreIndex storeIndex, Folder folder,
            /*TEMP*/ ExecutorService service) throws
            MessagingException {
        if (folder != null) {
            final String folderName = folder.getFullName();
            storeIndex.getFolders().add(folderName);
            //Folder recursion. Get all children
            if ((folder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS) {
                for (Folder child : folder.list()) {
                    crawlFolders(storeIndex, child, service);
                }
            }
            if ((folder.getType() & Folder.HOLDS_MESSAGES) == Folder.HOLDS_MESSAGES) {
                //Update total message count
                folder.open(Folder.READ_WRITE);
                storeIndex.setMessageCount(storeIndex.getMessageCount() + folder.getMessageCount());
                final Message[] messgeArray = folder.getMessages();
                final List<Message> messages = Arrays.asList(messgeArray);
                storeIndex.getFolderMessages().put(folderName, messages);
                final int batch = 200;
                int pos = 0;
                while (pos + batch < messages.size()) {
                    service.execute(new Handler(storeIndex, folder, messages.subList(pos, pos
                            + batch)));
                    pos = pos + batch;
                }
                service.execute(new Handler(storeIndex, folder, messages.subList(pos, messages.
                        size())));
            }
        }
        return storeIndex;
    }

//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
}
