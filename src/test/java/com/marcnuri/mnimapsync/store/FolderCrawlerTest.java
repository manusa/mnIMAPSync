/*
 * FolderCrawlerTest.java
 *
 * Created on 2019-08-16, 7:00
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
package com.marcnuri.mnimapsync.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-16.
 */
class FolderCrawlerTest {

  private IMAPStore imapStore;
  private Folder folder;
  private StoreIndex storeIndex;

  @BeforeEach
  void setUp() throws Exception {
    imapStore = Mockito.mock(IMAPStore.class);
    folder = Mockito.mock(Folder.class);
    doReturn(folder).when(imapStore).getFolder(anyString());
    storeIndex = Mockito.spy(new StoreIndex());
  }

  @AfterEach
  void tearDown() {
    storeIndex = null;
    folder = null;
    imapStore = null;
  }

  @Test
  void run_emptyFolder_shouldOnlyUpdateIndexes() throws Exception {
    // Given
    final FolderCrawler folderCrawler = new FolderCrawler(
        imapStore, "FolderName", 0, 100, storeIndex);
    doReturn(new Message[0]).when(folder).getMessages(eq(0), eq(100));
    // When
    folderCrawler.run();
    // Then
    verify(imapStore, times(1)).getFolder(eq("FolderName"));
    verify(storeIndex, times(1)).updatedIndexedMessageCount(eq(0L));
    verify(storeIndex, times(1)).updatedSkippedMessageCount(eq(0L));
  }

  @Test
  void run_notEmptyFolderAndStoreWithExceptions_shouldReturn() throws Exception {
    // Given
    final FolderCrawler folderCrawler = new FolderCrawler(
        imapStore, "FolderName", 0, 100, storeIndex);
    final Message message = Mockito.mock(Message.class);
    doReturn(new Message[]{message}).when(folder).getMessages(eq(0), eq(100));
    doReturn(true).when(storeIndex).hasCrawlException();
    // When
    folderCrawler.run();
    // Then
    verify(imapStore, times(1)).getFolder(eq("FolderName"));
    verify(storeIndex, times(0)).updatedIndexedMessageCount(anyLong());
    verify(storeIndex, times(0)).updatedSkippedMessageCount(anyLong());
  }

  @Test
  void run_notEmptyFolderAndRepeatedMessages_shouldUpdateIndexes() throws Exception {
    // Given
    final FolderCrawler folderCrawler = new FolderCrawler(
        imapStore, "FolderName", 0, 100, storeIndex);
    final IMAPMessage message = Mockito.mock(IMAPMessage.class);
    doReturn(new String[]{"1337"}).when(message).getHeader("Message-Id");
    final IMAPMessage repeatedMessage = Mockito.mock(IMAPMessage.class);
    doReturn(new String[]{"313373"}).when(repeatedMessage).getHeader("Message-Id");
    storeIndex.getFolderMessages("FolderName").add(new MessageId(repeatedMessage));
    doReturn(new Message[]{message, repeatedMessage}).when(folder).getMessages(eq(0), eq(100));
    // When
    folderCrawler.run();
    // Then
    verify(imapStore, times(1)).getFolder(eq("FolderName"));
    assertThat(storeIndex.getIndexedMessageCount(), equalTo(1L));
    assertThat(storeIndex.getSkippedMessageCount(), equalTo(1L));
  }

  @Test
  void run_notEmptyFolderAndThrowsMessageIdExceptionWithCause_shouldUpdateIndexesAndAddCrawlException() throws Exception {
    // Given
    final FolderCrawler folderCrawler = new FolderCrawler(
        imapStore, "FolderName", 0, 100, storeIndex);
    final IMAPMessage message = Mockito.mock(IMAPMessage.class);
    doThrow(new MessagingException()).when(message).getHeader("Message-Id");
    doReturn(new Message[]{message}).when(folder).getMessages(eq(0), eq(100));
    // When
    folderCrawler.run();
    // Then
    verify(imapStore, times(1)).getFolder(eq("FolderName"));
    verify(storeIndex, times(1)).updatedIndexedMessageCount(eq(0L));
    verify(storeIndex, times(1)).updatedSkippedMessageCount(eq(0L));
    assertThat(storeIndex.getIndexedMessageCount(), equalTo(0L));
    assertThat(storeIndex.getSkippedMessageCount(), equalTo(0L));
    assertThat(storeIndex.hasCrawlException(), equalTo(true));
    assertThat(storeIndex.getCrawlExceptions(), hasSize(1));
  }

}
