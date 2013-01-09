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

import org.n52.wns.WNSException;
import org.n52.wns.WNSServiceException;

/**
 * Interface to define the mechanism to store messages
 * 
 * @author Dennis Dahlmann, Johannes Echterhoff
 * 
 */
public interface MessageDAO {

   /**
    * Stores a Notification message
    * 
    * @param message
    *           The message to store
    * @param id
    *           The ID corresponding to the message, the message ID
    * @throws WNSServiceException
    */
   public void storeNotificationMessage(DoNotificationType message, int id)
         throws WNSException;

   /**
    * Returns the message with the give messageID
    * 
    * @param messageID
    *           The message ID
    * @return The Message with the messageID
    * @throws WNSException
    * @throws WNSServiceException
    */
   public GetMessageResponseDocument getMessage(String messageID)
         throws WNSException, WNSServiceException;

   /**
    * Returns the count of messages
    * 
    * @return The number of messages that are currently stored
    * @throws WNSServiceException
    */
   public int getMaxMessageNumber() throws WNSException;

   /**
    * Removes a stored message from the database. If no message with given ID
    * exists, nothing is changed and no exception will be thrown.
    * 
    * @param id
    *           ID of the message to remove.
    * @throws WNSException
    *            if the message could not be deleted for some reason other than
    *            that no message with given ID was found.
    */
   public void deleteMessage(String id) throws WNSException;

   /**
    * Retrieves the hash table containing the IDs of all stored messages
    * together with their expiration date. If that table does not exist yet, it
    * will be created.
    * 
    * @return a hash table containing the IDs of all stored messages together
    *         with their expiration date
    * @throws WNSException
    */
   public XMLHashTableDocument getMessagesHashTable() throws WNSException;

   /**
    * Saves the given message hash table in the database.
    * 
    * @param table
    *           contains the IDs of all stored messages together with their
    *           expiration date
    * @throws WNSException
    */
   public void storeMessagesHashTable(XMLHashTableDocument table)
         throws WNSException;
}
