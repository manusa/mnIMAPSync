/*
 * IMAPUtilsTest.java
 *
 * Created on 2019-08-31, 12:21
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
package com.marcnuri.mnimapsync.imap;

import static com.marcnuri.mnimapsync.imap.IMAPUtils.openStore;
import static com.marcnuri.mnimapsync.imap.IMAPUtils.sourceFolderNameToTarget;
import static com.marcnuri.mnimapsync.imap.IMAPUtils.targetToSourceFolderName;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.marcnuri.mnimapsync.HostDefinition;
import com.marcnuri.mnimapsync.index.Index;
import com.sun.mail.imap.IMAPSSLStore;
import com.sun.mail.imap.IMAPStore;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Session;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-31.
 */
class IMAPUtilsTest {

  private Session session;
  private Index sourceIndex;
  private Index targetIndex;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    session = mock(Session.class);
    new MockUp<Session>() {
      @Mock
      Session getInstance(Properties props, Authenticator authenticator) {
        return session;
      }
    };
    sourceIndex = mock(Index.class);
    doReturn(".").when(sourceIndex).getFolderSeparator();
    doReturn("InBox").when(sourceIndex).getInbox();
    targetIndex = mock(Index.class);
    doReturn("|").when(targetIndex).getFolderSeparator();
    doReturn("inbox").when(targetIndex).getInbox();
  }

  @AfterEach
  void tearDown() {
    session = null;
    sourceIndex = null;
    targetIndex = null;
  }

  @Test
  void openStore_nonSSLConnection_shouldReturnIMAPStore() throws Exception {
    // Given
    final IMAPStore mockedStore = mock(IMAPStore.class);
    doReturn(mockedStore).when(session).getStore(eq("imap"));
    final HostDefinition hostDefinition = new HostDefinition();
    hostDefinition.setHost("mail.host");
    hostDefinition.setPort(1337);
    hostDefinition.setUser("the-user");
    hostDefinition.setPassword("the-pw");
    // When
    final IMAPStore store = openStore(hostDefinition, 1);
    // Then
    assertThat(store, equalTo(mockedStore));
    verify(store, times(1))
        .connect(eq("mail.host"), eq(1337), eq("the-user"), eq("the-pw"));
  }

  @Test
  void openStore_SSLConnection_shouldReturnIMAPSSLStore() throws Exception {
    // Given
    final IMAPSSLStore mockedStore = mock(IMAPSSLStore.class);
    doReturn(mockedStore).when(session).getStore(eq("imaps"));
    final HostDefinition hostDefinition = new HostDefinition();
    hostDefinition.setHost("mail.host");
    hostDefinition.setPort(1337);
    hostDefinition.setUser("the-user");
    hostDefinition.setPassword("the-pw");
    hostDefinition.setSsl(true);
    // When
    final IMAPStore store = openStore(hostDefinition, 1);
    // Then
    assertThat(store, equalTo(mockedStore));
    verify(store, times(1))
        .connect(eq("mail.host"), eq(1337), eq("the-user"), eq("the-pw"));
  }

  @Test
  void sourceFolderNameToTarget_sourceIsInbox_shouldReturnTargetInboxName() {
    // Given
    final String sourceFolderFullName = "InBox";
    // When
    final String result = sourceFolderNameToTarget(sourceFolderFullName, sourceIndex, targetIndex);
    // Then
    assertThat(result, equalTo("inbox"));
  }

  @Test
  void sourceFolderNameToTarget_sourceIsNotInbox_shouldReturnTranslatedTargetFolderName() {
    // Given
    final String sourceFolderFullName = "Folder.With.Separator";
    // When
    final String result = sourceFolderNameToTarget(sourceFolderFullName, sourceIndex, targetIndex);
    // Then
    assertThat(result, equalTo("Folder|With|Separator"));
  }

  @Test
  void targetToSourceFolderName_targetIsInbox_shouldReturnSourceInboxName() {
    // Given
    final String targetFolderFullName = "inbox";
    // When
    final String result = targetToSourceFolderName(targetFolderFullName, sourceIndex, targetIndex);
    // Then
    assertThat(result, equalTo("InBox"));
  }

  @Test
  void targetToSourceFolderName_targetIsNotInbox_shouldReturnTranslatedSourceFolderName() {
    // Given
    final String sourceFolderFullName = "Folder|With|Separator";
    // When
    final String result = targetToSourceFolderName(sourceFolderFullName, sourceIndex, targetIndex);
    // Then
    assertThat(result, equalTo("Folder.With.Separator"));
  }
}
