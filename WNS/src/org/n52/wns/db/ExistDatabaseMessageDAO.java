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

package org.n52.wns.db;

import net.opengis.wns.x00.DoNotificationType;
import net.opengis.wns.x00.GetMessageResponseDocument;
import noNamespace.XMLHashTableDocument;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.GDuration;
import org.apache.xmlbeans.XmlException;
import org.n52.wns.WNSDatabaseThread;
import org.n52.wns.WNSException;
import org.n52.wns.WNSInitParamContainer;
import org.n52.wns.WNSServiceException;
import org.n52.wns.WNSServiceException.ExceptionCode;
import org.n52.wns.WNSServiceException.ExceptionLevel;
import org.x52North.wns.WNSConfigDocument.WNSConfig;
import org.x52North.wns.WNSConfigDocument.WNSConfig.ServiceProperties.DataBaseProperties;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.ErrorCodes;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

/**
 * Class to handle MessageDAO request and store them in the eXistDatabase
 * 
 * @author Dennis Dahlmann, Johannes Echterhoff
 * 
 */
public class ExistDatabaseMessageDAO implements MessageDAO {

   private WNSConfig wnsConfig = null;

   private String dbURL = "";

   private String dbName = "";

   private String dbUser = "";

   private String dbPwd = "";

   private String connectionURL = "";

   private Collection col = null;

   private GDuration maxTTLofMessage = null;

   private WNSDatabaseThread thread = null;

   private ExceptionLevel exceptionLevel;

   private static Logger log = Logger.getLogger(ExistDatabaseMessageDAO.class.getName());

   /**
    * This is the constructor, it inits the database to store messages
    * 
    * @param properties
    *           The properties of the database
    * @throws WNSException
    */
   public ExistDatabaseMessageDAO(DataBaseProperties properties)
         throws WNSException {

      log.debug("Creating ExistDatabaseMessageDAO instance");
      
      WNSInitParamContainer initParams = WNSInitParamContainer.getInstance();
      
      this.exceptionLevel = initParams.getExceptionLevel();
      
      this.thread = initParams.getDatabaseThread();
      
      this.wnsConfig = initParams.getWnsConfig();
      this.maxTTLofMessage = this.wnsConfig.getServiceProperties().getMaxTTLOfMessages();
      this.dbURL = this.wnsConfig.getServiceProperties().getDataBaseProperties().getURL();
      this.dbName = this.wnsConfig.getServiceProperties().getDataBaseProperties().getDBName();
      this.dbUser = this.wnsConfig.getServiceProperties().getDataBaseProperties().getUser();
      this.dbPwd = this.wnsConfig.getServiceProperties().getDataBaseProperties().getPassword();

      this.connectionURL = "xmldb:exist://" + this.dbURL + "/xmlrpc" + "/db/"
            + this.dbName;

      String driver = "org.exist.xmldb.DatabaseImpl";
      Class cl;
      try {
         cl = Class.forName(driver);
         Database database = (Database) cl.newInstance();
         DatabaseManager.registerDatabase(database);
         
         this.col = DatabaseManager.getCollection(this.connectionURL,
               this.dbUser, this.dbPwd);
         
         if (this.col == null) {
            // initialize the collection
            Collection root = DatabaseManager.getCollection("xmldb:exist://"
                  + this.dbURL + "/xmlrpc" + "/db", this.dbUser, this.dbPwd);
            CollectionManagementService mgtService = (CollectionManagementService) root.getService(
                  "CollectionManagementService", "1.1");
            this.col = mgtService.createCollection(this.dbName);
         }
         
         log.debug("ExistDatabaseMessageDAO instance successfully build");
      } catch (ClassNotFoundException e) {
         log.fatal("Error during init ExistDatabaseMessageDAO request: "
               + e.toString());
         throw new WNSException(
               "Error during init ExistDatabaseMessageDAO request: ", e);
      } catch (InstantiationException e) {
         log.fatal("Error during init ExistDatabaseMessageDAO request: "
               + e.toString());
         throw new WNSException(
               "Error during init ExistDatabaseMessageDAO request: ", e);
      } catch (IllegalAccessException e) {
         log.fatal("Error during init ExistDatabaseMessageDAO request: "
               + e.toString());
         throw new WNSException(
               "Error during init ExistDatabaseMessageDAO request: ", e);
      } catch (XMLDBException e) {
         log.fatal("Error during init ExistDatabaseMessageDAO request: "
               + e.toString());
         throw new WNSException(
               "Error during init ExistDatabaseMessageDAO request: ", e);
      }
   }

   /**
    * @throws WNSServiceException
    * @see org.n52.wns.db.MessageDAO#getMaxMessageNumber()
    */
   public int getMaxMessageNumber() throws WNSException {

      log.debug("Trying to get stored messages");
      int id = this.loadMessages();
      log.debug("Found " + id + " stored messages");
      return id;
   }

   /**
    * @see org.n52.wns.db.MessageDAO#getMessage(java.lang.String)
    */
   public GetMessageResponseDocument getMessage(String mID) throws WNSException, WNSServiceException {

      XMLResource res;
      GetMessageResponseDocument response = GetMessageResponseDocument.Factory.newInstance();
      log.debug("Trying to getMessage: " + mID);
            
      try {
         res = (XMLResource) this.col.getResource(mID);
         
         // first make sure that the message exists
         if(res != null && mID.substring(0, 1).equalsIgnoreCase("n")) {
               net.opengis.wns.x00.GetMessageResponseType mes;
               
               try {
                  mes = net.opengis.wns.x00.GetMessageResponseType.Factory.parse(res.getContent().toString());
                  response.addNewGetMessageResponse().addNewMessage();
                  response.getGetMessageResponse().getMessage().set(mes);
                  
                  return response;
                  
               } catch (XmlException e) {
                  log.fatal("Exception while trying to retrieve message from database.",e);
                  WNSException we = new WNSException("Error during getMessage request.",e);
                  throw we;
               }   
               
         } else {
            log.debug("MessageID expired: " + mID);
            
            WNSServiceException se = new WNSServiceException(this.exceptionLevel);
            se.addCodedException(ExceptionCode.MessageIDExpired, null, "Message ID "+mID+" is not known to this service. Maybe the ID has already expired.");
            throw se;
         }         
         
      } catch (XMLDBException e) {
         log.fatal("Exception while trying to retrieve message from database.",e);
         WNSException we = new WNSException("Error during getMessage request.",e);
         throw we;
      }      

   }


   /**
    * @see org.n52.wns.db.MessageDAO#storeNotificationMessage(net.opengis.wns.x00.DoNotificationType, int)
    */
   public void storeNotificationMessage(DoNotificationType message,
         int messageID) throws WNSException {

      XMLResource document;
      try {
         document = (XMLResource) this.col.createResource("N" + messageID,
               "XMLResource");
         document.setContent(message.getMessage().xmlText());
         this.col.storeResource(document);
         this.col.close();

         String id = "N" + messageID;
         if (message.isSetMaxTTLOfMessage()
               && (message.getMaxTTLOfMessage().compareToGDuration(
                     this.maxTTLofMessage) < 0)) {
            this.thread.add(id, message.getMaxTTLOfMessage());
         } else {
            this.thread.add(id, this.maxTTLofMessage);
         }
         log.debug("NotificationMessage stored with ID " + id);

      } catch (XMLDBException e) {
         log.fatal(e.toString());
         WNSException we = new WNSException(
               "Error during storeNotificationMessage request: " + e.toString());
         throw we;
      } catch (WNSException e) {
         log.fatal(e.toString());
         WNSException we = new WNSException(
               "Error during storeNotificationMessage request: " + e.toString());
         throw we;
      }

   }

   private int loadMessages() throws WNSException {

      int messageID = 0;
      try {
         String[] list = this.col.listResources();
         for (String message : list) {
            if (!message.equalsIgnoreCase("savedIDs")) {
               if (new Integer(message.substring(1)) > messageID) {
                  messageID = new Integer(message.substring(1));
               }
            }
         }
         return messageID;
      } catch (Exception e) {
         log.fatal("Cannot load stored message numbers: " + e.toString());
         WNSException we = new WNSException(
               "Error during private operation loadMessages request: "
                     + e.toString());
         throw we;

      }
   }

   /**
    * @see org.n52.wns.db.MessageDAO#deleteMessage(java.lang.String)
    */
   public void deleteMessage(String id) throws WNSException {

      try {
         XMLResource r = (XMLResource) this.col.createResource(id,
               "XMLResource");
         this.col.removeResource(r);
         this.col.close();
      } catch (XMLDBException e) {
         if(e.errorCode == ErrorCodes.INVALID_RESOURCE) {
            /* Ok. If for any reason the message was not found there is no
             * need to delete it.
             * */
         } else {
            throw new WNSException("XMLDBException" + e.getLocalizedMessage());
         }
      }

   }

   /**
    * @see org.n52.wns.db.MessageDAO#getMessagesHashTable()
    */
   public XMLHashTableDocument getMessagesHashTable() throws WNSException {

      XMLResource r;
      try {
         r = (XMLResource) this.col.getResource("savedIDs");
         
         XMLHashTableDocument xmlhash;
         if (r != null) {
            xmlhash = XMLHashTableDocument.Factory.parse(r
                  .getContentAsDOM());            
         } else {
            // initialize new hash table
            xmlhash = XMLHashTableDocument.Factory.newInstance();
            xmlhash.addNewXMLHashTable();
            this.storeMessagesHashTable(xmlhash);
         }
         this.col.close();
         
         return xmlhash;
         
      } catch (XMLDBException e) {
         throw new WNSException("XMLDBException" + e.getLocalizedMessage());
      } catch (XmlException e) {
         throw new WNSException("XmlException" + e.getLocalizedMessage());
      }
   }

   /**
    * @see org.n52.wns.db.MessageDAO#storeMessagesHashTable(noNamespace.XMLHashTableDocument)
    */
   public void storeMessagesHashTable(XMLHashTableDocument table)
         throws WNSException {

      XMLResource document;
      try {
         document = (XMLResource) this.col.createResource("savedIDs",
               "XMLResource");
         document.setContent(table);
         this.col.storeResource(document);
         this.col.close();
      } catch (XMLDBException e) {
         throw new WNSException("XMLDBException"
               + e.getLocalizedMessage());
      }
      
   }

}
