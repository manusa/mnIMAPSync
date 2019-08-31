/*
 * CliMonitorReportTest.java
 *
 * Created on 2019-08-31, 8:25
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

import static com.marcnuri.mnimapsync.cli.CliMonitorReport.getMonitorReportAsText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.marcnuri.mnimapsync.MNIMAPSync;
import com.marcnuri.mnimapsync.store.StoreCopier;
import com.marcnuri.mnimapsync.store.StoreDeleter;
import com.marcnuri.mnimapsync.store.StoreIndex;
import org.junit.jupiter.api.Test;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-31.
 */
class CliMonitorReportTest {

  @Test
  void getMonitorReportAsText_nullStores_shouldPrintEmptyReport() throws Exception {
    // Given
    final MNIMAPSync syncInstance = mock(MNIMAPSync.class);
    final StoreIndex storeIndex = mock(StoreIndex.class);
    final StoreCopier storeCopier = null;
    final StoreDeleter storeDeleter = null;
    // When
    final String result = getMonitorReportAsText(syncInstance, storeIndex, storeCopier,
        storeDeleter);
    // Then
    assertThat(result, is("Indexed (target): 0/0  Copied: 0/0 Deleted: 0/0 Speed: 0 m/s"));
  }

  @Test
  void getMonitorReportAsText_validCopierAndNullDeleter_shouldPrintCopyReport() throws Exception {
    // Given
    final MNIMAPSync syncInstance = mock(MNIMAPSync.class);
    final StoreIndex storeIndex = mock(StoreIndex.class);
    doReturn(0L).when(storeIndex).getIndexedMessageCount();
    doReturn(1337L).when(storeIndex).getSkippedMessageCount();
    final StoreCopier storeCopier = mock(StoreCopier.class);
    doReturn(1L).when(storeCopier).getMessagesCopiedCount();
    doReturn(336L).when(storeCopier).getMessagesSkippedCount();
    doReturn(253L).when(syncInstance).getElapsedTimeInSeconds();
    final StoreDeleter storeDeleter = null;
    // When
    final String result = getMonitorReportAsText(syncInstance, storeIndex, storeCopier,
        storeDeleter);
    // Then
    assertThat(result, is("Indexed (target): 0/1337  Copied: 1/337 Deleted: 0/0 Speed: 1.33 m/s"));
  }

  @Test
  void getMonitorReportAsText_validCopierAndDeleter_shouldPrintCopyAndDeleteReport() throws Exception {
    // Given
    final MNIMAPSync syncInstance = mock(MNIMAPSync.class);
    final StoreIndex storeIndex = mock(StoreIndex.class);
    doReturn(0L).when(storeIndex).getIndexedMessageCount();
    doReturn(1337L).when(storeIndex).getSkippedMessageCount();
    final StoreCopier storeCopier = mock(StoreCopier.class);
    doReturn(1L).when(storeCopier).getMessagesCopiedCount();
    doReturn(336L).when(storeCopier).getMessagesSkippedCount();
    doReturn(253L).when(syncInstance).getElapsedTimeInSeconds();
    final StoreDeleter storeDeleter = mock(StoreDeleter.class);
    doReturn(13L).when(storeDeleter).getMessagesDeletedCount();
    doReturn(24L).when(storeDeleter).getMessagesSkippedCount();
    // When
    final String result = getMonitorReportAsText(syncInstance, storeIndex, storeCopier,
        storeDeleter);
    // Then
    assertThat(result, is("Indexed (target): 0/1337  Copied: 1/337 Deleted: 13/37 Speed: 1.33 m/s"));
  }

}
