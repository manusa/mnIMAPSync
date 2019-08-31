/*
 * CliSummaryReportTest.java
 *
 * Created on 2019-08-30, 14:25
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

import static com.marcnuri.mnimapsync.cli.CliSummaryReport.getSummaryReportAsText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.marcnuri.mnimapsync.MNIMAPSync;
import com.marcnuri.mnimapsync.store.StoreCopier;
import com.marcnuri.mnimapsync.store.StoreDeleter;
import org.junit.jupiter.api.Test;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-30.
 */
class CliSummaryReportTest {

  @Test
  void getSummaryReportAsText_nullStores_shouldPrintEmptyReport() throws Exception {
    // Given
    final MNIMAPSync syncInstance = mock(MNIMAPSync.class);
    // When
    final String result = getSummaryReportAsText(syncInstance);
    // Then
    assertThat(result,
        is("================================================================================\n"
            + "Sync Process Finished.\n"
            + "================================================================================\n"
            + "\n"
            + "  Folders copied:   0/0\n"
            + "  Messages copied:  0/0\n"
            + "  Speed:            0 messages/second\n"
            + "  Exceptions:       false\n"
            + "\n"
            + "  Folders deleted:  0/0\n"
            + "  Messages deleted: 0/0\n"
            + "\n"
            + "  Elapsed time:     0 seconds\n"
            + "\n"
            + "================================================================================\n"));
  }

  @Test
  void getSummaryReportAsText_validCopierAndNullDeleter_shouldPrintCopyReport() throws Exception {
    // Given
    final MNIMAPSync syncInstance = mock(MNIMAPSync.class);
    final StoreCopier storeCopier = mock(StoreCopier.class);
    doReturn(storeCopier).when(syncInstance).getSourceCopier();
    doReturn(13).when(storeCopier).getFoldersCopiedCount();
    doReturn(24).when(storeCopier).getFoldersSkippedCount();
    doReturn(1L).when(storeCopier).getMessagesCopiedCount();
    doReturn(336L).when(storeCopier).getMessagesSkippedCount();
    doReturn(2520L).when(syncInstance).getElapsedTimeInSeconds();
    // When
    final String result = getSummaryReportAsText(syncInstance);
    // Then
    assertThat(result,
        is("================================================================================\n"
            + "Sync Process Finished.\n"
            + "================================================================================\n"
            + "\n"
            + "  Folders copied:   13/37\n"
            + "  Messages copied:  1/337\n"
            + "  Speed:            0.13 messages/second\n"
            + "  Exceptions:       false\n"
            + "\n"
            + "  Folders deleted:  0/0\n"
            + "  Messages deleted: 0/0\n"
            + "\n"
            + "  Elapsed time:     2520 seconds\n"
            + "\n"
            + "================================================================================\n"));
  }

  @Test
  void getSummaryReportAsText_validCopierAndDeleter_shouldPrintCopyAndDeleteReport() throws Exception {
    // Given
    final MNIMAPSync syncInstance = mock(MNIMAPSync.class);
    final StoreCopier storeCopier = mock(StoreCopier.class);
    doReturn(storeCopier).when(syncInstance).getSourceCopier();
    doReturn(13).when(storeCopier).getFoldersCopiedCount();
    doReturn(24).when(storeCopier).getFoldersSkippedCount();
    doReturn(1L).when(storeCopier).getMessagesCopiedCount();
    doReturn(336L).when(storeCopier).getMessagesSkippedCount();
    doReturn(2520L).when(syncInstance).getElapsedTimeInSeconds();
    final StoreDeleter storeDeleter = mock(StoreDeleter.class);
    doReturn(storeDeleter).when(syncInstance).getTargetDeleter();
    doReturn(1).when(storeDeleter).getFoldersDeletedCount();
    doReturn(336).when(storeDeleter).getFoldersSkippedCount();
    doReturn(13L).when(storeDeleter).getMessagesDeletedCount();
    doReturn(24L).when(storeDeleter).getMessagesSkippedCount();
    // When
    final String result = getSummaryReportAsText(syncInstance);
    // Then
    assertThat(result,
        is("================================================================================\n"
            + "Sync Process Finished.\n"
            + "================================================================================\n"
            + "\n"
            + "  Folders copied:   13/37\n"
            + "  Messages copied:  1/337\n"
            + "  Speed:            0.13 messages/second\n"
            + "  Exceptions:       false\n"
            + "\n"
            + "  Folders deleted:  1/337\n"
            + "  Messages deleted: 13/37\n"
            + "\n"
            + "  Elapsed time:     2520 seconds\n"
            + "\n"
            + "================================================================================\n"));
  }
}
