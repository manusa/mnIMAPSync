/*
 * CliMonitorReport.java
 *
 * Created on 2019-08-30, 17:31
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
import com.marcnuri.mnimapsync.store.StoreIndex;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-30.
 */
public class CliMonitorReport extends CliReport {

  private static final String MONITOR_REPORT_TEMPLATE = "/CliMonitorReport.template";

  private CliMonitorReport() {
  }

  public static String getMonitorReportAsText(MNIMAPSync syncInstance, StoreIndex targetIndex,
      StoreCopier sourceCopier, StoreDeleter targetDeleter) throws IOException {

    return replaceTemplateVariables(
        loadTemplate(MONITOR_REPORT_TEMPLATE).replaceAll("[\r\n]", ""),
        initTemplateVariables(syncInstance, targetIndex, sourceCopier, targetDeleter)
    );
  }


  private static Map<String, String> initTemplateVariables(MNIMAPSync syncInstance,
      StoreIndex targetIndex, StoreCopier sourceCopier, StoreDeleter targetDeleter) {

    final Map<String, String> variables = new HashMap<>();
    final long indexedMessageTotalCount =
        targetIndex.getIndexedMessageCount() + targetIndex.getSkippedMessageCount();
    variables.put("indexedMessageCount", String.valueOf(targetIndex.getIndexedMessageCount()));
    variables.put("indexedMessageTotalCount", String.valueOf(indexedMessageTotalCount));
    variables.put("messagesCopiedCount", "0");
    variables.put("messagesToCopyCount", "0");
    variables.put("messagesPerSecond", "0");
    variables.put("messagesDeletedCount", "0");
    variables.put("messagesToDeleteCount", "0");
    if (sourceCopier != null) {
      final long messagesToCopy =
          sourceCopier.getMessagesCopiedCount() + sourceCopier.getMessagesSkippedCount();
      final double messagesPerSecond =
          messagesToCopy / ((double) syncInstance.getElapsedTimeInSeconds());
      variables.put("messagesCopiedCount", String.valueOf(sourceCopier.getMessagesCopiedCount()));
      variables.put("messagesToCopyCount", String.valueOf(messagesToCopy));
      variables.put("messagesPerSecond", String.format(Locale.ENGLISH, "%.2f", messagesPerSecond));
    }
    if (targetDeleter != null) {
      final long messagesToDelete =
          targetDeleter.getMessagesDeletedCount() + targetDeleter.getMessagesSkippedCount();
      variables
          .put("messagesDeletedCount", String.valueOf(targetDeleter.getMessagesDeletedCount()));
      variables.put("messagesToDeleteCount", String.valueOf(messagesToDelete));
    }
    return variables;
  }
}
