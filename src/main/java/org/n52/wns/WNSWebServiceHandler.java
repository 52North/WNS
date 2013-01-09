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

import java.io.DataOutputStream;
import java.net.URL;
import java.net.URLConnection;

import net.opengis.wns.x00.CommunicationMessageDocument;
import net.opengis.wns.x00.DoNotificationDocument;
import net.opengis.wns.x00.NotificationMessageDocument;
import net.opengis.wns.x00.ReplyMessageDocument;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods to send data to other servlets using HTTP POST
 * 
 * @author Dennis Dahlmann
 * @version 2.0
 */
public class WNSWebServiceHandler {

	private Logger log = LoggerFactory.getLogger(WNSWebServiceHandler.class);

	/**
	 * Constructor
	 */
	public WNSWebServiceHandler() {
	}

	/**
	 * sends a notification message to another servlet using POST
	 * 
	 * @param dnd
	 *            The DoNotificationDocument
	 * @param targets
	 *            A String array of targets
	 * @throws WNSException
	 */
	public void sendNotificationMessage(DoNotificationDocument dnd,
			String[] targets) throws WNSException {
		try {
			this.log.debug("Trying to send message");
			for (int i = 0; i <= targets.length - 1; i++) {
				// connect to the servlet
				String location = targets[i].toString();
				URL destinationServlet = new URL(location);
				URLConnection servletConnection = destinationServlet
						.openConnection();

				// inform the connection that we will send output and accept
				// input
				servletConnection.setDoInput(true);
				servletConnection.setDoOutput(true);

				// Don't use a cached version of URL connection.
				servletConnection.setUseCaches(false);
				servletConnection.setDefaultUseCaches(false);

				// Specify the content type that we will send xml data
				servletConnection.setRequestProperty("Content-Type",
						"application/xml");

				// send the message object to the servlet using serialization
				DataOutputStream outputToServlet = new DataOutputStream(
						servletConnection.getOutputStream());

				NotificationMessageDocument no = null;
				CommunicationMessageDocument co = null;
				ReplyMessageDocument re = null;
				boolean noti = false;
				boolean comu = false;
				boolean repl = false;

				try {
					no = NotificationMessageDocument.Factory.parse(dnd
							.getDoNotification().getMessage().toString());
					noti = true;
				} catch (XmlException e) {
					noti = false;
				}
				try {
					co = CommunicationMessageDocument.Factory.parse(dnd
							.getDoNotification().getMessage().toString());
					comu = true;
				} catch (XmlException e) {
					comu = false;
				}

				try {
					re = ReplyMessageDocument.Factory.parse(dnd
							.getDoNotification().getMessage().toString());
					repl = true;
				} catch (XmlException e) {
					repl = false;
				}

				if (noti) {
					outputToServlet.writeBytes(no.toString());
				} else {
					if (comu) {
						outputToServlet.writeBytes(co.toString());
					} else {
						if (repl) {
							outputToServlet.writeBytes(re.toString());
						} else {
							net.opengis.wns.x00.DoNotificationType.Message notdoc = net.opengis.wns.x00.DoNotificationType.Message.Factory
									.newInstance();
							notdoc.set(dnd.getDoNotification().getMessage());
							outputToServlet.writeBytes(notdoc.toString());
						}
					}
				}

				// write the message
				// Message notdoc = Message.Factory.newInstance();
				// notdoc.set(dnd.getDoNotification().getMessage());
				// outputToServlet.writeBytes(notdoc.toString());

				outputToServlet.flush();
				outputToServlet.close();

				servletConnection.getInputStream();
				this.log.debug("Successfully send message");
			}
		} catch (Exception e) {
			this.log.error("Error while sending message: " + e.toString());
			WNSException we = new WNSException("Error while sending message: "
					+ e.toString());
			throw we;
		}
	}
}
