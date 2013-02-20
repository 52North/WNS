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

import java.util.Vector;

import net.opengis.wns.x00.CommunicationProtocolType;
import net.opengis.wns.x00.RegisterMultiUserType;
import net.opengis.wns.x00.RegisterSingleUserType;
import net.opengis.wns.x00.UpdateMultiUserRegistrationType.AddUser;
import net.opengis.wns.x00.UpdateMultiUserRegistrationType.RemoveUser;

import org.x52North.wns.v2.WNSUserDocument;
import org.x52North.wns.v2.WNSUserDocument.WNSUser.MultiUser.User.AssociatedUsers;
import org.x52North.wns.v2.WNSUserDocument.WNSUser.SingleUser.User.Channel;

/**
 * This class provides the necassary methods to handle user
 * 
 * @author Dennis Dahlmann
 * @version 2.0
 */
public class WNSUser {
	private String name;

	private long userID;

	private String[] multiUserID;

	private boolean multiUser;

	private String[] email;

	private String[] fax;

	private String[] sms;

	private String[] xmpp;

	private String[] phone;

	private String[] http;

	/**
	 * 
	 * @param rsut
	 *            The XML document to register a SingleUser, see schema
	 * @param id
	 */
	public WNSUser(RegisterSingleUserType rsut, long id) {
		this.name = rsut.getName();
		this.userID = id;

		if (rsut.getCommunicationProtocol().getEmailArray() != null) {
			this.email = rsut.getCommunicationProtocol().getEmailArray();
		}
		if (rsut.getCommunicationProtocol().getXMPPArray() != null) {
			this.xmpp = rsut.getCommunicationProtocol().getXMPPArray();
		}
		if (rsut.getCommunicationProtocol().getHTTPArray() != null) {
			this.http = rsut.getCommunicationProtocol().getHTTPArray();
		}
		if (rsut.getCommunicationProtocol().getFaxArray() != null) {
			this.fax = new String[rsut.getCommunicationProtocol().getFaxArray().length];
			for (int i = 0; i <= this.fax.length - 1; i++) {
				this.fax[i] = rsut.getCommunicationProtocol().getFaxArray(i)
						.toString();
			}

		}
		if (rsut.getCommunicationProtocol().getPhoneArray() != null) {
			this.phone = new String[rsut.getCommunicationProtocol()
					.getPhoneArray().length];
			for (int i = 0; i <= this.phone.length - 1; i++) {
				this.phone[i] = rsut.getCommunicationProtocol()
						.getPhoneArray(i).toString();
			}

		}
		if (rsut.getCommunicationProtocol().getSMSArray() != null) {
			this.sms = new String[rsut.getCommunicationProtocol().getSMSArray().length];
			for (int i = 0; i <= this.sms.length - 1; i++) {
				this.sms[i] = rsut.getCommunicationProtocol().getSMSArray(i)
						.toString();
			}
		}
	}

	/**
	 * 
	 * @param rmut
	 *            The XML document to register a MultiUser
	 * @param id
	 */
	public WNSUser(RegisterMultiUserType rmut, long id) {
		this.userID = id;
		this.multiUser = true;
		this.multiUserID = rmut.getUserIDArray();
	}

	public WNSUser(
			WNSUserDocument.WNSUser.SingleUser.User userArray) {
		this.name = userArray.getName();
		this.userID = Long.parseLong(userArray.getID());
		for (int i = 0; i <= userArray.getChannelArray().length - 1; i++) {
			if (userArray.getChannelArray()[i]
					.getProtocol()
					.equals(
							WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.EMAIL)) {
				this.email = userArray.getChannelArray()[i].getTargetArray();
			}
			if (userArray.getChannelArray()[i]
					.getProtocol()
					.equals(
							WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.FAX)) {
				this.fax = userArray.getChannelArray()[i].getTargetArray();
			}
			if (userArray.getChannelArray()[i]
					.getProtocol()
					.equals(
							WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.PHONE)) {
				this.phone = userArray.getChannelArray()[i].getTargetArray();
			}
			if (userArray.getChannelArray()[i]
					.getProtocol()
					.equals(
							WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.SMS)) {
				this.sms = userArray.getChannelArray()[i].getTargetArray();
			}
			if (userArray.getChannelArray()[i]
					.getProtocol()
					.equals(
							WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.HTTP)) {
				this.http = userArray.getChannelArray()[i].getTargetArray();
			}
			if (userArray.getChannelArray()[i]
					.getProtocol()
					.equals(
							WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.XMPP)) {
				this.xmpp = userArray.getChannelArray()[i].getTargetArray();
			}

		}

	}

	public WNSUser(
			WNSUserDocument.WNSUser.MultiUser.User userArray) {
		this.userID = Long.parseLong(userArray.getID());
		this.multiUser = true;
		this.multiUserID = userArray.getAssociatedUsers().getIDArray();
	}

	/**
	 * Returns the name of the user
	 * 
	 * @return name String
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name of the user
	 * 
	 * @param name
	 *            user name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @return The associated users as in the schema
	 */
	public AssociatedUsers getAssociatedUsers() {
		if (this.isMultiUser()) {
			WNSUserDocument.WNSUser.MultiUser.User user = WNSUserDocument.WNSUser.MultiUser.User.Factory
					.newInstance();
			user.addNewAssociatedUsers();
			user.getAssociatedUsers().setIDArray(this.multiUserID);
			return user.getAssociatedUsers();
		} else {
			return null;
		}
	}

	/**
	 * Returns all available channels from a WNSUser
	 * 
	 * @return The vector which contains all available channels
	 */
	public Channel[] getChannels() {
		WNSUserDocument.WNSUser.SingleUser.User user = WNSUserDocument.WNSUser.SingleUser.User.Factory
				.newInstance();
		if ((this.email != null) && (this.email.length > 0)) {
			Channel chan = user.addNewChannel();
			chan
					.setProtocol(WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.EMAIL);
			chan.setTargetArray(this.email);
		}
		if ((this.fax != null) && (this.fax.length > 0)) {
			Channel chan = user.addNewChannel();
			chan
					.setProtocol(WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.FAX);
			chan.setTargetArray(this.fax);
		}
		if ((this.phone != null) && (this.phone.length > 0)) {
			Channel chan = user.addNewChannel();
			chan
					.setProtocol(WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.PHONE);
			chan.setTargetArray(this.phone);
		}
		if ((this.sms != null) && (this.sms.length > 0)) {
			Channel chan = user.addNewChannel();
			chan
					.setProtocol(WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.SMS);
			chan.setTargetArray(this.sms);
		}
		if ((this.http != null) && (this.http.length > 0)) {
			Channel chan = user.addNewChannel();
			chan
					.setProtocol(WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.HTTP);
			chan.setTargetArray(this.http);
		}
		if ((this.xmpp != null) && (this.xmpp.length > 0)) {
			Channel chan = user.addNewChannel();
			chan
					.setProtocol(WNSUserDocument.WNSUser.SingleUser.User.Channel.Protocol.XMPP);
			chan.setTargetArray(this.xmpp);
		}
		return user.getChannelArray();
	}

	/**
	 * gets the UserID
	 * 
	 * @return UserID long
	 */
	public long getUserID() {
		return this.userID;
	}

	/**
	 * sets the UserID
	 * 
	 * @param userid
	 *            long
	 */
	public void setUserID(long userid) {
		this.userID = userid;
	}

	/**
	 * Checks, if the WNSUser is a MultiUser or not
	 * 
	 * @return true, if the user is a MultiUser, false if not
	 */
	public boolean isMultiUser() {
		return this.multiUser;
	}

	/**
	 * 
	 * @param removeCommunicationProtocols
	 *            The protocols that should be removed
	 */
	public void removeProtocol(
			CommunicationProtocolType removeCommunicationProtocols) {

		if ((this.email != null) && (this.email.length > 0)
				&& (removeCommunicationProtocols.sizeOfEmailArray() > 0)) {
			// Only one element?
			if (this.email.length == 1) {
				this.email = null;
			} else {
				int emailInt = 0;
				String[] remove = removeCommunicationProtocols.getEmailArray();
				for (int i = 0; i <= remove.length - 1; i++) {
					for (int j = 0; j <= this.email.length - 1; j++) {
						if (remove[i].equalsIgnoreCase(this.email[j])) {
							this.email[j] = null;
							emailInt++;
						}
					}
				}
				String[] temp = new String[this.email.length - emailInt];
				int index = 0;
				for (int i = 0; i < this.email.length; i++) {
					if (this.email[i] != null) {
						temp[index] = this.email[i];
					}
				}
				this.email = temp;
			}
		}
		if ((this.fax != null) && (this.fax.length > 0)
				&& (removeCommunicationProtocols.sizeOfFaxArray() > 0)) {
			// Only one element?
			if (this.fax.length == 1) {
				this.fax = null;

			} else {
				int faxInt = 0;
				String[] remove = new String[removeCommunicationProtocols
						.sizeOfFaxArray()];
				for (int i = 0; i < this.fax.length; i++) {
					remove[i] = removeCommunicationProtocols.getFaxArray(i)
							.toString();
				}

				for (int i = 0; i <= remove.length - 1; i++) {
					for (int j = 0; j <= this.fax.length - 1; j++) {
						if (remove[i].equalsIgnoreCase(this.fax[j])) {
							this.fax[j] = null;
							faxInt++;
						}
					}
				}
				String[] temp = new String[this.fax.length - faxInt];
				int index = 0;
				for (int i = 0; i < this.fax.length; i++) {
					if (this.fax[i] != null) {
						temp[index] = this.fax[i];
					}
				}
				this.fax = temp;
			}
		}
		if ((this.phone != null) && (this.phone.length > 0)
				&& (removeCommunicationProtocols.sizeOfPhoneArray() > 0)) {
			// Only one element?
			if (this.phone.length == 1) {
				this.phone = null;

			} else {
				int phoneInt = 0;
				String[] remove = new String[removeCommunicationProtocols
						.sizeOfPhoneArray()];
				for (int i = 0; i <= removeCommunicationProtocols
						.getPhoneArray().length - 1; i++) {
					remove[i] = removeCommunicationProtocols.getPhoneArray(i)
							.toString();
				}

				for (int i = 0; i <= remove.length - 1; i++) {
					for (int j = 0; j <= this.phone.length - 1; j++) {
						if (remove[i].equalsIgnoreCase(this.phone[j])) {
							this.phone[j] = null;
							phoneInt++;
						}
					}
				}
				String[] temp = new String[this.phone.length - phoneInt];
				int index = 0;
				for (int i = 0; i < this.phone.length; i++) {
					if (this.phone[i] != null) {
						temp[index] = this.phone[i];
					}
				}
				this.phone = temp;
			}
		}
		if ((this.sms != null) && (this.sms.length > 0)
				&& (removeCommunicationProtocols.sizeOfSMSArray() > 0)) {
			// Only one element?
			if (this.sms.length == 1) {
				this.sms = null;

			} else {
				int smsInt = 0;
				String[] remove = new String[removeCommunicationProtocols
						.sizeOfSMSArray()];
				for (int i = 0; i <= removeCommunicationProtocols.getSMSArray().length - 1; i++) {
					remove[i] = removeCommunicationProtocols.getSMSArray(i)
							.toString();
				}

				for (int i = 0; i <= remove.length - 1; i++) {
					for (int j = 0; j <= this.sms.length - 1; j++) {
						if (remove[i].equalsIgnoreCase(this.sms[j])) {
							this.sms[j] = null;
							smsInt++;
						}
					}
				}
				String[] temp = new String[this.sms.length - smsInt];
				int index = 0;
				for (int i = 0; i < this.sms.length; i++) {
					if (this.sms[i] != null) {
						temp[index] = this.sms[i];
					}
				}
				this.sms = temp;
			}
		}
		if ((this.http != null) && (this.http.length > 0)
				&& (removeCommunicationProtocols.sizeOfHTTPArray() > 0)) {
			// Only one element?
			if (this.http.length == 1) {
				this.http = null;

			} else {
				int httpInt = 0;
				String[] remove = removeCommunicationProtocols.getHTTPArray();
				for (int i = 0; i <= remove.length - 1; i++) {
					for (int j = 0; j <= this.http.length - 1; j++) {
						if (remove[i].equalsIgnoreCase(this.http[j])) {
							this.http[j] = null;
							httpInt++;
						}
					}
				}
				String[] temp = new String[this.http.length - httpInt];
				int index = 0;
				for (int i = 0; i < this.http.length; i++) {
					if (this.http[i] != null) {
						temp[index] = this.http[i];
					}
				}
				this.http = temp;
			}
		}
		if ((this.xmpp != null) && (this.xmpp.length > 0)
				&& (removeCommunicationProtocols.sizeOfXMPPArray() > 0)) {
			// Only one element?
			if (this.xmpp.length == 1) {
				this.xmpp = null;

			} else {
				int xmppInt = 0;
				String[] remove = removeCommunicationProtocols.getXMPPArray();
				for (int i = 0; i <= remove.length - 1; i++) {
					for (int j = 0; j <= this.xmpp.length - 1; j++) {
						if (remove[i].equalsIgnoreCase(this.xmpp[j])) {
							this.xmpp[j] = null;
							xmppInt++;
						}
					}
				}
				String[] temp = new String[this.xmpp.length - xmppInt];
				int index = 0;
				for (int i = 0; i < this.xmpp.length; i++) {
					if (this.xmpp[i] != null) {
						temp[index] = this.xmpp[i];
					}
				}
				this.xmpp = temp;
			}
		}

	}

	/**
	 * 
	 * @param addCommunicationProtocols
	 *            The protocols that should be added
	 */
	public void addProtocol(CommunicationProtocolType addCommunicationProtocols) {

		if (addCommunicationProtocols.sizeOfEmailArray() > 0) {
			if (this.email != null) {
				String[] add = addCommunicationProtocols.getEmailArray();
				String[] newer = new String[this.email.length + add.length];
				for (int i = 0; i <= this.email.length - 1; i++) {
					newer[i] = this.email[i];
				}
				for (int j = 0; j <= add.length - 1; j++) {
					newer[this.email.length + j] = add[j];
				}
				this.email = newer;
			} else {
				this.email = addCommunicationProtocols.getEmailArray();
			}
		}

		if (addCommunicationProtocols.sizeOfFaxArray() > 0) {
			if (this.fax != null) {
				String[] temp = new String[addCommunicationProtocols
						.getFaxArray().length];
				for (int i = 0; i < temp.length; i++) {
					temp[i] = addCommunicationProtocols.getFaxArray(i)
							.toString();
				}

				String[] newer = new String[this.fax.length + temp.length];
				for (int i = 0; i <= this.fax.length - 1; i++) {
					newer[i] = this.fax[i];
				}
				for (int j = 0; j <= temp.length - 1; j++) {
					newer[this.fax.length + j] = temp[j];
				}
				this.fax = newer;
			} else {
				String[] temp = new String[addCommunicationProtocols
						.getFaxArray().length];
				for (int i = 0; i < temp.length; i++) {
					temp[i] = addCommunicationProtocols.getFaxArray(i)
							.toString();
				}
				this.fax = temp;
			}
		}
		if (addCommunicationProtocols.sizeOfPhoneArray() > 0) {
			if (this.phone != null) {
				String[] temp = new String[addCommunicationProtocols
						.getPhoneArray().length];
				for (int i = 0; i < temp.length; i++) {
					temp[i] = addCommunicationProtocols.getPhoneArray(i)
							.toString();
				}
				String[] newer = new String[this.phone.length + temp.length];
				for (int i = 0; i <= this.phone.length - 1; i++) {
					newer[i] = this.phone[i];
				}
				for (int j = 0; j <= temp.length - 1; j++) {
					newer[this.phone.length + j] = temp[j];
				}
				this.phone = newer;
			} else {
				String[] temp = new String[addCommunicationProtocols
						.getPhoneArray().length];
				for (int i = 0; i < temp.length; i++) {
					temp[i] = addCommunicationProtocols.getPhoneArray(i)
							.toString();
				}
				this.phone = temp;
			}
		}

		if (addCommunicationProtocols.sizeOfSMSArray() > 0) {
			if (this.sms != null) {
				String[] temp = new String[addCommunicationProtocols
						.getSMSArray().length];
				for (int i = 0; i < temp.length; i++) {
					temp[i] = addCommunicationProtocols.getSMSArray(i)
							.toString();
				}
				String[] newer = new String[this.sms.length + temp.length];
				for (int i = 0; i <= this.sms.length - 1; i++) {
					newer[i] = this.sms[i];
				}
				for (int j = 0; j <= temp.length - 1; j++) {
					newer[this.sms.length + j] = temp[j];
				}
				this.sms = newer;
			} else {
				String[] temp = new String[addCommunicationProtocols
						.getSMSArray().length];
				for (int i = 0; i < temp.length; i++) {
					temp[i] = addCommunicationProtocols.getSMSArray(i)
							.toString();
				}
				this.sms = temp;
			}
		}
		if (addCommunicationProtocols.sizeOfXMPPArray() > 0) {
			if (this.xmpp != null) {
				String[] add = addCommunicationProtocols.getXMPPArray();
				String[] newer = new String[this.xmpp.length + add.length];
				for (int i = 0; i <= this.xmpp.length - 1; i++) {
					newer[i] = this.xmpp[i];
				}
				for (int j = 0; j <= add.length - 1; j++) {
					newer[this.xmpp.length + j] = add[j];
				}
				this.xmpp = newer;
			} else {
				this.xmpp = addCommunicationProtocols.getXMPPArray();
			}
		}
		if (addCommunicationProtocols.sizeOfHTTPArray() > 0) {
			if (this.http != null) {
				String[] add = addCommunicationProtocols.getHTTPArray();
				String[] newer = new String[this.http.length + add.length];
				for (int i = 0; i <= this.http.length - 1; i++) {
					newer[i] = this.http[i];
				}
				for (int j = 0; j <= add.length - 1; j++) {
					newer[this.http.length + j] = add[j];
				}
				this.http = newer;
			} else {
				this.http = addCommunicationProtocols.getHTTPArray();
			}
		}
	}

	/**
	 * 
	 * @param removeUser
	 *            The user that should be removed from this MultiUser
	 * @return A String array of the users that are not valid, null if every
	 *         user is valid
	 */
	public String[] removeMultiUser(RemoveUser removeUser) {
		int size = removeUser.sizeOfIDArray();
		int found = 0, ok = 0;
		boolean foundbool = false;
		Vector<String> notFound = new Vector<String>();
		for (int i = 0; i < removeUser.sizeOfIDArray(); i++) {
			foundbool = false;
			for (int j = 0; j < this.multiUserID.length; j++) {
				if (removeUser.getIDArray(i).equalsIgnoreCase(
						this.multiUserID[j])) {
					this.multiUserID[j] = "";
					foundbool = true;
					ok++;
				}
			}
			if (!foundbool) {
				notFound.add(found, removeUser.getIDArray(i));
				found++;
			}
		}
		if (ok != size) {
			notFound.trimToSize();
			String[] temp = new String[notFound.size()];
			for (int i = 0; i < notFound.size(); i++) {
				temp[i] = notFound.get(i);
			}
			// Remove every empty entry
			String[] tempString = new String[this.multiUserID.length];
			int tempInt = 0;
			for (int i = 0; i < this.multiUserID.length; i++) {
				if (!this.multiUserID[i].equalsIgnoreCase("")) {
					tempString[tempInt] = this.multiUserID[i];
					tempInt++;
				}
			}
			this.multiUserID = new String[tempInt];
			for (int i = 0; i < tempInt; i++) {
				this.multiUserID[i] = tempString[i];
			}

			return this.multiUserID;
		}
		// Remove every empty entry
		String[] tempString = new String[this.multiUserID.length];
		int tempInt = 0;
		for (int i = 0; i < this.multiUserID.length; i++) {
			if (!this.multiUserID[i].equalsIgnoreCase("")) {
				tempString[tempInt] = this.multiUserID[i];
				tempInt++;
			}
		}
		this.multiUserID = new String[tempInt];
		for (int i = 0; i < tempInt; i++) {
			this.multiUserID[i] = tempString[i];
		}

		return null;
	}

	/**
	 * 
	 * @param addUser
	 *            The user that should be added
	 */
	public void addMultiUser(AddUser addUser) {
		// Workaround with arrays, not fine!!!
		Vector<Integer> vector = new Vector<Integer>();
		for (int i = 0; i < this.multiUserID.length; i++) {
			for (int j = 0; j < addUser.sizeOfIDArray(); j++) {
				try {
					if (this.multiUserID[i].equalsIgnoreCase(addUser
							.getIDArray(j))) {
						vector.addElement(j);
					}
				} catch (Exception e) {
				}

			}
		}

		Vector<String> newer = new Vector<String>();
		for (int i = 0; i < this.multiUserID.length; i++) {
			newer.addElement(this.multiUserID[i]);
		}
		for (int i = 0; i < addUser.sizeOfIDArray(); i++) {
			if (!vector.contains(i)) {
				newer.addElement(addUser.getIDArray(i));
			}
		}
		// Workaround
		this.multiUserID = new String[newer.size()];
		for (int i = 0; i < newer.size(); i++) {
			this.multiUserID[i] = newer.get(i);
		}
	}

	/**
	 * 
	 * @param id
	 *            The user that should be removed
	 */
	public void removeMultiUser(String id) {
		for (int j = 0; j < this.multiUserID.length; j++) {
			if (this.multiUserID[j].equalsIgnoreCase(id)) {
				this.multiUserID[j] = "";
			}
		}
		// Remove every empty entry
		String[] tempString = new String[this.multiUserID.length];
		int tempInt = 0;
		for (int i = 0; i < this.multiUserID.length; i++) {
			if (!this.multiUserID[i].equalsIgnoreCase("")) {
				tempString[tempInt] = this.multiUserID[i];
				tempInt++;
			}
		}
		this.multiUserID = new String[tempInt];
		for (int i = 0; i < tempInt; i++) {
			this.multiUserID[i] = tempString[i];
		}
	}

}
