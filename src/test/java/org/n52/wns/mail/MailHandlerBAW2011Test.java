package org.n52.wns.mail;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;


public class MailHandlerBAW2011Test {

    @Test public void
    shouldReplaceMultipleSpacesAndLineBreaksWithASingleSpace()
    throws Exception {
        MailHandlerBAW2011 handler = new MailHandlerBAW2011();
        String actual = "   My        String \t  \n \r to   test!  ";
        String expected = "My String to test!";
        assertThat(handler.normalizeString(actual), is(expected));
    }
}
