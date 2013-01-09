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
import org.n52.wns.WNSServiceException;
import org.n52.wns.mail.MailHandler;

/**
 * Provides functionality to send SMSs to Clickatells SMS server via
 * Clickatell's SMTP-API.
 * 
 * @author Johannes Echterhoff
 */
public class SMSHandlerClickatell implements SMSHandler {

	private static Logger log = Logger.getLogger(SMSHandlerClickatell.class
			.getName());

	private String api_id;

	private String header;

	/**
	 * This adress is prescribed by Clickatell's SMTP-API and should not be
	 * changed.
	 */
	private String mailtoAdress = "sms@messaging.clickatell.com";

	/**
	 * Used for sending emails which will be converted to SMSs.
	 */
	private MailHandler mh = null;

	private String password;

	private String username;

	private String wnsURL;

	/**
	 * Creates an instance of SMSHandlerClickatell.
	 * 
	 * @throws WNSException
	 *             if an exception occurred while getting an instance of the
	 *             required MailHandler
	 */
	public SMSHandlerClickatell() throws WNSException {
		log.debug("Init SMSHandlerClickatell");
		// loads the MailHandler used to send mails to Clickatell's SMS server
		this.mh = org.n52.wns.mail.MailHandlerFactory.getInstance();
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

	/**
	 * Sends the message via the mailhandler. The message will be provided in
	 * the body of the email which is sent to the mailtoAdress. The subject is
	 * ignored.
	 * 
	 * @param message
	 *            shall be sent
	 * @throws WNSServiceException
	 *             if an exception occurred while sending the email
	 */
	private void sendMessage(String message) throws WNSException {

		this.mh.sendExternalMessage(this.mailtoAdress, "", message);
		System.out.println("Sending message '" + message + "' to '"
				+ this.mailtoAdress + "'.");
	}
	
	/**
	 * @see org.n52.wns.sms.SMSHandler#sendNotificationMessage(net.opengis.wns.x00.DoNotificationDocument, java.lang.String[], java.lang.String)
	 */
	public void sendNotificationMessage(DoNotificationDocument dnd,
			String[] targets, String messageID) throws WNSException {
		log.debug("Trying to send NotificationMessage");
		String body = this.header;

		// add destination adresses
		for (int i = 0; i < targets.length; i++) {
			String tmp = targets[i].toString();
			String to = this.normalizePhoneNumber(tmp);
			body = body + "to:" + to + "\n";
		}

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

		// add actual message
		String data = "N MsgID:" + messageID + " WNSURL: " + this.wnsURL
				+ " ServiceURL: " + service + " Msg: "
				+ dnd.getDoNotification().getShortMessage();

		body = body + "data:" + data;

		// send the message
		this.sendMessage(body);

	}

	/**
	 * Configures the handler. The Host setting in the given config must contain
	 * the api_id to use when sending SMS via Clickatell.
	 * 
	 * @see org.n52.wns.sms.SMSHandler#setProperties(org.x52North.wns.WNSConfigDocument.WNSConfig.RegisteredHandlers.SMSHandler,
	 *      java.lang.String)
	 */
	public void setProperties(
			org.x52North.wns.WNSConfigDocument.WNSConfig.RegisteredHandlers.SMSHandler shd,
			String wns) {
		log.debug("setting properties");
		this.username = shd.getUser();
		this.password = shd.getPasswd();
		this.api_id = shd.getHost();

		// create message header used by clickatell for authentication
		// the from:WNS option might not work in all cases, consult Clickatell's
		// SMTP-API for further details
		this.header = "api_id:" + this.api_id + "\n" + "user:" + this.username
				+ "\n" + "password:" + this.password + "\n" + "from:WNS\n";

		this.wnsURL = wns;
	}

}
