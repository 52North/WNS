package org.n52.wns.mail;

import net.opengis.wns.x00.DoNotificationDocument;

import org.n52.wns.WNSException;
import org.x52North.wns.v2.WNSConfigDocument;

public interface IMailHandler {

	/**
	 * Sends a notification email
	 * 
	 * @param dnd
	 *            The DoNotificationDocument
	 * @param targets
	 *            A string array containing targets
	 * @throws WNSException
	 */
	public abstract void sendNotificationMessage(DoNotificationDocument dnd,
			String[] targets) throws WNSException;

	/**
	 * 
	 * @param target
	 *            The target adress
	 * @param subject
	 *            The subject of the email
	 * @param body
	 *            the message body
	 * @throws WNSException
	 */
	public abstract void sendExternalMessage(String target, String subject,
			String body) throws WNSException;

	/**
	 * 
	 * @param targets
	 *            A string array of targets
	 * @param subject
	 *            The subject of the email
	 * @param body
	 *            the message body
	 * @throws WNSException
	 */
	public abstract void sendExternalMessage(String[] targets, String subject,
			String body) throws WNSException;

	/** sets the mail session properties at startup */
	public abstract void setProperties(
			WNSConfigDocument.WNSConfig.RegisteredHandlers.EMailHandler mailhandler);

}