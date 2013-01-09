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

package org.n52.wns;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Set;

import noNamespace.XMLHashTableDocument;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.GDuration;
import org.n52.wns.db.MessageDAO;

/**
 * This class provides the necassary methods to add and delete entries from the
 * database after a specific duration
 * 
 * @author Dennis Dahlmann, Johannes Echterhoff
 * @version 2.0
 */
public class WNSDatabaseThread implements Runnable {

	private int updateRate;

	private final int minutes = 60000;

	private volatile Thread thread;
	
	// MessageID : ExpiredDate
	private Hashtable<String, Long> hashTable = new Hashtable<String, Long>();

	private XMLHashTableDocument xmlhash = XMLHashTableDocument.Factory
			.newInstance();

	private SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

	private static Logger log = Logger.getLogger(WNSDatabaseThread.class
			.getName());
	
	private MessageDAO msgDao;

	/**
	 * 
	 * @param updateRate
	 *            The time in minutes to check for old database entries
	 * @param msgDao
	 *            DAO for access to message relevant information
	 * @throws WNSException
	 */
	public WNSDatabaseThread(int updateRate, MessageDAO msgDao) {
		
		log.debug("init WNSDatabaseThread");
		
		this.msgDao = msgDao;
				
		this.updateRate = updateRate * this.minutes;
				
		try {
         this.xmlhash = msgDao.getMessagesHashTable();

         for (int i = 0; i < this.xmlhash.getXMLHashTable()
               .sizeOfHashArray(); i++) {
            this.hashTable.put(this.xmlhash.getXMLHashTable()
                  .getHashArray(i).getMessageID(), this.xmlhash
                  .getXMLHashTable().getHashArray(i).getDate());
         }
			
			thread = new Thread(this);
	      thread.setDaemon(true);
	      thread.start();
	      
		} catch (WNSException e) {
			log.error("Error during database checkup: " + e.toString());
		}
	}

	/**
	 * Run method to start the thread
	 */
	public void run() {

		while (true) {
			try {

				if (!this.hashTable.isEmpty()) {
					this.checkExpiredIDs();
				} else {
					log.info("No messages stored, so no checking is needed");
				}

				Thread.sleep(this.updateRate);

			} catch (InterruptedException e) {
				// no need to print out this exception
			} catch (WNSException e) {
				log.error("Error during database checkup: " + e.toString());
			}
		}
	}

	private void checkExpiredIDs() throws WNSException {
		log.info("Checking for expired messages");

		GregorianCalendar cal = new GregorianCalendar();
		Long time = Long.parseLong(this.format.format(cal.getTime()));

		if (!this.hashTable.isEmpty()) {

			Set<String> messageIds = this.hashTable.keySet();
			ArrayList<String> msgIdsToDelete = new ArrayList<String>();

			// find out which messages should be deleted
			for (String msgId : messageIds) {
				if (this.hashTable.get(msgId) <= time) {
					msgIdsToDelete.add(msgId);
				}
			}
			
			if (msgIdsToDelete.size() == 0) {
			 
				log.info("There are no expired messages");
				
			} else {

   			// delete messages
   			for (String msgIdToDelete : msgIdsToDelete) {
   
   				// remove message from database
   			   this.msgDao.deleteMessage(msgIdToDelete);
   
   				log.info("MessageID: " + msgIdToDelete + " expired");
   				
   				// search through XMLHashTable for correct entry
   				this.hashTable.remove(msgIdToDelete);
   				
   				for (int j = 0; j < this.xmlhash.getXMLHashTable()
   						.sizeOfHashArray(); j++) {
   					if (this.xmlhash.getXMLHashTable().getHashArray(j)
   							.getMessageID().equalsIgnoreCase(msgIdToDelete)) {
   						this.xmlhash.getXMLHashTable().removeHash(j);
   						break;
   					}
   				}
   				log.info("MessageID: " + msgIdToDelete + " deleted");
   			}
			}

			// as all expired messages should have been deleted, save current
			// XMLHashTable
			this.msgDao.storeMessagesHashTable(this.xmlhash);
			
		} else {
			log.info("There are no expired messages");
		}

	}

	/**
	 * 
	 * @param id
	 *            The ID of the message
	 * @param duration
	 *            The duration the message will be in the database
	 * @throws WNSException
	 */
	public synchronized void add(String id, GDuration duration) throws WNSException {
	   
		log.debug("Trying to store message: " + id);
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(Calendar.YEAR, duration.getYear());
		cal.add(Calendar.MONTH, duration.getMonth());
		cal.add(Calendar.DATE, duration.getDay());
		cal.add(Calendar.HOUR, duration.getHour());
		cal.add(Calendar.MINUTE, duration.getMinute());
		cal.add(Calendar.SECOND, duration.getSecond());

		Long time = Long.parseLong(this.format.format(cal.getTime()));

		this.hashTable.put(id, time);

		noNamespace.XMLHashTableDocument.XMLHashTable.Hash hash = this.xmlhash
				.getXMLHashTable().addNewHash();

		hash.setDate(time);
		hash.setMessageID(id);

		this.msgDao.storeMessagesHashTable(this.xmlhash);
		
		log.info("Message: " + id + " stored");

	}

}
