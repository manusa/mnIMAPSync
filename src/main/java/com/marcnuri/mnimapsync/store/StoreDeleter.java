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
    private final char sourceSeparator;
    private final IMAPStore targetStore;
    private final char targetSeparator;
    private final StoreIndex sourceIndex;
    private final AtomicInteger foldersDeletedCount;
    private final AtomicInteger foldersSkippedCount;
    private final AtomicLong messagesDeletedCount;
    private final AtomicLong messagesSkippedCount;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    public StoreDeleter(IMAPStore sourceStore, StoreIndex sourceIndex, IMAPStore targetStore,
            int threads) throws MessagingException {
        service = Executors.newFixedThreadPool(threads);
        this.sourceStore = sourceStore;
        this.sourceSeparator = sourceStore.getDefaultFolder().getSeparator();
        this.targetStore = targetStore;
        this.targetSeparator = targetStore.getDefaultFolder().getSeparator();
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
            deleteTargetFolder(targetStore.getDefaultFolder());
            //Copy messages
            deleteTargetMessages((IMAPFolder) targetStore.getDefaultFolder());
        } catch (MessagingException ex) {
            Logger.getLogger(StoreDeleter.class.getName()).log(Level.SEVERE, null, ex);
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.DAYS);
    }

    private void deleteTargetMessages(IMAPFolder targetFolder) throws MessagingException {
        if (targetFolder != null) {
            final String targetFolderName = targetFolder.getFullName();
            final String sourceFolderName = MNIMAPSync.translateFolderName(targetSeparator,
                    sourceSeparator, targetFolderName);
            if ((targetFolder.getType() & Folder.HOLDS_MESSAGES) == Folder.HOLDS_MESSAGES) {
                targetFolder.open(Folder.READ_WRITE);
                if (targetFolder.getMode() != Folder.READ_ONLY) {
                    targetFolder.expunge();
                }
                final int messageCount = targetFolder.getMessageCount();
                targetFolder.close(false);
                int pos = 1;
                while (pos + MNIMAPSync.BATCH_SIZE <= messageCount) {
                    service.execute(
                            new MessageDeleter(this, sourceFolderName, targetFolderName, pos,
                                    pos + MNIMAPSync.BATCH_SIZE, false, sourceIndex.
                                    getFolderMessages(sourceFolderName)));
                    pos = pos + MNIMAPSync.BATCH_SIZE;
                }
                service.execute(new MessageDeleter(this, sourceFolderName, targetFolderName,
                        pos, messageCount, true, sourceIndex.getFolderMessages(sourceFolderName)));
            }
            //Folder recursion. Get all children
            if ((targetFolder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS) {
                for (Folder child : targetFolder.list()) {
                    deleteTargetMessages((IMAPFolder) child);
                }
            }
        }
    }

    private void deleteTargetFolder(Folder folder) throws MessagingException {
        final String targetFolderName = folder.getFullName();
        final String sourceFolderName = MNIMAPSync.translateFolderName(targetSeparator,
                sourceSeparator,
                targetFolderName);
        //Delete folder
        if (!sourceIndex.getFolders().contains(sourceFolderName)) {
            //Delete recursively
            targetStore.getFolder(targetFolderName).delete(true);
            updatedFoldersDeletedCount(1);
        }
        //Folder recursion. Get all children
        if ((folder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS) {
            for (Folder child : folder.list()) {
                deleteTargetFolder(child);
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
