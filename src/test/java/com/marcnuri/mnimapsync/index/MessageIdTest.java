/*
 * MessageIdTest.java
 *
 * Created on 2019-08-18, 18:30
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;

import com.marcnuri.mnimapsync.index.MessageId.MessageIdException;
import com.sun.mail.imap.IMAPMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-18.
 */
public class MessageIdTest {

  @Test
  void constructor_noValidIdFields_shouldThrowException() {
    assertThrows(MessageIdException.class, () -> {
      // Given
      final IMAPMessage imapMessage = Mockito.mock(IMAPMessage.class);
      // When
      new MessageId(imapMessage);
      // Then
      fail();
    });
  }

  @Test
  void constructor_allValidFields_shouldReturnMessageId() throws Exception {
    // Given
    final IMAPMessage imapMessage = Mockito.mock(IMAPMessage.class);
    doReturn(new String[]{"Id!\"·$%&/-1337"}).when(imapMessage).getHeader("Message-Id");
    doReturn(new String[]{"\"Mr. Pink\" <mrpink@email.com>"}).when(imapMessage).getHeader("From");
    doReturn(new String[]{
        "\"Mr. Blonde\" <mrblonde@email.com>",
        "mrblue@email.com",
        "<mrorange@email.com>",
    }).when(imapMessage).getHeader("To");
    doReturn(new String[]{"Subje#ctNº1^*!·%"}).when(imapMessage).getHeader("Subject");
    // When
    final MessageId messageId = new MessageId(imapMessage);
    // Then
    assertThat(messageId, notNullValue());
  }

  // Checks parseAddress behavior
  @Test
  void equalTo_matchingToInDifferentOrderAndFormat_shouldBeEqual() throws Exception {
    // Given
    final IMAPMessage imapMessage = Mockito.mock(IMAPMessage.class);
    doReturn(new String[]{"SAME ID FOR BOTH"}).when(imapMessage).getHeader("Message-Id");
    doReturn(new String[]{
        "\"Mr. Blonde\" <mrblonde@email.com>",
        "mrblue@email.com",
        "<mrorange@email.com>",
    }).doReturn(new String[]{
        "mrorange@email.com",
        "\"Mr. Blue\" <mrblue@email.com>",
        "\"Mr. Blonde\" <mrblonde@email.com>",
    }).when(imapMessage).getHeader("To");
    final MessageId firstMessageId = new MessageId(imapMessage);
    final MessageId secondMessageId = new MessageId(imapMessage);
    // When
    final boolean result = firstMessageId.equals(secondMessageId);
    // Then
    assertThat(result, equalTo(true));
  }

  // Checks parseAddress behavior
  @Test
  void equalTo_nonMatchingToInDifferentOrderAndFormat_shouldNotBeEqual() throws Exception {
    // Given
    final IMAPMessage imapMessage = Mockito.mock(IMAPMessage.class);
    doReturn(new String[]{"SAME ID FOR BOTH"}).when(imapMessage).getHeader("Message-Id");
    doReturn(new String[]{
        "\"Mr. Blonde\" <mrblonde@email.com>",
        "mrblue@email.com",
        "<mrorange@email.com>",
    }).doReturn(new String[]{
        "mrorange@email.com",
        "\"Mr. Blue\" <mrblue.different@email.com>",
        "\"Mr. Blonde\" <mrblonde@email.com>",
    }).when(imapMessage).getHeader("To");
    final MessageId firstMessageId = new MessageId(imapMessage);
    final MessageId secondMessageId = new MessageId(imapMessage);
    // When
    final boolean result = firstMessageId.equals(secondMessageId);
    // Then
    assertThat(result, equalTo(false));
  }
}
