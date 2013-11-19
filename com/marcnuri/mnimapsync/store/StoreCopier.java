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

import com.marcnuri.mnimapsync.MNIMAPSync;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
public class StoreCopier {

//**************************************************************************************************
//  Fields
//**************************************************************************************************
    private final ExecutorService service;
    private final IMAPStore sourceStore;
    private final IMAPStore targetStore;
    private final StoreIndex targetIndex;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    public StoreCopier(IMAPStore sourceStore, IMAPStore targetStore, StoreIndex targetIndex) {
        this.sourceStore = sourceStore;
        this.targetStore = targetStore;
        this.targetIndex = targetIndex;
        service = Executors.newFixedThreadPool(MNIMAPSync.THREADS);
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
    public final void copy() throws InterruptedException {
        try {
            //Copy Folder Structure
            copyFolder(sourceStore.getDefaultFolder());
            //Copy messages
            copyMessages((IMAPFolder) sourceStore.getDefaultFolder());

        } catch (MessagingException ex) {
            Logger.getLogger(StoreCopier.class.getName()).log(Level.SEVERE, null, ex);
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.DAYS);

    }

    /**
     * Create folders in the target server recursively from the source.
     *
     * @param folder
     * @throws MessagingException
     */
    private void copyFolder(Folder folder) throws MessagingException {
        final String folderName = folder.getFullName();
        if (!targetIndex.getFolders().contains(folderName)) {
            targetStore.getFolder(folderName).create(folder.getType());
        }
        //Folder recursion. Get all children
        if ((folder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS) {
            for (Folder child : folder.list()) {
                copyFolder(child);
            }
        }
    }

    private void copyMessages(IMAPFolder sourceFolder) throws MessagingException {
        if (sourceFolder != null) {
            final String folderName = sourceFolder.getFullName();
            if ((sourceFolder.getType() & Folder.HOLDS_MESSAGES) == Folder.HOLDS_MESSAGES) {
                sourceFolder.open(Folder.READ_WRITE);
                sourceFolder.expunge();
                final int messageCount = sourceFolder.getMessageCount();
                sourceFolder.close(false);
                int pos = 1;
                while (pos + MNIMAPSync.BATCH_SIZE <= messageCount) {
                    service.execute(new MessageCopier(sourceStore, targetStore, folderName, pos,
                            pos + MNIMAPSync.BATCH_SIZE, targetIndex.getFolderMessages(folderName)));
                    pos = pos + MNIMAPSync.BATCH_SIZE;
                }
                service.execute(new MessageCopier(sourceStore, targetStore, folderName, pos,
                        messageCount, targetIndex.getFolderMessages(folderName)));
            }
            //Folder recursion. Get all children
            if ((sourceFolder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS) {
                for (Folder child : sourceFolder.list()) {
                    copyMessages((IMAPFolder) child);
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
//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
    private static final class MessageCopier implements Runnable {

        private final IMAPStore sourceStore;
        private final IMAPStore targetStore;
        private final String folderName;
        private final int start;
        private final int end;
        private final Set<MessageId> targetFolderMessages;

        public MessageCopier(IMAPStore sourceStore, IMAPStore targetStore, String folderName,
                int start, int end, Set<MessageId> targetFolderMessages) {
            this.sourceStore = sourceStore;
            this.targetStore = targetStore;
            this.folderName = folderName;
            this.start = start;
            this.end = end;
            this.targetFolderMessages = targetFolderMessages;
        }

        public void run() {
            try {
                final Folder sourceFolder = sourceStore.getFolder(folderName);
                final Folder targetFolder = targetStore.getFolder(folderName);
                //Opens a new connection per Thread
                sourceFolder.open(Folder.READ_WRITE);
                final Message[] sourceMessages = sourceFolder.getMessages(start, end);
                final FetchProfile envelopeProfile = new FetchProfile();
                envelopeProfile.add(FetchProfile.Item.ENVELOPE);
                envelopeProfile.add(MNIMAPSync.HEADER_SUBJECT);
                sourceFolder.fetch(sourceMessages, envelopeProfile);
                final List<Message> toCopy = new ArrayList<Message>();
                for (Message message : sourceMessages) {
                    final MessageId id = new MessageId(
                            ((IMAPMessage) message).getMessageID(),
                            ((IMAPMessage) message).getFrom(),
                            ((IMAPMessage) message).getRecipients(Message.RecipientType.TO),
                            message.getSubject()
//message.getHeader(MNIMAPSync.HEADER_SUBJECT)
                    );
                    if (!targetFolderMessages.contains(id)) {
                        toCopy.add(message);
                    } else {
                        System.out.println("Skipped message: " + message.getSubject());
                    }
                }
                targetFolder.open(Folder.READ_WRITE);
                final FetchProfile fullProfile = new FetchProfile();
                fullProfile.add(FetchProfile.Item.ENVELOPE);
                fullProfile.add(FetchProfile.Item.CONTENT_INFO);
                fullProfile.add(FetchProfile.Item.FLAGS);
                fullProfile.add(MNIMAPSync.HEADER_SUBJECT);
                sourceFolder.fetch(sourceMessages, fullProfile);
                for (Message message : toCopy) {
                    targetFolder.appendMessages(new Message[]{message});
                    System.out.println("Copied message: " + message.getSubject());
                }
                targetFolder.close(false);
                sourceFolder.close(false);
            } catch (MessagingException messagingException) {
                Logger.getLogger(StoreIndex.class.getName()).log(Level.SEVERE, null,
                        messagingException);
            }
        }

    }
}
