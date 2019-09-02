/*
 * StoreCopierTest.java
 *
 * Created on 2019-08-18, 19:08
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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import com.marcnuri.mnimapsync.index.Index;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import javax.mail.Folder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-18.
 */
class StoreCopierTest {

  private IMAPFolder imapFolder;
  private IMAPStore imapStore;
  private Index sourceIndex;
  private Index targetIndex;

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
    sourceIndex = Mockito.spy(new Index());
    sourceIndex.setFolderSeparator(".");
    targetIndex = Mockito.spy(new Index());
    targetIndex.setFolderSeparator("_");
  }

  @AfterEach
  void tearDown() {
    targetIndex = null;
    sourceIndex = null;
    imapStore = null;
    imapFolder = null;
  }

  @Test
  void copy_targetEmpty_shouldCopyFoldersAndMessages() throws Exception {
    // Given
    doReturn(true).when(imapFolder).create(eq(Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS));
    final StoreCopier storeCopier = new StoreCopier(imapStore, sourceIndex, imapStore, targetIndex, 1);
    // When
    storeCopier.copy();
    // Then
    assertThat(storeCopier.getCopyExceptions(), empty());
    assertThat(storeCopier.hasCopyException(), equalTo(false));
    assertThat(storeCopier.getFoldersCopiedCount(), equalTo(1));
    assertThat(storeCopier.getFoldersSkippedCount(), equalTo(0));
    assertThat(sourceIndex.containsFolder("INBOX"), equalTo(true));
  }

  @Test
  void copy_targetContainsCurrentFolder_shouldNotCopyFoldersAndMessages() throws Exception {
    // Given
    doReturn(true).when(imapFolder).create(eq(Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS));
    targetIndex.addFolder("INBOX");
    final StoreCopier storeCopier = new StoreCopier(imapStore, sourceIndex, imapStore, targetIndex, 1);
    // When
    storeCopier.copy();
    // Then
    assertThat(storeCopier.getCopyExceptions(), empty());
    assertThat(storeCopier.hasCopyException(), equalTo(false));
    assertThat(storeCopier.getFoldersCopiedCount(), equalTo(0));
    assertThat(storeCopier.getFoldersSkippedCount(), equalTo(1));
    assertThat(sourceIndex.containsFolder("INBOX"), equalTo(true));
  }
}
