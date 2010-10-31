/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package webofknowledge;

import javax.xml.namespace.QName;

import com.thomsonreuters.wokmws.cxf.auth.WOKMWSAuthenticate;
import com.thomsonreuters.wokmws.cxf.auth.WOKMWSAuthenticateService;

import java.util.Map;
import java.net.URL;

import javax.xml.ws.BindingProvider;
import javax.servlet.http.Cookie;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

/**
 *
 * @author David Cliff
 */
public class Authorize
{

    private String SID = "";
    private WOKMWSAuthenticate authPort = null;

    /**
     *
     * The public constructor that uses User Name and Password. The alternative
     * constructor is IP only (no user credentials).
     *
     * @param username
     * @param password
     */
    public Authorize(String username, String password)
    {

        QName authServiceName = new QName(
            "http://auth.cxf.wokmws.thomsonreuters.com",
            "WOKMWSAuthenticateService"
        );

        URL authWsdlLocation = null;

        try
        {
            authWsdlLocation = new URL("http://search.isiknowledge.com/esti/wokmws/ws/WOKMWSAuthenticate?wsdl");
        }
        catch(Exception e)
        {

        }

        //  Locate the WOKMWSAuthenticate service

        WOKMWSAuthenticateService authService = new WOKMWSAuthenticateService(authWsdlLocation, authServiceName);

        authPort = authService.getWOKMWSAuthenticatePort();

        // Retrieve the outgoing message context
        // in order to set the setMaintainSession flag ( participate in session )

        BindingProvider bp = (BindingProvider) authPort;
        Map<String, Object> context = bp.getRequestContext();

        // Set our maintain session flag
        context.put(javax.xml.ws.BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        // Now that we have agreed to participate in the server session,
        // set the Authorization HTTP header
        // Set the Username password in the Authorization header

        context.put(javax.xml.ws.BindingProvider.USERNAME_PROPERTY, username);
        context.put(javax.xml.ws.BindingProvider.PASSWORD_PROPERTY, password);

        // Send request to WOKMWSAuthenticate.authenticate
        // Return object is a String: the session identifier

        String session_identifier = null;

        try
        {
            session_identifier = authPort.authenticate();
        }
        catch (Exception e)
        {
              System.out.println(e);
        }

        if(session_identifier != null)
        {
            SID = session_identifier;
        }
    }

    /**
     *
     * This is the IP Only public constructor
     *
     */
    public Authorize()
    {

        QName authServiceName = new QName(
            "http://auth.cxf.wokmws.thomsonreuters.com",
            "WOKMWSAuthenticateService"
        );

        URL authWsdlLocation = null;

        try
        {
            authWsdlLocation = new URL("http://search.isiknowledge.com/esti/wokmws/ws/WOKMWSAuthenticate?wsdl");
        }
        catch(Exception e)
        {

        }

        //  Locate the WOKMWSAuthenticate service

        WOKMWSAuthenticateService authService = new WOKMWSAuthenticateService(authWsdlLocation, authServiceName);

        authPort = authService.getWOKMWSAuthenticatePort();

        // Retrieve the outgoing message context
        // in order to set the setMaintainSession flag ( participate in session )

        BindingProvider bp = (BindingProvider) authPort;
        Map<String, Object> context = bp.getRequestContext();

        // Set our maintain session flag
        context.put(javax.xml.ws.BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        // Send request to WOKMWSAuthenticate.authenticate
        // Return object is a String: the session identifier

        String session_identifier = null;

        try
        {
            session_identifier = authPort.authenticate();
        }
        catch (Exception e)
        {
              System.out.println(e);
        }

        if(session_identifier != null)
        {
            SID = session_identifier;
        }
    }

    /**
     *
     * @return
     *      returns SID
     *
     *      <br><br>
     *
     *      The SID string is the authentication token used for searches
     *      against the WoS service
     */
    public String returnSID()
    {
        return SID;
    }

    /**
     *
     * @return
     *      returns WOKMWSAuthenticate
     *
     *      <br><br>
     *
     *      The WOKMWSAuthenticate object contains the SID variable used for
     *      searching the WoS Service
     */
    public WOKMWSAuthenticate retrieveAuthPort()
    {
        return authPort;
    }

    /**
     *
     */
    public void closeSession()
    {
        //using SID
        BindingProvider bindingProvider = (BindingProvider)authPort;
        Map<String, Object> requestContext = bindingProvider.getRequestContext();

        requestContext.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        Cookie cookie = new Cookie("SID", SID);
        Client client = ClientProxy.getClient(authPort);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setCookie(cookie.getName() + "=" + cookie.getValue());
        http.setClient(httpClientPolicy);

        try
        {
            authPort.closeSession();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }
}