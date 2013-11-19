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
    private long messageCount;
    //If true, the other process shouldn't continue
    private final List<MessagingException> crawlExceptions;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    private StoreIndex() {
        this.folders = Collections.synchronizedList(new ArrayList<String>());
        this.folderMessages = Collections.synchronizedMap(new HashMap<String, Set<MessageId>>());
        this.messageCount = 0;
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
            return crawlExceptions.isEmpty();
        }
    }

//**************************************************************************************************
//  Getter/Setter Methods
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
    public static final StoreIndex getInstance(IMAPStore store)
            throws MessagingException, InterruptedException {
        MessagingException messagingException = null;
        final StoreIndex ret = new StoreIndex();
        //Crawl
        synchronized (ret.getFolders()) {
            final ExecutorService service = Executors.newFixedThreadPool(MNIMAPSync.THREADS);
            try {
                crawlFolders(store, ret, store.getDefaultFolder(), service);
            } catch (MessagingException ex) {
                messagingException = ex;
            }
            service.shutdown();
            service.awaitTermination(1, TimeUnit.HOURS);
            if(ret.hasCrawlException()){
                messagingException = ret.getCrawlExceptions().get(0);
            }
            try {
                crawlDebug(store.getDefaultFolder(), ret);
            } catch (MessagingException ex) {
                messagingException = ex;
            }
        }
        if (messagingException != null) {
            throw messagingException;
        }
        return ret;
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
                //Update total message count
                storeIndex.setMessageCount(storeIndex.getMessageCount() + messageCount);
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

    private static void crawlDebug(Folder folder, StoreIndex storeIndex) throws
            MessagingException {
        if ((folder.getType() & Folder.HOLDS_MESSAGES) == Folder.HOLDS_MESSAGES) {
            folder.open(Folder.READ_ONLY);
            System.out.println(folder.getFullName() + " Messages:" + folder.getMessageCount()
                    + " Crawled:" + storeIndex.getFolderMessages(folder.getFullName()).size());
            folder.close(false);
        }
        if ((folder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS) {
            for (Folder child : folder.list()) {
                crawlDebug(child, storeIndex);
            }
        }

    }

//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
}
