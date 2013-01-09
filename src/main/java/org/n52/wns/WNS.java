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

 ***************************************************************/

package org.n52.wns;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.x52North.wns.v2.WNSConfigDocument;

/**
 * @author Dennis Dahlmann, Johannes Echterhoff
 */
public class WNS extends HttpServlet {

	private static final long serialVersionUID = 6403401662907244818L;

	private static final Logger log = LoggerFactory.getLogger(WNS.class);

	private WNSInitParamContainer initParams = null;

	/**
	 * Init method to check the config parameters
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

//      Properties props = new Properties();
//      props.put("REL_PATH", this.getServletContext().getRealPath("/"));
//      InputStream in = null;
//      try {
//         in = this.getServletContext().getResourceAsStream(
//               "/WEB-INF/conf/log4j.properties");
//         if (in != null) {
//            props.load(in);
//         } else {
//            System.out.println("Could not load log4j properties.");
//         }
//      } catch (IOException ioe) {
//         System.out.println("IO Exception loading log4j properties. "
//               + ioe.getMessage());
//      } finally {
//         if (in != null) {
//            try {
//               in.close();
//            } catch (Throwable ignore) {
//               // ignore it
//            }
//         }
//      }
//
////      props.list(System.out);
//      
//      PropertyConfigurator.configure(props);            
//      log.info("logging properties loaded");
      
      
		// --- Ausgabe der Ini-Parameter zur Kontrolle

		Enumeration enumer = this.getInitParameterNames();

		while (enumer.hasMoreElements()) {
			String key = (String) enumer.nextElement();
			Object value = this.getInitParameter(key);
			this.log.info("Context_Init_Parameter: " + key + " = " + value);
		}
		String wnsConfig = this
				.getInitParameter("WNS_CONFIG_FILE");
		
		try {
		   
		   WNSConfigDocument wnscd = null;

         // parse WNSConfig
         try {
            
            wnscd = WNSConfigDocument.Factory.parse(
                  this.getServletContext().getResourceAsStream(
                        "/WEB-INF/conf/" + wnsConfig));

            // validate WNSConfig
            ArrayList validationErrors = new ArrayList();
            XmlOptions validationOptions = new XmlOptions();
            validationOptions.setErrorListener(validationErrors);
            boolean isValid = wnscd.validate(validationOptions);

            // create XmlException with error-message if the XML is invalid.
            if (!isValid) {
               StringBuffer sb = new StringBuffer();
               Iterator iter = validationErrors.iterator();
               while (iter.hasNext()) {
                  sb.append("[Validation-Error] " + iter.next() + "\n");
               }

               throw new WNSException("The WNSConfig is invalid!\n"
                     + sb.toString());

            } else {
               log.info("The WNSConfig is valid.");
               
               // print info if debug mode is enabled
               if(wnscd.getWNSConfig().getServiceProperties().getDebugMode()) {
            	   log.info("\n#################################################\n" +
            	   		"\n\t\tDEBUG is ACTIVATED" +
            	   		"\n\n\tAdditional functionality enabled." +
            	   		"\n\tSee documentation for details" +
            	   		"\n\n#################################################");
               }
               
               this.initParams = WNSInitParamContainer.createInstance(wnscd);
            }             
            
         } catch (Exception e) {
            String message = "Failure while trying to read WNSConfig. ";
            log.error(message,e);
            throw new WNSException(message, e);
         }         
			
		} catch (WNSException e) {
			this.log.error("Could not initialise WNS.",e);
			throw new ServletException("Could not initialise WNS", e);
		}
	}

	/**
	 * Returns the Servlet info
	 */
	@Override
	public String getServletInfo() {
		return "Web Notification Service";
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		try {
			WNSRequestHandler requestHandler = new WNSRequestHandler();
			requestHandler.handleGetRequest(request, response);

		} catch (WNSException e) {

			this.log.error("An exception occurred while handling Get request: "
					+ e.getMessage(),e);

			// try to send ExceptionReport
			WNSServiceException se = new WNSServiceException(this.initParams
					.getExceptionLevel());
			se
					.addCodedException(
							WNSServiceException.ExceptionCode.NoApplicableCode,
							null, e);
			try {
				WNSUtil.sendResponse(se.getDocument(), this.initParams
						.getOgcExceptionSchemaLocation(), response);
			} catch (WNSException e1) {
				// log that the exception could not be sent
				this.log.error("The exception could not be sent: "
						+ e.getMessage(),e);
			}
		}
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		try {
			WNSRequestHandler requestHandler = new WNSRequestHandler();
			requestHandler.handlePostRequest(request, response);

		} catch (Exception e) {

			this.log
					.error("An exception occurred while handling Post request: "
							+ e.getMessage(),e);

			// try to send ExceptionReport
			WNSServiceException se = new WNSServiceException(this.initParams
					.getExceptionLevel());
			se
					.addCodedException(
							WNSServiceException.ExceptionCode.NoApplicableCode,
							null, e);
			try {
				WNSUtil.sendResponse(se.getDocument(), this.initParams
						.getOgcExceptionSchemaLocation(), response);
			} catch (WNSException e1) {
				// log that the exception could not be sent
				this.log.error("The exception could not be sent: "
						+ e.getMessage(),e);
			}
		}
	}
}