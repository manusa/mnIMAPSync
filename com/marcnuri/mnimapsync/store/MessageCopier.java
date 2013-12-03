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

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
public final class MessageCopier implements Runnable {

//**************************************************************************************************
//  Fields
//**************************************************************************************************
    private final StoreCopier storeCopier;
    private final String sourceFolderName;
    private final String targetFolderName;
    private final int start;
    private final int end;
    private final Set<MessageId> targetFolderMessages;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    public MessageCopier(StoreCopier storeCopier, String sourceFolderName, String targetFolderName,
            int start, int end, Set<MessageId> targetFolderMessages) {
        this.storeCopier = storeCopier;
        this.sourceFolderName = sourceFolderName;
        this.targetFolderName = targetFolderName;
        this.start = start;
        this.end = end;
        this.targetFolderMessages = targetFolderMessages;
    }

//**************************************************************************************************
//  Abstract Methods
//**************************************************************************************************
//**************************************************************************************************
//  Overridden Methods
//**************************************************************************************************
    public void run() {
        final int updateCount = 20;
        long copied = 0l, skipped = 0l;
        try {
            final Folder sourceFolder = storeCopier.getSourceStore().getFolder(sourceFolderName);
            //Opens a new connection per Thread
            sourceFolder.open(Folder.READ_WRITE);
            final Message[] sourceMessages = sourceFolder.getMessages(start, end);
            sourceFolder.fetch(sourceMessages, MessageId.addHeaders(new FetchProfile()));
            final List<Message> toCopy = new ArrayList<Message>();
            for (Message message : sourceMessages) {
                try {
                    final MessageId id = new MessageId((IMAPMessage) message);
                    //Index message for deletion (if necessary)
                    if (storeCopier.getSourceIndex() != null) {
                        storeCopier.getSourceIndex().getFolderMessages(sourceFolderName).add(id);
                    }
                    if (!targetFolderMessages.contains(id)) {
                        ((IMAPMessage)message).setPeek(true);
                        toCopy.add(message);
                    } else {
                        skipped++;
                    }
                } catch (MessageId.MessageIdException ex) {
                    //Usually messages that ran into this exception are spammy, so we skip them.
                    skipped++;
                }
            }
            if (!toCopy.isEmpty()) {
                final FetchProfile fullProfile = MessageId.addHeaders(new FetchProfile());
                fullProfile.add(FetchProfile.Item.CONTENT_INFO);
                fullProfile.add(FetchProfile.Item.FLAGS);
                fullProfile.add(IMAPFolder.FetchProfileItem.HEADERS);
                fullProfile.add(IMAPFolder.FetchProfileItem.SIZE);
                sourceFolder.fetch(toCopy.toArray(new Message[toCopy.size()]), fullProfile);
                final Folder targetFolder = storeCopier.getTargetStore().getFolder(targetFolderName);
                targetFolder.open(Folder.READ_WRITE);
                for (Message message : toCopy) {
                    targetFolder.appendMessages(new Message[]{message});
                    try {
                        targetFolderMessages.add(new MessageId((IMAPMessage) message));
                        copied++;
                        if (copied % updateCount == 0) {
                            storeCopier.updatedMessagesCopiedCount(copied);
                            copied = 0l;
                        }
                    } catch (MessageId.MessageIdException ex) {
                        //No exception should be thrown because id was generated previously and worked
                        Logger.getLogger(StoreCopier.class.getName()).
                                log(Level.SEVERE, null, ex);
                    }
                }
                targetFolder.close(false);
            }
            sourceFolder.close(false);
        } catch (MessagingException messagingException) {
            storeCopier.getCopyExceptions().add(messagingException);
            Logger.getLogger(StoreIndex.class.getName()).log(Level.SEVERE, null,
                    messagingException);
        }
        storeCopier.updatedMessagesCopiedCount(copied);
        storeCopier.updateMessagesSkippedCount(skipped);
        if (storeCopier.getSourceIndex() != null) {
            //Quick way to update count (not precise)
            storeCopier.getSourceIndex().updatedIndexedMessageCount(copied + skipped);
        }
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
//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
}
