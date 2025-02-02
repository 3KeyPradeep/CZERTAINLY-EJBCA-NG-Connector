
package com.czertainly.ca.connector.ejbca.ws;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.9-b130926.1035
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "EjbcaWSService", targetNamespace = "http://ws.protocol.core.ejbca.org/", wsdlLocation = "file:/tmp/ejbca-ws-import/ejbcaws.wsdl")
public class EjbcaWSService
    extends Service
{

    private final static URL EJBCAWSSERVICE_WSDL_LOCATION;
    private final static WebServiceException EJBCAWSSERVICE_EXCEPTION;
    private final static QName EJBCAWSSERVICE_QNAME = new QName("http://ws.protocol.core.ejbca.org/", "EjbcaWSService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("file:/tmp/ejbca-ws-import/ejbcaws.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        EJBCAWSSERVICE_WSDL_LOCATION = url;
        EJBCAWSSERVICE_EXCEPTION = e;
    }

    public EjbcaWSService() {
        super(__getWsdlLocation(), EJBCAWSSERVICE_QNAME);
    }

    public EjbcaWSService(WebServiceFeature... features) {
        super(__getWsdlLocation(), EJBCAWSSERVICE_QNAME, features);
    }

    public EjbcaWSService(URL wsdlLocation) {
        super(wsdlLocation, EJBCAWSSERVICE_QNAME);
    }

    public EjbcaWSService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, EJBCAWSSERVICE_QNAME, features);
    }

    public EjbcaWSService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public EjbcaWSService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns EjbcaWS
     */
    @WebEndpoint(name = "EjbcaWSPort")
    public EjbcaWS getEjbcaWSPort() {
        return super.getPort(new QName("http://ws.protocol.core.ejbca.org/", "EjbcaWSPort"), EjbcaWS.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns EjbcaWS
     */
    @WebEndpoint(name = "EjbcaWSPort")
    public EjbcaWS getEjbcaWSPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://ws.protocol.core.ejbca.org/", "EjbcaWSPort"), EjbcaWS.class, features);
    }

    private static URL __getWsdlLocation() {
        if (EJBCAWSSERVICE_EXCEPTION!= null) {
            throw EJBCAWSSERVICE_EXCEPTION;
        }
        return EJBCAWSSERVICE_WSDL_LOCATION;
    }

}
