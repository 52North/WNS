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

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;

/**
 * Utility class for the WNS
 * 
 * @author Johannes Echterhoff
 * @version 2.0
 * 
 */
public class WNSUtil {

	private static Logger log = Logger.getLogger(WNSUtil.class.getName());

	/**
	 * Returns the parameter value for the passed parameter. This method is used
	 * to provide caseinsensitive parameter spelling.
	 * 
	 * @param param
	 *            the parameter, for which the parameter value schould be
	 *            returned
	 * @param req
	 *            the request from which the parameter value should be extracted
	 * @return returns the parameter value for the passed parameter (the search
	 *         for the parameter is caseinsensitive) or null if the given
	 *         parameter does not exist
	 */
	public static String getValue4Param(String param, HttpServletRequest req) {
		String paramValue = null;
		Enumeration params = req.getParameterNames();
		while (params.hasMoreElements()) {
			String p = (String) params.nextElement();
			if (p.equalsIgnoreCase(param)) {
				return req.getParameter(p);
			}
		}
		return paramValue;
	}

	/**
	 * Sends the given XML to via the given response object. The given schema
	 * location is added to the XML.
	 * 
	 * The XML will be pretty printed.
	 * 
	 * @param xts
	 *            Contains the XML encoded response.
	 * @param schemaLocation
	 *            Will be added to the XML if not null.
	 * @param response
	 *            Will deliver the XML.
	 * @throws SPSException
	 *             If the response could not be sent.
	 */
	public static void sendResponse(XmlTokenSource xts, String schemaLocation,
			HttpServletResponse response) throws WNSException {
		log.debug("Trying to send response");
		response.setContentType("text/xml");
		ServletOutputStream out = null;
		try {
			out = response.getOutputStream();

			XmlOptions options = new XmlOptions();

			options.setSavePrettyPrint();
			options.setUseDefaultNamespace();
			options.setSaveAggressiveNamespaces();

			// If the schemaLocation is null do not insert schemaLocation to
			// xml-instance
			if ((schemaLocation != null)
					&& (schemaLocation.trim().length() > 0)) {
				XmlCursor c = xts.newCursor();
				c.toFirstChild();
				c.setAttributeText(new QName(
						"http://www.w3.org/2001/XMLSchema-instance",
						"schemaLocation", "xsi"), schemaLocation);
				c.dispose();
			}

			xts.save(out, options);
			log.debug("Successfully send response");

		} catch (IOException e) {
			log.fatal("Could not send response: " + e.toString());
			throw new WNSException("Could not send response", e);
		} finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					log.fatal("Cannot close response output stream: "
							+ e.toString());
					throw new WNSException(
							"Cannot close response output stream", e);
				}
			}
		}
	}

	public static void sendResponse(String string, HttpServletResponse response)
			throws WNSException {
		log.debug("Trying to send response");
		response.setContentType("text/xml");
		ServletOutputStream out = null;
		try {
			out = response.getOutputStream();

			XmlOptions options = new XmlOptions();

			options.setSavePrettyPrint();
			options.setUseDefaultNamespace();
			options.setSaveAggressiveNamespaces();

			out.write(string.getBytes());
			log.debug("Successfully send response");

		} catch (IOException e) {
			log.fatal("Could not send response: " + e.toString());
			throw new WNSException("Could not send response", e);
		} finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					log.fatal("Cannot close response output stream: "
							+ e.toString());
					throw new WNSException(
							"Cannot close response output stream", e);
				}
			}
		}
	}

}
