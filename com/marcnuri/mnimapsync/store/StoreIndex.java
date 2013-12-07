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
import com.sun.mail.imap.IMAPStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.mail.Folder;
import javax.mail.MessagingException;

/**
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public class StoreIndex {

//**************************************************************************************************
//  Fields
//**************************************************************************************************
    private final List<String> folders;
    private final Map<String, Set<MessageId>> folderMessages;
    private final AtomicLong indexedMessageCount;
    private final AtomicLong skippedMessageCount;
    //If no empty, the other processess shouldn't continue
    private final List<MessagingException> crawlExceptions;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    public StoreIndex() {
        this.folders = Collections.synchronizedList(new ArrayList<String>());
        this.folderMessages = Collections.synchronizedMap(new HashMap<String, Set<MessageId>>());
        this.indexedMessageCount = new AtomicLong();
        this.skippedMessageCount = new AtomicLong();
        this.crawlExceptions = Collections.synchronizedList(new ArrayList<MessagingException>());
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
    public final boolean hasCrawlException() {
        synchronized (crawlExceptions) {
            return !crawlExceptions.isEmpty();
        }
    }

    protected final void updatedIndexedMessageCount(long delta) {
        indexedMessageCount.getAndAdd(delta);
    }

    protected final void updatedSkippedMessageCount(long delta) {
        skippedMessageCount.getAndAdd(delta);
    }
//**************************************************************************************************
//  Getter/Setter Methods
//**************************************************************************************************

    public final synchronized List<String> getFolders() {
        return folders;
    }

    public final long getIndexedMessageCount() {
        return indexedMessageCount.longValue();
    }

    public final long getSkippedMessageCount() {
        return skippedMessageCount.longValue();
    }

    public synchronized Set<MessageId> getFolderMessages(String folder) {
        synchronized (folderMessages) {
            if (!folderMessages.containsKey(folder)) {
                folderMessages.put(folder, Collections.synchronizedSet(new HashSet<MessageId>()));
            }
            return folderMessages.get(folder);
        }
    }

    public final synchronized List<MessagingException> getCrawlExceptions() {
        return crawlExceptions;
    }

//**************************************************************************************************
//  Static Methods
//**************************************************************************************************
    /**
     * Static method to populate a {@link StoreIndex} with the messages in an {@link IMAPStore}
     *
     * @param index
     * @param store
     * @return
     * @throws MessagingException
     * @throws InterruptedException
     */
    public static final StoreIndex populateFromStore(final StoreIndex index, IMAPStore store,
            int threads) throws MessagingException, InterruptedException {
        MessagingException messagingException = null;
        //Crawl
        synchronized (index.getFolders()) {
            final ExecutorService service = Executors.newFixedThreadPool(threads);
            try {
                crawlFolders(store, index, store.getDefaultFolder(), service);
            } catch (MessagingException ex) {
                messagingException = ex;
            }
            service.shutdown();
            service.awaitTermination(1, TimeUnit.HOURS);
            if (index.hasCrawlException()) {
                messagingException = index.getCrawlExceptions().get(0);
            }
        }
        if (messagingException != null) {
            throw messagingException;
        }
        return index;
    }

    private static StoreIndex crawlFolders(IMAPStore store, StoreIndex storeIndex, Folder folder,
            ExecutorService service) throws MessagingException {
        if (folder != null) {
            final String folderName = folder.getFullName();
            storeIndex.getFolders().add(folderName);
            if ((folder.getType() & Folder.HOLDS_MESSAGES) == Folder.HOLDS_MESSAGES) {
                folder.open(Folder.READ_ONLY);
                folder.expunge();
                final int messageCount = folder.getMessageCount();
                folder.close(false);
                int pos = 1;
                while (pos + MNIMAPSync.BATCH_SIZE <= messageCount) {
                    service.execute(new FolderCrawler(store, folderName, pos,
                            pos + MNIMAPSync.BATCH_SIZE, storeIndex));
                    pos = pos + MNIMAPSync.BATCH_SIZE;
                }
                service.execute(new FolderCrawler(store, folderName, pos, messageCount, storeIndex));
            }
            //Folder recursion. Get all children
            if ((folder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS) {
                for (Folder child : folder.list()) {
                    crawlFolders(store, storeIndex, child, service);
                }
            }
        }
        return storeIndex;
    }

//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
}
