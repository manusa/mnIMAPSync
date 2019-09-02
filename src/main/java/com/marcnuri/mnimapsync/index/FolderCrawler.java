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

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

/**
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public final class FolderCrawler implements Runnable {

    private final Store store;
    private final String folderName;
    private final int start;
    private final int end;
    private final Index index;

    protected FolderCrawler(Store store, String folderName, int start, int end,
            Index index) {
        this.store = store;
        this.folderName = folderName;
        this.start = start;
        this.end = end;
        this.index = index;
    }

    public void run() {
        long indexedMessages = 0L;
        long skippedMessages = 0L;
        try {
            final Folder folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            final Message[] messages = folder.getMessages(start, end);
            folder.fetch(messages, MessageId.addHeaders(new FetchProfile()));
            for (Message message : messages) {
                //Don't bother crawling if index has exceptions. Process won't continue
                if (index.hasCrawlException()) {
                    return;
                }
                try {
                    final MessageId messageId = new MessageId(message);
                    if (index.getFolderMessages(folderName).add(messageId)) {
                        indexedMessages++;
                    } else {
                        skippedMessages++;
                    }
                } catch (MessageId.MessageIdException ex) {
                    if (ex.getCause() != null) {
                        throw new MessagingException();
                    }
                    skippedMessages++;
                }
            }
            folder.close(false);
        } catch (MessagingException messagingException) {
            index.getCrawlExceptions().add(messagingException);
        }
        index.updatedIndexedMessageCount(indexedMessages);
        index.updatedSkippedMessageCount(skippedMessages);
    }
}
