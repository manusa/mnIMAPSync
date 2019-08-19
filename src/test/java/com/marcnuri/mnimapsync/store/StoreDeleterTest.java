/*
 * StoreDeleterTest.java
 *
 * Created on 2019-08-19, 19:19
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import javax.mail.Folder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-19.
 */
public class StoreDeleterTest {

  private IMAPFolder imapFolder;
  private IMAPStore imapStore;
  private StoreIndex sourceIndex;

  @BeforeEach
  void setUp() throws Exception {
    imapFolder = Mockito.mock(IMAPFolder.class);
    doReturn('.').doReturn('_').when(imapFolder).getSeparator();
    doReturn("INBOX").when(imapFolder).getFullName();
    doReturn(Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS).when(imapFolder).getType();
    doReturn(new Folder[0]).when(imapFolder).list();
    imapStore = Mockito.mock(IMAPStore.class);
    doReturn(imapFolder).when(imapStore).getFolder(anyString());
    doReturn(imapFolder).when(imapStore).getDefaultFolder();
    sourceIndex = Mockito.spy(new StoreIndex());
  }

  @AfterEach
  void tearDown() {
    sourceIndex = null;
    imapStore = null;
    imapFolder = null;
  }

  @Test
  void delete_sourceFolderDoesntExistAndTargetExists_shouldDeleteFoldersAndMessages() throws Exception {
    // Given
    final StoreDeleter storeDeleter = new StoreDeleter(imapStore, sourceIndex, imapStore, 1);
    // When
    storeDeleter.delete();
    // Then
    assertThat(storeDeleter.getFoldersDeletedCount(), equalTo(1));
    assertThat(storeDeleter.getFoldersSkippedCount(), equalTo(0));
    assertThat(storeDeleter.getMessagesDeletedCount(), equalTo(0L));
    assertThat(storeDeleter.getMessagesSkippedCount(), equalTo(0L));
  }

  @Test
  void delete_sourceFolderAndTargetExist_shouldNotDeleteFoldersAndMessages() throws Exception {
    // Given
    sourceIndex.getFolders().add("INBOX");
    final StoreDeleter storeDeleter = new StoreDeleter(imapStore, sourceIndex, imapStore, 1);
    // When
    storeDeleter.delete();
    // Then
    assertThat(storeDeleter.getFoldersDeletedCount(), equalTo(0));
    assertThat(storeDeleter.getFoldersSkippedCount(), equalTo(0));
    assertThat(storeDeleter.getMessagesDeletedCount(), equalTo(0L));
    assertThat(storeDeleter.getMessagesSkippedCount(), equalTo(0L));
  }
}
