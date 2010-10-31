package webofknowledge;

import com.thomsonreuters.wokmws.cxf.woksearch.WokSearch;
import com.thomsonreuters.wokmws.cxf.woksearch.TimeSpan;
import com.thomsonreuters.wokmws.cxf.woksearch.EditionDesc;
import com.thomsonreuters.wokmws.cxf.woksearch.QueryField;
import com.thomsonreuters.wokmws.cxf.woksearch.QueryParameters;
import com.thomsonreuters.wokmws.cxf.woksearch.RetrieveParameters;
import com.thomsonreuters.wokmws.cxf.woksearch.FullRecordSearchResults;

import java.io.*;
import java.util.*;

/**
 *
 * @author sandy
 */

public class Main
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        boolean IPOnly = true;
        boolean connectionChosen = false;

        String username = "";
        String password = "";
        String outputFilename = "records.xml";

        String userQueryString = "";

        for (int i = 0; i < args.length; i++) 
        {
            try
            {
                //IPOnly flag or Username + Password
                if(args[i].equalsIgnoreCase("-IPOnly"))
                {
                    //default setting
                    connectionChosen = true;
                }
                else if(args[i].equalsIgnoreCase("-login"))
                {
                    IPOnly = false;
                    connectionChosen = true;
                    username = args[(i + 1)];
                    password = args[(i + 2)];
                }
                else if(args[i].equalsIgnoreCase("-output"))
                {
                    outputFilename = args[(i + 1)];
                }
                else if(args[i].equalsIgnoreCase("-userQuery"))
                {
                    userQueryString = stripLeadingAndTrailingQuotes(args[(i + 1)]);
                }
                else if(args[i].equalsIgnoreCase("-h"))
                {
                    consoleHelp();
                }
            }
            catch(Exception e)
            {
                consoleHelp();                
            }
        }

        if(!connectionChosen)
        {
            consoleHelp();            
        }

        String finalOutput = "";

        String timeBeginString = " ";
        String timeEndString = " ";        
        String collectionString = " ";
        String editionString = " ";

        //delete old output if it exists
        File r = new File(outputFilename);

        if(r.exists())
        {
            r.delete();
        }     

        //retrieving search query details from external file
        try
        {
            FileInputStream fstream = new FileInputStream("connection.settings");

            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            //Read File Line By Line
            while ((strLine = br.readLine()) != null)
            {
                StringBuilder sB = new StringBuilder(strLine);

                //Retrieve setting
                if(strLine.indexOf("TIMEBEGIN:") != -1)
                {
                    int startChar = strLine.indexOf(" ");
                    sB.delete(0, startChar);
                    String s = sB.toString();
                    timeBeginString = s.trim();
                }
                else if(strLine.indexOf("TIMEEND:") != -1)
                {
                    int startChar = strLine.indexOf(" ");
                    sB.delete(0, startChar);
                    String s = sB.toString();

                    s.trim();
                    timeEndString = s.trim();
                }
                else if(strLine.indexOf("COLLECTION:") != -1)
                {
                    int startChar = strLine.indexOf(" ");
                    sB.delete(0, startChar);
                    String s = sB.toString();
                    collectionString = s.trim();
                }
                else if(strLine.indexOf("EDITION:") != -1)
                {
                    int startChar = strLine.indexOf(" ");
                    sB.delete(0, startChar);
                    String s = sB.toString();                    
                    editionString = s.trim();
                }
            }

            //Close the input stream
            in.close();
        }
        catch (Exception e)
        {
            //Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        String SID = null;
        Authorize a = null;

        while(SID == null)
        {
            if(IPOnly)
            {
                a = new Authorize();
            }
            else
            {
                a = new Authorize(username, password);
            }

            SID = a.returnSID();

            if(SID != null)
            {
                //this is a kludge due to the fact that WoS occasionally includes
                //the @ symbol in the SID, yet seems unable to process it in the
                //string. URL encoding didn't improve the situation, so the method
                //to get a valid SID is put in a loop to ensure that the one used
                //doesn't contain the offending character.

                if(SID.contains("@"))
                {
                    //we dont close the session because, sadly, we can't use the
                    //SID
                    SID = null;
                }
                else
                {
                    System.out.println(SID);
                }
            }
        }
        
        Search s = new Search();

        WokSearch searchPort = s.retrieveSearchPort();

        //create QueryParameters
        String databaseId = "WOS";

        //set TimeSpan values
        TimeSpan timeSpan = new TimeSpan();
        timeSpan.setBegin(timeBeginString);
        timeSpan.setEnd(timeEndString);

        String userQuery = userQueryString;

        //set EditionDesc values
        EditionDesc edition = new EditionDesc();
        edition.setCollection(collectionString);
        edition.setEdition(editionString);

        List<EditionDesc> editionList = new ArrayList();
        editionList.add(edition);

        QueryParameters qP = s.getQueryParameters(databaseId, timeSpan, userQuery, editionList);

        //create RetrieveParamaters
        int totalRecords = 0;
        int recordCount = 0;
        int maxResult = 100;
        int firstRecord = 1;

        //loop to make sure all records are extracted into the XML output
        boolean loopComplete = false;

        while(loopComplete == false)
        {
            //set QueryField values
            QueryField sortField = new QueryField();
            sortField.setName("Date");
            sortField.setSort("D");

            List<QueryField> sortFieldList = new ArrayList();
            sortFieldList.add(sortField);

            RetrieveParameters rP = s.getRetrieveParameters(maxResult, firstRecord, sortFieldList);

            FullRecordSearchResults sR = null;

            //execute the search
            try
            {
                sR = s.retrieveSearchResults(searchPort, SID, qP, rP);
            }
            catch(Exception e)
            {
                System.out.println(e);
            }

            if(sR != null)
            {
                finalOutput = sR.getRecords();

                if(totalRecords == 0)
                {
                    totalRecords = sR.getRecordsFound();
                    System.out.println("Total Record Count: " + totalRecords);
                }
                else
                {
                    finalOutput = finalOutput.replace("<records>", "");
                }


                if((countRecords(sR.getRecords(), "</REC>") >= 1) && (recordCount < totalRecords))
                {
                    recordCount += (countRecords(sR.getRecords(), "</REC>"));
                    firstRecord = recordCount + 1;

                    if(firstRecord >= totalRecords)
                    {
                        loopComplete = true;
                    }
                }

                try
                {
                    BufferedWriter out = new BufferedWriter(new FileWriter(outputFilename, true));                    

                    if(loopComplete == false)
                    {
                        finalOutput = finalOutput.replace("\n</records>", "");
                    }

                    out.append(finalOutput);
                    out.close();
                }
                catch (IOException e)
                {
                    System.out.println(e);
                }

            }
            System.out.println("Cumulative Record Count: " + recordCount);
        }

        a.closeSession();

    }

    private static int countRecords(String text, String search)
    {
        int count = 0;
        for (int fromIndex = 0; fromIndex > -1; count++)
            fromIndex = text.indexOf(search, fromIndex + ((count > 0) ? 1 : 0));
        return count - 1;
    }

    private static String stripLeadingAndTrailingQuotes(String str)
    {
      if (str.startsWith("\""))
      {
          str = str.substring(1, str.length());
      }
      if (str.endsWith("\""))
      {
          str = str.substring(0, str.length() - 1);
      }
      return str;
    }

    private static void consoleHelp()
    {
        System.out.print("Correct usage of this application: ");
        System.out.println("java -jar WebOfScience.jar <flags>\n");
        System.out.println("-IPOnly OR -login must be used");
        System.out.println("If an output filename isn't chosen, the default records.xml will be used");
        System.out.println("\n\tFlags:");
        System.out.println("\n\t-IPOnly");
        System.out.println("\t-login username password");
        System.out.println("\t-userQuery \"query\"");
        System.out.println("\t-output filename");
        System.out.println("\t-h - Display this help menu\n");
        System.exit(0);
    }
}