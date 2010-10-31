/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package webofknowledge;

import javax.xml.namespace.QName;
import javax.servlet.http.Cookie;
import javax.xml.ws.BindingProvider;

import java.net.URL;

import java.util.Map;
import java.util.List;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import com.thomsonreuters.wokmws.cxf.woksearch.QueryParameters;
import com.thomsonreuters.wokmws.cxf.woksearch.RetrieveParameters;
import com.thomsonreuters.wokmws.cxf.woksearch.FullRecordSearchResults;
import com.thomsonreuters.wokmws.cxf.woksearch.WokSearch;
import com.thomsonreuters.wokmws.cxf.woksearch.WokSearchService;
import com.thomsonreuters.wokmws.cxf.woksearch.QueryField;
import com.thomsonreuters.wokmws.cxf.woksearch.EditionDesc;
import com.thomsonreuters.wokmws.cxf.woksearch.TimeSpan;

/**
 *
 * @author David Cliff
 */
public class Search
{

    private WokSearch searchPort = null;

    /**
     *
     */
    public Search()
    {
        QName searchServiceName = new QName(
            "http://woksearch.cxf.wokmws.thomsonreuters.com",
            "WokSearchService"
        );

        URL searchWsdlLocation = null;

        try
        {
            searchWsdlLocation = new URL("http://search.isiknowledge.com/esti/wokmws/ws/WokSearch?wsdl");
        }
        catch(Exception e)
        {

        }

        // Get our service and context
        WokSearchService searchService = new WokSearchService(searchWsdlLocation, searchServiceName);
        WokSearch sP = searchService.getWokSearchPort();

        searchPort = sP;
    }

    /**
     *
     * @return
     *      returns WokSearch
     *
     *      <br><br>
     *
     *      The WokSearch object contains the methods to perform a search against
     *      the WoS service.
     */
    public WokSearch retrieveSearchPort()
    {
        return searchPort;
    }

    /**
     *
     * @param databaseId
     * @param timeSpan
     * @param userQuery
     * @param editions
     * @return
     *      returns QueryParameters
     *
     *      <br><br>
     *
     *      The QueryParameters object is required to retrieve search results
     *      with the retrieveSearchResults method.
     */
    @SuppressWarnings("CallToThreadDumpStack")
    public QueryParameters getQueryParameters(String databaseId, TimeSpan timeSpan, String userQuery, List<EditionDesc> editions)
    {
        QueryParameters queryParameters = new QueryParameters();

        try
        {
            queryParameters.setDatabaseID(databaseId);
        }
        catch (Exception ex)
        {
            System.out.println(ex);
        }

        queryParameters.setQueryLanguage("en");

        try
        {
            queryParameters.setTimeSpan(timeSpan);
            queryParameters.setSymbolicTimeSpan(null);
        }
        catch (Exception ex)
        {
            System.out.println(ex);
        }

        try
        {
            queryParameters.setUserQuery(userQuery);
        }
        catch (Exception ex)
        {
            System.out.println(ex);
        }

        try
        {
            for (EditionDesc edition : editions)
            {
                queryParameters.getEditions().add(edition);
            }
        }
        catch (NullPointerException ex)
        {
            System.out.println("Error: Editions list is null - " + ex);
        }

        return queryParameters;
    }

    /**
     *
     * @param maxResultCount
     * @param firstRecord
     * @param sortFields
     * @return
     *      returns RetrieveParameters
     *
     *      <br><br>
     *
     *      The RetrieveParameters object is required to retrieve search results
     *      with the retrieveSearchResults method.
     */
    public RetrieveParameters getRetrieveParameters(int maxResultCount, int firstRecord, List<QueryField> sortFields)
    {
        RetrieveParameters retrieveParameters = new RetrieveParameters();

        try
        {
            retrieveParameters.setCount(maxResultCount);
        }
        catch (NullPointerException ex)
        {
            System.out.println("Error: Maximum records to retrieve is null - " + ex);
        }
        try
        {
            retrieveParameters.setFirstRecord(firstRecord);
        }
        catch (NullPointerException ex)
        {
            System.out.println("Error: Start record is null - " + ex);
        }

        try
        {
            for (QueryField queryField : sortFields)
            {
                retrieveParameters.getFields().add(queryField);
            }
        }
        catch (NullPointerException ex)
        {
            System.out.println("Error: Sort fields list is null - " + ex);
        }

        return retrieveParameters;
    }

    /**
     *
     * @param searchPort
     * @param sessionId
     * @param queryParameters
     * @param retrieveParameters
     * @return
     *      returns FullRecordSearchResults
     *
     *      <br><br>
     *
     *      The FullRecordSearchResults object contains the records found after
     *      performing a search.
     * @throws Exception
     */
    public FullRecordSearchResults retrieveSearchResults
    (
        WokSearch searchPort,
        String sessionId,
        QueryParameters queryParameters,
        RetrieveParameters retrieveParameters
    )
    throws Exception
    {
        //using SID
        BindingProvider bindingProvider = (BindingProvider)searchPort;
        Map<String, Object> requestContext = bindingProvider.getRequestContext();

        requestContext.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        Cookie cookie = new Cookie("SID", sessionId);
        Client client = ClientProxy.getClient(searchPort);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setCookie(cookie.getName() + "=" + cookie.getValue());
        http.setClient(httpClientPolicy);

        FullRecordSearchResults sR = new FullRecordSearchResults();

        try
        {
           sR = searchPort.search(queryParameters, retrieveParameters);
        }
        catch (Exception e)
        {
            System.out.println(e);
        }

        return sR;
    }



}