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

import java.io.Serializable;

/**
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public class SyncOptions implements Serializable {

//**************************************************************************************************
//  Fields
//**************************************************************************************************
    private static final long serialVersionUID = 1L;
    private final HostDefinition host1;
    private final HostDefinition host2;
    private boolean delete;
    private int threads;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    public SyncOptions() {
        this.host1 = new HostDefinition();
        this.host2 = new HostDefinition();
        delete = false;
        threads = MNIMAPSync.THREADS;
    }

//**************************************************************************************************
//  Abstract Methods
//**************************************************************************************************
//**************************************************************************************************
//  Overridden Methods
//**************************************************************************************************
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + (this.host1 != null ? this.host1.hashCode() : 0);
        hash = 59 * hash + (this.host2 != null ? this.host2.hashCode() : 0);
        hash = 59 * hash + (this.delete ? 1 : 0);
        hash = 59 * hash + this.threads;
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
        if (this.host1 != other.host1 && (this.host1 == null || !this.host1.equals(other.host1))) {
            return false;
        }
        if (this.host2 != other.host2 && (this.host2 == null || !this.host2.equals(other.host2))) {
            return false;
        }
        if (this.delete != other.delete) {
            return false;
        }
        if (this.threads != other.threads) {
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
    public HostDefinition getHost1() {
        return host1;
    }

    public HostDefinition getHost2() {
        return host2;
    }

    public boolean getDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }
//**************************************************************************************************
//  Static Methods
//**************************************************************************************************
//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************

}
