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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.opengis.ows.CapabilitiesBaseType;
import net.opengis.ows.SectionsType;
import net.opengis.wns.x00.CapabilitiesDocument;
import net.opengis.wns.x00.CapabilitiesDocument.Capabilities;
import net.opengis.wns.x00.ContentsDocument.Contents;
import net.opengis.wns.x00.DoNotificationDocument;
import net.opengis.wns.x00.DoNotificationResponseDocument;
import net.opengis.wns.x00.GetCapabilitiesDocument;
import net.opengis.wns.x00.GetCapabilitiesDocument.GetCapabilities;
import net.opengis.wns.x00.GetMessageDocument;
import net.opengis.wns.x00.GetMessageResponseDocument;
import net.opengis.wns.x00.ProtocolsType;
import net.opengis.wns.x00.RegisterDocument;
import net.opengis.wns.x00.RegisterResponseDocument;
import net.opengis.wns.x00.UnregisterDocument;
import net.opengis.wns.x00.UnregisterResponseDocument;
import net.opengis.wns.x00.UpdateMultiUserRegistrationDocument;
import net.opengis.wns.x00.UpdateMultiUserRegistrationResponseDocument;
import net.opengis.wns.x00.UpdateSingleUserRegistrationDocument;
import net.opengis.wns.x00.UpdateSingleUserRegistrationResponseDocument;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.n52.wns.WNSServiceException.ExceptionCode;
import org.n52.wns.WNSServiceException.ExceptionLevel;
import org.n52.wns.db.DAOFactory;
import org.n52.wns.db.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.x52North.wns.v2.WNSConfigDocument.WNSConfig;
import org.x52North.wns.v2.WNSUserDocument;

/**
 * RequestHandler handles all incoming requests
 * 
 * @author Johannes Echterhoff, Dennis Dahlmann
 * @version 2.0
 */

public class WNSRequestHandler {

	private static Logger log = LoggerFactory.getLogger(WNSRequestHandler.class);

	private String ogcExcSchemaLocation;

	private String wnsSchemaLocation;

	private ExceptionLevel excLevel;

	private WNSConfig config;

	public WNSRequestHandler() {
		WNSInitParamContainer initParams = WNSInitParamContainer.getInstance();

		this.config = initParams.getWnsConfig();

		this.ogcExcSchemaLocation = initParams.getOgcExceptionSchemaLocation();
		this.wnsSchemaLocation = initParams.getWnsSchemaLocation();
		this.excLevel = initParams.getExceptionLevel();
	}

	public void handleGetRequest(HttpServletRequest request,
			HttpServletResponse response) throws WNSException {

		WNSServiceException se = new WNSServiceException(this.excLevel);
		boolean requestFound = false;

		Enumeration params = request.getParameterNames();
		while (params.hasMoreElements()) {

			// search for the request-parameter to determine which operation
			// shall be performed
			String p = (String) params.nextElement();

			if (p.equalsIgnoreCase(WNSConstants.REQUEST)) {
				requestFound = true;
				String paramReq = request.getParameter(p);

				// Is it a LISTWNSUSER request???
				if (config.getServiceProperties().getDebugMode() && 
						paramReq.equalsIgnoreCase(WNSConstants.LISTWNSUSER)) {
					log.info("LISTWNSUSER request from: "
							+ request.getRemoteAddr());

					WNSInitParamContainer initParams = WNSInitParamContainer
							.getInstance();
					DAOFactory daofac = initParams.getDAOFactory();
					UserDAO userdao = null;
					try {
						userdao = daofac.getUserDAO();
					} catch (NullPointerException e) {
						log.error("Error during init USER DAO " + e.toString());
						throw new WNSException("Error during init USER DAO: "
								+ e.toString());
					}
					WNSUserDocument wnsud = userdao.getUserDocument();
					WNSUtil.sendResponse(wnsud, null, response);
				} else if (paramReq.equalsIgnoreCase(WNSConstants.GETMESSAGE)) {

				   WNSInitParamContainer initParams = WNSInitParamContainer
               .getInstance();
				   
					GetMessageResponseDocument gmrd;
					try {
						gmrd = initParams.getDAOFactory().getMessageDAO().getMessage(
								WNSUtil.getValue4Param("MessageID", request));
						WNSUtil.sendResponse(gmrd, this.wnsSchemaLocation,
								response);
					} catch (WNSServiceException e) {
						WNSUtil.sendResponse(e.getDocument(),
								this.ogcExcSchemaLocation, response);
					}
				} else
				// Check if it is a GetWSDL request
				if (paramReq.equalsIgnoreCase(WNSConstants.GETWSDL)) {
					log.info("GetWSDL request from: "+ request.getRemoteAddr());
					// check parameter SERVICE
					String service = WNSUtil.getValue4Param("Service", request);
					if (service == null) {
						log.error("The mandatory parameter 'SERVICE' was not found in the request");
						se.addCodedException(
										WNSServiceException.ExceptionCode.MissingParameterValue,
										"SERVICE",
										"The mandatory parameter 'SERVICE' was not found in the request");
						WNSUtil.sendResponse(se.getDocument(),
								this.ogcExcSchemaLocation, response);

					} else if (!service.equalsIgnoreCase(WNSConstants.SERVICE)) {
						log
								.error("The value of the mandatory parameter 'SERVICE'"
										+ "must be '"
										+ WNSConstants.SERVICE
										+ "'. Delivered value was: " + service);
						se
								.addCodedException(
										WNSServiceException.ExceptionCode.InvalidParameterValue,
										"SERVICE",
										"The value of the mandatory parameter 'SERVICE'"
												+ "must be '"
												+ WNSConstants.SERVICE
												+ "'. Delivered value was: "
												+ service);
						WNSUtil.sendResponse(se.getDocument(),
								this.ogcExcSchemaLocation, response);
					}
					// check parameter VERSION
					String version = WNSUtil.getValue4Param("VERSION", request);
					if (version == null) {
						log
								.error("The mandatory parameter 'VERSION' was not fo"
										+ "und in the request");
						se
								.addCodedException(
										WNSServiceException.ExceptionCode.MissingParameterValue,
										"VERSION",
										"The mandatory parameter 'VERSION' was not fo"
												+ "und in the request");
						WNSUtil.sendResponse(se.getDocument(),
								this.ogcExcSchemaLocation, response);

					} else if (!version.equalsIgnoreCase(WNSConstants.VERSION)) {
						log.error("The value of the mandatory parameter 'VERSION'"
										+ "must be '"
										+ WNSConstants.VERSION
										+ "'. Delivered value was: " + version);
						se.addCodedException(
										WNSServiceException.ExceptionCode.InvalidParameterValue,
										"VERSION",
										"The value of the mandatory parameter 'VERSION'"
												+ "must be '"
												+ WNSConstants.VERSION
												+ "'. Delivered value was: "
												+ version);
						WNSUtil.sendResponse(se.getDocument(),
								this.ogcExcSchemaLocation, response);
					}
					File wsdl = new File(WNSInitParamContainer.getInstance().getWSDLDocumentPath());
					FileReader reader;
					try {
						reader = new FileReader(wsdl);
						String text = "";
						int c;
						while ((c = reader.read()) != -1) {
							text += (char) c;
						}
						reader.close();
						WNSUtil.sendResponse(text, response);
					} catch (FileNotFoundException e) {
						WNSServiceException exce = new WNSServiceException(
								WNSInitParamContainer.getInstance()
										.getExceptionLevel());
						exce.addCodedException(ExceptionCode.NoApplicable,
								null, e.toString());
						WNSUtil.sendResponse(exce.getDocument(),
								this.ogcExcSchemaLocation, response);
					} catch (IOException e) {
						WNSServiceException exce = new WNSServiceException(
								WNSInitParamContainer.getInstance()
										.getExceptionLevel());
						exce.addCodedException(ExceptionCode.NoApplicable,
								null, e.toString());
						WNSUtil.sendResponse(exce.getDocument(),
								this.ogcExcSchemaLocation, response);
					}

				} else if (paramReq
						.equalsIgnoreCase(WNSConstants.GETCAPABILITIES)) {
					log.info("GETCAPABILITIES request from: "
							+ request.getRemoteAddr());

					// check parameter SERVICE
					String service = WNSUtil.getValue4Param("Service", request);
					if (service == null) {
						log
								.error("The mandatory parameter 'SERVICE' was not fo"
										+ "und in the request");
						se
								.addCodedException(
										WNSServiceException.ExceptionCode.MissingParameterValue,
										"SERVICE",
										"The mandatory parameter 'SERVICE' was not fo"
												+ "und in the request");
						WNSUtil.sendResponse(se.getDocument(),
								this.ogcExcSchemaLocation, response);

					} else if (!service.equalsIgnoreCase(WNSConstants.SERVICE)) {
						se
								.addCodedException(
										WNSServiceException.ExceptionCode.InvalidParameterValue,
										"SERVICE",
										"The value of the mandatory parameter 'SERVICE'"
												+ "must be '"
												+ WNSConstants.SERVICE
												+ "'. Delivered value was: "
												+ service);
						WNSUtil.sendResponse(se.getDocument(),
								this.ogcExcSchemaLocation, response);
					}

					// check parameter VERSION
					String version = WNSUtil.getValue4Param("VERSION", request);
					if (version == null) {
						log
								.error("The mandatory parameter 'VERSION' was not fo"
										+ "und in the request");
						se
								.addCodedException(
										WNSServiceException.ExceptionCode.MissingParameterValue,
										"VERSION",
										"The mandatory parameter 'VERSION' was not fo"
												+ "und in the request");
						WNSUtil.sendResponse(se.getDocument(),
								this.ogcExcSchemaLocation, response);

					} else if (!version.equalsIgnoreCase(WNSConstants.VERSION)) {
						log
								.error("The value of the mandatory parameter 'VERSION'"
										+ "must be '"
										+ WNSConstants.VERSION
										+ "'. Delivered value was: " + version);
						se
								.addCodedException(
										WNSServiceException.ExceptionCode.VersionNegotiationFailed,
										"SERVICE",
										"The value of the mandatory parameter 'VERSION'"
												+ "must be '"
												+ WNSConstants.VERSION
												+ "'. Delivered value was: "
												+ version);
						WNSUtil.sendResponse(se.getDocument(),
								this.ogcExcSchemaLocation, response);
					}

					// check parameter SECTIONS
					String sections = WNSUtil.getValue4Param("Sections",
							request);
					if (sections != null) {

						boolean all = false;
						boolean serviceIdentification = false;
						boolean serviceProvider = false;
						boolean operationsMetadata = false;
						boolean contents = false;

						boolean nothing = true;

						String[] tokens = sections.split(",");
						for (int i = 0; i < tokens.length; i++) {

							if (tokens[i].trim().equalsIgnoreCase("all")) {
								nothing = false;
								all = true;
								break;
							} else if (tokens[i].trim().equalsIgnoreCase(
									"serviceIdentification")) {
								nothing = false;
								serviceIdentification = true;
							} else if (tokens[i].trim().equalsIgnoreCase(
									"serviceProvider")) {
								nothing = false;
								serviceProvider = true;
							} else if (tokens[i].trim().equalsIgnoreCase(
									"operationsMetadata")) {
								nothing = false;
								operationsMetadata = true;
							} else if (tokens[i].trim().equalsIgnoreCase(
									"contents")) {
								nothing = false;
								contents = true;
							}
						}

						if (nothing || all) {
							// simply return all
							GetCapabilitiesDocument gcd = GetCapabilitiesDocument.Factory
									.newInstance();
							GetCapabilities gc = gcd.addNewGetCapabilities();
							gc.setService(WNSConstants.SERVICE);

							try {
								this.handleGetCapabilities(gcd, response);
							} catch (Exception e) {
								throw new WNSException(
										"Could not handle GetCapabilities request.",
										e);
							}
						} else {

							// create custom CapabilitiesResponse
							GetCapabilitiesDocument gcd = GetCapabilitiesDocument.Factory
									.newInstance();
							GetCapabilities gc = gcd.addNewGetCapabilities();
							gc.setService(WNSConstants.SERVICE);
							SectionsType st = gc.addNewSections();

							if (serviceIdentification) {
								st.addSection("serviceIdentification");
							}
							if (serviceProvider) {
								st.addSection("serviceProvider");
							}
							if (operationsMetadata) {
								st.addSection("operationsMetadata");
							}
							if (contents) {
								st.addSection("contents");
							}

							try {
								this.handleGetCapabilities(gcd, response);
							} catch (Exception e) {
								throw new WNSException(
										"Could not handle GetCapabilities request.",
										e);
							}
						}

					} else {
						// return whole document
						GetCapabilitiesDocument gcd = GetCapabilitiesDocument.Factory
								.newInstance();
						GetCapabilities gc = gcd.addNewGetCapabilities();
						gc.setService(WNSConstants.SERVICE);

						try {
							this.handleGetCapabilities(gcd, response);
						} catch (Exception e) {
							log
									.error("Could not handle GetCapabilities request: "
											+ e.toString());
							throw new WNSException(
									"Could not handle GetCapabilities request.",
									e);
						}
					}
				} else {
					log.error("OperationNotSupported: " + paramReq);
					se
							.addCodedException(
									WNSServiceException.ExceptionCode.OperationNotSupported,
									null,
									"The requested operation '"
											+ paramReq
											+ "' is"
											+ " not supported by the HTTP-GET binding");
					WNSUtil.sendResponse(se.getDocument(),
							this.ogcExcSchemaLocation, response);
				}
			}
		}

		if (!requestFound) {
			log.error("MissingParameterValue: REQUEST");
			se.addCodedException(
					WNSServiceException.ExceptionCode.MissingParameterValue,
					"REQUEST",
					"The mandatory parameter 'REQUEST' was not found in the "
							+ "request");
			WNSUtil.sendResponse(se.getDocument(), this.ogcExcSchemaLocation,
					response);
		}
	}

	public void handleGetCapabilities(GetCapabilitiesDocument gcd,
			HttpServletResponse response) throws WNSException {

		GetCapabilities gc = gcd.getGetCapabilities();
		WNSServiceException se = new WNSServiceException(this.excLevel);

		if (!gc.getService().equals(WNSConstants.SERVICE)) {
			se.addCodedException(
					WNSServiceException.ExceptionCode.InvalidParameterValue,
					"SERVICE", "You did not request the service '"
							+ WNSConstants.SERVICE + "'.");
			WNSUtil.sendResponse(se.getDocument(), this.ogcExcSchemaLocation,
					response);

		} else {

			if (gc.isSetSections()) {

				boolean all = false;
				boolean serviceIdentification = false;
				boolean serviceProvider = false;
				boolean operationsMetadata = false;
				boolean contents = false;
				boolean nothing = true;

				SectionsType st = gc.getSections();
				String[] sections = st.getSectionArray();
				for (int i = 0; i < sections.length; i++) {
					if (sections[i].trim().equalsIgnoreCase("all")) {
						nothing = false;
						all = true;
						break;
					} else if (sections[i].trim().equalsIgnoreCase(
							"serviceIdentification")) {
						nothing = false;
						serviceIdentification = true;
					} else if (sections[i].trim().equalsIgnoreCase(
							"serviceProvider")) {
						nothing = false;
						serviceProvider = true;
					} else if (sections[i].trim().equalsIgnoreCase(
							"operationsMetadata")) {
						nothing = false;
						operationsMetadata = true;
					} else if (sections[i].trim().equalsIgnoreCase("contents")) {
						nothing = false;
						contents = true;
					}
				}

				if (nothing || all) {
					// simply return all
					this.sendCompleteCapabilities(response);
				} else {

					CapabilitiesBaseType cbt = (CapabilitiesBaseType) this.config
							.getServiceProperties()
							.getCapabilitiesBaseInformation();

					// create custom CapabilitiesResponse
					CapabilitiesDocument capsd = CapabilitiesDocument.Factory
							.newInstance();
					Capabilities caps = capsd.addNewCapabilities();

					caps.setVersion(cbt.getVersion());
					caps.setUpdateSequence(cbt.getUpdateSequence());
					if (serviceIdentification) {
						caps.setServiceIdentification(cbt
								.getServiceIdentification());
					}
					if (serviceProvider) {
						caps.setServiceProvider(cbt.getServiceProvider());
					}
					if (operationsMetadata) {
						caps.setOperationsMetadata(cbt.getOperationsMetadata());
					}
					if (contents) {
						caps.setContents(this.createContents());
					}

					WNSUtil.sendResponse(capsd, this.wnsSchemaLocation,
							response);
				}

			} else {
				// return whole document
				this.sendCompleteCapabilities(response);
			}
		}
	}

	private void sendCompleteCapabilities(HttpServletResponse response)
			throws WNSException {

		CapabilitiesBaseType cbt;
		try {
			cbt = CapabilitiesBaseType.Factory.parse(this.config
					.getServiceProperties().getCapabilitiesBaseInformation()
					.toString());
			CapabilitiesDocument capsd = CapabilitiesDocument.Factory
					.newInstance();
			Capabilities caps = capsd.addNewCapabilities();
			caps.set(cbt);
			caps.setContents(this.createContents());

			WNSUtil.sendResponse(capsd, this.wnsSchemaLocation, response);
		} catch (XmlException e) {
			log.error("XmlException occured: " + e.toString());
			WNSServiceException se = new WNSServiceException(this.excLevel);
			se
					.addCodedException(
							WNSServiceException.ExceptionCode.NoApplicableCode,
							null, e);
			WNSUtil.sendResponse(se.getDocument(), this.ogcExcSchemaLocation,
					response);
		}

	}

	private Contents createContents() {

		Contents c = Contents.Factory.newInstance();
		ProtocolsType pt = c.addNewSupportedCommunicationProtocols();

		if (this.config.getRegisteredHandlers().isSetXMPPHandler()) {
			pt.setXMPP(true);// addProtocol(ProtocolsType.Factory.newInstance().setXMPP(true));
		}
		if (this.config.getRegisteredHandlers().isSetFaxHandler()) {
			pt.setFax(true);// addProtocol(ProtocolType.FAX);
		}
		if (this.config.getRegisteredHandlers().isSetEMailHandler()) {
			pt.setEmail(true);// addProtocol(ProtocolType.EMAIL);
		}
		if (this.config.getRegisteredHandlers().isSetPhoneHandler()) {
			pt.setPhone(true);// addProtocol(ProtocolType.PHONE);
		}
		if (this.config.getRegisteredHandlers().isSetSMSHandler()) {
			pt.setSMS(true);// addProtocol(ProtocolType.SMS);
		}

		c.setMaxTTLOfMessages(this.config.getServiceProperties()
				.getMaxTTLOfMessages());

		return c;
	}

	public void handlePostRequest(HttpServletRequest request,
			HttpServletResponse response) throws WNSException {

		try {

			String input = null;

			try {
				// Read the request
				InputStream in = request.getInputStream();
				String inputString = "";
				String decodedString = "";
				// read every byte of the request and put it into a StringWriter
				byte[] buffer = new byte[8192]; // FIXME What is the source for 8192?
				StringWriter sw = new StringWriter();
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1) {
					sw.write(new String(buffer, 0, bytesRead));
				}
				inputString = sw.toString();
				// discard "request="-Input String header
				if (inputString.startsWith("request=")) {
					// discard "request="-Input String header
					inputString = inputString
							.substring(8, inputString.length());

					// decode the application/x-www-form-urlencoded query string
					decodedString = java.net.URLDecoder.decode(inputString,
							"UTF-8");
					input = decodedString;
				} else {
					input = inputString;
				}

			} catch (IOException e) {
				log.error("IOException occured: " + e.toString(),e);
				WNSServiceException se = new WNSServiceException(this.excLevel);
				se.addCodedException(
						WNSServiceException.ExceptionCode.NoApplicableCode,
						null, e);
				throw se;
			}

			// parse request and handle accordingly
			XmlObject xobj = null;
			ArrayList parsingErrors = new ArrayList();
			XmlOptions parsingOptions = new XmlOptions();
			parsingOptions.setErrorListener(parsingErrors);

			try {

				xobj = XmlObject.Factory.parse(input, parsingOptions);

			} catch (XmlException e) {
				throw this.createInvalidRequestException(parsingErrors,
						this.excLevel, this.ogcExcSchemaLocation, response);
			}

			// handle request
			ArrayList validationErrors = new ArrayList();
			XmlOptions validationOptions = new XmlOptions();
			validationOptions.setErrorListener(validationErrors);
			boolean isValid = true;

			WNSUserHandler userhandler = WNSUserHandler.getInstance();

			WNSMessageHandler messageHandler = WNSMessageHandler
					.getInstance(userhandler);

			SchemaType type = xobj.schemaType();

			if (type == GetCapabilitiesDocument.type) {
				log.info("GetCapabilities request from: "
						+ request.getRemoteAddr());
				GetCapabilitiesDocument gcd = (GetCapabilitiesDocument) xobj;
				isValid = gcd.validate(validationOptions);
				if (!isValid) {
					throw this.createInvalidRequestException(validationErrors,
							this.excLevel, this.ogcExcSchemaLocation, response);
				} else {
					this.handleGetCapabilities(gcd, response);
				}

			} else if (type == RegisterDocument.type) {
				log.info("Register request from: " + request.getRemoteAddr());
				RegisterDocument rd = (RegisterDocument) xobj;
				isValid = rd.validate(validationOptions);
				if (!isValid) {
					throw this.createInvalidRequestException(validationErrors,
							this.excLevel, this.ogcExcSchemaLocation, response);
				} else {
					RegisterResponseDocument rurd = userhandler.register(rd);
					WNSUtil
							.sendResponse(rurd, this.wnsSchemaLocation,
									response);
				}

			} else if (type == UpdateSingleUserRegistrationDocument.type) {
				log.info("UpdateSingelUser request from: "
						+ request.getRemoteAddr());
				UpdateSingleUserRegistrationDocument usurd = (UpdateSingleUserRegistrationDocument) xobj;
				isValid = usurd.validate(validationOptions);
				if (!isValid) {
					throw this.createInvalidRequestException(validationErrors,
							this.excLevel, this.ogcExcSchemaLocation, response);
				} else {
					UpdateSingleUserRegistrationResponseDocument usurrd = userhandler
							.updateSingleUser(usurd);
					WNSUtil.sendResponse(usurrd, this.wnsSchemaLocation,
							response);
				}

			} else if (type == UpdateMultiUserRegistrationDocument.type) {
				log.info("UpdateMultiUser request from: "
						+ request.getRemoteAddr());
				UpdateMultiUserRegistrationDocument umurd = (UpdateMultiUserRegistrationDocument) xobj;
				isValid = umurd.validate(validationOptions);
				if (!isValid) {
					throw this.createInvalidRequestException(validationErrors,
							this.excLevel, this.ogcExcSchemaLocation, response);
				} else {
					UpdateMultiUserRegistrationResponseDocument umurrd = userhandler
							.updateMultiUser(umurd);
					WNSUtil.sendResponse(umurrd, this.wnsSchemaLocation,
							response);
				}

			} else if (type == UnregisterDocument.type) {
				log.info("Unregister request from: " + request.getRemoteAddr());
				UnregisterDocument ud = (UnregisterDocument) xobj;
				isValid = ud.validate(validationOptions);
				if (!isValid) {
					throw this.createInvalidRequestException(validationErrors,
							this.excLevel, this.ogcExcSchemaLocation, response);
				} else {
					UnregisterResponseDocument urd = userhandler.unregister(ud);
					WNSUtil.sendResponse(urd, this.wnsSchemaLocation, response);
				}

			} else if (type == DoNotificationDocument.type) {
				log.info("DoNotification request from: "
						+ request.getRemoteAddr());
				DoNotificationDocument dnd = (DoNotificationDocument) xobj;
				isValid = dnd.validate(validationOptions);
				if (!isValid) {
					throw this.createInvalidRequestException(validationErrors,
							this.excLevel, this.ogcExcSchemaLocation, response);
				} else {
					DoNotificationResponseDocument dnrd = messageHandler
							.doNotification(dnd);
					WNSUtil
							.sendResponse(dnrd, this.wnsSchemaLocation,
									response);
				}

			} else if (type == GetMessageDocument.type) {
				log.info("GetMEssage request from: " + request.getRemoteAddr());
				GetMessageDocument gmd = (GetMessageDocument) xobj;
				isValid = gmd.validate(validationOptions);
				if (!isValid) {
					throw this.createInvalidRequestException(validationErrors,
							this.excLevel, 
							this.ogcExcSchemaLocation,
							response);
				} else {
					GetMessageResponseDocument gmrd = messageHandler.getMessage(gmd);
					WNSUtil.sendResponse(gmrd,
							this.wnsSchemaLocation,
							response);
				}

			} else {
				// unknown request
				log.error("OperationNotSupported from: "
						+ request.getRemoteAddr());
				WNSServiceException se = new WNSServiceException(this.excLevel);
				//
				// try to get the name of the operation by identifying the schema type
				// prevent NPEs
				//
				String locator = (type != null?
						(type.getName() != null?
								type.getName().toString():type.toString())
							:null);
				String message = "The operation you requested (\""
					+ locator
					+ "\") is not supported by "
					+ "this service. Maybe the namespace is not well defined!";
				se.addCodedException(WNSServiceException.ExceptionCode.OperationNotSupported,locator, message);
				throw se;
			}

		} catch (WNSServiceException se) {
			log.error(se.toString(),se);
			WNSUtil.sendResponse(se.getDocument(), this.ogcExcSchemaLocation,
					response);
		}
	}

	private WNSServiceException createInvalidRequestException(ArrayList errors,
			ExceptionLevel excLevel, String owsSchemaLocation,
			HttpServletResponse response) {
		StringBuffer sb = new StringBuffer();
		Iterator iter = errors.iterator();
		while (iter.hasNext()) {
			sb.append("[Error] " + iter.next() + "\n");
		}

		WNSServiceException se = new WNSServiceException(excLevel);
		se
				.addCodedException(
						WNSServiceException.ExceptionCode.InvalidRequest, sb
								.toString(),
						"Your request was invalid. Examine locator-element for more details.");

		return se;
	}
}
