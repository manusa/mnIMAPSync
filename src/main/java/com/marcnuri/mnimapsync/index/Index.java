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
package com.marcnuri.mnimapsync.index;

import static com.marcnuri.mnimapsync.imap.IMAPUtils.INBOX_MAILBOX;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.mail.MessagingException;

/**
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public class Index {

    private final AtomicReference<String> folderSeparator;
    private final AtomicReference<String> inbox;
    private final Set<String> folders;
    private final Map<String, Set<MessageId>> folderMessages;
    private final AtomicLong indexedMessageCount;
    private final AtomicLong skippedMessageCount;
    //If no empty, the other processes shouldn't continue
    private final Set<MessagingException> crawlExceptions;

    public Index() {
        this.folderSeparator = new AtomicReference<>();
        this.inbox = new AtomicReference<>();
        this.folders = ConcurrentHashMap.newKeySet();
        this.folderMessages = new ConcurrentHashMap<>();
        this.indexedMessageCount = new AtomicLong();
        this.skippedMessageCount = new AtomicLong();
        this.crawlExceptions = ConcurrentHashMap.newKeySet();
    }

    public final boolean hasCrawlException() {
        return !crawlExceptions.isEmpty();
    }

    public final void updatedIndexedMessageCount(long delta) {
        indexedMessageCount.getAndAdd(delta);
    }

    protected final void updatedSkippedMessageCount(long delta) {
        skippedMessageCount.getAndAdd(delta);
    }

    public String getFolderSeparator() {
        return folderSeparator.get();
    }

    public void setFolderSeparator(String folderSeparator) {
        this.folderSeparator.set(folderSeparator);
    }

    public String getInbox() {
        return inbox.get();
    }

    private void setInbox(String inbox) {
        this.inbox.set(inbox);
    }

    public final void addFolder(String folderFullName) {
        if (folderFullName.equalsIgnoreCase(INBOX_MAILBOX) && getInbox() == null) {
            setInbox(folderFullName);
        }
        folders.add(folderFullName);
    }

    public boolean containsFolder(String folder) {
        return folders.contains(folder);
    }

    public final long getIndexedMessageCount() {
        return indexedMessageCount.longValue();
    }

    public final long getSkippedMessageCount() {
        return skippedMessageCount.longValue();
    }

    public Set<MessageId> getFolderMessages(String folder) {
        return folderMessages.computeIfAbsent(folder, k -> ConcurrentHashMap.newKeySet());
    }

    final void addCrawlException(MessagingException exception) {
        crawlExceptions.add(exception);
    }

    public final Set<MessagingException> getCrawlExceptions() {
        return Collections.unmodifiableSet(crawlExceptions);
    }


}
