/*
 * SyncMonitorTest.java
 *
 * Created on 2019-08-31, 9:02
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.marcnuri.mnimapsync.MNIMAPSync;
import com.marcnuri.mnimapsync.index.Index;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-31.
 */
@TestMethodOrder(OrderAnnotation.class)
class SyncMonitorTest {

  private ByteArrayOutputStream outputBuffer;

  @BeforeEach
  void setUp() {
    outputBuffer = spy(new ByteArrayOutputStream());
    System.setOut(new PrintStream(outputBuffer));
    Logger.getLogger(SyncMonitor.class.getName())
        .addHandler(new StreamHandler(outputBuffer, new Formatter() {
          @Override
          public String format(LogRecord record) {
            return record.getThrown().getMessage();
          }
        }));
  }

  @Test
  @Order(2)
  void run_validMonitorPrint_shouldPrintToSystemOut() throws Exception {
    // Given
    final MNIMAPSync mnImapSync = mock(MNIMAPSync.class);
    doReturn(mock(Index.class)).when(mnImapSync).getTargetIndex();
    final SyncMonitor syncMonitor = new SyncMonitor(mnImapSync);
    // When
    syncMonitor.run();
    // Then
    assertThat(outputBuffer.toString(StandardCharsets.UTF_8.name()),
        is("\rIndexed (target): 0/0  Copied: 0/0 Deleted: 0/0 Speed: 0 m/s"));
  }

  @Test
  @Order(1)
  void run_throwsException_shouldLogException() throws Exception {
    // Given
    new MockUp<CliMonitorReport>() {
      @Mock
      @SuppressWarnings("unused")
      String getMonitorReportAsText(MNIMAPSync syncInstance) throws IOException {
        throw new IOException("Everything is fine");
      }
    };
    final SyncMonitor syncMonitor = new SyncMonitor(null);
    // When
    syncMonitor.run();
    // Then
    Arrays.stream(Logger.getLogger(SyncMonitor.class.getName()).getHandlers()).forEach(s -> s.flush());
    assertThat(outputBuffer.toString(StandardCharsets.UTF_8.name()), is("Everything is fine"));
  }

}
