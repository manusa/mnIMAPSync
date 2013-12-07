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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
public final class StoreCopier {

//**************************************************************************************************
//  Fields
//**************************************************************************************************
    private final ExecutorService service;
    private final IMAPStore sourceStore;
    private final char sourceSeparator;
    private final IMAPStore targetStore;
    private final char targetSeparator;
    private final StoreIndex sourceIndex;
    private final StoreIndex targetIndex;
    private final AtomicInteger foldersCopiedCount;
    private final AtomicInteger foldersSkippedCount;
    private final AtomicLong messagesCopiedCount;
    private final AtomicLong messagesSkippedCount;
    //If no empty, we shouldn't allow deletion
    private final List<MessagingException> copyExceptions;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    public StoreCopier(IMAPStore sourceStore, StoreIndex sourceIndex, IMAPStore targetStore,
            StoreIndex targetIndex, int threads) throws MessagingException {
        this.sourceStore = sourceStore;
        this.sourceSeparator = sourceStore.getDefaultFolder().getSeparator();
        this.sourceIndex = sourceIndex;
        this.targetStore = targetStore;
        this.targetSeparator = targetStore.getDefaultFolder().getSeparator();
        this.targetIndex = targetIndex;
        service = Executors.newFixedThreadPool(threads);
        foldersCopiedCount = new AtomicInteger();
        foldersSkippedCount = new AtomicInteger();
        messagesCopiedCount = new AtomicLong();
        messagesSkippedCount = new AtomicLong();
        this.copyExceptions = Collections.synchronizedList(new ArrayList<MessagingException>());
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
            copySourceFolder(sourceStore.getDefaultFolder());
            //Copy messages
            copySourceMessages((IMAPFolder) sourceStore.getDefaultFolder());
        } catch (MessagingException ex) {
            Logger.getLogger(StoreCopier.class.getName()).log(Level.SEVERE, null, ex);
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.DAYS);
    }

    /**
     * Create folders in the target server recursively from the source.
     *
     * It also indexes the source store folders if we want to delete target folders that no longer
     * exist
     *
     * @param folder
     * @throws MessagingException
     */
    private void copySourceFolder(Folder folder) throws MessagingException {
        final String sourceFolderName = folder.getFullName();
        final String targetFolderName
                = MNIMAPSync.translateFolderName(sourceSeparator, targetSeparator, sourceFolderName);
        //Index for delete after copy (if necessary)
        if (sourceIndex != null) {
            sourceIndex.getFolders().add(sourceFolderName);
        }
        //Copy folder
        if (!targetIndex.getFolders().contains(targetFolderName)) {
            if (!targetStore.getFolder(targetFolderName).create(folder.getType())) {
                throw new MessagingException(String.format(
                        "Couldn't create folder: %s in target server.", sourceFolderName));
            }
            updatedFoldersCopiedCount(1);
        } else {
            updatedFoldersSkippedCount(1);
        }
        //Folder recursion. Get all children
        if ((folder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS) {
            for (Folder child : folder.list()) {
                copySourceFolder(child);
            }
        }
    }

    /**
     * Once the folder structure has been created it copies messages recursively from the root
     * folder.
     *
     * @param sourceFolder
     * @throws MessagingException
     */
    private void copySourceMessages(IMAPFolder sourceFolder) throws MessagingException {
        if (sourceFolder != null) {
            final String sourceFolderName = sourceFolder.getFullName();
            final String targetFolderName
                    = MNIMAPSync.translateFolderName(sourceSeparator, targetSeparator,
                            sourceFolderName);
            if ((sourceFolder.getType() & Folder.HOLDS_MESSAGES) == Folder.HOLDS_MESSAGES) {
                sourceFolder.open(Folder.READ_WRITE);
                sourceFolder.expunge();
                final int messageCount = sourceFolder.getMessageCount();
                sourceFolder.close(false);
                int pos = 1;
                while (pos + MNIMAPSync.BATCH_SIZE <= messageCount) {
                    //Copy messages
                    service.execute(new MessageCopier(this, sourceFolderName, targetFolderName, pos,
                            pos + MNIMAPSync.BATCH_SIZE, targetIndex.getFolderMessages(
                                    targetFolderName)));
                    pos = pos + MNIMAPSync.BATCH_SIZE;
                }
                service.execute(new MessageCopier(this, sourceFolderName, targetFolderName, pos,
                        messageCount,
                        targetIndex.getFolderMessages(targetFolderName)));
            }
            //Folder recursion. Get all children
            if ((sourceFolder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS) {
                for (Folder child : sourceFolder.list()) {
                    copySourceMessages((IMAPFolder) child);
                }
            }
        }
    }
    public final boolean hasCopyException() {
        synchronized (copyExceptions) {
            return !copyExceptions.isEmpty();
        }
    }
    
    protected final void updatedFoldersCopiedCount(int delta) {
        foldersCopiedCount.getAndAdd(delta);
    }

    protected final void updatedFoldersSkippedCount(int delta) {
        foldersSkippedCount.getAndAdd(delta);
    }

    protected final void updatedMessagesCopiedCount(long delta) {
        messagesCopiedCount.getAndAdd(delta);
    }

    protected final void updateMessagesSkippedCount(long delta) {
        messagesSkippedCount.getAndAdd(delta);
    }

//**************************************************************************************************
//  Getter/Setter Methods
//**************************************************************************************************
    public final int getFoldersCopiedCount() {
        return foldersCopiedCount.get();
    }

    public final int getFoldersSkippedCount() {
        return foldersSkippedCount.get();
    }

    public final long getMessagesCopiedCount() {
        return messagesCopiedCount.get();
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
    
    public final synchronized List<MessagingException> getCopyExceptions() {
        return copyExceptions;
    }
//**************************************************************************************************
//  Static Methods
//**************************************************************************************************
//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************

}
