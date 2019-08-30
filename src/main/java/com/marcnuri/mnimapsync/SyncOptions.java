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
import java.util.Objects;

/**
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public class SyncOptions implements Serializable {

    private static final long serialVersionUID = -4119342475628224319L;

    private final HostDefinition sourceHost;
    private final HostDefinition targetHost;
    private boolean delete;
    private int threads;

    public SyncOptions() {
        this.sourceHost = new HostDefinition();
        this.targetHost = new HostDefinition();
        delete = false;
        threads = MNIMAPSync.THREADS;
    }

    public HostDefinition getSourceHost() {
        return sourceHost;
    }

    public HostDefinition getTargetHost() {
        return targetHost;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SyncOptions that = (SyncOptions) o;
        return delete == that.delete &&
            threads == that.threads &&
            Objects.equals(sourceHost, that.sourceHost) &&
            Objects.equals(targetHost, that.targetHost);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceHost, targetHost, delete, threads);
    }

}
