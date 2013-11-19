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
package com.marcnuri.mnimapsync.store;

import java.io.Serializable;
import java.util.Arrays;
import javax.mail.Address;

/**
 * Class to create a reusable message ID for maps and comparisons
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public class MessageId implements Serializable {

//**************************************************************************************************
//  Fields
//**************************************************************************************************
    private static final long serialVersionUID = 1L;
    private final String messageId;
    private final Address[] from;
    private final Address[] to;
    private final String subject;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    public MessageId(String messageId, Address[] from, Address[] to, String subject) {
        this.messageId = messageId.trim();
        this.from = from;
        this.to = to;
        this.subject = subject.replaceAll("[^a-zA-Z1-9\\\\.\\\\-]", "");
    }

//**************************************************************************************************
//  Abstract Methods
//**************************************************************************************************
//**************************************************************************************************
//  Overridden Methods
//**************************************************************************************************
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.messageId != null ? this.messageId.hashCode() : 0);
        hash = 97 * hash + Arrays.deepHashCode(this.from);
        hash = 97 * hash + Arrays.deepHashCode(this.to);
        hash = 97 * hash + (this.subject != null ? this.subject.hashCode() : 0);
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
        final MessageId other = (MessageId) obj;
        if ((this.messageId == null) ? (other.messageId != null) : !this.messageId.equals(
                other.messageId)) {
            return false;
        }
        if (!Arrays.deepEquals(this.from, other.from)) {
            return false;
        }
        if (!Arrays.deepEquals(this.to, other.to)) {
            return false;
        }
        if ((this.subject == null) ? (other.subject != null) : !this.subject.equals(other.subject)) {
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
//**************************************************************************************************
//  Static Methods
//**************************************************************************************************
//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
}
