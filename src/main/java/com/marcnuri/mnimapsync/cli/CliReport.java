/*
 * CliReport.java
 *
 * Created on 2019-08-30, 17:34
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-30.
 */
abstract class CliReport {

  private static final String BEGINNING_OF_INPUT = "\\A";

  CliReport() {
  }

  static String loadTemplate(String reportTemplateResourcePath) throws IOException {
    try (final InputStream is = CliSummaryReport.class
        .getResourceAsStream(reportTemplateResourcePath)) {
      return new Scanner(is, StandardCharsets.UTF_8.name())
          .useDelimiter(BEGINNING_OF_INPUT).next();
    }
  }

  static String replaceTemplateVariables(String template, Map<String, String> variables) {
    String result = template;
    for (Entry<String, String> entry : variables.entrySet()) {
      result = result.replace(String.format("${%s}", entry.getKey()), entry.getValue());
    }
    return result;
  }

}
