package org.n52.wns.mail;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;

import org.junit.Before;
import org.junit.Test;
import org.n52.wns.WNSException;


public class MailHandlerBAW2011Test {
    
    private MailHandlerBAW2011 handler;

    @Before public void
    setUp()
    throws WNSException {
        handler = new MailHandlerBAW2011();
    }

    @Test public void
    shouldReplaceMultipleSpacesAndLineBreaksWithASingleSpace()
    throws Exception {
        String actual = "   My        String \t  \n \r to   test!  ";
        String expected = "My String to test!";
        assertThat(handler.normalizeString(actual), is(expected));
    }
    
    @Test public void
    shouldReturnUnchangedSubjectWhenNoRegelNameInMessage()
    throws MessagingException {
        Message msg = new TestableMessage();
        String presetSubject = "Subject to replace";
        msg.setSubject(presetSubject);
        String message = "   My        String \t  \n \r to   test!  ";
        handler.setSubjectWithRegelName(msg, handler.normalizeString(message));
        assertThat(msg.getSubject(), is(presetSubject));
    }
    
    @Test public void
    shouldAdjustEntrySubjectWhenRegelNameIsInMessage()
    throws MessagingException {
        Message msg = new TestableMessage();
        msg.setSubject("Subject to replace");
        String message = "Regel" +
        		" \"_NORDERNEY RIFFGAT under 600cm_\" hat einen Alarm" +
        		" ausgeloest um 13:09 Uhr am" +
        		" 28.3.2011";
        handler.setSubjectWithRegelName(msg, handler.normalizeString(message));
        assertThat(msg.getSubject(), containsString("NORDERNEY RIFFGAT under 600cm"));
    }
    
    @Test public void
    shouldAdjustExitSubjectWhenRegelNameIsInMessage()
    throws MessagingException {
        Message msg = new TestableMessage();
        msg.setSubject("Subject to replace");
        String message = "Alarmzustand fuer Regel NORDERNEY_RIFFGAT_under_600cm beendet um 3:03 Uhr am 4.3.2013";
        handler.setSubjectWithRegelName(msg, message);
        assertThat(msg.getSubject(), containsString("NORDERNEY_RIFFGAT_under_600cm"));
    }
    
    private class TestableMessage extends Message {

        private String subject;

        public int getSize() throws MessagingException {
            // TODO Auto-generated method stub
            return 0;
            
        }

        public int getLineCount() throws MessagingException {
            // TODO Auto-generated method stub
            return 0;
            
        }

        public String getContentType() throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        public boolean isMimeType(String mimeType) throws MessagingException {
            // TODO Auto-generated method stub
            return false;
            
        }

        public String getDisposition() throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        public void setDisposition(String disposition) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        public String getDescription() throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        public void setDescription(String description) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        public String getFileName() throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        public void setFileName(String filename) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        public InputStream getInputStream() throws IOException, MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        public DataHandler getDataHandler() throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        public Object getContent() throws IOException, MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        public void setDataHandler(DataHandler dh) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        public void setContent(Object obj, String type) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        public void setText(String text) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        public void setContent(Multipart mp) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        public void writeTo(OutputStream os) throws IOException, MessagingException {
            // TODO Auto-generated method stub
            
        }

        public String[] getHeader(String header_name) throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        public void setHeader(String header_name, String header_value) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        public void addHeader(String header_name, String header_value) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        public void removeHeader(String header_name) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        public Enumeration getAllHeaders() throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        public Enumeration getMatchingHeaders(String[] header_names) throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        public Enumeration getNonMatchingHeaders(String[] header_names) throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        @Override
        public Address[] getFrom() throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        @Override
        public void setFrom() throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setFrom(Address address) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void addFrom(Address[] addresses) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Address[] getRecipients(RecipientType type) throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        @Override
        public void setRecipients(RecipientType type, Address[] addresses) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void addRecipients(RecipientType type, Address[] addresses) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getSubject() throws MessagingException {
            return subject;
        }

        @Override
        public void setSubject(String subject) throws MessagingException {
            this.subject = subject;
        }

        @Override
        public Date getSentDate() throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        @Override
        public void setSentDate(Date date) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Date getReceivedDate() throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        @Override
        public Flags getFlags() throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        @Override
        public void setFlags(Flags flag, boolean set) throws MessagingException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Message reply(boolean replyToAll) throws MessagingException {
            // TODO Auto-generated method stub
            return null;
            
        }

        @Override
        public void saveChanges() throws MessagingException {
            // TODO Auto-generated method stub
            
        }
        
    }
}
