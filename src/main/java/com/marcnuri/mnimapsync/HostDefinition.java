/*
 * Copyright 2013 Marc Nuri San Felix
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
package com.marcnuri.mnimapsync;

/**
 * Defines the connection settings for an IMAP server account
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public class HostDefinition {

//**************************************************************************************************
//  Fields
//**************************************************************************************************
    private static final long serialVersionUID = 1L;
    private String host;
    private int port;
    private String user;
    private String password;
    private boolean ssl;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
//**************************************************************************************************
//  Abstract Methods
//**************************************************************************************************
//**************************************************************************************************
//  Overridden Methods
//**************************************************************************************************
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (this.host != null ? this.host.hashCode() : 0);
        hash = 47 * hash + this.port;
        hash = 47 * hash + (this.user != null ? this.user.hashCode() : 0);
        hash = 47 * hash + (this.password != null ? this.password.hashCode() : 0);
        hash = 47 * hash + (this.ssl ? 1 : 0);
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
        final HostDefinition other = (HostDefinition) obj;
        if ((this.host == null) ? (other.host != null) : !this.host.equals(other.host)) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        if ((this.user == null) ? (other.user != null) : !this.user.equals(other.user)) {
            return false;
        }
        if ((this.password == null) ? (other.password != null) : !this.password.equals(
                other.password)) {
            return false;
        }
        if (this.ssl != other.ssl) {
            return false;
        }
        return true;
    }

//**************************************************************************************************
//  Other Methods
//**************************************************************************************************
//**************************************************************************************************
//  Getter/Setter Methods
//**************************************************************************************************
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

//**************************************************************************************************
//  Static Methods
//**************************************************************************************************
//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
}
