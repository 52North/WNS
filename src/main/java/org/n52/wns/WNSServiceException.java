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

import java.util.ArrayList;

import net.opengis.ows.ExceptionDocument;
import net.opengis.ows.ExceptionReportDocument;
import net.opengis.ows.ExceptionReportDocument.ExceptionReport;
import net.opengis.ows.ExceptionType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception class
 * 
 * @author Johannes Echterhoff, Dennis Dahlmann
 * @version 2.0
 * 
 */
public class WNSServiceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -767146317340022719L;

	public enum ExceptionCode {
		// standard OGC codes
		OperationNotSupported, MissingParameterValue, InvalidParameterValue, VersionNegotiationFailed, InvalidUpdateSequence, NoApplicableCode,

		// WNS codes
		InvalidRequest, MessageIDExpired, MessageSendingFailed, UnknownUserID, ProtocolNotSupported, NoApplicable, XMPPServerLoginError;
	}

	public enum ExceptionLevel {
		DetailedExceptions, PlainExceptions
	}

	private static Logger logger = LoggerFactory.getLogger(WNSServiceException.class);

	private ExceptionLevel excLevel = null;

	private ArrayList<ExceptionType> excs = new ArrayList<ExceptionType>();

	public WNSServiceException(ExceptionLevel excLevelIn) {
		this.excLevel = excLevelIn;
	}

	public void addCodedException(ExceptionCode code, String locator) {

		ExceptionType et = ExceptionType.Factory.newInstance();
		et.setExceptionCode(code.toString());

		if (locator != null) {
			et.setLocator(locator);
		}

		this.excs.add(et);
	}

	public void addCodedException(ExceptionCode code, String locator,
			Exception e) {

		ExceptionType et = ExceptionType.Factory.newInstance();
		et.setExceptionCode(code.toString());
		if (locator != null) {
			et.setLocator(locator);
		}

		String name = e.getClass().getName();
		String message = e.getMessage();
		StackTraceElement[] stackTraces = e.getStackTrace();

		StringBuffer sb = new StringBuffer();
		sb.append("[EXC] internal service exception");
		if (this.excLevel.compareTo(ExceptionLevel.PlainExceptions) == 0) {
			sb.append(". Message: " + message);
		} else if (this.excLevel.compareTo(ExceptionLevel.DetailedExceptions) == 0) {
			sb.append(": " + name + "\n");
			sb.append("[EXC] message: " + message + "\n");
			for (int i = 0; i < stackTraces.length; i++) {
				StackTraceElement element = stackTraces[i];
				sb.append("[EXC] " + element.toString() + "\n");
			}
		} else {
			logger.warn("addCodedException: unknown ExceptionLevel " + "("
					+ this.excLevel.toString() + ")occurred.");
		}

		et.addExceptionText(sb.toString());
		// TODO i guess there is a better way to format an exception

		this.excs.add(et);
	}

	public void addCodedException(ExceptionCode code, String locator,
			String message) {

		ExceptionType et = ExceptionType.Factory.newInstance();
		et.setExceptionCode(code.toString());

		if (locator != null) {
			et.setLocator(locator);
		}
		if (message != null) {
			et.addExceptionText(message);
		}

		this.excs.add(et);
	}

	public void addCodedException(ExceptionCode code, String locator,
			String[] messages) {
		ExceptionType et = ExceptionType.Factory.newInstance();
		et.setExceptionCode(code.toString());
		if (locator != null) {
			et.setLocator(locator);
		}
		for (int i = 0; i < messages.length; i++) {
			String string = messages[i];
			et.addExceptionText(string);
		}
		this.excs.add(et);
	}

	public void addExceptionReport(ExceptionDocument ed) {
		this.excs.add(ed.getException());
	}

	public void addExceptionReport(ExceptionReportDocument erd) {
		for (ExceptionType et : erd.getExceptionReport().getExceptionArray()) {
			this.excs.add(et);
		}
	}

	public void addServiceException(WNSServiceException seIn) {
		this.excs.addAll(seIn.getExceptions());
	}

	public boolean containsCode(ExceptionCode ec) {
		for (ExceptionType et : this.excs) {
			if (et.getExceptionCode().equalsIgnoreCase(ec.toString())) {
				return true;
			}
		}
		return false;
	}

	public boolean containsExceptions() {
		return this.excs.size() > 0;
	}

	public ExceptionReportDocument getDocument() {

		ExceptionReportDocument erd = ExceptionReportDocument.Factory
				.newInstance();
		ExceptionReport er = ExceptionReport.Factory.newInstance();
		er.setLanguage("en");
		er.setVersion(WNSConstants.OWSEXCEPTIONREPORTVERSION); // TODO what
		// version??
		// insert the
		// number
		// dynamically??
		er.setExceptionArray(this.excs.toArray(new ExceptionType[this.excs
				.size()]));
		erd.setExceptionReport(er);

		return erd;
	}

	public ArrayList<ExceptionType> getExceptions() {
		return this.excs;
	}

	@Override
	public String getMessage() {
		return this.getDocument().toString();
	}
}
