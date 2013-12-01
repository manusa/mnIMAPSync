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
import com.sun.mail.imap.IMAPStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Folder;
import javax.mail.MessagingException;

/**
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public class StoreDeleter {

//**************************************************************************************************
//  Fields
//**************************************************************************************************
    private final ExecutorService service;
    private final IMAPStore sourceStore;
    private final IMAPStore targetStore;
    private final StoreIndex sourceIndex;
    private final AtomicInteger foldersDeletedCount;
    private final AtomicInteger foldersSkippedCount;
    private final AtomicLong messagesDeletedCount;
    private final AtomicLong messagesSkippedCount;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    public StoreDeleter(IMAPStore sourceStore, StoreIndex sourceIndex, IMAPStore targetStore) {
        service = Executors.newFixedThreadPool(MNIMAPSync.THREADS);
        this.sourceStore = sourceStore;
        this.targetStore = targetStore;
        this.sourceIndex = sourceIndex;
        this.foldersDeletedCount = new AtomicInteger();
        this.foldersSkippedCount = new AtomicInteger();
        this.messagesDeletedCount = new AtomicLong();
        this.messagesSkippedCount = new AtomicLong();
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
    public final void delete() throws InterruptedException {
        try {
            //Delete Folder Structure
            deleteFolder(targetStore.getDefaultFolder());
            //Copy messages
            deleteMessages((IMAPFolder) targetStore.getDefaultFolder());
        } catch (MessagingException ex) {
            Logger.getLogger(StoreDeleter.class.getName()).log(Level.SEVERE, null, ex);
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.DAYS);
    }

    private void deleteMessages(IMAPFolder targetFolder) throws MessagingException {
        if (targetFolder != null) {
            final String folderName = targetFolder.getFullName();
            if ((targetFolder.getType() & Folder.HOLDS_MESSAGES) == Folder.HOLDS_MESSAGES) {
                targetFolder.open(Folder.READ_WRITE);
                targetFolder.expunge();
                final int messageCount = targetFolder.getMessageCount();
                targetFolder.close(false);
                int pos = 1;
                while (pos + MNIMAPSync.BATCH_SIZE <= messageCount) {
                    service.execute(new MessageDeleter(this, folderName, pos,
                            pos + MNIMAPSync.BATCH_SIZE, sourceIndex.getFolderMessages(folderName)));
                    pos = pos + MNIMAPSync.BATCH_SIZE;
                }
                service.execute(new MessageDeleter(this, folderName, pos, messageCount,
                        sourceIndex.getFolderMessages(folderName)));
            }
            //Folder recursion. Get all children
            if ((targetFolder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS) {
                for (Folder child : targetFolder.list()) {
                    deleteMessages((IMAPFolder) child);
                }
            }
        }
    }

    private void deleteFolder(Folder folder) throws MessagingException {
        final String folderName = folder.getFullName();
        //Delete folder
        if (!sourceIndex.getFolders().contains(folderName)) {
            //Delete recursively
            targetStore.getFolder(folderName).delete(true);
            updatedFoldersDeletedCount(1);
        }
        //Folder recursion. Get all children
        if ((folder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS) {
            for (Folder child : folder.list()) {
                deleteFolder(child);
            }
        }
    }

    protected final void updatedFoldersDeletedCount(int delta) {
        foldersDeletedCount.getAndAdd(delta);
    }

    protected final void updatedFoldersSkippedCount(int delta) {
        foldersSkippedCount.getAndAdd(delta);
    }

    protected final void updatedMessagesDeletedCount(long delta) {
        messagesDeletedCount.getAndAdd(delta);
    }

    protected final void updateMessagesSkippedCount(long delta) {
        messagesSkippedCount.getAndAdd(delta);
    }

//**************************************************************************************************
//  Getter/Setter Methods
//**************************************************************************************************
    public final int getFoldersDeletedCount() {
        return foldersDeletedCount.get();
    }

    public final int getFoldersSkippedCount() {
        return foldersSkippedCount.get();
    }

    public final long getMessagesDeletedCount() {
        return messagesDeletedCount.get();
    }

    public final long getMessagesSkippedCount() {
        return messagesSkippedCount.get();
    }

    protected final IMAPStore getSourceStore() {
        return sourceStore;
    }

    protected final StoreIndex getSourceIndex() {
        return sourceIndex;
    }

    protected final IMAPStore getTargetStore() {
        return targetStore;
    }
//**************************************************************************************************
//  Static Methods
//**************************************************************************************************
//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
}
