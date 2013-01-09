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

/**
 * Exception class to handle exceptions
 * 
 * @author Dennis Dahlmann
 * @version 2.0
 */

public class WNSException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2748869774496419343L;

	public final static String NESTED_EXCEPTION = "Nested Exception";

	// locator and exception
	private String locator;

	private Exception nested;

	/** constructor just calls super constructor */
	public WNSException() {
		super();
	}

	/**
	 * constructor
	 * 
	 * @param message
	 *            String representation of the exception message
	 */
	public WNSException(String message) {
		super(message);
	}

	/**
	 * constructor
	 * 
	 * @param message
	 *            String representation of the exception message
	 * @param locator
	 *            String representation of the locator
	 */
	public WNSException(String message, String locator) {
		this(message);
		this.locator = locator;
	}

	/**
	 * constructor
	 * 
	 * @param nested
	 *            Exception
	 */
	public WNSException(Exception nested) {
		this(NESTED_EXCEPTION, nested);
	}

	/**
	 * constructor
	 * 
	 * @param message
	 *            String representation of the exception message
	 * @param nested
	 *            exception
	 */
	public WNSException(String message, Exception nested) {
		super(message);
		this.nested = nested;
	}

	/**
	 * gets a String representation of the exception message
	 * 
	 * @return String
	 */
	@Override
	public String getMessage() {
		StringBuffer sb = new StringBuffer();
		if (super.getMessage() != null) {
			sb.append(super.getMessage());
		}
		if (this.nested != null) {
			sb.append(" >> ");
			sb.append(this.nested.getMessage());
		}
		return sb.toString();
	}

	/**
	 * gets the locator
	 * 
	 * @return locator
	 */
	public String getLocator() {
		return this.locator;
	}

	public String getDefinition() {
		return null;
	}

	private String getElementName() {
		return "WNSException";
	}

	/**
	 * private getValue
	 * 
	 * @return String representation
	 */
	private String getValue() {
		StringBuffer sb = new StringBuffer();
		sb.append("<Exception>");
		if (this.locator != null) {
			sb.append("<Locator>");
			sb.append(this.locator);
			sb.append("</Locator>");
		}
		sb.append("<Message>");
		if (this.getMessage() != null) {
			sb.append(this.getMessage());
		} else {
			sb.append("No detailed message available");
		}
		sb.append("</Message>");
		sb.append("</Exception>");
		return sb.toString();
	}
}
