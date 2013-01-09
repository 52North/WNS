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

package org.n52.wns;

/**
 * This class provides the necessary methods to handle messages
 * 
 * @author Dennis Dahlmann, Johannes Echterhoff
 * @version 2.0
 */

import net.opengis.wns.x00.DoNotificationDocument;
import net.opengis.wns.x00.DoNotificationResponseDocument;
import net.opengis.wns.x00.DoNotificationResponseType;
import net.opengis.wns.x00.GetMessageDocument;
import net.opengis.wns.x00.GetMessageResponseDocument;

import org.n52.wns.WNSServiceException.ExceptionCode;
import org.n52.wns.communication.FaxHandler;
import org.n52.wns.communication.FaxHandlerFactory;
import org.n52.wns.communication.PhoneHandler;
import org.n52.wns.communication.PhoneHandlerFactory;
import org.n52.wns.db.DAOFactory;
import org.n52.wns.db.MessageDAO;
import org.n52.wns.mail.IMailHandler;
import org.n52.wns.mail.MailHandlerFactory;
import org.n52.wns.sms.SMSHandler;
import org.n52.wns.sms.SMSHandlerFactory;
import org.n52.wns.xmpp.XMPPHandler;
import org.n52.wns.xmpp.XMPPHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.x52North.wns.v2.WNSConfigDocument.WNSConfig;
import org.x52North.wns.v2.WNSUserDocument;

public class WNSMessageHandler implements WNSMessageHandlerI {

	private static WNSMessageHandler instance = null;

	private static Logger log = LoggerFactory.getLogger(WNSMessageHandler.class);

	private IMailHandler mailHandler = null;

	private SMSHandler smsHandler = null;

	private XMPPHandler xmppHandler = null;

	private PhoneHandler phoneHandler = null;

	private FaxHandler faxHandler = null;

	private WNSWebServiceHandler wsHandler = null;

	private WNSUserHandler userHandler = null;

	private int messageID = 0;

	private MessageDAO messageDAO = null;

	/**
	 * 
	 * @param userHandler
	 *            The handler to admin the user data
	 * @throws WNSServiceException
	 */
	public WNSMessageHandler(WNSUserHandler userHandler)
			throws WNSServiceException {

		WNSInitParamContainer initParams = WNSInitParamContainer.getInstance();
		WNSConfig wnsConfig = initParams.getWnsConfig();
		this.userHandler = userHandler;
		log.debug("Trying to init Message DAO");
		DAOFactory daofac = initParams.getDAOFactory();
		try {
			this.messageDAO = daofac.getMessageDAO();
		} catch (WNSException e1) {
			WNSServiceException s = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			s.addCodedException(WNSServiceException.ExceptionCode.NoApplicable,
					null, e1);
			throw s;
		}
		log.debug("Message DAO successfully initated");

		try {
			this.loadMessages();
		} catch (WNSException e1) {
			log.error(e1.toString());
			WNSServiceException s = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			s.addCodedException(WNSServiceException.ExceptionCode.NoApplicable,
					e1.toString());
			throw s;
		}

		if (wnsConfig.getRegisteredHandlers().isSetEMailHandler()) {
			if(log.isDebugEnabled()) { log.debug("Loading Mail Handler..."); }
			try {
				this.mailHandler = MailHandlerFactory.getInstance(wnsConfig
						.getRegisteredHandlers().getEMailHandler());
				if(log.isDebugEnabled()) { log.debug("Loading Mail Handler finished."); }
			} catch (Exception e) {
				log.error("Error during MailHandlerFactory getInstance: "
						+ e.toString());
			}
		}
		if (wnsConfig.getRegisteredHandlers().isSetSMSHandler()) {
			try {
				if (this.mailHandler != null) {
					this.smsHandler = SMSHandlerFactory.getInstance(wnsConfig
							.getRegisteredHandlers().getSMSHandler(), wnsConfig
							.getServiceProperties().getWNSURL());
				}
			} catch (Exception e) {
				log.error("Error during SMSHandlerFactory getInstance: "
						+ e.toString());
			}
		}
		if (wnsConfig.getRegisteredHandlers().isSetPhoneHandler()) {
			try {
				if (this.mailHandler != null) {
					this.phoneHandler = PhoneHandlerFactory
							.getInstance(wnsConfig.getRegisteredHandlers()
									.getPhoneHandler(), wnsConfig
									.getServiceProperties().getWNSURL());
				}
			} catch (Exception e) {
				log.error("Error during PhoneHandlerFactory getInstance: "
						+ e.toString());
			}
		}
		if (wnsConfig.getRegisteredHandlers().isSetFaxHandler()) {
			try {
				if (this.mailHandler != null) {
					this.faxHandler = FaxHandlerFactory.getInstance(wnsConfig
							.getRegisteredHandlers().getFaxHandler());
				}
			} catch (Exception e) {
				log.error("Error during FaxHandlerFactory getInstance: "
						+ e.toString());
			}
		}
		if (wnsConfig.getRegisteredHandlers().isSetXMPPHandler()) {
			try {
				this.xmppHandler = XMPPHandlerFactory.getInstance(wnsConfig
						.getRegisteredHandlers().getXMPPHandler());
			} catch (Exception e) {
				log.error("Error during XMPPHandlerFactory getInstance: "
						+ e.toString());
			}

		}
		try {
			this.wsHandler = new WNSWebServiceHandler();
		} catch (Exception e) {
			log.error("Error during WNSWebServiceHandler getInstance: "
					+ e.toString());
		}

	}

	/**
	 * Singleton
	 * 
	 * @param userhandler
	 * @return The WNSMessageHandler
	 * @throws WNSServiceException
	 */
	public static synchronized WNSMessageHandler getInstance(
			WNSUserHandler userhandler) throws WNSServiceException {
		log.debug("getInstance request");
		if (instance == null) {
			instance = new WNSMessageHandler(userhandler);
		}
		return instance;
	}

	public static WNSMessageHandler getInstance() throws WNSException {
		log.debug("getInstance request");
		if (instance == null) {
			throw new WNSException("Illegal call of getInstance");
		}
		return instance;
	}

	/**
	 * Method to get the messageIDs from the database to increase from this
	 * number
	 * 
	 */
	private void loadMessages() throws WNSException {

		this.messageID = this.messageDAO.getMaxMessageNumber();
		this.messageID++;
	}

	/**
	 * MAIN function to send a notification
	 */
	public synchronized DoNotificationResponseDocument doNotification(
			DoNotificationDocument dnd) throws WNSServiceException {
		DoNotificationResponseDocument response = DoNotificationResponseDocument.Factory
				.newInstance();
		WNSUserDocument.WNSUser.SingleUser.User.Channel[] channel = null;
		log.debug("Trying to doNotification");
		try {
			channel = this.userHandler.getMessageChannels(dnd.getDoNotification().getUserID());
		} catch (WNSException e) {
			log.error("UnknownUserID: " + dnd.getDoNotification().getUserID());
			WNSServiceException s = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			s.addCodedException(
					WNSServiceException.ExceptionCode.UnknownUserID, dnd
							.getDoNotification().getUserID());
			throw s;
		} catch (Exception e) {
			log.error("UnknownUserID: " + dnd.getDoNotification().getUserID());
			WNSServiceException s = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			s.addCodedException(
					WNSServiceException.ExceptionCode.UnknownUserID, dnd
							.getDoNotification().getUserID());
			throw s;
		}
		boolean storeMessage = false;
		boolean errorOcurred = false;
		String locator = "";
		for (int i = 0; i < channel.length; i++) {
			switch (channel[i].getProtocol().intValue()) {
			case WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.INT_EMAIL:

				if (this.mailHandler != null) {
					try {
						this.mailHandler.sendNotificationMessage(dnd,
								channel[i].getTargetArray());
					} catch (WNSException e) {
						log.error(e.toString());
						errorOcurred = true;
						locator += "EMAIL ";
					}
				}
				break;
			case WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.INT_XMPP:
				if (this.xmppHandler != null) {
					try {
						this.xmppHandler.sendNotificationMessage(dnd,
								channel[i].getTargetArray());
					} catch (WNSException e) {
						log.error(e.toString());
						errorOcurred = true;
						locator += "XMPP ";
					}
				}
				break;
			case WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.INT_SMS:
				if (this.smsHandler != null) {
					try {
						this.smsHandler.sendNotificationMessage(dnd, channel[i]
								.getTargetArray(), "N" + this.messageID);
						storeMessage = true;
					} catch (WNSException e) {
						log.error(e.toString());
						errorOcurred = true;
						locator += "SMS ";
					}
				}
				break;
			case WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.INT_HTTP:
				if (this.wsHandler != null) {
					try {

						this.wsHandler.sendNotificationMessage(dnd, channel[i]
								.getTargetArray());
					} catch (WNSException e) {
						log.error(e.toString());
						errorOcurred = true;
						locator += "HTTP ";
					}
				}
				break;
			case WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.INT_PHONE:
				if (this.phoneHandler != null) {
					try {

						this.phoneHandler.sendNotificationMessage(dnd,
								channel[i].getTargetArray(), "N"
										+ this.messageID);
						storeMessage = true;
					} catch (WNSException e) {
						log.error(e.toString());
						errorOcurred = true;
						locator += "PHONE ";
					}
				}
				break;
			case WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.INT_FAX:
				if (this.faxHandler != null) {
					try {

						this.faxHandler.sendNotificationMessage(dnd, channel[i]
								.getTargetArray(), "N" + this.messageID);
						storeMessage = true;
					} catch (WNSException e) {
						log.error(e.toString());
						errorOcurred = true;
						locator += "FAX ";
					}
				}
				break;

			default:
				WNSServiceException service = new WNSServiceException(
						WNSInitParamContainer.getInstance().getExceptionLevel());
				service.addCodedException(ExceptionCode.MessageSendingFailed,
						null);
				throw service;
			}
		}

		if (errorOcurred) {
			WNSServiceException s = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			s.addCodedException(
					WNSServiceException.ExceptionCode.MessageSendingFailed,
					locator);
			log.error("Error during sending in: " + locator);

			throw s;

		}

		if (storeMessage) {
			try {
				this.messageDAO.storeNotificationMessage(dnd
						.getDoNotification(), this.messageID);
				this.messageID++;
			} catch (WNSException e) {
				log.error(e.toString());
				WNSServiceException s = new WNSServiceException(
						WNSInitParamContainer.getInstance().getExceptionLevel());
				s
						.addCodedException(
								WNSServiceException.ExceptionCode.NoApplicable,
								null, e);
				throw s;
			}
		}
		response.addNewDoNotificationResponse().setStatus(
				DoNotificationResponseType.Status.SUCCESS);
		log.debug("DoNotification successful");
		return response;
	}

	/**
	 * MAIN function to get the fully XML message
	 */
	public synchronized GetMessageResponseDocument getMessage(
			GetMessageDocument gmd) throws WNSServiceException {
		String mID = gmd.getGetMessage().getMessageID();

		GetMessageResponseDocument response = null;
		log.debug("Trying to GetMessage");
		try {
			response = this.messageDAO.getMessage(mID);
		} catch (WNSException e) {
			log.error(e.toString());
			WNSServiceException s = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			s.addCodedException(WNSServiceException.ExceptionCode.NoApplicable,
					null, e);
			throw s;
		}
		log.debug("GetMessage successful");
		return response;
	}

	/**
	 * MAIN function to get the fully XML message
	 */
	public synchronized GetMessageResponseDocument getMessage(String messageID)
			throws WNSServiceException {

		GetMessageResponseDocument response = null;
		log.debug("Trying to GetMessage");
		try {
			response = this.messageDAO.getMessage(messageID);
		} catch (WNSException e) {
			log.error(e.toString());
			WNSServiceException s = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			s.addCodedException(WNSServiceException.ExceptionCode.NoApplicable,
					null, e);
			throw s;
		}
		log.debug("GetMessage successful");
		return response;
	}

	/**
	 * This methods returns true if the handler is available, else false
	 * 
	 * @param handler
	 *            A string represantation of the available handlers. Allowed are
	 * @return True if the handler is available, else false
	 */
	public boolean isHandlerAvailable(String handler) {
		if (handler.equals(WNSConstants.EMAIL)) {
			if (this.mailHandler != null) {
				return true;
			}
		}
		if (handler.equals(WNSConstants.SMS)) {
			if (this.smsHandler != null) {
				return true;
			}
		}
		if (handler.equals(WNSConstants.FAX)) {
			if (this.faxHandler != null) {
				return true;
			}
		}
		if (handler.equals(WNSConstants.PHONE)) {
			if (this.phoneHandler != null) {
				return true;
			}
		}
		if (handler.equals(WNSConstants.XMPP)) {
			if (this.xmppHandler != null) {
				return true;
			}
		}
		return false;
	}
}
