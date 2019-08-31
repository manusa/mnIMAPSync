/*
 * CliSummaryReport.java
 *
 * Created on 2019-08-30, 14:02
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

import com.marcnuri.mnimapsync.MNIMAPSync;
import com.marcnuri.mnimapsync.store.StoreCopier;
import com.marcnuri.mnimapsync.store.StoreDeleter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-30.
 */
public class CliSummaryReport extends CliReport {

  private static final String SUMMARY_REPORT_TEMPLATE = "/CliSummaryReport.template";

  private CliSummaryReport() {
  }

  public static String getSummaryReportAsText(MNIMAPSync syncInstance) throws IOException {

    return replaceTemplateVariables(
        loadTemplate(SUMMARY_REPORT_TEMPLATE),
        initTemplateVariables(syncInstance)
    );
  }

  private static Map<String, String> initTemplateVariables(MNIMAPSync syncInstance) {
    final StoreCopier sourceCopier = syncInstance.getSourceCopier();
    final StoreDeleter targetDeleter = syncInstance.getTargetDeleter();
    final Map<String, String> variables = new HashMap<>();
    variables.put("elapsedTimeInSeconds", String.valueOf(syncInstance.getElapsedTimeInSeconds()));
    variables.put("foldersCopiedCount", "0");
    variables.put("foldersToCopyCount", "0");
    variables.put("messagesCopiedCount", "0");
    variables.put("messagesToCopyCount", "0");
    variables.put("messagesPerSecond", "0");
    variables.put("hasCopyException", "false");
    variables.put("foldersDeletedCount", "0");
    variables.put("foldersToDeleteCount", "0");
    variables.put("messagesDeletedCount", "0");
    variables.put("messagesToDeleteCount", "0");
    if (sourceCopier != null) {
      final int foldersToCopy =
          sourceCopier.getFoldersCopiedCount() + sourceCopier.getFoldersSkippedCount();
      final long messagesToCopy =
          sourceCopier.getMessagesCopiedCount() + sourceCopier.getMessagesSkippedCount();
      final double messagesPerSecond =
          messagesToCopy / ((double) syncInstance.getElapsedTimeInSeconds());
      variables.put("foldersCopiedCount", String.valueOf(sourceCopier.getFoldersCopiedCount()));
      variables.put("foldersToCopyCount", String.valueOf(foldersToCopy));
      variables.put("messagesCopiedCount", String.valueOf(sourceCopier.getMessagesCopiedCount()));
      variables.put("messagesToCopyCount", String.valueOf(messagesToCopy));
      variables.put("messagesPerSecond", String.format(Locale.ENGLISH, "%.2f", messagesPerSecond));
      variables.put("hasCopyException", String.valueOf(sourceCopier.hasCopyException()));
    }
    if (targetDeleter != null) {
      final int foldersToDelete =
          targetDeleter.getFoldersDeletedCount() + targetDeleter.getFoldersSkippedCount();
      final long messagesToDelete =
          targetDeleter.getMessagesDeletedCount() + targetDeleter.getMessagesSkippedCount();
      variables.put("foldersDeletedCount", String.valueOf(targetDeleter.getFoldersDeletedCount()));
      variables.put("foldersToDeleteCount", String.valueOf(foldersToDelete));
      variables
          .put("messagesDeletedCount", String.valueOf(targetDeleter.getMessagesDeletedCount()));
      variables.put("messagesToDeleteCount", String.valueOf(messagesToDelete));
    }
    return variables;
  }
}
