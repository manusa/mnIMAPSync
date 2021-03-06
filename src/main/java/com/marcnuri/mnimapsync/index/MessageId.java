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
package com.marcnuri.mnimapsync.index;

import com.sun.mail.imap.IMAPFolder;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.FetchProfile;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Class to create a reusable message ID for identification in maps and comparisons of source/target
 * messages.
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public class MessageId implements Serializable {

    private static final long serialVersionUID = 8724942298665055562L;

    private static final String HEADER_SUBJECT = "Subject";
    private static final String HEADER_MESSAGE_ID = "Message-Id";
    private static final String HEADER_FROM = "From";
    private static final String HEADER_TO = "To";
    private static final Pattern emailPattern = Pattern.compile(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}");
    private final String messageIdHeader;
    private final String[] from;
    private final String[] to;
    private final String subject;

    //Method using headers Safer but slower
    /**
     * All of this process could be done just by using the ENVELOPE response from the IMAP fetch
     * command. The problem is that ENVELOPE is not consistent amongst different servers, so
     * sometimes a same e-mail will have different envelope responses in different servers, so they
     * will duplicate.
     *
     * It's a pity because fetching all of the HEADERS is a performance HOG
     */
    public MessageId(Message message) throws MessageIdException {
        try {
            final String[] idHeader = message.getHeader(HEADER_MESSAGE_ID);
            final String[] subjectHeader = message.getHeader(HEADER_SUBJECT);
            this.messageIdHeader = idHeader != null && idHeader.length > 0
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
            if (this.messageIdHeader.equals("") && subject.equals("")) {
                throw new MessageIdException("No good fields for Id", null);
            }
        } catch (MessagingException messagingException) {
            throw new MessageIdException("Messaging Exception", messagingException);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MessageId messageId1 = (MessageId) o;
        return Objects.equals(messageIdHeader, messageId1.messageIdHeader) &&
            Arrays.equals(from, messageId1.from) &&
            Arrays.equals(to, messageId1.to) &&
            Objects.equals(subject, messageId1.subject);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(messageIdHeader, subject);
        result = 31 * result + Arrays.hashCode(from);
        result = 31 * result + Arrays.hashCode(to);
        return result;
    }

    /**
     * Really important. Different servers return different address values when they are invalid.
     */
    private static String[] parseAddress(String[] addresses) {
        if (addresses != null) {
            final List<String> ret = new ArrayList<>(addresses.length);
            for (String address : addresses) {
                final Matcher matcher = emailPattern.matcher(address.toUpperCase());
                while (matcher.find()) {
                    ret.add(matcher.group());
                }
            }
            Collections.sort(ret);
            return ret.toArray(new String[0]);
        }
        return new String[0];
    }


    /**
     * Adds required headers to fetch profile
     */
    public static FetchProfile addHeaders(FetchProfile fetchProfile) {
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

    public static final class MessageIdException extends Exception {

        MessageIdException(String message, MessagingException cause) {
            super(message, cause);
        }

        @Override
        public synchronized MessagingException getCause() {
            return (MessagingException) super.getCause();
        }

    }
}
