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

package org.n52.wns.xmpp;

import net.opengis.wns.x00.CommunicationMessageDocument;
import net.opengis.wns.x00.DoNotificationDocument;
import net.opengis.wns.x00.NotificationMessageDocument;
import net.opengis.wns.x00.ReplyMessageDocument;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SSLXMPPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.n52.wns.WNSException;
import org.n52.wns.WNSServiceException;
import org.x52North.wns.WNSConfigDocument;

/**
 * XMPPHandlerJabber organizes the sending of instant messages with XMPP
 * 
 * @author Dennis Dahlmann
 * @version 2.0
 */
public class XMPPHandlerJabber implements org.n52.wns.xmpp.XMPPHandler {

	private static Logger log = Logger.getLogger(XMPPHandlerJabber.class
			.getName());

	/**
	 * The XMPPConnection variable
	 */
	private XMPPConnection connection;

	/**
	 * A String represantation of the username
	 */
	private String user;

	/**
	 * A String represantation of the password
	 */
	private String pwd;

	/**
	 * A String represantation of the server adress
	 */
	private String server = "";

	private int port = 5222; // default Jabber port

	public XMPPHandlerJabber() throws WNSServiceException {

	}

	/**
	 * Setting up the properties
	 */
	public void setProperties(
			WNSConfigDocument.WNSConfig.RegisteredHandlers.XMPPHandler xmppHandler)
			throws WNSException {
		log.debug("setting properties");

		this.user = xmppHandler.getUser();
		this.pwd = xmppHandler.getPasswd();
		this.port = xmppHandler.getPort();
		this.server = xmppHandler.getHost();
		try {
			this.setup();
			this.login();
		} catch (Exception e) {
			log.info(e.toString());
			throw new WNSException(e.toString());
		}

	}

	/**
	 * Set up the Instant Messsage environment, called from setProperties
	 */
	private void setup() throws WNSException {
		try {
			log.debug("Trying to connect to: " + this.server + " at port: "
					+ this.port);
			this.connection = new XMPPConnection(this.server, this.port);
			log.debug("Connected");
		} catch (XMPPException e) {
			try {
				log.debug("Trying to connect via SSL to: " + this.server
						+ " at port: " + this.port);
				this.connection = new SSLXMPPConnection(this.server, this.port);
				log.debug("Connected via SSL");
			} catch (Exception e1) {
				log.fatal("Error duriong XMPP connection establishment: "
						+ e1.toString());
				throw new WNSException("Failure to connect to XMPP server");
			}

		}
	}

	/**
	 * Login creates a connection to the xmpp server and tries to login the
	 * user, called from setProperties
	 */
	private void login() throws WNSException {
		try {
			log.debug("Trying to login");
			SASLAuthentication auth = this.connection.getSASLAuthentication();
			auth.authenticate(this.user, this.pwd, "");
			log.debug("Logged in");
		} catch (XMPPException e) {
			log.fatal("Login to XMPP server failed: " + e.toString());
			throw new WNSException("Login to XMPP server failed ");
		}
	}

	/**
	 * This sends the message to the recipient
	 * 
	 * @param recipient
	 *            The enduser adress
	 * @param message
	 *            The message as a string
	 * @throws WNSException
	 */
	private void sendMessage(String recipient, String message)
			throws WNSException {
		try {
			this.setup();
			this.login();
			log.debug("Trying to send message to: " + recipient);
			this.connection.createChat(recipient).sendMessage(message);
			this.connection.close();
			log.debug("Sending successfully");
		} catch (XMPPException e1) {
			log.fatal("Message sending failed, recipient: " + recipient + " : "
					+ e1.toString());
			throw new WNSException("Message sending failed: " + e1.toString());
		}
	}

	/**
	 * sends a Notification Message
	 * 
	 * @see org.n52.wns.xmpp.XMPPHandler#sendNotificationMessage(DoNotificationDocument
	 *      dnd, String[] targets)
	 * 
	 */
	public void sendNotificationMessage(DoNotificationDocument dnd,
			String[] targets) throws WNSException {
		String bodyText;
		// get the address
		for (int i = 0; i < targets.length; i++) {
			String recipient = targets[i];

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
				bodyText = no.toString();
			} else {
				if (comu) {
					bodyText = co.toString();
				} else {
					if (repl) {
						bodyText = re.toString();
					} else {
						net.opengis.wns.x00.DoNotificationType.Message notdoc = net.opengis.wns.x00.DoNotificationType.Message.Factory
								.newInstance();
						notdoc.set(dnd.getDoNotification().getMessage());
						bodyText = notdoc.toString();
					}
				}
			}

			this.sendMessage(recipient, (bodyText.toString()));
		}

	}
}
