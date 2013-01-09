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

package org.n52.wns.communication;

import net.opengis.wns.x00.CommunicationMessageDocument;
import net.opengis.wns.x00.DoNotificationDocument;
import net.opengis.wns.x00.NotificationMessageDocument;
import net.opengis.wns.x00.ReplyMessageDocument;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.n52.wns.WNSException;
import org.n52.wns.mail.MailHandler;
import org.x52North.wns.WNSConfigDocument;

/**
 * Class to send messages via ecall.ch fax service
 * 
 * @author Dennis Dahlmann
 * @version 2.0
 */
public class FaxHandlerEcall implements FaxHandler {

	private Logger log = Logger.getLogger(FaxHandlerEcall.class.getName());

	private String host;

	private MailHandler mh;

	/**
	 * constructor FaxHandlerEcall loads the MailHandler
	 * 
	 * @throws WNSException
	 */
	public FaxHandlerEcall() throws WNSException {
		this.log.debug("Init FaxHandlerEcall");
		this.mh = org.n52.wns.mail.MailHandlerFactory.getInstance();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.n52.communication.FaxHandler#setProperties(org.jdom.Element)
	 */
	public void setProperties(
			WNSConfigDocument.WNSConfig.RegisteredHandlers.FaxHandler faxHandler) {
		this.log.debug("setting up properties");
		this.host = faxHandler.getHost();

	}

	/**
	 * sends a FAX NotificationMessage
	 * 
	 * @see org.n52.wns.communication.FaxHandler
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

			String to = msisdn + "@" + this.host;

			subject = ("Web Notification Service");

			NotificationMessageDocument no = null;
			CommunicationMessageDocument co = null;
			ReplyMessageDocument re = null;
			boolean noti = false;
			boolean comu = false;
			boolean repl = false;

			try {
				no = NotificationMessageDocument.Factory.parse(dnd
						.getDoNotification().getMessage().toString());
				noti = true;
			} catch (XmlException e) {
				noti = false;
			}
			try {
				co = CommunicationMessageDocument.Factory.parse(dnd
						.getDoNotification().getMessage().toString());
				comu = true;
			} catch (XmlException e) {
				comu = false;
			}

			try {
				re = ReplyMessageDocument.Factory.parse(dnd.getDoNotification()
						.getMessage().toString());
				repl = true;
			} catch (XmlException e) {
				repl = false;
			}

			if (noti) {
				body = (no.toString());
			} else {
				if (comu) {
					body = (co.toString());
				} else {
					if (repl) {
						body = (re.toString());
					} else {
						body = dnd.getDoNotification().getMessage().toString();
					}
				}
			}

			this.mh.sendExternalMessage(to, subject, body);

		}
	}
}
