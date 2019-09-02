/*
 * IMAPUtils.java
 *
 * Created on 2019-08-31, 9:33
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

import com.marcnuri.mnimapsync.HostDefinition;
import com.marcnuri.mnimapsync.index.Index;
import com.sun.mail.imap.IMAPSSLStore;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.util.MailSSLSocketFactory;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-31.
 */
public class IMAPUtils {

  public static final String INBOX_MAILBOX = "INBOX";

  private static MailSSLSocketFactory mailSSLSocketFactory;

  private static MailSSLSocketFactory getSSLSocketFactory() throws GeneralSecurityException {
    if (mailSSLSocketFactory == null) {
      mailSSLSocketFactory = new MailSSLSocketFactory();
      mailSSLSocketFactory.setTrustAllHosts(true);
    }
    return mailSSLSocketFactory;
  }

  private IMAPUtils() {
  }

  /**
   * Open an {@link IMAPStore} for the provided {@link HostDefinition}
   *
   * @param hostDefinition for the IMAPStore connection
   * @param threads that will be consuming the IMAPStore connection
   * @return the open IMAPStore
   */
  public static IMAPStore openStore(HostDefinition hostDefinition, int threads)
      throws MessagingException, GeneralSecurityException {
    final Properties properties = new Properties();
    properties.put("mail.debug", "false");
    properties.put("mail.imap.starttls.enable", true);
    properties.setProperty("mail.imap.connectionpoolsize", String.valueOf(threads));
    if (hostDefinition.isSsl()) {
      properties.put("mail.imap.ssl.enable", hostDefinition.isSsl());
      properties.setProperty("mail.imaps.connectionpoolsize", String.valueOf(threads));
      properties.put("mail.imaps.socketFactory.port", hostDefinition.getPort());
      properties.put("mail.imap.ssl.socketFactory", getSSLSocketFactory());
      properties.put("mail.imap.ssl.socketFactory.fallback", false);
    }
    final Session session = Session.getInstance(properties, null);
    final IMAPStore ret;
    if (hostDefinition.isSsl()) {
      ret = (IMAPSSLStore) session.getStore("imaps");
    } else {
      ret = (IMAPStore) session.getStore("imap");
    }
    ret.connect(hostDefinition.getHost(), hostDefinition.getPort(), hostDefinition.getUser(),
        hostDefinition.getPassword());
    return ret;
  }

  private static Optional<String> translateInbox(String folderName, String inboxName) {
    if (INBOX_MAILBOX.equalsIgnoreCase(folderName)) {
      return Optional.ofNullable(inboxName);
    }
    return Optional.empty();
  }

  private static String translateFolder(String folderName, Index sourceIndex, Index targetIndex) {
    return folderName.replace(sourceIndex.getFolderSeparator(), targetIndex.getFolderSeparator());
  }

  public static String sourceFolderNameToTarget(String sourceFolderFullName,
      Index sourceIndex, Index targetIndex) {

    return translateInbox(sourceFolderFullName, targetIndex.getInbox())
        .orElse(translateFolder(sourceFolderFullName, sourceIndex, targetIndex));
  }
  public static String targetToSourceFolderName(String targetFolderFullName,
      Index sourceIndex, Index targetIndex) {

    return translateInbox(targetFolderFullName, sourceIndex.getInbox())
        .orElse(translateFolder(targetFolderFullName, targetIndex, sourceIndex));
  }
}
