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
package org.n52.wns.db;

import org.apache.xmlbeans.XmlException;
import org.n52.wns.WNSException;
import org.n52.wns.WNSInitParamContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.x52North.wns.v2.WNSConfigDocument.WNSConfig;
import org.x52North.wns.v2.WNSConfigDocument.WNSConfig.ServiceProperties.DataBaseProperties;
import org.x52North.wns.v2.WNSUserDocument;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

/**
 * Class to handle UserDAO request and store them in the eXistDatabase
 * 
 * @author Dennis Dahlmann
 * @version 2.0
 * 
 */

public class ExistDatabaseUserDAO implements UserDAO {

	private WNSConfig wnsConfig = null;

	private String dbURL = "";

	private String dbName = "";

	private String dbUser = "";

	private String dbPwd = "";

	private String connectionURL = "";

	private Collection col = null;

	private static Logger log = LoggerFactory.getLogger(ExistDatabaseUserDAO.class);

	/**
	 * This is the constructor and initializes the USER DAO database
	 * 
	 * @param properties
	 * @throws WNSException
	 */
	public ExistDatabaseUserDAO(DataBaseProperties properties)
			throws WNSException {
		log.debug("Creating ExistDatabaseUserDAO instance");
		WNSInitParamContainer initParams = WNSInitParamContainer.getInstance();
		this.wnsConfig = initParams.getWnsConfig();
		this.dbURL = this.wnsConfig.getServiceProperties()
				.getDataBaseProperties().getURL();
		// dbName = wnsConfig.getServiceProperties().getDataBaseProperties()
		// .getDBName()+"/userdata";
		this.dbName = "52nWNSUserdata";

		this.dbUser = this.wnsConfig.getServiceProperties()
				.getDataBaseProperties().getUser();
		this.dbPwd = this.wnsConfig.getServiceProperties()
				.getDataBaseProperties().getPassword();

		this.connectionURL = this.dbURL + this.dbName;

		String driver = "org.exist.xmldb.DatabaseImpl";
		Class cl;
		try {
			cl = Class.forName(driver);
			Database database = (Database) cl.newInstance();

			DatabaseManager.registerDatabase(database);

			this.col = DatabaseManager.getCollection(this.connectionURL,
					this.dbUser, this.dbPwd);

			if (this.col == null) {

				Collection root = DatabaseManager.getCollection(
						// TODO Change DATABASE NAME
						dbURL,
						this.dbUser, this.dbPwd);

				CollectionManagementService mgtService = (CollectionManagementService) root
						.getService("CollectionManagementService", "1.0");

				this.col = mgtService.createCollection(this.dbName);
				XMLResource document;
				try {
					document = (XMLResource) this.col.createResource(
							"userdocument", "XMLResource");
					WNSUserDocument wnsud = WNSUserDocument.Factory
							.newInstance();
					wnsud.addNewWNSUser().addNewMultiUser();
					wnsud.getWNSUser().addNewSingleUser();
					document.setContent(wnsud.xmlText());
					this.col.storeResource(document);
					this.col.close();
					log.debug("WNSUserDocument build");
				} catch (XMLDBException e) {
					String message = "Could not create empty WNSUser document: ";
					log.error(message + e.toString());
					throw new WNSException(message + e.toString());

				}

			}
			log.debug("ExistDatabaseUserDAO instance successfully build");

		} catch (ClassNotFoundException e) {
			log.error("Error during init ExistDatabaseUserDAO request: "
					+ e.toString());
			throw new WNSException(
					"Error during init ExistDatabaseUserDAO request: "
							+ e.toString());
		} catch (InstantiationException e) {
			log.error("Error during init ExistDatabaseUserDAO request: "
					+ e.toString());
			throw new WNSException(
					"Error during init ExistDatabaseUserDAO request: "
							+ e.toString());
		} catch (IllegalAccessException e) {
			log.error("Error during init ExistDatabaseUserDAO request: "
					+ e.toString());
			throw new WNSException(
					"Error during init ExistDatabaseUserDAO request: "
							+ e.toString());
		} catch (XMLDBException e) {
			log.error("Error during init ExistDatabaseUserDAO request: "
					+ e.toString());
			throw new WNSException(
					"Error during init ExistDatabaseUserDAO request: "
							+ e.toString());
		}

	}

	/**
	 * @throws WNSException
	 * @see org.n52.wns.db.UserDAO#getUserDocument()
	 */
	public WNSUserDocument getUserDocument() throws WNSException {
		XMLResource res;
		WNSUserDocument wnsuser = null;
		try {
			res = (XMLResource) this.col.getResource("userdocument");
			wnsuser = WNSUserDocument.Factory.parse(res.getContentAsDOM());
		} catch (XMLDBException e) {
			String message = "Could not load WNSUser document: ";
			log.error(message + e.toString());
			throw new WNSException(message + e.toString());
		} catch (XmlException e) {
			String message = "Could not load WNSUser document: ";
			log.error(message + e.toString());
			throw new WNSException(message + e.toString());
		} catch (NullPointerException e) {
			String message = "Could not load WNSUser document: ";
			log.error(message + e.toString());
			throw new WNSException(message + e.toString());
		}

		return wnsuser;
	}

	/**
	 * @throws WNSException
	 * @see org.n52.wns.db.UserDAO#storeWNSUserDocument(WNSUserDocument wnsud)
	 */
	public void storeWNSUserDocument(WNSUserDocument wnsud) throws WNSException {
		log.debug("Trying to store WNSUserDocument");
		XMLResource document;
		try {
			document = (XMLResource) this.col.createResource("userdocument",
					"XMLResource");
			document.setContent(wnsud.xmlText());
			this.col.storeResource(document);
			this.col.close();
			log.debug("WNSUserDocument stored");
		} catch (XMLDBException e) {
			String message = "Could not store WNSUser document: ";
			log.error(message + e.toString());
			throw new WNSException(message + e.toString());
		}

	}

}
