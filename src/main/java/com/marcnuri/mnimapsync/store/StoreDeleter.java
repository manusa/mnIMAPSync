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

import static com.marcnuri.mnimapsync.imap.IMAPUtils.targetToSourceFolderName;

import com.marcnuri.mnimapsync.MNIMAPSync;
import com.marcnuri.mnimapsync.index.Index;
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

    private final ExecutorService service;
    private final IMAPStore targetStore;
    private final Index targetIndex;
    private final Index sourceIndex;
    private final AtomicInteger foldersDeletedCount;
    private final AtomicInteger foldersSkippedCount;
    private final AtomicLong messagesDeletedCount;
    private final AtomicLong messagesSkippedCount;

    public StoreDeleter(Index sourceIndex, Index targetIndex, IMAPStore targetStore,
        int threads) {

        service = Executors.newFixedThreadPool(threads);
        this.targetStore = targetStore;
        this.sourceIndex = sourceIndex;
        this.targetIndex = targetIndex;
        this.foldersDeletedCount = new AtomicInteger();
        this.foldersSkippedCount = new AtomicInteger();
        this.messagesDeletedCount = new AtomicLong();
        this.messagesSkippedCount = new AtomicLong();
    }

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
            final String sourceFolderName = targetToSourceFolderName(targetFolderName, sourceIndex, targetIndex);
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
                            new MessageDeleter(this, targetFolderName, pos,
                                    pos + MNIMAPSync.BATCH_SIZE, false, sourceIndex.
                                    getFolderMessages(sourceFolderName)));
                    pos = pos + MNIMAPSync.BATCH_SIZE;
                }
                service.execute(new MessageDeleter(this, targetFolderName,
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
        final String sourceFolderName = targetToSourceFolderName(targetFolderName, sourceIndex, targetIndex);
        //Delete folder
        if (!sourceIndex.containsFolder(sourceFolderName)) {
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

    private void updatedFoldersDeletedCount(int delta) {
        foldersDeletedCount.getAndAdd(delta);
    }

    protected final void updatedMessagesDeletedCount(long delta) {
        messagesDeletedCount.getAndAdd(delta);
    }

    protected final void updateMessagesSkippedCount(long delta) {
        messagesSkippedCount.getAndAdd(delta);
    }

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

    final IMAPStore getTargetStore() {
        return targetStore;
    }
}
