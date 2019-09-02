/*
 * StoreCrawler.java
 *
 * Created on 2019-09-02, 9:36
 *
 * Copyright 2019 Marc Nuri San Felix
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

import com.marcnuri.mnimapsync.MNIMAPSync;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-09-02.
 */
public class StoreCrawler {

  private StoreCrawler() {
  }

  /**
   * Static method to populate a {@link Index} with the messages in an {@link Store}
   */
  public static Index populateFromStore(final Index index, Store store, int threads)
      throws MessagingException, InterruptedException {

    MessagingException messagingException = null;
    final ExecutorService service = Executors.newFixedThreadPool(threads);
    try {
      index.setFolderSeparator(String.valueOf(store.getDefaultFolder().getSeparator()));
      crawlFolders(store, index, store.getDefaultFolder(), service);
    } catch (MessagingException ex) {
      messagingException = ex;
    }
    service.shutdown();
    service.awaitTermination(1, TimeUnit.HOURS);
    if (index.hasCrawlException()) {
      messagingException = index.getCrawlExceptions().get(0);
    }
    if (messagingException != null) {
      throw messagingException;
    }
    return index;
  }

  private static void crawlFolders(Store store, Index index, Folder folder,
      ExecutorService service) throws MessagingException {
    if (folder != null) {
      final String folderName = folder.getFullName();
      index.addFolder(folderName);
      if ((folder.getType() & Folder.HOLDS_MESSAGES) == Folder.HOLDS_MESSAGES) {
        folder.open(Folder.READ_ONLY);
        if (folder.getMode() != Folder.READ_ONLY) {
          folder.expunge();
        }
        final int messageCount = folder.getMessageCount();
        folder.close(false);
        int pos = 1;
        while (pos + MNIMAPSync.BATCH_SIZE <= messageCount) {
          service.execute(new FolderCrawler(store, folderName, pos,
              pos + MNIMAPSync.BATCH_SIZE, index));
          pos = pos + MNIMAPSync.BATCH_SIZE;
        }
        service.execute(new FolderCrawler(store, folderName, pos, messageCount, index));
      }
      //Folder recursion. Get all children
      if ((folder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS) {
        for (Folder child : folder.list()) {
          crawlFolders(store, index, child, service);
        }
      }
    }
  }
}
