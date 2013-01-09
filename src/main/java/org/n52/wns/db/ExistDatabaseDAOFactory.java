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

import org.n52.wns.WNSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.x52North.wns.v2.WNSConfigDocument.WNSConfig.ServiceProperties.DataBaseProperties;

/**
 * Interafce to define DAO request
 * 
 * @author Dennis Dahlmann
 * @version 2.0
 * 
 */
public class ExistDatabaseDAOFactory implements DAOFactory {
	private Logger log = LoggerFactory.getLogger(ExistDatabaseDAOFactory.class);

	private DataBaseProperties database = null;

	/**
	 * This is the constructor of the DAOFactory
	 * 
	 */
	public ExistDatabaseDAOFactory() {}	
	

	public void configure(DataBaseProperties props) {
	   //TODO use real properties instead of exist related properties
	     this.database = props;
	}
	
	/**
	 * @throws WNSException
	 * @see org.n52.wns.db.DAOFactory#getUserDAO()
	 */
	public UserDAO getUserDAO() throws WNSException {
		this.log.debug("Trying to get ExistDatabaseUserDAO");
		ExistDatabaseUserDAO us = new ExistDatabaseUserDAO(this.database);
		this.log.debug("Successfully get ExistDatabaseUserDAO");
		return us;

	}

	/**
	 * @throws WNSException
	 * @see org.n52.wns.db.DAOFactory#getMessageDAO()
	 */
	public MessageDAO getMessageDAO() throws WNSException {
		this.log.debug("Trying to get ExistDatabaseMessageDAO");
		ExistDatabaseMessageDAO me = new ExistDatabaseMessageDAO(this.database);
		this.log.debug("Successfully get ExistDatabaseMEssageDAO");
		return me;

	}

}
