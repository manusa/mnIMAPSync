/*
 * SyncMonitor.java
 *
 * Created on 2019-08-31, 8:53
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

import com.marcnuri.mnimapsync.MNIMAPSync;
import java.io.IOException;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-31.
 */
public class SyncMonitor extends TimerTask {

  private static final Logger logger = Logger.getLogger(SyncMonitor.class.getName());

  private final MNIMAPSync syncInstance;

  public SyncMonitor(MNIMAPSync syncInstance) {
    this.syncInstance = syncInstance;
  }

  @Override
  public void run() {
    try {
      //noinspection UseOfSystemOutOrSystemErr
      System.out.print(String.format("\r%s", getMonitorReportAsText(syncInstance)));
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "", ex);
    }
  }
}
