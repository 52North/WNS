/***************************************************************
 /"A service by which a client may conduct asynchronous dialogues 
 (message interchanges) with one or more other services. This 
 service is useful when many collaborating services are required 
 to satisfy a client request, and/or when significant delays are 
 involved is satisfying the request. This service was defined 
 under OWS 1.2 in support of SPS operations. WNS has broad 
 applicability in many such multi-service applications.
 
 Copyright (C) 2007 by 52°North Initiative for Geospatial 
 Open Source Software GmbH

 Author: Dennis Dahlmann, University of Muenster

 Contact: Andreas Wytzisk, 52°North Initiative for Geospatial 
 Open Source Software GmbH,  Martin-Luther-King-Weg 24,
 48155 Muenster, Germany, info@52north.org

 This program is free software; you can redistribute and/or  
 modify it under the terms of the GNU General Public License 
 version 2 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,  
 but WITHOUT ANY WARRANTY; without even the implied warranty of  
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the  
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License 
 along with this program (see gnu-gpl v2.txt); if not, write to  the 
 Free Software Foundation, Inc., 59 Temple Place - Suite 330,  
 Boston, MA 02111-1307, USA or visit the Free Software Foundation's  
 web page, http://www.fsf.org.

 Created on: 2006-07-28

 //Last changes on: 2007-03-15
 //Last changes by: Dennis Dahlmann

 ***************************************************************/

package org.n52.wns.sms;

import java.util.ArrayList;

import net.opengis.wns.x00.CommunicationMessageDocument;
import net.opengis.wns.x00.DoNotificationDocument;
import net.opengis.wns.x00.NotificationMessageDocument;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.n52.wns.WNSException;
import org.n52.wns.mail.MailHandler;

/**
 * For email-2-SMS, send an email to [cellnumber]@sms.vine.co.za e.g.
 * 27821231234@sms.vine.co.za. The subject line of the email must be your
 * authentication code and the body of the email your SMS message.
 * 
 * @author Johannes Echterhoff
 * 
 */
public class SMSHandlerGrapevine implements SMSHandler {

	private static Logger log = Logger.getLogger(SMSHandlerGrapevine.class
			.getName());

	private MailHandler mh = null;

	/**
	 * The host part of the email address (most probably sms.vine.co.za).
	 */
	private String mailtoAdress;

	/**
	 * The authentication code which will be incorporated in the subject of an
	 * email.
	 */
	private String password;

	private String wnsURL;

	/**
	 * SMSHandlerGrapevine will get the systems default mail handler required
	 * for sending mails.
	 * 
	 * @throws WNSException
	 */
	public SMSHandlerGrapevine() throws WNSException {
		log.debug("Init SMSHandlerGrapevine");
		// loads the MailHandler used to send mails to grapevine's SMS server
		this.mh = org.n52.wns.mail.MailHandlerFactory.getInstance();
	}

	/**
	 * @see org.n52.wns.sms.SMSHandler#setProperties(org.x52North.wns.WNSConfigDocument.WNSConfig.RegisteredHandlers.SMSHandler,
	 *      java.lang.String)
	 */
	public void setProperties(
			org.x52North.wns.WNSConfigDocument.WNSConfig.RegisteredHandlers.SMSHandler shd,
			String wns) {
		log.debug("setting properties");
		this.mailtoAdress = shd.getHost();
		this.password = shd.getPasswd();
		this.wnsURL = wns;
		// we do not need user information right now, the authentication code in
		// the password is enough
	}

	public void sendNotificationMessage(DoNotificationDocument dnd,
			String[] targets, String messageID) throws WNSException {
		log.debug("Trying to send NotificationMessage");

		String service = "";
		// handle request
		ArrayList validationErrors = new ArrayList();
		XmlOptions validationOptions = new XmlOptions();
		validationOptions.setErrorListener(validationErrors);

		// parse request and handle accordingly
		XmlObject xobj = null;
		ArrayList parsingErrors = new ArrayList();
		XmlOptions parsingOptions = new XmlOptions();
		parsingOptions.setErrorListener(parsingErrors);

		try {

			xobj = XmlObject.Factory.parse(dnd.getDoNotification().getMessage()
					.toString(), parsingOptions);

		} catch (XmlException e) {
			// TODO Handle Exception
		}
		try {

			SchemaType type = xobj.schemaType();

			if (type == NotificationMessageDocument.type) {
				NotificationMessageDocument notdoc = NotificationMessageDocument.Factory
						.parse(dnd.getDoNotification().getMessage().toString());

				service = notdoc.getNotificationMessage()
						.getServiceDescription().getServiceURL();
			}
			if (type == CommunicationMessageDocument.type) {
				CommunicationMessageDocument notdoc = CommunicationMessageDocument.Factory
						.parse(dnd.getDoNotification().getMessage().toString());

				service = notdoc.getCommunicationMessage()
						.getServiceDescription().getServiceURL();
			}
		} catch (XmlException e) {
			log.fatal("Error while sending Message via Phonehandler: "
					+ e.toString());
			throw new WNSException(
					"Error while sending Message via Phonehandler: "
							+ e.toString());
		}

		String message = "N MsgID:" + messageID + " WNSURL: " + this.wnsURL
				+ " ServiceURL: " + service + " Msg: "
				+ dnd.getDoNotification().getShortMessage();

		String[] mailtargets = new String[targets.length];

		for (int i = 0; i <= targets.length - 1; i++) {
			String msisdn = targets[i].toString();
			// verify format and set subject
			String to = this.normalizePhoneNumber(msisdn) + "@"
					+ this.mailtoAdress;
			mailtargets[i] = to;
		}

		this.mh.sendExternalMessage(mailtargets, this.password, message);
		System.out.println("SMS sending successful");
	}

	/**
	 * Wipes out all illegal characters so that in the end the number consists
	 * only of pure digits without leading zeros.
	 * 
	 * @param phoneNumber
	 * @return the normalized phone number
	 */
	public String normalizePhoneNumber(String phoneNumber) {

		// trim number
		String number = phoneNumber.trim();

		// remove all illegal characters
		StringBuffer buffer = new StringBuffer(number);
		int i = -1;

		while ((i = buffer.indexOf("(0)")) != -1) {
			buffer.deleteCharAt(i);
			buffer.deleteCharAt(i);
			buffer.deleteCharAt(i);
		}
		while ((i = buffer.indexOf("(")) != -1) {
			buffer.deleteCharAt(i);
		}
		while ((i = buffer.indexOf(")")) != -1) {
			buffer.deleteCharAt(i);
		}
		while ((i = buffer.indexOf("-")) != -1) {
			buffer.deleteCharAt(i);
		}
		while ((i = buffer.indexOf("/")) != -1) {
			buffer = buffer.deleteCharAt(i);
		}
		while ((i = buffer.indexOf(" ")) != -1) {
			buffer = buffer.deleteCharAt(i);
		}
		while ((i = buffer.indexOf(".")) != -1) {
			buffer = buffer.deleteCharAt(i);
		}

		while ((i = buffer.indexOf("+")) != -1) {
			buffer = buffer.deleteCharAt(i);
		}

		// delete leading zeros
		while (buffer.charAt(0) == '0') {
			buffer.deleteCharAt(0);
		}

		return buffer.toString();
	}
}
