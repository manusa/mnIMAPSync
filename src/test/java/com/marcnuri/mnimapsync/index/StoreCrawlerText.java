/*
 * StoreCrawlerText.java
 *
 * Created on 2019-08-20, 6:46
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

import static com.marcnuri.mnimapsync.index.StoreCrawler.populateFromStore;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import javax.mail.Folder;
import javax.mail.MessagingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-20.
 */
class StoreCrawlerText {

  private IMAPFolder defaultFolder;
  private IMAPStore imapStore;

  @BeforeEach
  void setUp() throws Exception {
    defaultFolder = mockFolder("INBOX");
    doReturn(new IMAPFolder[]{
        mockFolder("Folder 1"), mockFolder("Folder 2")
    }).when(defaultFolder).list();
    imapStore = Mockito.mock(IMAPStore.class);
    doReturn(defaultFolder).when(imapStore).getDefaultFolder();
    doAnswer(invocation -> mockFolder(invocation.getArgument(0)))
        .when(imapStore).getFolder(anyString());
  }

  @AfterEach
  void tearDown() {
    imapStore = null;
    defaultFolder = null;
  }

  @Test
  void populateFromStore_storeHasFolders_shouldPopulateIndex() throws Exception {
    // Given
    final Index index = new Index();
    doReturn(1).when(defaultFolder).getMessageCount();
    // When
    populateFromStore(index, imapStore, 1);
    // Then
    verify(defaultFolder, times(1)).expunge();
    assertThat(index.containsFolder("INBOX"), equalTo(true));
    assertThat(index.containsFolder("Folder 1"), equalTo(true));
    assertThat(index.containsFolder("Folder 2"), equalTo(true));
    assertThat(index.getCrawlExceptions(), empty());
    assertThat(index.hasCrawlException(), equalTo(false));
  }

  @Test
  void populateFromStore_indexHasExceptionsAndStoreHasFolders_shouldThrowException() throws Exception {
    // Given
    final Index index = new Index();
    index.addCrawlException(new MessagingException("Indexing tasks went wrong at some point"));
    doReturn(1).when(defaultFolder).getMessageCount();
    // When
    final MessagingException result = assertThrows(MessagingException.class, () -> {
      populateFromStore(index, imapStore, 1);
      fail();
    });
    // Then
    verify(defaultFolder, times(1)).expunge();
    assertThat(index.containsFolder("INBOX"), equalTo(true));
    assertThat(index.containsFolder("Folder 1"), equalTo(true));
    assertThat(index.containsFolder("Folder 2"), equalTo(true));
    assertThat(index.hasCrawlException(), equalTo(true));
    assertThat(result.getMessage(), equalTo("Indexing tasks went wrong at some point"));
  }

  private static IMAPFolder mockFolder(String name) throws MessagingException {
    final IMAPFolder mockFolder = Mockito.mock(IMAPFolder.class);
    doReturn(name).when(mockFolder).getFullName();
    doReturn(Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS).when(mockFolder).getType();
    doReturn(Folder.READ_WRITE).when(mockFolder).getMode();
    doReturn(new IMAPFolder[0]).when(mockFolder).list();
    doReturn(new IMAPMessage[0]).when(mockFolder).getMessages(anyInt(), anyInt());
    return mockFolder;
  }
}
