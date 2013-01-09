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

import org.apache.log4j.Logger;
import org.n52.wns.WNSException;

/**
 * Factory to load MailHandler classes to send emails
 * 
 * @author Dennis Dahlmann
 * @version 2.0
 */
public class MailHandlerFactory {

	private static Logger log = Logger.getLogger(MailHandlerFactory.class
			.getName());

	private static MailHandler instance = null;

	/**
	 * Get method for singelton MailHandler
	 * 
	 * @return The MailHandler
	 * @throws WNSException
	 */
	public static MailHandler getInstance() throws WNSException {
		log.debug("MailHandler getInstance request");
		if (instance == null) {
			log
					.fatal("MailHandlerFactory: Cannot instanciate without configuration data! Use getInstance with parameter org.x52North.wns.WNSConfigDocument.WNSConfig.RegisteredHandlers.EMailHandler");
			throw new WNSException(
					"MailHandlerFactory: Cannot instanciate without configuration data!");
		} else {
			log.debug("MailHandler getInstance request successful");
			return instance;
		}
	}

	/**
	 * Get method for singleton MailHandler
	 * 
	 * @return MailHandler
	 */
	public static MailHandler getInstance(
			org.x52North.wns.WNSConfigDocument.WNSConfig.RegisteredHandlers.EMailHandler mailHandler) {
		log.debug("MailHandler getInstance request");
		if (instance == null) {
			synchronized (org.n52.wns.mail.MailHandlerFactory.class) {
				if (instance == null) {
					instance = new org.n52.wns.mail.MailHandler(mailHandler);
				}
			}
		}
		log.debug("MailHandler getInstance request successful");
		return instance;
	}
}
