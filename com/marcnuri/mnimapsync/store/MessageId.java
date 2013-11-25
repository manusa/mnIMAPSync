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

import com.sun.mail.imap.IMAPMessage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

/**
 * Class to create a reusable message ID for identification in maps and comparisons of source/target
 * messages.
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public class MessageId implements Serializable {

//**************************************************************************************************
//  Fields
//**************************************************************************************************
    private static final long serialVersionUID = 1L;
    public static final String HEADER_SUBJECT = "Subject";
    public static final String HEADER_MESSAGE_ID = "Message-Id";
    private final String messageId;
    private final String[] from;
    private final String[] to;
    private final String subject;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    //Method using headers Safer but slower
    public MessageId(IMAPMessage message) throws MessageIdException {
        try {
            //Must recheck all of this.
            //Should work using envelope and if envelope fails then load the rest.
            final String[] idHeader = message.getHeader(HEADER_MESSAGE_ID);
            final String[] subjectHeader = message.getHeader(HEADER_SUBJECT);
            this.messageId = idHeader != null && idHeader.length > 0
                    ? idHeader[0].trim().replaceAll("[^a-zA-Z0-9\\\\.\\\\-\\\\@]", "")
                    : "";
            this.from = parseAddress(message.getFrom());
            this.to = parseAddress(message.getRecipients(Message.RecipientType.TO));
            //Regular subject may have some problems when using non ascii characters
            //Loss of precision, but I don't think it's necessary
            this.subject = subjectHeader != null && subjectHeader.length > 0
                    ? subjectHeader[0].replaceAll("[^a-zA-Z0-9\\\\.\\\\-]", "")
                    : "";
            if (this.messageId.equals("") && subject.equals("")) {
                throw new MessageIdException("No good fields for Id", null);
            }
        } catch (MessagingException messagingException) {
            throw new MessageIdException("Messaging Exception", messagingException);
        }
    }
//    public MessageId(IMAPMessage message) throws MessageIdException {
//        try {
//            this.messageId = message.getMessageID() != null
//                    ? message.getMessageID().trim().replaceAll("[^a-zA-Z0-9\\\\.\\\\-\\\\@]", "")
//                    : "";
//            this.from = parseAddress(message.getFrom());
//            this.to = parseAddress(message.getRecipients(Message.RecipientType.TO));
//            //Regular subject may have some problems when using non ascii characters
//            //Loss of precision, but I don't think it's necessary
//            this.subject = message.getSubject() != null
//                    ? message.getSubject().replaceAll("[^a-zA-Z0-9\\\\.\\\\-]", "")
//                    : "";
//            if (this.messageId.equals("") && subject.equals("")) {
//                throw new MessageIdException("No good fields for Id", null);
//            }
//        } catch (MessagingException messagingException) {
//            throw new MessageIdException("Messaging Exception", messagingException);
//        }
//    }

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
    /**
     * Really important. Different servers return different address values when they are invalid.
     *
     * @param addresses
     * @return
     */
    private static String[] parseAddress(Address[] addresses) {
        if (addresses != null) {
            final List<String> ret = new ArrayList(addresses.length);
            for (Address address : addresses) {
                final String temp = ((InternetAddress) address).getAddress();
                if (temp.toUpperCase().matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}")) {
                    ret.add(temp);
                }
            }
            return ret.toArray(new String[ret.size()]);
        }
        return null;
    }

    /**
     * Adds required headers to fetch profile
     *
     * @param fetchProfile
     * @return
     */
    public static final FetchProfile addHeaders(FetchProfile fetchProfile) {
        fetchProfile.add(FetchProfile.Item.ENVELOPE);
        //If using the header consructor add this for performance.
        for (String header : new String[]{MessageId.HEADER_MESSAGE_ID, MessageId.HEADER_SUBJECT}) {
            fetchProfile.add(header.toUpperCase());
        }
        return fetchProfile;
    }

//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
    public static final class MessageIdException extends Exception {

        private static final long serialVersionUID = 1L;

        public MessageIdException(String message, MessagingException cause) {
            super(message, cause);
        }

        @Override
        public synchronized MessagingException getCause() {
            return (MessagingException) super.getCause();
        }

    }
}
