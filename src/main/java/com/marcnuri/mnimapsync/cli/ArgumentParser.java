/*
 * ArgumentParser.java
 *
 * Created on 2019-08-30, 8:08
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

import com.marcnuri.mnimapsync.SyncOptions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-08-30.
 */
public class ArgumentParser {
	
	  private static final Logger logger = Logger.getLogger(ArgumentParser.class.getName());
  private ArgumentParser() {
  }
  

  public static SyncOptions parseCliArguments(String[] arguments) throws IOException {
    final SyncOptions result = new SyncOptions();

    //Check if it might parse a properties file provided
    if(!arguments[0].startsWith("--")) {
    	Properties props = new Properties();
      FileInputStream fileInputStream = new FileInputStream(arguments[0]);
      props.load(fileInputStream);

      result.getSourceHost().setHost(props.getProperty("host1"));
      result.getSourceHost().setPort(Integer.parseInt(props.getProperty("port1")));
      result.getSourceHost().setUser(props.getProperty("user1"));
      result.getSourceHost().setPassword(props.getProperty("password1"));
      result.getSourceHost().setSsl(Boolean.parseBoolean(props.getProperty("ssl1")));
                
      result.getTargetHost().setHost(props.getProperty("host2"));
      result.getTargetHost().setPort(Integer.parseInt(props.getProperty("port2")));
      result.getTargetHost().setUser(props.getProperty("user2"));
      result.getTargetHost().setPassword(props.getProperty("password2"));
      result.getTargetHost().setSsl(Boolean.parseBoolean(props.getProperty("ssl2"))); 
      result.setDelete(true);
      //TODO : handle as well the possibility to specify the number of threads to launch
      //result.setThreads(Integer.parseInt(props.getProperty("threads")));

    }
    else {
        final Queue<String> argumentQueue = new LinkedList<>(Arrays.asList(arguments));
        
        String currentArgument;
        
        while ((currentArgument = argumentQueue.peek()) != null) {
          parseArgument("--host1", argumentQueue,
              key -> result.getSourceHost().setHost(argumentQueue.poll()));
          parseArgument("--port1", argumentQueue,
              key -> result.getSourceHost().setPort(parseIntValue(key, argumentQueue.poll())));
          parseArgument("--user1", argumentQueue,
              key -> result.getSourceHost().setUser(argumentQueue.poll()));
          parseArgument("--password1", argumentQueue,
              key -> result.getSourceHost().setPassword(argumentQueue.poll()));
          parseArgument("--ssl1", argumentQueue, key -> result.getSourceHost().setSsl(true));
          parseArgument("--host2", argumentQueue,
              key -> result.getTargetHost().setHost(argumentQueue.poll()));
          parseArgument("--port2", argumentQueue,
              key -> result.getTargetHost().setPort(parseIntValue(key, argumentQueue.poll())));
          parseArgument("--user2", argumentQueue,
              key -> result.getTargetHost().setUser(argumentQueue.poll()));
          parseArgument("--password2", argumentQueue,
              key -> result.getTargetHost().setPassword(argumentQueue.poll()));
          parseArgument("--ssl2", argumentQueue, key -> result.getTargetHost().setSsl(true));
          parseArgument("--delete", argumentQueue, key -> result.setDelete(true));
          parseArgument("--threads", argumentQueue,
              key -> result.setThreads(parseIntValue(key, argumentQueue.poll())));
          if (currentArgument.equals(argumentQueue.peek())) {
            throw new IllegalArgumentException(
                String.format("Unrecognized argument: %s", currentArgument));
          }
        }
    }
    return result;
  }

  private static void parseArgument(String expectedKey, Queue<String> arguments,
      ParserAction parserAction) {
    if (expectedKey.equals(arguments.peek())) {
      parserAction.action(arguments.poll());
    }
  }

  private static int parseIntValue(String key, String intValue) {
    try {
      return Integer.parseInt(Optional.ofNullable(intValue)
          .orElseThrow(() -> new IllegalArgumentException(
              String.format("%s requires a valid integer as a value", key))));
    } catch (NumberFormatException numberFormatException) {
      throw new IllegalArgumentException(String.format("%s value should be an integer", key));
    }
  }

  @FunctionalInterface
  private interface ParserAction {

    void action(String key);
  }
}
