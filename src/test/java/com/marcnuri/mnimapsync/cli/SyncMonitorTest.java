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
import com.marcnuri.mnimapsync.store.StoreIndex;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-31.
 */
class SyncMonitorTest {

  private ByteArrayOutputStream systemOut;

  @BeforeEach
  void setUp() {
    systemOut = spy(new ByteArrayOutputStream());
    System.setOut(new PrintStream(systemOut));
  }

  @Test
  void run_validMonitorPrint_shouldPrintToSystemOut() throws Exception {
    // Given
    final MNIMAPSync mnImapSync = mock(MNIMAPSync.class);
    doReturn(mock(StoreIndex.class)).when(mnImapSync).getTargetIndex();
    final SyncMonitor syncMonitor = new SyncMonitor(mnImapSync);
    // When
    syncMonitor.run();
    // Then
    assertThat(systemOut.toString(StandardCharsets.UTF_8.name()),
        is("\rIndexed (target): 0/0  Copied: 0/0 Deleted: 0/0 Speed: 0 m/s"));
  }

}
