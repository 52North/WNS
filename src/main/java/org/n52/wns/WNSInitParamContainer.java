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

import org.n52.wns.WNSServiceException.ExceptionLevel;
import org.n52.wns.db.DAOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.x52North.wns.v2.WNSConfigDocument;
import org.x52North.wns.v2.WNSConfigDocument.WNSConfig;

/**
 * Singleton holding information and objects required by other components of the WNS.
 * 
 * @author Johannes Echterhoff
 * 
 */
public class WNSInitParamContainer {

    private static Logger log = LoggerFactory.getLogger(WNSInitParamContainer.class);

    private static WNSInitParamContainer instance = null;

    private WNSConfig wnsc = null;

    // private Properties daoProps = null;

    private ExceptionLevel exceptionLevel = ExceptionLevel.PlainExceptions;

    private DAOFactory daoFactory = null;

    private String wnsSchemaLocation = null;

    private String ogcExcSchemaLocation = null;

    private String wnsUrl = null;

    private String wsdlPath = null;

    private WNSDatabaseThread thread = null;

    private WNSInitParamContainer(WNSConfig wnsc) throws WNSException {

        this.wnsc = wnsc;

        try {
            // initialize DAOFactory
            String driver = wnsc.getServiceProperties().getDAOFactoryDriver();
            Class c = Class.forName(driver);
            this.daoFactory = (DAOFactory) c.newInstance();
            this.daoFactory.configure(wnsc.getServiceProperties().getDataBaseProperties());

        }
        catch (Exception e) {
            String message = "Failed to initialise DAOFactory. ";
            log.error(message, e);
            throw new WNSException(message, e);
        }

        // this.daoProps = daoProps;

        if (wnsc.getServiceProperties().getExceptionLevel().equals(WNSConfig.ServiceProperties.ExceptionLevel.PLAIN_EXCEPTIONS)) {
            this.exceptionLevel = ExceptionLevel.PlainExceptions;
        }
        else {
            this.exceptionLevel = ExceptionLevel.DetailedExceptions;
        }

        this.wnsSchemaLocation = wnsc.getServiceProperties().getWNSSchemaLocation();
        this.ogcExcSchemaLocation = wnsc.getServiceProperties().getOWSSchemaLocation();
        this.wnsUrl = wnsc.getServiceProperties().getWNSURL();

        this.wsdlPath = wnsc.getServiceProperties().getWSDLDocumentPath();

    }

    /**
     * @param wnscd
     * @return The WNSInitParamContainer singleton
     * @see WNSInitParamContainer
     * @throws WNSException
     *         If the WNSInitParamContainer could not be initialised.
     */
    public static synchronized WNSInitParamContainer createInstance(WNSConfigDocument wnscd) throws WNSException {

        if (instance == null) {

            // load properties for DAOFactory
            // String daoFacPropsLocation = wnscd.getWNSConfig()
            // .getServiceProperties().getDAOFactoryPropertiesLocation();
            // Properties daoProps = new Properties();
            // FileInputStream in;
            // try {
            // in = new FileInputStream(daoFacPropsLocation);
            // daoProps.load(in);
            // in.close();
            // } catch (IOException e) {
            // String message = "Could not load properties for DAOFactory. ";
            // log.fatal(message + e.toString());
            // throw new WNSException(message, e);
            // }

            // instance = new WNSInitParamContainer(wnscd.getWNSConfig(),
            // daoProps);
            instance = new WNSInitParamContainer(wnscd.getWNSConfig());

            instance.thread = new WNSDatabaseThread(wnscd.getWNSConfig().getServiceProperties().getDataBaseProperties().getCheckDBDuration(),
                                                    instance.daoFactory.getMessageDAO());

        }
        return instance;
    }

    public static WNSInitParamContainer getInstance() {
        return instance;
    }

    public ExceptionLevel getExceptionLevel() {
        return this.exceptionLevel;
    }

    // public Properties getWNSDAOFactoryProperties() {
    // return daoProps;
    // }

    public String getOgcExceptionSchemaLocation() {
        return this.ogcExcSchemaLocation;
    }

    public String getWnsSchemaLocation() {
        return this.wnsSchemaLocation;
    }

    /**
     * @return Returns the url of the WNS.
     */
    public String getWnsUrl() {
        return this.wnsUrl;
    }

    /**
     * @return Returns the DAOFactory.
     */
    public DAOFactory getDAOFactory() {
        return this.daoFactory;
    }

    public WNSConfig getWnsConfig() {
        return this.wnsc;
    }

    public String getWSDLDocumentPath() {
        return this.wsdlPath;
    }

    public WNSDatabaseThread getDatabaseThread() {
        return thread;
    }

}
