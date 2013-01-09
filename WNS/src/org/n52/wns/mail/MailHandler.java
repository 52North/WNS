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

package org.n52.wns.mail;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.opengis.wns.x00.CommunicationMessageDocument;
import net.opengis.wns.x00.DoNotificationDocument;
import net.opengis.wns.x00.NotificationMessageDocument;
import net.opengis.wns.x00.ReplyMessageDocument;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.n52.wns.WNSException;
import org.x52North.wns.WNSConfigDocument;

/**
 * Class to send emails
 * 
 * @author Dennis Dahlmann
 * @version 2.0
 */

public class MailHandler {

	private String host, smtpuser, smtppwd;

	private String from;

	private boolean ssl = false;

	private boolean auth = true;

	private boolean tls = true;

	private String subject = "52 North Web Notification Service";

	private Properties props = System.getProperties();

	private Session session = null;

	private int port = 25;

	private boolean looping = false;

	private static Logger log = Logger.getLogger(MailHandler.class.getName());

	/** constuctor */
	protected MailHandler(
			WNSConfigDocument.WNSConfig.RegisteredHandlers.EMailHandler mailhandler) {
		this.setProperties(mailhandler);
	}

	/**
	 * Sends a notification email
	 * 
	 * @param dnd
	 *            The DoNotificationDocument
	 * @param targets
	 *            A string array containing targets
	 * @throws WNSException
	 */
	public void sendNotificationMessage(DoNotificationDocument dnd,
			String[] targets) throws WNSException {
		log.debug("Trying to send NotificationMessage to: ");
		this.session = Session.getInstance(this.props);
		for (int i = 0; i < targets.length; i++) {
			try {
				String recipient = targets[i];
				log.debug(recipient);
				Message msg = new MimeMessage(this.session);
				Transport t = null;
				if (this.ssl) {
					t = this.session.getTransport("smtps");
				} else {
					t = this.session.getTransport("smtp");
				}

				msg.setFrom(new InternetAddress(this.from));

				msg.setRecipient(Message.RecipientType.TO, new InternetAddress(
						recipient));

				msg.setSubject(this.subject);
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
					re = ReplyMessageDocument.Factory.parse(dnd
							.getDoNotification().getMessage().toString());
					repl = true;
				} catch (XmlException e) {
					repl = false;
				}

				if (noti) {
					msg.setText(no.toString());
				} else {
					if (comu) {
						msg.setText(co.toString());
					} else {
						if (repl) {
							msg.setText(re.toString());
						} else {
							net.opengis.wns.x00.DoNotificationType.Message notdoc = net.opengis.wns.x00.DoNotificationType.Message.Factory
									.newInstance();
							notdoc.set(dnd.getDoNotification().getMessage());
							msg.setText(notdoc.toString());
						}
					}
				}

				t.connect(this.host, this.port, this.smtpuser, this.smtppwd);
				msg.saveChanges();

				t.sendMessage(msg, msg.getAllRecipients());
				this.looping = false;
				log.debug("Successful");
				// Auth enabled?
			} catch (SendFailedException ex) {
				if (this.looping) {
					log.fatal("Error while sending NotificationMessage: "
							+ ex.toString());
					throw new WNSException(
							"Error while sending NotificationMessage: "
									+ ex.toString());
				}
				log.fatal("Error while sending NotificationMessage: "
						+ ex.toString());
				this.looping = true;
				if (ex.toString().contains("aut")) {
					this.props.put("mail.smtp.auth", "true");
					this.props.put("mail.smtps.auth", "true");
					log.fatal("Switching auth to true");
					this.sendNotificationMessage(dnd, targets);
				}
				// Trying to send over SSL, but maybe not supported
			} catch (MessagingException ex) {
				if (this.looping) {
					log.fatal("Error while sending NotificationMessage: "
							+ ex.toString());
					throw new WNSException(
							"Error while sending NotificationMessage: "
									+ ex.toString());
				}
				log.fatal("Error while sending NotificationMessage: "
						+ ex.toString());
				this.looping = true;
				if (ex.toString().contains("ssl")) {
					this.ssl = false;
					log.fatal("Switching connection mode to non SSL");
					this.sendNotificationMessage(dnd, targets);
				} else {
					throw new WNSException(
							"Error while sending NotificationMessage: "
									+ ex.toString());
				}

			} catch (Exception ex) {
				log.fatal("Error while sending NotificationMessage: "
						+ ex.toString());
				throw new WNSException(
						"Error while sending NotificationMessage: "
								+ ex.toString());
			}
		}

	}

	/**
	 * 
	 * @param target
	 *            The target adress
	 * @param subject
	 *            The subject of the email
	 * @param body
	 *            the message body
	 * @throws WNSException
	 */
	public void sendExternalMessage(String target, String subject, String body)
			throws WNSException {
		try {
			log.debug("Trying to send message to: " + target);
			this.session = Session.getInstance(this.props);
			Message msg = new MimeMessage(this.session);
			Transport t = null;
			if (this.ssl) {
				t = this.session.getTransport("smtps");
			} else {
				t = this.session.getTransport("smtp");
			}

			msg.setFrom(new InternetAddress(this.from));

			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(
					target));

			msg.setSubject(subject);
			msg.setText(body);
			t.connect(this.host, this.port, this.smtpuser, this.smtppwd);
			msg.saveChanges();

			t.sendMessage(msg, msg.getAllRecipients());
			this.looping = false;
			log.debug("Successful");
			// Auth enabled?
		} catch (SendFailedException ex) {
			if (this.looping) {
				log.fatal("Error while sending Message: " + ex.toString());
				throw new WNSException(
						"Error while sending NotificationMessage: "
								+ ex.toString());
			}
			log.fatal("Error while sending Message: " + ex.toString());
			this.looping = true;
			if (ex.toString().contains("aut")) {
				this.props.put("mail.smtp.auth", "true");
				this.props.put("mail.smtps.auth", "true");
				log.fatal("Switching auth to true");
				this.sendExternalMessage(target, subject, body);
			}
			// Trying to send over SSL, but maybe not supported
		} catch (MessagingException ex) {
			if (this.looping) {
				log.fatal("Error while sending Message: " + ex.toString());
				throw new WNSException(
						"Error while sending NotificationMessage: "
								+ ex.toString());
			}
			log.fatal("Error while sending Message: " + ex.toString());
			this.looping = true;
			if (ex.toString().contains("ssl")) {
				this.ssl = false;
				log.fatal("Switching connection mode to non SSL");
				this.sendExternalMessage(target, subject, body);
			}
		} catch (Exception e) {
			log.fatal("Error while sending Message: " + e.toString());
			throw new WNSException("Error while sending NotificationMessage: "
					+ e.toString());
		}
	}

	/**
	 * 
	 * @param targets
	 *            A string array of targets
	 * @param subject
	 *            The subject of the email
	 * @param body
	 *            the message body
	 * @throws WNSException
	 */
	public void sendExternalMessage(String[] targets, String subject,
			String body) throws WNSException {
		try {
			log.debug("Trying to send message to:");
			this.session = Session.getInstance(this.props);
			Message msg = new MimeMessage(this.session);
			Transport t = null;
			if (this.ssl) {
				t = this.session.getTransport("smtps");
			} else {
				t = this.session.getTransport("smtp");
			}

			msg.setFrom(new InternetAddress(this.from));
			for (int i = 0; i < targets.length; i++) {
				log.debug(targets[i]);
				msg.setRecipient(Message.RecipientType.TO, new InternetAddress(
						targets[i]));
			}
			msg.setSubject(subject);
			msg.setText(body);
			t.connect(this.host, this.port, this.smtpuser, this.smtppwd);
			msg.saveChanges();

			t.sendMessage(msg, msg.getAllRecipients());
			this.looping = false;
			log.debug("Successful");
			// Auth enabled?
		} catch (SendFailedException ex) {
			if (this.looping) {
				log.fatal("Error while sending Message: " + ex.toString());
				throw new WNSException(
						"Error while sending NotificationMessage: "
								+ ex.toString());
			}
			log.fatal("Error while sending Message: " + ex.toString());
			this.looping = true;
			if (ex.toString().contains("aut")) {
				this.props.put("mail.smtp.auth", "true");
				this.props.put("mail.smtps.auth", "true");
				log.fatal("Switching auth to true");
				this.sendExternalMessage(targets, subject, body);
			}
			// Trying to send over SSL, but maybe not supported
		} catch (MessagingException ex) {
			if (this.looping) {
				log.fatal("Error while sending Message: " + ex.toString());
				throw new WNSException(
						"Error while sending NotificationMessage: "
								+ ex.toString());
			}
			log.fatal("Error while sending Message: " + ex.toString());
			this.looping = true;
			if (ex.toString().contains("SSL")) {
				this.ssl = false;
				log.fatal("Switching connection mode to non SSL");
				this.sendExternalMessage(targets, subject, body);
			}
		} catch (Exception e) {
			log.fatal("Error while sending Message: " + e.toString());
			throw new WNSException("Error while sending NotificationMessage: "
					+ e.toString());
		}
	}

	/** sets the mail session properties at startup */
	private void setProperties(
			WNSConfigDocument.WNSConfig.RegisteredHandlers.EMailHandler mailhandler) {
		log.debug("Setting up properties");
		this.host = mailhandler.getSMTP().getHost();
		this.smtppwd = mailhandler.getSMTP().getPasswd();
		this.smtpuser = mailhandler.getSMTP().getUser();
		this.from = mailhandler.getSMTP().getSender();
		this.tls = mailhandler.getSMTP().getTLS();
		this.ssl = mailhandler.getSMTP().getSSL();
		this.auth = mailhandler.getSMTP().getAuthenticate();
		this.subject = mailhandler.getSMTP().getEmailSubject();
		this.port = mailhandler.getSMTP().getPort();

		if (this.ssl) {
			this.props.put("mail.smtps.host", this.host);
			this.props.put("mail.smtps.user", this.smtpuser);
			this.props.put("mail.smtps.port", this.port);
			this.props.put("mail.smtps.from", this.from);
			if (this.auth) {
				this.props.put("mail.smtps.auth", "true");
			}
		} else {
			this.props.put("mail.smtp.host", this.host);
			this.props.put("mail.smtp.user", this.smtpuser);
			this.props.put("mail.smtp.port", this.port);
			this.props.put("mail.smtp.from", this.from);
			if (this.tls) {
				this.props.put("mail.smtp.starttls.enable", "true");
			}
			if (this.auth) {
				this.props.put("mail.smtp.auth", "true");
				this.props.put("mail.smtps.auth", "true");
			}
		}
		log.debug("Properties successfully set");

	}
}
