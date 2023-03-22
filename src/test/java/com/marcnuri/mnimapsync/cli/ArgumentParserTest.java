/*
 * ArgumentParserTest.java
 *
 * Created on 2019-08-30, 8:13
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
package com.marcnuri.mnimapsync.cli;

import static com.marcnuri.mnimapsync.cli.ArgumentParser.parseCliArguments;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import com.marcnuri.mnimapsync.SyncOptions;
import org.junit.jupiter.api.Test;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-30.
 */
class ArgumentParserTest {

  @Test
  void parseCliArguments_invalidArgument_shouldThrowException() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      // Given
      final String[] arguments = new String[]{"I'm an Invalid argument"};
      // When
      parseCliArguments(arguments);
      // Then
      fail();
    });
    assertThat(exception.getMessage(), is("Unrecognized argument: I'm an Invalid argument"));
  }

  @Test
  void parseCliArguments_validAndInvalidArgument_shouldThrowException() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      // Given
      final String[] arguments = new String[]{"--host1", "mail.host.com", "--invalidArgument"};
      // When
      parseCliArguments(arguments);
      // Then
      fail();
    });
    assertThat(exception.getMessage(), is("Unrecognized argument: --invalidArgument"));
  }

  @Test
  void parseCliArguments_missingPort_shouldThrowException() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      // Given
      final String[] arguments = new String[]{"--port1"};
      // When
      parseCliArguments(arguments);
      // Then
      fail();
    });
    assertThat(exception.getMessage(), is("--port1 requires a valid integer as a value"));
  }

  @Test
  void parseCliArguments_invalidPort_shouldThrowException() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      // Given
      final String[] arguments = new String[]{"--port1", "notANumber"};
      // When
      parseCliArguments(arguments);
      // Then
      fail();
    });
    assertThat(exception.getMessage(), is("--port1 value should be an integer"));
  }

  @Test
  void parseCliArguments_validArgumentss_shouldReturnValidSyncOptions() throws IOException {
    // Given
    final String[] arguments = new String[]{
        "--host1", "mail.source.com",
        "--host2", "mail.target.com",
        "--port1", "1337",
        "--user1", "source-user",
        "--password1", "S3cret",
        "--ssl1",
        "--user2", "target-user",
        "--ssl2",
        "--port2", "313373",
        "--password2", "s3cr3t",
        "--threads", "9",
        "--delete"
    };
    // When
    final SyncOptions result = parseCliArguments(arguments);
    // Then
    assertThat(result.getSourceHost().getHost(), is("mail.source.com"));
    assertThat(result.getSourceHost().getPort(), is(1337));
    assertThat(result.getSourceHost().getUser(), is("source-user"));
    assertThat(result.getSourceHost().getPassword(), is("S3cret"));
    assertThat(result.getSourceHost().isSsl(), is(true));
    assertThat(result.getTargetHost().getHost(), is("mail.target.com"));
    assertThat(result.getTargetHost().getPort(), is(313373));
    assertThat(result.getTargetHost().getUser(), is("target-user"));
    assertThat(result.getTargetHost().getPassword(), is("s3cr3t"));
    assertThat(result.getTargetHost().isSsl(), is(true));
    assertThat(result.getThreads(), is(9));
    assertThat(result.getDelete(), is(true));
  }
}
