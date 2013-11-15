/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.marcnuri.mnimapsync;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public class MNIMAPSync {

//**************************************************************************************************
//  Fields
//**************************************************************************************************
    private final SyncOptions syncOptions;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    public MNIMAPSync(SyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

//**************************************************************************************************
//  Abstract Methods
//**************************************************************************************************
//**************************************************************************************************
//  Overridden Methods
//**************************************************************************************************
//**************************************************************************************************
//  Other Methods
//**************************************************************************************************
//**************************************************************************************************
//  Getter/Setter Methods
//**************************************************************************************************
//**************************************************************************************************
//  Static Methods
//**************************************************************************************************
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            final MNIMAPSync sync = new MNIMAPSync(parseArgs(args, new SyncOptions(), 0));
            System.out.println("Finished");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private static SyncOptions parseArgs(String[] args, SyncOptions options, int current) {
        while (current < args.length) {
            final String arg = args[current++];
            if (arg.equals("--host1")) {
                options.setHost1(args[current++]);
            } else if (arg.equals("--port1")) {
                try {
                    options.setPort1(Integer.parseInt(args[current++]));
                } catch (NumberFormatException numberFormatException) {
                    throw new IllegalArgumentException("Port1 should be an integer");
                }
            } else if (arg.equals("--user1")) {
                options.setUser1(args[current++]);
            } else if (arg.equals("--password1")) {
                options.setPassword1(args[current++]);
            } else if (arg.equals("--ssl1")) {
                options.setSsl1(true);
            } else if (arg.equals("--host2")) {
                options.setHost2(args[current++]);
            } else if (arg.equals("--port2")) {
                try {
                    options.setPort2(Integer.parseInt(args[current++]));
                } catch (NumberFormatException numberFormatException) {
                    throw new IllegalArgumentException("Port2 should be an integer");
                }
            } else if (arg.equals("--user2")) {
                options.setUser2(args[current++]);
            } else if (arg.equals("--password2")) {
                options.setPassword2(args[current++]);
            } else if (arg.equals("--ssl2")) {
                options.setSsl2(true);
            } else {
                throw new IllegalArgumentException("Unrecognized argument: " + arg);
            }
        }
        return options;
    }

//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
    public static final class SyncOptions implements Serializable {

        private static final long serialVersionUID = 1L;
        private final UUID id;
        private String host1;
        private int port1;
        private String user1;
        private String password1;
        private boolean ssl1;
        private String host2;
        private int port2;
        private String user2;
        private String password2;
        private boolean ssl2;

        public SyncOptions() {
            this.id = UUID.randomUUID();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + (this.id != null ? this.id.hashCode() : 0);
            hash = 53 * hash + (this.host1 != null ? this.host1.hashCode() : 0);
            hash = 53 * hash + (this.host2 != null ? this.host2.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SyncOptions other = (SyncOptions) obj;
            if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
                return false;
            }
            if ((this.host1 == null) ? (other.host1 != null) : !this.host1.equals(other.host1)) {
                return false;
            }
            if ((this.host2 == null) ? (other.host2 != null) : !this.host2.equals(other.host2)) {
                return false;
            }
            return true;
        }

        public String getHost1() {
            return host1;
        }

        public void setHost1(String host1) {
            this.host1 = host1;
        }

        public int getPort1() {
            return port1;
        }

        public void setPort1(int port1) {
            this.port1 = port1;
        }

        public String getUser1() {
            return user1;
        }

        public void setUser1(String user1) {
            this.user1 = user1;
        }

        public String getPassword1() {
            return password1;
        }

        public void setPassword1(String password1) {
            this.password1 = password1;
        }

        public boolean isSsl1() {
            return ssl1;
        }

        public void setSsl1(boolean ssl1) {
            this.ssl1 = ssl1;
        }

        public String getHost2() {
            return host2;
        }

        public void setHost2(String host2) {
            this.host2 = host2;
        }

        public int getPort2() {
            return port2;
        }

        public void setPort2(int port2) {
            this.port2 = port2;
        }

        public String getUser2() {
            return user2;
        }

        public void setUser2(String user2) {
            this.user2 = user2;
        }

        public String getPassword2() {
            return password2;
        }

        public void setPassword2(String password2) {
            this.password2 = password2;
        }

        public boolean isSsl2() {
            return ssl2;
        }

        public void setSsl2(boolean ssl2) {
            this.ssl2 = ssl2;
        }

    }
}
