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

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.FetchProfile;
import javax.mail.MessagingException;

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
    public static final String HEADER_FROM = "From";
    public static final String HEADER_TO = "To";
    public static final Pattern emailPattern = Pattern.compile(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}");
    private final String messageId;
    private final String[] from;
    private final String[] to;
    private final String subject;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************
    //Method using headers Safer but slower
    /**
     * All of this process could be done just by using the ENVELOPE response from the IMAP fetch
     * command. The problem is that ENVELOPE is not consistent amongst different servers, so
     * sometimes a same e-mail will have different envelope responses in different servers, so they
     * will duplicate.
     * 
     * It's a pity because fetching all of the HEADERS is a performance HOG
     *
     * @param message
     * @throws com.marcnuri.mnimapsync.store.MessageId.MessageIdException
     */
    public MessageId(IMAPMessage message) throws MessageIdException {
        try {
            final String[] idHeader = message.getHeader(HEADER_MESSAGE_ID);
            final String[] subjectHeader = message.getHeader(HEADER_SUBJECT);
            this.messageId = idHeader != null && idHeader.length > 0
                    ? idHeader[0].trim().replaceAll("[^a-zA-Z0-9\\\\.\\\\-\\\\@]", "")
                    : "";
            //Irregular mails have more than one header for From or To fields
            //This can cause that different servers respond differently
            this.from = parseAddress(message.getHeader(HEADER_FROM));
            this.to = parseAddress(message.getHeader(HEADER_TO));
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
    private static String[] parseAddress(String[] addresses) {
        if (addresses != null) {
            final List<String> ret = new ArrayList(addresses.length);
            for (String address : addresses) {
                final Matcher matcher = emailPattern.matcher(address.toUpperCase());
                while (matcher.find()) {
                    ret.add(matcher.group());
                }
            }
            Collections.sort(ret);
            return ret.toArray(new String[ret.size()]);
        }
        return new String[0];
    }


    /**
     * Adds required headers to fetch profile
     *
     * @param fetchProfile
     * @return
     */
    public static final FetchProfile addHeaders(FetchProfile fetchProfile) {
        fetchProfile.add(FetchProfile.Item.ENVELOPE);
        //Some servers respond to get a header request with a partial response of the header
        //when hMailServer is fetched for To or From, it returns only the first entry,
        //so when compared with other server versions, e-mails appear to be different.
        fetchProfile.add(IMAPFolder.FetchProfileItem.HEADERS);
        //If using the header consructor add this for performance.
//        for (String header : new String[]{
//            MessageId.HEADER_MESSAGE_ID,
//            MessageId.HEADER_SUBJECT,
//            MessageId.HEADER_FROM,
//            MessageId.HEADER_TO}) {
//            fetchProfile.add(header.toUpperCase());
//        }
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
