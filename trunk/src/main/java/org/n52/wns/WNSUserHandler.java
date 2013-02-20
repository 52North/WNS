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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import net.opengis.wns.x00.RegisterDocument;
import net.opengis.wns.x00.RegisterMultiUserType;
import net.opengis.wns.x00.RegisterResponseDocument;
import net.opengis.wns.x00.UnregisterDocument;
import net.opengis.wns.x00.UnregisterResponseDocument;
import net.opengis.wns.x00.UnregisterResponseType;
import net.opengis.wns.x00.UpdateMultiUserRegistrationDocument;
import net.opengis.wns.x00.UpdateMultiUserRegistrationResponseDocument;
import net.opengis.wns.x00.UpdateMultiUserRegistrationResponseType;
import net.opengis.wns.x00.UpdateSingleUserRegistrationDocument;
import net.opengis.wns.x00.UpdateSingleUserRegistrationResponseDocument;
import net.opengis.wns.x00.UpdateSingleUserRegistrationResponseType.Status;

import org.n52.wns.WNSServiceException.ExceptionCode;
import org.n52.wns.db.DAOFactory;
import org.n52.wns.db.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.x52North.wns.v2.WNSUserDocument;
import org.x52North.wns.v2.WNSUserDocument.WNSUser.SingleUser.User.Channel;

/**
 * This class provides all the methods to handle the user data
 * 
 * @author Dennis Dahlmann
 * @version 2.0
 */

public class WNSUserHandler {

	private static WNSUserHandler instance = null;

	private static Logger log = LoggerFactory.getLogger(WNSUserHandler.class);

	private UserDAO userDAO;

	private Hashtable<Long, WNSUser> singleHashthable = new Hashtable<Long, WNSUser>(); // Table

	// to
	// store

	// the UserID to the

	// user ID:WNSUser

	private Hashtable<Long, WNSUser> multiUserHashtable = new Hashtable<Long, WNSUser>(); // Table

	// to
	// store

	// the UserID to the

	// associated user ID:associated IDs

	private long userIDs = 0;

	// private Vector<Long> freeUserIDs = new Vector<Long>();

	private WNSUserDocument userdoc;

	/**
	 * Constructor
	 * 
	 * @throws WNSServiceException
	 * 
	 */
	private WNSUserHandler() throws WNSServiceException {

		WNSInitParamContainer initParams = WNSInitParamContainer.getInstance();
		DAOFactory daofac = initParams.getDAOFactory();
		log.debug("Trying to init USER DAO");
		try {
			this.userDAO = daofac.getUserDAO();
			log.debug("USER DAO successfully init");
		} catch (WNSException e) {
			log.error("Error during init USER DAO " + e.toString());
			WNSServiceException service = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			service.addCodedException(ExceptionCode.NoApplicable,
					"WNSUserHandler", "Error during init USER DAO "
							+ e.toString());
			throw service;
		}
		try {
			log.debug("Trying to get WNSUserDocument");
			this.userdoc = this.userDAO.getUserDocument();
			log.debug("WNSUserDocument successfully retrieved");
		} catch (WNSException e) {
			log.error("Error during getting WNSUserDocument: " + e.toString());
			WNSServiceException s = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			s.addCodedException(WNSServiceException.ExceptionCode.NoApplicable,
					"WNSUserHandler", "Error during getting WNSUserDocument: "
							+ e.toString());
			throw s;
		}
		this.initUsers();
	}

	/**
	 * Singleton
	 * 
	 * @return The WNSUserHandler
	 * @throws WNSServiceException
	 */
	public static synchronized WNSUserHandler getInstance()
			throws WNSServiceException {
		if (instance == null) {
			instance = new WNSUserHandler();
		}
		return instance;
	}

	/**
	 * MAIN function to register a user
	 * 
	 * @param rd
	 *            The XML document, see the schema
	 * @return The RegisterUserResponseDocument
	 * @throws WNSServiceException
	 */
	public synchronized RegisterResponseDocument register(RegisterDocument rd)
			throws WNSServiceException {
		long id = ++this.userIDs;

		// SingleUser
		if (rd.getRegister().isSetSingleUser()) {
			log.debug("SingleUser registration requested");
			RegisterResponseDocument rurd = RegisterResponseDocument.Factory
					.newInstance();
			WNSMessageHandler messageHandler;
			try {
				messageHandler = WNSMessageHandler.getInstance();
			} catch (WNSException e1) {
				log.error(e1.toString());
				WNSServiceException service = new WNSServiceException(
						WNSInitParamContainer.getInstance().getExceptionLevel());
				service.addCodedException(ExceptionCode.NoApplicable,
						"WNSUserHandler", e1);
				throw service;
			}

			String unknownProtocol = "";
			boolean unknownProFound = false;

			// Check if the transfert communication protocol is supported
			// Check for EMAIL
			if (rd.getRegister().getSingleUser().getCommunicationProtocol()
					.sizeOfEmailArray() > 0) {
				if (!messageHandler.isHandlerAvailable(WNSConstants.EMAIL)) {
					unknownProFound = true;
					unknownProtocol += "EMAIL ";
				}
			}
			// Check for FAX
			if (rd.getRegister().getSingleUser().getCommunicationProtocol()
					.sizeOfFaxArray() > 0) {
				if (!messageHandler.isHandlerAvailable(WNSConstants.FAX)) {
					unknownProFound = true;
					unknownProtocol += "FAX ";
				}
			}
			// Check for PHONE
			if (rd.getRegister().getSingleUser().getCommunicationProtocol()
					.sizeOfPhoneArray() > 0) {
				if (!messageHandler.isHandlerAvailable(WNSConstants.PHONE)) {
					unknownProFound = true;
					unknownProtocol += "PHONE ";
				}
			}
			// Check for SMS
			if (rd.getRegister().getSingleUser().getCommunicationProtocol()
					.sizeOfSMSArray() > 0) {
				if (!messageHandler.isHandlerAvailable(WNSConstants.SMS)) {
					unknownProFound = true;
					unknownProtocol += "SMS ";
				}
			}
			// Check for XMPP
			if (rd.getRegister().getSingleUser().getCommunicationProtocol()
					.sizeOfXMPPArray() > 0) {
				if (!messageHandler.isHandlerAvailable(WNSConstants.XMPP)) {
					unknownProFound = true;
					unknownProtocol += "XMPP ";
				}
			}

			if (unknownProFound) {
				WNSServiceException service = new WNSServiceException(
						WNSInitParamContainer.getInstance().getExceptionLevel());
				service.addCodedException(ExceptionCode.ProtocolNotSupported,
						unknownProtocol);
				this.userIDs--;
				throw service;
			}

			WNSUser wnsUser = new WNSUser(rd.getRegister().getSingleUser(), id);
			WNSUserDocument.WNSUser singleuserdoc = this.userdoc.getWNSUser();
			WNSUserDocument.WNSUser.SingleUser single = singleuserdoc
					.getSingleUser();
			WNSUserDocument.WNSUser.SingleUser.User singleUser = single
					.addNewUser();
			singleUser.setID(String.valueOf(wnsUser.getUserID()));
			singleUser.setName(wnsUser.getName());
			singleUser.setChannelArray(wnsUser.getChannels());

			try {
				this.userDAO.storeWNSUserDocument(this.userdoc);
			} catch (WNSException e) {
				WNSServiceException service = new WNSServiceException(
						WNSInitParamContainer.getInstance().getExceptionLevel());
				service.addCodedException(ExceptionCode.NoApplicable,
						"WNSUserhandler", e);
				throw service;
			}
			this.singleHashthable.put(new Long(wnsUser.getUserID()), wnsUser);
			rurd.addNewRegisterResponse().setUserID(Long.toString(id));
			log.debug("SingleUser registration successful");
			return rurd;

			// MultiUser
		} else {
			log.debug("MultiUser registration requested");
			RegisterResponseDocument rurd = RegisterResponseDocument.Factory
					.newInstance();
			RegisterMultiUserType multi = rd.getRegister().getMultiUser();
			// Check if the userIDs are valid
			String unknownUserID = "";
			boolean unknownUserFound = false;
			for (int i = 0; i < rd.getRegister().getMultiUser()
					.sizeOfUserIDArray(); i++) {
				if ((this.singleHashthable.get(Long.parseLong(rd.getRegister()
						.getMultiUser().getUserIDArray(i))) == null)
						&& (this.multiUserHashtable.get(Long
								.parseLong(rd.getRegister().getMultiUser()
										.getUserIDArray(i))) == null)) {
					unknownUserID += rd.getRegister().getMultiUser()
							.getUserIDArray(i)
							+ " ";
					unknownUserFound = true;
					// multi.removeUserID(i);
				}
			}
			// If any user is not valid, throw an exception
			if (unknownUserFound) {
				WNSServiceException service = new WNSServiceException(
						WNSInitParamContainer.getInstance().getExceptionLevel());
				service.addCodedException(ExceptionCode.UnknownUserID,
						unknownUserID);
				throw service;
			}

			WNSUser wnsUser = new WNSUser(multi, id);
			WNSUserDocument.WNSUser user = this.userdoc.getWNSUser();
			WNSUserDocument.WNSUser.MultiUser multiUser = user.getMultiUser();
			WNSUserDocument.WNSUser.MultiUser.User multuser = multiUser
					.addNewUser();
			multuser.setID(String.valueOf(wnsUser.getUserID()));
			multuser.setAssociatedUsers(wnsUser.getAssociatedUsers());
			try {
				this.userDAO.storeWNSUserDocument(this.userdoc);
			} catch (WNSException e) {
				WNSServiceException service = new WNSServiceException(
						WNSInitParamContainer.getInstance().getExceptionLevel());
				service.addCodedException(ExceptionCode.NoApplicable,
						"WNSUserhandler", e);
				throw service;
			}
			this.multiUserHashtable.put(new Long(wnsUser.getUserID()), wnsUser);
			rurd.addNewRegisterResponse().setUserID(Long.toString(id));
			log.debug("MultiUser registration successful");
			return rurd;

		}
	}

	/**
	 * MAIN function to update the SingleUser, e.g. edit name
	 * 
	 * @param usurd
	 *            The XML dochument
	 * @return UpdateSingleUserRegistrationResponseDocument
	 * @throws WNSServiceException
	 */
	public synchronized UpdateSingleUserRegistrationResponseDocument updateSingleUser(
			UpdateSingleUserRegistrationDocument usurd)
			throws WNSServiceException {
		UpdateSingleUserRegistrationResponseDocument usurrd = UpdateSingleUserRegistrationResponseDocument.Factory
				.newInstance();
		WNSMessageHandler messageHandler;
		try {
			messageHandler = WNSMessageHandler.getInstance();
		} catch (WNSException e1) {
			WNSServiceException service = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			service.addCodedException(ExceptionCode.NoApplicable,
					"WNSUserhandler", e1);
			throw service;
		}

		// FIXME WORKAROUND BUG 511
		Long userID = this.parseUserID(usurd.getUpdateSingleUserRegistration().getUserID());
		boolean unknownUser = (userID==null?true:false);
		if(unknownUser) {
			log.error("UnknownUserID: "
					+ usurd.getUpdateSingleUserRegistration().getUserID());
			WNSServiceException s = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			s.addCodedException(
					WNSServiceException.ExceptionCode.UnknownUserID, usurd
							.getUpdateSingleUserRegistration().getUserID());
			throw s;
		}
		// Update Name
		if (!unknownUser && usurd.getUpdateSingleUserRegistration().isSetUpdateName()) {
			log.debug("Trying to update the name");
			if (this.singleHashthable.containsKey(userID)) {
				WNSUser wnsUser = this.singleHashthable.get(userID);
				wnsUser.setName(usurd.getUpdateSingleUserRegistration()
						.getUpdateName());
				WNSUserDocument.WNSUser user = this.userdoc.getWNSUser();
				WNSUserDocument.WNSUser.SingleUser single = user
						.getSingleUser();
				for (int i = 0; i <= single.sizeOfUserArray() - 1; i++) {
					if (single.getUserArray(i).getID()
							.equalsIgnoreCase(userID.toString())) {
						single.getUserArray(i).setName(
								usurd.getUpdateSingleUserRegistration()
										.getUpdateName());
					}
				}
				try {
					this.userDAO.storeWNSUserDocument(this.userdoc);
				} catch (WNSException e) {
					WNSServiceException service = new WNSServiceException(
							WNSInitParamContainer.getInstance()
									.getExceptionLevel());
					service.addCodedException(ExceptionCode.NoApplicable,
							"WNSUserhandler", e);
					throw service;
				}
			} else {
				log.error("UnknownUserID: "
						+ usurd.getUpdateSingleUserRegistration().getUserID());
				WNSServiceException s = new WNSServiceException(
						WNSInitParamContainer.getInstance().getExceptionLevel());
				s.addCodedException(
						WNSServiceException.ExceptionCode.UnknownUserID, usurd
								.getUpdateSingleUserRegistration().getUserID());
				throw s;
			}
			log.debug("Update successful");

			// usurrd.addNewUpdateSingleUserRegistrationResponse().setStatus(
			// Status.SUCCESS);
			// return usurrd;
		}

		// Remove protocol
		if (!unknownUser && usurd.getUpdateSingleUserRegistration()
				.isSetRemoveCommunicationProtocol()) {
			log.debug("Trying to remove protocol");
			// usurrd.addNewUpdateSingleUserRegistrationResponse().setStatus(
			// Status.SUCCESS);
			Vector<Integer> removingChannels = new Vector<Integer>();
			if (this.singleHashthable.containsKey(userID)) {
				WNSUser wnsUser = this.singleHashthable.get(userID);
				wnsUser.removeProtocol(usurd.getUpdateSingleUserRegistration()
						.getRemoveCommunicationProtocol());
				WNSUserDocument.WNSUser user = this.userdoc.getWNSUser();
				WNSUserDocument.WNSUser.SingleUser single = user
						.getSingleUser();
				for (int i = 0; i < single.sizeOfUserArray(); i++) {
					// Find the user
					if (single.getUserArray(i).getID().equalsIgnoreCase(userID.toString()) ) {
						for (int j = 0; j < single.getUserArray(i)
								.sizeOfChannelArray(); j++) {
							// EMAIL
							if (single.getUserArray(i).getChannelArray(j)
									.getProtocol().equals(
											Channel.Protocol.EMAIL)
									&& (usurd.getUpdateSingleUserRegistration()

									.getRemoveCommunicationProtocol()
											.sizeOfEmailArray() > 0)) {
								// Only one target?, delete the complete element
								if (single.getUserArray(i).getChannelArray(j)
										.sizeOfTargetArray() == 1) {
									removingChannels.addElement(j);
								} else {
									for (int k = 0; k < single.getUserArray(i)
											.getChannelArray(j)
											.sizeOfTargetArray(); k++) {
										for (int index = 0; index < usurd
												.getUpdateSingleUserRegistration()

												.getRemoveCommunicationProtocol()
												.sizeOfEmailArray(); index++) {
											if (single
													.getUserArray(i)
													.getChannelArray(j)
													.getTargetArray(k)
													.equalsIgnoreCase(
															usurd
																	.getUpdateSingleUserRegistration()

																	.getRemoveCommunicationProtocol()
																	.getEmailArray(
																			index))) {
												single.getUserArray(i)
														.getChannelArray(j)
														.removeTarget(k);
											}
										}
									}
								}
							}
							// FAX
							if (single.getUserArray(i).getChannelArray(j)
									.getProtocol().equals(Channel.Protocol.FAX)
									&& (usurd.getUpdateSingleUserRegistration()

									.getRemoveCommunicationProtocol()
											.sizeOfFaxArray() > 0)) {
								// Only one element?
								if (single.getUserArray(i).getChannelArray(j)
										.sizeOfTargetArray() == 1) {
									removingChannels.addElement(j);

								} else {

									String[] remove = new String[usurd
											.getUpdateSingleUserRegistration()

											.getRemoveCommunicationProtocol()
											.sizeOfFaxArray()];
									for (int x = 0; x < remove.length; x++) {
										remove[x] = usurd
												.getUpdateSingleUserRegistration()

												.getRemoveCommunicationProtocol()
												.getFaxArray(x).toString();
									}
									for (int k = 0; k < single.getUserArray(i)
											.getChannelArray(j)
											.sizeOfTargetArray(); k++) {
										for (int index = 0; index < usurd
												.getUpdateSingleUserRegistration()
												.getRemoveCommunicationProtocol()
												.sizeOfFaxArray(); index++) {
											if (single.getUserArray(i)
													.getChannelArray(j)
													.getTargetArray(k)
													.equalsIgnoreCase(
															remove[index])) {
												single.getUserArray(i)
														.getChannelArray(j)
														.removeTarget(k);
											}
										}
									}
								}
							}

							// PHONE
							if (single.getUserArray(i).getChannelArray(j)
									.getProtocol().equals(
											Channel.Protocol.PHONE)
									&& (usurd.getUpdateSingleUserRegistration()
											.getRemoveCommunicationProtocol()
											.sizeOfPhoneArray() > 0)) {
								if (single.getUserArray(i).getChannelArray(j)
										.sizeOfTargetArray() == 1) {
									removingChannels.addElement(j);
								} else {
									String[] remove = new String[usurd
											.getUpdateSingleUserRegistration()
											.getRemoveCommunicationProtocol()
											.sizeOfPhoneArray()];
									for (int x = 0; x < remove.length; x++) {
										remove[x] = usurd
												.getUpdateSingleUserRegistration()
												.getRemoveCommunicationProtocol()
												.getPhoneArray(x).toString();
									}
									for (int k = 0; k < single.getUserArray(i)
											.getChannelArray(j)
											.sizeOfTargetArray(); k++) {
										for (int index = 0; index < usurd
												.getUpdateSingleUserRegistration()
												.getRemoveCommunicationProtocol()
												.sizeOfPhoneArray(); index++) {
											if (single.getUserArray(i)
													.getChannelArray(j)
													.getTargetArray(k)
													.equalsIgnoreCase(
															remove[index])) {
												single.getUserArray(i)
														.getChannelArray(j)
														.removeTarget(k);
											}
										}
									}
								}
							}
							// SMS
							if (single.getUserArray(i).getChannelArray(j)
									.getProtocol().equals(Channel.Protocol.SMS)
									&& (usurd.getUpdateSingleUserRegistration()
											.getRemoveCommunicationProtocol()
											.sizeOfSMSArray() > 0)) {

								if (single.getUserArray(i).getChannelArray(j)
										.sizeOfTargetArray() == 1) {
									removingChannels.addElement(j);
								} else {
									String[] remove = new String[usurd
											.getUpdateSingleUserRegistration()
											.getRemoveCommunicationProtocol()
											.sizeOfSMSArray()];
									for (int x = 0; x < remove.length; x++) {
										remove[x] = usurd
												.getUpdateSingleUserRegistration()
												.getRemoveCommunicationProtocol()
												.getSMSArray(x).toString();
									}
									for (int k = 0; k < single.getUserArray(i)
											.getChannelArray(j)
											.sizeOfTargetArray(); k++) {
										for (int index = 0; index < usurd
												.getUpdateSingleUserRegistration()
												.getRemoveCommunicationProtocol()
												.sizeOfSMSArray(); index++) {
											if (single.getUserArray(i)
													.getChannelArray(j)
													.getTargetArray(k)
													.equalsIgnoreCase(
															remove[index])) {
												single.getUserArray(i)
														.getChannelArray(j)
														.removeTarget(k);
											}
										}
									}
								}
							}
							// XMPP
							if (single.getUserArray(i).getChannelArray(j)
									.getProtocol()
									.equals(Channel.Protocol.XMPP)
									&& (usurd.getUpdateSingleUserRegistration()
											.getRemoveCommunicationProtocol()
											.sizeOfXMPPArray() > 0)) {
								if (single.getUserArray(i).getChannelArray(j)
										.sizeOfTargetArray() == 1) {
									removingChannels.addElement(j);
								} else {

									for (int k = 0; k < single.getUserArray(i)
											.getChannelArray(j)
											.sizeOfTargetArray(); k++) {
										for (int index = 0; index < usurd
												.getUpdateSingleUserRegistration()
												.getRemoveCommunicationProtocol()
												.sizeOfXMPPArray(); index++) {
											if (single
													.getUserArray(i)
													.getChannelArray(j)
													.getTargetArray(k)
													.equalsIgnoreCase(
															usurd
																	.getUpdateSingleUserRegistration()
																	.getRemoveCommunicationProtocol()
																	.getXMPPArray(
																			index))) {
												single.getUserArray(i)
														.getChannelArray(j)
														.removeTarget(k);
											}
										}
									}
								}
							}
							// HTTP
							if (single.getUserArray(i).getChannelArray(j)
									.getProtocol()
									.equals(Channel.Protocol.HTTP)
									&& (usurd.getUpdateSingleUserRegistration()
											.getRemoveCommunicationProtocol()
											.sizeOfHTTPArray() > 0)) {
								if (single.getUserArray(i).getChannelArray(j)
										.sizeOfTargetArray() == 1) {
									removingChannels.addElement(j);
								} else {

									for (int k = 0; k < single.getUserArray(i)
											.getChannelArray(j)
											.sizeOfTargetArray(); k++) {
										for (int index = 0; index < usurd
												.getUpdateSingleUserRegistration()
												.getRemoveCommunicationProtocol()
												.sizeOfHTTPArray(); index++) {
											if (single
													.getUserArray(i)
													.getChannelArray(j)
													.getTargetArray(k)
													.equalsIgnoreCase(
															usurd
																	.getUpdateSingleUserRegistration()
																	.getRemoveCommunicationProtocol()
																	.getHTTPArray(
																			index))) {
												single.getUserArray(i)
														.getChannelArray(j)
														.removeTarget(k);
											}
										}
									}
								}
							}
						}
						removingChannels.trimToSize();
						// Workaround to remove the channels from the last to
						// the first element, otherwise a NULLPOINTER Exception
						// occurs

						// for (int z = 0; z < removingChannels.size(); z++) {
						for (int z = removingChannels.size() - 1; z >= 0; z--) {
							single.getUserArray(i).removeChannel(
									removingChannels.get(z));
						}
					}

				}
				try {
					this.userDAO.storeWNSUserDocument(this.userdoc);
				} catch (WNSException e) {
					WNSServiceException service = new WNSServiceException(
							WNSInitParamContainer.getInstance()
									.getExceptionLevel());
					service.addCodedException(ExceptionCode.NoApplicable,
							"WNSUserhandler", e);
					throw service;
				}
				log.debug("Update successful");
				// return usurrd;
			} else {
				log.error("UnknownUserID: "
						+ usurd.getUpdateSingleUserRegistration().getUserID());
				WNSServiceException s = new WNSServiceException(
						WNSInitParamContainer.getInstance().getExceptionLevel());
				s.addCodedException(
						WNSServiceException.ExceptionCode.UnknownUserID, usurd
								.getUpdateSingleUserRegistration().getUserID());
				throw s;
			}
		}
		// Add protocol
		if (!unknownUser && usurd.getUpdateSingleUserRegistration()
				.isSetAddCommunicationProtocol()) {
			log.debug("Trying to add protocol");
			if (this.singleHashthable.containsKey(userID)) {
				WNSUser wnsUser = this.singleHashthable.get(userID);
				wnsUser.addProtocol(usurd.getUpdateSingleUserRegistration()
						.getAddCommunicationProtocol());

				WNSUserDocument.WNSUser user = this.userdoc.getWNSUser();
				WNSUserDocument.WNSUser.SingleUser single = user
						.getSingleUser();
				for (int i = 0; i < single.sizeOfUserArray(); i++) {
					if (single.getUserArray(i).getID()
							.equalsIgnoreCase(userID.toString())) {

						// IF the user has already channels
						if (single.getUserArray(i).getChannelArray().length > 0) {
							// EMAIL
							if (usurd.getUpdateSingleUserRegistration()
									.getAddCommunicationProtocol()
									.sizeOfEmailArray() > 0) {
								if (messageHandler
										.isHandlerAvailable(WNSConstants.EMAIL)) {
									boolean found = false;
									for (int j = 0; j < single.getUserArray(i)
											.getChannelArray().length; j++) {
										if (single.getUserArray(i)
												.getChannelArray(j)
												.getProtocol().equals(
														Channel.Protocol.EMAIL)) {
											found = true;
											for (int k = 0; k < usurd
													.getUpdateSingleUserRegistration()

													.getAddCommunicationProtocol()
													.sizeOfEmailArray(); k++) {
												single
														.getUserArray(i)
														.getChannelArray(j)
														.addTarget(
																usurd
																		.getUpdateSingleUserRegistration()
																		.getAddCommunicationProtocol()
																		.getEmailArray(
																				k));
											}

										}
									}
									if (!found) {
										Channel chan = single.getUserArray(i)
												.addNewChannel();
										chan
												.setProtocol(Channel.Protocol.EMAIL);
										chan
												.setTargetArray(usurd
														.getUpdateSingleUserRegistration()
														.getAddCommunicationProtocol()
														.getEmailArray());

									}

								}
							}
							// FAX
							if (usurd.getUpdateSingleUserRegistration()
									.getAddCommunicationProtocol()
									.sizeOfFaxArray() > 0) {
								if (messageHandler
										.isHandlerAvailable(WNSConstants.FAX)) {
									boolean found = false;

									for (int j = 0; j < single.getUserArray(i)
											.getChannelArray().length; j++) {
										if (single.getUserArray(i)
												.getChannelArray(j)
												.getProtocol().equals(
														Channel.Protocol.FAX)) {
											found = true;
											for (int x = 0; x < usurd
													.getUpdateSingleUserRegistration()
													.getAddCommunicationProtocol()
													.sizeOfFaxArray(); x++) {
												single
														.getUserArray(i)
														.getChannelArray(j)
														.addTarget(
																usurd
																		.getUpdateSingleUserRegistration()
																		.getAddCommunicationProtocol()
																		.getFaxArray(
																				x)
																		.toString());
											}
										}
									}

									if (!found) {
										Channel chan = single.getUserArray(i)
												.addNewChannel();
										chan.setProtocol(Channel.Protocol.FAX);
										for (int j = 0; j < usurd
												.getUpdateSingleUserRegistration()
												.getAddCommunicationProtocol()
												.sizeOfFaxArray(); j++) {
											chan
													.addTarget(usurd
															.getUpdateSingleUserRegistration()
															.getAddCommunicationProtocol()
															.getFaxArray(j)
															.toString());
										}

									}
								}
							}
							// SMS
							if (usurd.getUpdateSingleUserRegistration()
									.getAddCommunicationProtocol()
									.sizeOfSMSArray() > 0) {
								if (messageHandler
										.isHandlerAvailable(WNSConstants.SMS)) {
									boolean found = false;
									for (int j = 0; j < single.getUserArray(i)
											.getChannelArray().length; j++) {
										if (single.getUserArray(i)
												.getChannelArray(j)
												.getProtocol().equals(
														Channel.Protocol.SMS)) {
											found = true;
											for (int x = 0; x < usurd
													.getUpdateSingleUserRegistration()
													.getAddCommunicationProtocol()
													.getSMSArray().length; x++) {
												single
														.getUserArray(i)
														.getChannelArray(j)
														.addTarget(
																usurd
																		.getUpdateSingleUserRegistration()
																		.getAddCommunicationProtocol()
																		.getSMSArray(
																				x)
																		.toString());
											}
										}
									}
									if (!found) {
										Channel chan = single.getUserArray(i)
												.addNewChannel();
										chan.setProtocol(Channel.Protocol.SMS);

										for (int j = 0; j < usurd
												.getUpdateSingleUserRegistration()
												.getAddCommunicationProtocol()
												.sizeOfSMSArray(); j++) {
											chan
													.addTarget(usurd
															.getUpdateSingleUserRegistration()
															.getAddCommunicationProtocol()
															.getSMSArray(j)
															.toString());
										}

									}
								}
							}
							// PHONE
							if (usurd.getUpdateSingleUserRegistration()
									.getAddCommunicationProtocol()
									.sizeOfPhoneArray() > 0) {
								if (messageHandler
										.isHandlerAvailable(WNSConstants.PHONE)) {
									boolean found = false;
									for (int j = 0; j < single.getUserArray(i)
											.getChannelArray().length; j++) {
										if (single.getUserArray(i)
												.getChannelArray(j)
												.getProtocol().equals(
														Channel.Protocol.PHONE)) {
											found = true;
											for (int x = 0; x < usurd
													.getUpdateSingleUserRegistration()

													.getAddCommunicationProtocol()
													.sizeOfPhoneArray(); x++) {
												single
														.getUserArray(i)
														.getChannelArray(j)
														.addTarget(
																usurd
																		.getUpdateSingleUserRegistration()

																		.getAddCommunicationProtocol()
																		.getPhoneArray(
																				x)
																		.toString());
											}
										}
									}
									if (!found) {
										Channel chan = single.getUserArray(i)
												.addNewChannel();
										chan
												.setProtocol(Channel.Protocol.PHONE);
										for (int j = 0; j < usurd
												.getUpdateSingleUserRegistration()

												.getAddCommunicationProtocol()
												.sizeOfPhoneArray(); j++) {
											chan
													.addTarget(usurd
															.getUpdateSingleUserRegistration()

															.getAddCommunicationProtocol()
															.getPhoneArray(j)
															.toString());
										}

									}
								}
							}
							// XMMP
							if (usurd.getUpdateSingleUserRegistration()
									.getAddCommunicationProtocol()
									.sizeOfXMPPArray() > 0) {
								if (messageHandler
										.isHandlerAvailable(WNSConstants.XMPP)) {
									boolean found = false;
									for (int j = 0; j < single.getUserArray(i)
											.getChannelArray().length; j++) {
										if (single.getUserArray(i)
												.getChannelArray(j)
												.getProtocol().equals(
														Channel.Protocol.XMPP)) {
											found = true;
											for (int k = 0; k < usurd
													.getUpdateSingleUserRegistration()

													.getAddCommunicationProtocol()
													.sizeOfXMPPArray(); k++) {
												single
														.getUserArray(i)
														.getChannelArray(j)
														.addTarget(
																usurd
																		.getUpdateSingleUserRegistration()

																		.getAddCommunicationProtocol()
																		.getXMPPArray(
																				k));
											}

										}
									}
									if (!found) {
										Channel chan = single.getUserArray(i)
												.addNewChannel();
										chan.setProtocol(Channel.Protocol.XMPP);
										chan
												.setTargetArray(usurd
														.getUpdateSingleUserRegistration()

														.getAddCommunicationProtocol()
														.getXMPPArray());

									}
								}
							}
							// HTTP
							if (usurd.getUpdateSingleUserRegistration()
									.getAddCommunicationProtocol()
									.sizeOfHTTPArray() > 0) {
								boolean found = false;
								for (int j = 0; j < single.getUserArray(i)
										.getChannelArray().length; j++) {
									if (single.getUserArray(i).getChannelArray(
											j).getProtocol().equals(
											Channel.Protocol.HTTP)) {
										found = true;
										for (int k = 0; k < usurd
												.getUpdateSingleUserRegistration()

												.getAddCommunicationProtocol()
												.sizeOfHTTPArray(); k++) {
											single
													.getUserArray(i)
													.getChannelArray(j)
													.addTarget(
															usurd
																	.getUpdateSingleUserRegistration()

																	.getAddCommunicationProtocol()
																	.getHTTPArray(
																			k));
										}

									}
								}
								if (!found) {
									Channel chan = single.getUserArray(i)
											.addNewChannel();
									chan.setProtocol(Channel.Protocol.HTTP);
									chan.setTargetArray(usurd
											.getUpdateSingleUserRegistration()

											.getAddCommunicationProtocol()
											.getHTTPArray());

								}
							}
						} else {
							// There are no previous channels registered to the
							// user
							if (usurd.getUpdateSingleUserRegistration()
									.getAddCommunicationProtocol()
									.sizeOfEmailArray() > 0) {
								if (messageHandler
										.isHandlerAvailable(WNSConstants.EMAIL)) {
									Channel chan = single.getUserArray(i)
											.addNewChannel();
									chan.setProtocol(Channel.Protocol.EMAIL);
									chan.setTargetArray(usurd
											.getUpdateSingleUserRegistration()

											.getAddCommunicationProtocol()
											.getEmailArray());
								}
							}
							if (usurd.getUpdateSingleUserRegistration()
									.getAddCommunicationProtocol()
									.sizeOfFaxArray() > 0) {
								if (messageHandler
										.isHandlerAvailable(WNSConstants.FAX)) {
									Channel chan = single.getUserArray(i)
											.addNewChannel();
									chan.setProtocol(Channel.Protocol.FAX);
									String[] temp = new String[usurd
											.getUpdateSingleUserRegistration()

											.getAddCommunicationProtocol()
											.sizeOfFaxArray()];
									for (int j = 0; j < temp.length; j++) {
										temp[j] = usurd
												.getUpdateSingleUserRegistration()

												.getAddCommunicationProtocol()
												.getFaxArray(j).toString();
									}
									chan.setTargetArray(temp);
								}
							}
							if (usurd.getUpdateSingleUserRegistration()
									.getAddCommunicationProtocol()
									.sizeOfSMSArray() > 0) {
								if (messageHandler
										.isHandlerAvailable(WNSConstants.SMS)) {
									Channel chan = single.getUserArray(i)
											.addNewChannel();
									chan.setProtocol(Channel.Protocol.SMS);
									String[] temp = new String[usurd
											.getUpdateSingleUserRegistration()

											.getAddCommunicationProtocol()
											.sizeOfSMSArray()];
									for (int j = 0; j < temp.length; j++) {
										temp[j] = usurd
												.getUpdateSingleUserRegistration()

												.getAddCommunicationProtocol()
												.getSMSArray(j).toString();
									}
									chan.setTargetArray(temp);
								}
							}
							if (usurd.getUpdateSingleUserRegistration()
									.getAddCommunicationProtocol()
									.sizeOfPhoneArray() > 0) {
								if (messageHandler
										.isHandlerAvailable(WNSConstants.PHONE)) {
									Channel chan = single.getUserArray(i)
											.addNewChannel();
									chan.setProtocol(Channel.Protocol.PHONE);
									String[] temp = new String[usurd
											.getUpdateSingleUserRegistration()

											.getAddCommunicationProtocol()
											.sizeOfPhoneArray()];
									for (int j = 0; j < temp.length; j++) {
										temp[j] = usurd
												.getUpdateSingleUserRegistration()

												.getAddCommunicationProtocol()
												.getPhoneArray(j).toString();
									}
									chan.setTargetArray(temp);
								}
							}
							if (usurd.getUpdateSingleUserRegistration()
									.getAddCommunicationProtocol()
									.sizeOfXMPPArray() > 0) {
								if (messageHandler
										.isHandlerAvailable(WNSConstants.XMPP)) {
									Channel chan = single.getUserArray(i)
											.addNewChannel();
									chan.setProtocol(Channel.Protocol.XMPP);
									chan.setTargetArray(usurd
											.getUpdateSingleUserRegistration()

											.getAddCommunicationProtocol()
											.getXMPPArray());
								}
							}
							if (usurd.getUpdateSingleUserRegistration()
									.getAddCommunicationProtocol()
									.sizeOfHTTPArray() > 0) {
								Channel chan = single.getUserArray(i)
										.addNewChannel();
								chan.setProtocol(Channel.Protocol.HTTP);
								chan.setTargetArray(usurd
										.getUpdateSingleUserRegistration()

										.getAddCommunicationProtocol()
										.getHTTPArray());
							}
						}
						i = single.sizeOfUserArray() + 1;
					}
				}

				try {
					this.userDAO.storeWNSUserDocument(this.userdoc);
				} catch (WNSException e) {
					WNSServiceException service = new WNSServiceException(
							WNSInitParamContainer.getInstance()
									.getExceptionLevel());
					service.addCodedException(ExceptionCode.NoApplicable,
							"WNSUserhandler", e);
					throw service;
				}
				// usurrd.addNewUpdateSingleUserRegistrationResponse().setStatus(
				// Status.SUCCESS);
				log.debug("Update successful");
				// return usurrd;
			} else {
				log.error("UnknownUserID: "
						+ usurd.getUpdateSingleUserRegistration().getUserID());
				WNSServiceException s = new WNSServiceException(
						WNSInitParamContainer.getInstance().getExceptionLevel());
				s.addCodedException(
						WNSServiceException.ExceptionCode.UnknownUserID, usurd
								.getUpdateSingleUserRegistration().getUserID());
				throw s;
			}

		}
		usurrd.addNewUpdateSingleUserRegistrationResponse().setStatus(
				Status.SUCCESS);
		return usurrd;
	}

	/**
	 * MAIN function to update a MultiUser
	 * 
	 * @param umurd
	 *            The XML document
	 * @return The UpdateMultiUserRegistrationResponseDocument
	 * @throws WNSServiceException
	 */
	// FIXME WORKAROUND BUG 511 einbauen
	public synchronized UpdateMultiUserRegistrationResponseDocument updateMultiUser(
			UpdateMultiUserRegistrationDocument umurd)
			throws WNSServiceException {
		UpdateMultiUserRegistrationResponseDocument response = UpdateMultiUserRegistrationResponseDocument.Factory
				.newInstance();
		response.addNewUpdateMultiUserRegistrationResponse().setStatus(
				UpdateMultiUserRegistrationResponseType.Status.SUCCESS);
		WNSUserDocument.WNSUser user = this.userdoc.getWNSUser();
		// Updating the hashtable
		if (this.multiUserHashtable.containsKey(new Long(umurd
				.getUpdateMultiUserRegistration().getMultiUserID()))) {
			WNSUser wnsUser = this.multiUserHashtable.get(new Long(umurd
					.getUpdateMultiUserRegistration().getMultiUserID()));
			// Adding User
			if (umurd.getUpdateMultiUserRegistration().isSetAddUser()) {
				log.debug("Trying to add user");
				String[] unknownIDs = new String[umurd
						.getUpdateMultiUserRegistration().getAddUser()
						.sizeOfIDArray()];
				int tempIndex = 0;
				for (int i = 0; i < umurd.getUpdateMultiUserRegistration()
						.getAddUser().sizeOfIDArray(); i++) {
					if (this.singleHashthable.containsKey(new Long(umurd
							.getUpdateMultiUserRegistration().getAddUser()
							.getIDArray(i)))
							|| this.multiUserHashtable.containsKey(new Long(
									umurd.getUpdateMultiUserRegistration()
											.getAddUser().getIDArray(i)))) {
						i = umurd.getUpdateMultiUserRegistration().getAddUser()
								.sizeOfIDArray();
					} else {
						// if (multiUserHashtable.containsKey(new Long(umurd
						// .getUpdateMultiUserRegistration().getAction()
						// .getAddUser().getIDArray(i)))) {
						// i = umurd.getUpdateMultiUserRegistration()
						// .getAction().getAddUser().sizeOfIDArray();
						// } else {
						unknownIDs[tempIndex] = umurd
								.getUpdateMultiUserRegistration().getAddUser()
								.getIDArray(i);
						tempIndex++;

						// }
					}
				}
				if (tempIndex > 0) {
					String[] temp = new String[tempIndex];
					for (int i = 0; i < tempIndex; i++) {
						temp[i] = unknownIDs[i];
					}
					WNSServiceException service = new WNSServiceException(
							WNSInitParamContainer.getInstance()
									.getExceptionLevel());
					service.addCodedException(ExceptionCode.UnknownUserID, temp
							.toString());
					log.error("Unknown UserID");
					throw service;

				}

				// Check if circular dependencies occur
				// The checking works like this:
				// 1. Check every associated user if it is an multiUser, if so
				// then add it into an vector.
				// 2. Check the users that should be added to the multiUser if
				// there is any multiUser, if so check recursive the associated
				// users if there are multiUser. If there is a multiUser compare
				// the id to those in the vector containig the already added
				// multiUsers
				Vector<String> doubleUsers = new Vector<String>();
				// Load all associated users that are MultiUser, even the own id
				doubleUsers.addElement(umurd.getUpdateMultiUserRegistration()
						.getMultiUserID());
				for (int i = 0; i < wnsUser.getAssociatedUsers()
						.sizeOfIDArray(); i++) {
					if (this.multiUserHashtable.containsKey(new Long(wnsUser
							.getAssociatedUsers().getIDArray(i)))) {
						doubleUsers.addElement(wnsUser.getAssociatedUsers()
								.getIDArray(i));
					}

				}
				// Iterate over every user that sould be added
				for (int i = 0; i < umurd.getUpdateMultiUserRegistration()
						.getAddUser().sizeOfIDArray(); i++) {
					// If it is a MultiUser check the associatedUsers
					if (this.multiUserHashtable.containsKey(new Long(umurd
							.getUpdateMultiUserRegistration().getAddUser()
							.getIDArray(i)))) {

						for (int j = 0; j < unknownIDs.length; j++) {

						}
						// Check if the MultiUser is already an associated user
						if (doubleUsers.contains(umurd
								.getUpdateMultiUserRegistration().getAddUser()
								.getIDArray(i))) {
							log.error("Circular dependency found in user: "
									+ umurd.getUpdateMultiUserRegistration()
											.getAddUser().getIDArray(i));

							WNSServiceException service = new WNSServiceException(
									WNSInitParamContainer.getInstance()
											.getExceptionLevel());
							service
									.addCodedException(
											ExceptionCode.NoApplicable,
											null,
											"The adding of the MultiUser "
													+ umurd
															.getUpdateMultiUserRegistration()
															.getAddUser()
															.getIDArray(i)
													+ " would lead to a circular dependency in the user file!");
							throw service;
						}

						try {
							this.recursiveChecking(umurd
									.getUpdateMultiUserRegistration()
									.getAddUser().getIDArray(i), doubleUsers);
						} catch (WNSException e) {
							log.error("Circular dependency found in user: "
									+ umurd.getUpdateMultiUserRegistration()
											.getAddUser().getIDArray(i));

							WNSServiceException service = new WNSServiceException(
									WNSInitParamContainer.getInstance()
											.getExceptionLevel());
							service.addCodedException(
									ExceptionCode.NoApplicable, null, e);
							throw service;
						}
					}
				}

				wnsUser.addMultiUser(umurd.getUpdateMultiUserRegistration()
						.getAddUser());

				// Updating the user.xml file
				WNSUserDocument.WNSUser.MultiUser multi = user
						.getMultiUser();
				// Workaround if one of the new users are already part of the
				// multiuser

				for (int i = 0; i < multi.sizeOfUserArray(); i++) {
					if (multi.getUserArray(i).getID().equalsIgnoreCase(
							umurd.getUpdateMultiUserRegistration()
									.getMultiUserID())) {
						Vector<Integer> vector = new Vector<Integer>();
						for (int y = 0; y < multi.getUserArray(i)
								.getAssociatedUsers().sizeOfIDArray(); y++) {
							for (int x = 0; x < umurd
									.getUpdateMultiUserRegistration()
									.getAddUser().sizeOfIDArray(); x++) {
								if (multi
										.getUserArray(i)
										.getAssociatedUsers()
										.getIDArray(y)
										.equalsIgnoreCase(
												umurd
														.getUpdateMultiUserRegistration()

														.getAddUser()
														.getIDArray(x))) {
									vector.addElement(x);

								}
							}
						}
						for (int j = 0; j < umurd
								.getUpdateMultiUserRegistration().getAddUser()
								.sizeOfIDArray(); j++) {
							if (!vector.contains(j)) {
								multi
										.getUserArray(i)
										.getAssociatedUsers()
										.addID(
												umurd
														.getUpdateMultiUserRegistration()

														.getAddUser()
														.getIDArray(j));
							}

						}
					}
				}
				log.debug("Update successful");
			}

			// Removing User
			if (umurd.getUpdateMultiUserRegistration().isSetRemoveUser()) {
				log.debug("Trying to remove user");
				String[] notfound = wnsUser.removeMultiUser(umurd
						.getUpdateMultiUserRegistration().getRemoveUser());
				if (notfound != null) {
					log.error("Unknown UserIDs. The user will not be removed");
				}

				// Updating the user.xml file
				WNSUserDocument.WNSUser.MultiUser multi = user
						.getMultiUser();
				for (int i = 0; i < multi.sizeOfUserArray(); i++) {
					if (multi.getUserArray(i).getID().equalsIgnoreCase(
							umurd.getUpdateMultiUserRegistration()
									.getMultiUserID())) {
						for (int j = 0; j < multi.getUserArray(i)
								.getAssociatedUsers().sizeOfIDArray(); j++) {
							for (int k = 0; k < umurd
									.getUpdateMultiUserRegistration()
									.getRemoveUser().sizeOfIDArray(); k++) {
								if (multi
										.getUserArray(i)
										.getAssociatedUsers()
										.getIDArray(j)
										.equalsIgnoreCase(
												umurd
														.getUpdateMultiUserRegistration()

														.getRemoveUser()
														.getIDArray(k))) {
									multi.getUserArray(i).getAssociatedUsers()
											.removeID(j);
								}
							}
						}
					}
				}
				log.debug("Update successful");
			}

		} else {
			// UserID not found in hashtable
			WNSServiceException service = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			service.addCodedException(ExceptionCode.UnknownUserID, umurd
					.getUpdateMultiUserRegistration().getMultiUserID());
			log.error("Unknown UserID");
			throw service;

		}
		try {
			this.userDAO.storeWNSUserDocument(this.userdoc);
		} catch (WNSException e) {
			WNSServiceException service = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			service.addCodedException(ExceptionCode.NoApplicable,
					"WNSUserhandler", e);
			throw service;
		}
		log.debug("Update successful");
		return response;
	}
	
	// method to check, if the user id is long or not
	// we use only long user ids => NumberFormatExceptions => unknow user
	private Long parseUserID(String userID) {
		if(log.isDebugEnabled()) {
			log.debug("try to parse user id: " + userID);
		}
		try {
			Long userId = new Long(userID);
			return userId;
		} catch (NumberFormatException e) {
			if(log.isDebugEnabled()) {
				log.debug("Could not parse user id",e);
			}
		}
		return null;
	}

	/**
	 * MAIN function to delete/ unregister a user
	 * 
	 * @param ud
	 * @return The UnregisterResponseDocument
	 * @throws WNSServiceException
	 */
	public synchronized UnregisterResponseDocument unregister(
			UnregisterDocument ud) throws WNSServiceException {
		UnregisterResponseDocument response = UnregisterResponseDocument.Factory
				.newInstance();
		WNSUserDocument.WNSUser user = this.userdoc.getWNSUser();
		log.debug("Trying to unregister user: " + ud.getUnregister().getID());
		// FIXME If userID being not a number but a textual ID this causes a numberformat exception
		// WORKAROUND BUG 511
		// @see https://bugzilla.52north.org/show_bug.cgi?id=511
		// 
		// kann die id in long geparsed werden
		// wenn ja, dann normaler verlauf
		// wenn nein, dann meldung unknown-user
		Long userID = this.parseUserID(ud.getUnregister().getID());
		boolean unknownUser = (userID==null?true:false);
		if (!unknownUser && this.singleHashthable.containsKey(userID)) {
			WNSUserDocument.WNSUser.SingleUser single = user
					.getSingleUser();
			for (int i = 0; i <= single.sizeOfUserArray() - 1; i++) {
				if (single.getUserArray(i).getID().equalsIgnoreCase(userID.toString())) {
					single.removeUser(i);
					this.singleHashthable.remove(userID.toString());
					this.checkMultiUser(userID.toString());
				}
			}

		} else if (!unknownUser && this.multiUserHashtable.containsKey(userID)) {
			WNSUserDocument.WNSUser.MultiUser multi = user
			.getMultiUser();
			for (int i = 0; i <= multi.sizeOfUserArray() - 1; i++) {
				if (multi.getUserArray(i).getID().equalsIgnoreCase(userID.toString())) {
					multi.removeUser(i);
					this.multiUserHashtable.remove(new Long(ud
							.getUnregister().getID()));
					this.checkMultiUser(userID.toString());
				}
			}
		} else {
			WNSServiceException service = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			service.addCodedException(ExceptionCode.UnknownUserID, ud
					.getUnregister().getID());
			log.error("UNKNOWN_ID: " + ud.getUnregister().getID());
			throw service;

		}


		try {
			this.userDAO.storeWNSUserDocument(this.userdoc);
		} catch (WNSException e) {
			WNSServiceException service = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			service.addCodedException(ExceptionCode.NoApplicable,
					"WNSUserhandler", e);
			throw service;
		}
		response.addNewUnregisterResponse().setStatus(
				UnregisterResponseType.Status.SUCCESS);
		log.debug("Unregister successful");
		return response;
	}

	private void checkMultiUser(String id) throws WNSServiceException {
		WNSUserDocument.WNSUser user = this.userdoc.getWNSUser();
		WNSUserDocument.WNSUser.MultiUser multi = user
				.getMultiUser();
		WNSUserDocument.WNSUser.MultiUser.User multiuser;

		// Hashtable
		Iterator elem;
		int temp = 0;
		if (this.multiUserHashtable.elements().hasMoreElements()) {
			elem = (Iterator) this.multiUserHashtable.elements();
		} else {
			return;
		}

		while (temp < this.multiUserHashtable.size()) {
			temp++;
			WNSUser wnsuser = (WNSUser) elem.next();
			for (int i = 0; i < wnsuser.getAssociatedUsers().sizeOfIDArray(); i++) {
				if (wnsuser.getAssociatedUsers().getIDArray(i)
						.equalsIgnoreCase(id)) {
					wnsuser.removeMultiUser(id);
				}
			}
		}
		// WNSUserDocument
		for (int i = 0; i < multi.sizeOfUserArray(); i++) {
			multiuser = multi.getUserArray(i);
			for (int j = 0; j < multiuser.getAssociatedUsers().sizeOfIDArray(); j++) {
				if (multiuser.getAssociatedUsers().getIDArray(j)
						.equalsIgnoreCase(id)) {
					multiuser.getAssociatedUsers().removeID(j);
				}
			}

		}
		try {
			this.userDAO.storeWNSUserDocument(this.userdoc);
		} catch (WNSException e) {
			log.error(e.toString());
			WNSServiceException service = new WNSServiceException(
					WNSInitParamContainer.getInstance().getExceptionLevel());
			service.addCodedException(ExceptionCode.NoApplicable,
					"WNSUserhandler", e);
			throw service;
		}

	}

	/**
	 * 
	 * 
	 * @param userID
	 * @return The array of Channel[] where a user is registered to
	 * @throws WNSException
	 */
	@SuppressWarnings("serial")
	public synchronized Channel[] getMessageChannels(String userID)
			throws WNSException {
		if (this.singleHashthable.containsKey(new Long(userID))) {
			return (this.singleHashthable.get(new Long(userID))).getChannels();
		} else {
			if (this.multiUserHashtable.containsKey(new Long(userID))) {
				WNSUserDocument.WNSUser.SingleUser.User user = WNSUserDocument.WNSUser.SingleUser.User.Factory
						.newInstance();
				Channel email = user.addNewChannel();
				email.setProtocol(Channel.Protocol.EMAIL);
				Channel fax = user.addNewChannel();
				fax.setProtocol(Channel.Protocol.FAX);
				Channel phone = user.addNewChannel();
				phone.setProtocol(Channel.Protocol.PHONE);
				Channel sms = user.addNewChannel();
				sms.setProtocol(Channel.Protocol.SMS);
				Channel xmpp = user.addNewChannel();
				xmpp.setProtocol(Channel.Protocol.XMPP);
				Channel http = user.addNewChannel();
				http.setProtocol(Channel.Protocol.HTTP);
				WNSUser wnsuser = (this.multiUserHashtable
						.get(new Long(userID)));
				for (int i = 0; i < wnsuser.getAssociatedUsers()
						.sizeOfIDArray(); i++) {
					WNSUser singleUser = this.singleHashthable.get(Long
							.parseLong(wnsuser.getAssociatedUsers().getIDArray(
									i)));
					// if the given ID is a SingleUser Id
					if (singleUser != null) {
						Channel[] chan = singleUser.getChannels();
						for (int j = 0; j < chan.length; j++) {
							if (chan[j].getProtocol().equals(
									Channel.Protocol.EMAIL)) {
								for (int k = 0; k < chan[j].getTargetArray().length; k++) {
									email.addTarget(chan[j].getTargetArray(k));
								}
							}
							if (chan[j].getProtocol().equals(
									Channel.Protocol.FAX)) {
								for (int k = 0; k < chan[j].getTargetArray().length; k++) {
									fax.addTarget(chan[j].getTargetArray(k));
								}
							}
							if (chan[j].getProtocol().equals(
									Channel.Protocol.PHONE)) {
								for (int k = 0; k < chan[j].getTargetArray().length; k++) {
									phone.addTarget(chan[j].getTargetArray(k));
								}
							}
							if (chan[j].getProtocol().equals(
									Channel.Protocol.SMS)) {
								for (int k = 0; k < chan[j].getTargetArray().length; k++) {
									sms.addTarget(chan[j].getTargetArray(k));
								}
							}
							if (chan[j].getProtocol().equals(
									Channel.Protocol.XMPP)) {
								for (int k = 0; k < chan[j].getTargetArray().length; k++) {
									xmpp.addTarget(chan[j].getTargetArray(k));
								}
							}
							if (chan[j].getProtocol().equals(
									Channel.Protocol.HTTP)) {
								for (int k = 0; k < chan[j].getTargetArray().length; k++) {
									http.addTarget(chan[j].getTargetArray(k));
								}
							}
						}
						// if the ID is a MultiUser ID
					} else {
						this.getChannels(wnsuser.getAssociatedUsers()
								.getIDArray(i), email, fax, phone, sms, xmpp,
								http);
					}
				}
				// remove unnessacary channel elemets
				if (user.getChannelArray(5).sizeOfTargetArray() == 0) {
					user.removeChannel(5);
				}
				if (user.getChannelArray(4).sizeOfTargetArray() == 0) {
					user.removeChannel(4);
				}
				if (user.getChannelArray(3).sizeOfTargetArray() == 0) {
					user.removeChannel(3);
				}
				if (user.getChannelArray(2).sizeOfTargetArray() == 0) {
					user.removeChannel(2);
				}
				if (user.getChannelArray(1).sizeOfTargetArray() == 0) {
					user.removeChannel(1);
				}
				if (user.getChannelArray(0).sizeOfTargetArray() == 0) {
					user.removeChannel(0);
				}

				return user.getChannelArray();
			}
		}
		throw new WNSException("No User found");
	}

	private void initUsers() {
		log.debug("init users");
		for (int i = 0; i <= this.userdoc.getWNSUser().getSingleUser()
				.getUserArray().length - 1; i++) {
			WNSUser user = new WNSUser(this.userdoc.getWNSUser()
					.getSingleUser().getUserArray(i));

			if (user.getUserID() > this.userIDs) {
				this.userIDs = user.getUserID();
			}
			this.singleHashthable.put(new Long(user.getUserID()), user);
		}
		for (int i = 0; i <= this.userdoc.getWNSUser().getMultiUser()
				.getUserArray().length - 1; i++) {
			WNSUser user = new WNSUser(this.userdoc.getWNSUser().getMultiUser()
					.getUserArray(i));
			if (user.getUserID() > this.userIDs) {
				this.userIDs = user.getUserID();
			}
			this.multiUserHashtable.put(new Long(user.getUserID()), user);
		}
	}

	/**
	 * Checks if a circular dependency would occur if a user would be added, it
	 * works recursiv
	 * 
	 * @param multiUserId
	 *            The String of the multiUser ID
	 * @param doubleUsers
	 *            A vector containing the already added multiUser IDs as String
	 * @throws WNSException
	 *             If the user that sould be added would lead to a circular
	 *             dependency
	 */
	private void recursiveChecking(String multiUserId,
			Vector<String> doubleUsers) throws WNSException {
		log.debug("Checking for possible circular dependencies");
		WNSUser wnsUser = this.multiUserHashtable.get(new Long(multiUserId));
		for (int i = 0; i < wnsUser.getAssociatedUsers().sizeOfIDArray(); i++) {
			if (this.multiUserHashtable.containsKey(new Long(wnsUser
					.getAssociatedUsers().getIDArray(i)))) {
				if (doubleUsers.contains(wnsUser.getAssociatedUsers()
						.getIDArray(i))) {
					log.error("Dependency found");
					throw new WNSException("By trying to adding multiUser "
							+ multiUserId
							+ " a circular dependecy would appear!");
				} else {
					this.recursiveChecking(wnsUser.getAssociatedUsers()
							.getIDArray(i), doubleUsers);
				}
			}
		}
		log.debug("No dependency found, that's good");
	}

	/**
	 * This method gets every messaging channel, it works recursiv
	 * 
	 * @param userID
	 *            The String of the userId
	 * @param email
	 *            The Channel element of email where to add the new adress
	 * @param fax
	 *            The Channel element of fax where to add the new adress
	 * @param phone
	 *            The Channel element of phone where to add the new adress
	 * @param sms
	 *            The Channel element of sms where to add the new adress
	 * @param xmpp
	 *            The Channel element of xmpp where to add the new adress
	 * @param http
	 *            The Channel element of http where to add the new adress
	 */
	private void getChannels(String userID, Channel email, Channel fax,
			Channel phone, Channel sms, Channel xmpp, Channel http) {
		log.debug("Trying to get the message channels");
		WNSUser wnsuser = (this.multiUserHashtable.get(new Long(userID)));
		for (int i = 0; i < wnsuser.getAssociatedUsers().sizeOfIDArray(); i++) {
			WNSUser singleUser = this.singleHashthable.get(Long
					.parseLong(wnsuser.getAssociatedUsers().getIDArray(i)));
			// if the given ID is a SingleUser Id
			if (singleUser != null) {
				Channel[] chan = singleUser.getChannels();
				for (int j = 0; j < chan.length; j++) {
					if (chan[j].getProtocol().equals(Channel.Protocol.EMAIL)) {
						for (int k = 0; k < chan[j].getTargetArray().length; k++) {
							email.addTarget(chan[j].getTargetArray(k));
						}
					}
					if (chan[j].getProtocol().equals(Channel.Protocol.FAX)) {
						for (int k = 0; k < chan[j].getTargetArray().length; k++) {
							fax.addTarget(chan[j].getTargetArray(k));
						}
					}
					if (chan[j].getProtocol().equals(Channel.Protocol.PHONE)) {
						for (int k = 0; k < chan[j].getTargetArray().length; k++) {
							phone.addTarget(chan[j].getTargetArray(k));
						}
					}
					if (chan[j].getProtocol().equals(Channel.Protocol.SMS)) {
						for (int k = 0; k < chan[j].getTargetArray().length; k++) {
							sms.addTarget(chan[j].getTargetArray(k));
						}
					}
					if (chan[j].getProtocol().equals(Channel.Protocol.XMPP)) {
						for (int k = 0; k < chan[j].getTargetArray().length; k++) {
							xmpp.addTarget(chan[j].getTargetArray(k));
						}
					}
					if (chan[j].getProtocol().equals(Channel.Protocol.HTTP)) {
						for (int k = 0; k < chan[j].getTargetArray().length; k++) {
							http.addTarget(chan[j].getTargetArray(k));
						}
					}
				}
				// if the ID is a MultiUser ID recursiv call of the method again
			} else {
				this.getChannels(wnsuser.getAssociatedUsers().getIDArray(i),
						email, fax, phone, sms, xmpp, http);
			}
		}
		log.debug("Message channels retrieved successully");
	}
}
