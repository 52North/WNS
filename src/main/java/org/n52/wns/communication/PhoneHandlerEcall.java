/***************************************************************
 /"A service by which a client may conduct asynchronous dialogues 
 (message interchanges) with one or more other services. This 
 service is useful when many collaborating services are required 
 to satisfy a client request, and/or when significant delays are 
 involved is satisfying the request. This service was defined 
 under OWS 1.2 in support of SPS operations. WNS has broad 
 applicability in many such multi-service applications.
 
 Copyright (C) 2007 by 52�North Initiative for Geospatial 
 Open Source Software GmbH

 Author: Dennis Dahlmann, University of Muenster

 Contact: Andreas Wytzisk, 52�North Initiative for Geospatial 
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

package org.n52.wns.communication;

import java.util.ArrayList;

import net.opengis.wns.x00.CommunicationMessageDocument;
import net.opengis.wns.x00.DoNotificationDocument;
import net.opengis.wns.x00.NotificationMessageDocument;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.n52.wns.WNSException;
import org.n52.wns.mail.IMailHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.x52North.wns.v2.WNSConfigDocument;

/**
 * Class to send messages via ecall.ch phone service
 * 
 * @author Dennis Dahlmann
 * @version 2.0
 */
public class PhoneHandlerEcall implements PhoneHandler {

	private Logger log = LoggerFactory.getLogger(PhoneHandlerEcall.class);

	// Ecall's pop server
	private String host;

	private IMailHandler mh;

	private String wns;

	/**
	 * constructor PhoneHandlerEcall loads the MailHandler
	 * 
	 * @throws WNSException
	 */
	public PhoneHandlerEcall() throws WNSException {
		this.log.debug("Init PhoneHandlerEcall");
		this.mh = org.n52.wns.mail.MailHandlerFactory.getInstance();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.n52.communication.PhoneHandler#setProperties(org.jdom.Element)
	 */
	public void setProperties(
			WNSConfigDocument.WNSConfig.RegisteredHandlers.PhoneHandler phoneHandler,
			String wns) {
		this.log.debug("setting up properties");
		this.host = phoneHandler.getHost();
		this.wns = wns;

	}

	/**
	 * @see org.n52.wns.communication.PhoneHandler
	 */
	public void sendNotificationMessage(DoNotificationDocument dnd,
			String[] targets, String messageID) throws WNSException {
		this.log.debug("Trying to send NotificationMessage");
		String subject;
		String body;
		for (int i = 0; i <= targets.length - 1; i++) {
			String msisdn = targets[i].toString();

			// verify format
			msisdn = PhoneNumber.convertToInternationalFormat(msisdn);

			// concat recipient <number>@host

			String to = msisdn + "@" + this.host;

			subject = ("Web Notification Service");

			// parse request and handle accordingly
			XmlObject xobj = null;
			ArrayList parsingErrors = new ArrayList();
			XmlOptions parsingOptions = new XmlOptions();
			parsingOptions.setErrorListener(parsingErrors);

			try {

				xobj = XmlObject.Factory.parse(dnd.getDoNotification()
						.getMessage().toString(), parsingOptions);

			} catch (XmlException e) {
				this.log.error(e.toString());
				throw new WNSException(e.toString());
			}

			String service = "";
			// handle request
			ArrayList validationErrors = new ArrayList();
			XmlOptions validationOptions = new XmlOptions();
			validationOptions.setErrorListener(validationErrors);

			try {

				SchemaType type = xobj.schemaType();

				if (type == NotificationMessageDocument.type) {
					NotificationMessageDocument notdoc = NotificationMessageDocument.Factory
							.parse(dnd.getDoNotification().getMessage()
									.toString());

					service = notdoc.getNotificationMessage()
							.getServiceDescription().getServiceURL();
				}
				if (type == CommunicationMessageDocument.type) {
					CommunicationMessageDocument notdoc = CommunicationMessageDocument.Factory
							.parse(dnd.getDoNotification().getMessage()
									.toString());

					service = notdoc.getCommunicationMessage()
							.getServiceDescription().getServiceURL();
				}
			} catch (XmlException e) {
				this.log.error("Error while sending Message via Phonehandler: "
						+ e.toString());
				throw new WNSException(
						"Error while sending Message via Phonehandler: "
								+ e.toString());
			}

			body = "Notification Message" + " MessageID: " + messageID
					+ " WNSURL: " + this.wns + " ServiceURL: " + service
					// TODO Where to get the information of the calling
					// service??? A URL would be nice for the recipient to
					// retriev the complete message!!!
					// + dnd.getDoNotification().getMessage()
					// .getServiceDescription().getServiceURL()
					+ " Message:" + dnd.getDoNotification().getShortMessage();

			this.mh.sendExternalMessage(to, subject, body);

		}
	}
}
