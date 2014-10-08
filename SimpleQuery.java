/*
    Written by Tyler Raborn 

        Prints the contents of N rows of M columns of the specified database to the command line or a specified output file.
      
        USAGE:
            
            The following flags can be used in any order without consequence.
            ./simplequery -q <"SQL Statement"> -> outputs the result of a custom SQL statement (NOTICE THE QUOTES!)
            ./simplequery -assert -> display only rows with failures
            ./simplequery -f <filename.txt> -> dump output to file 
            ./simplequery -df <datafeed> -> print table data for specific datafeed
            ./simplequery -r <# of rows> ->  specify number of rows to output (default is 15, -1 is unlimited)
            ./simplequery -db <database name> -> override default database name
            ./simplequery -hn <host name> -> override default hostname
            ./simplequery -hp <host port #> -> override default host port #
            ./simplequery -exec <filename.txt> -> execute queries in given file

            The following must be run individually.
            ./simplequery -sd -> set default username/password
            ./simplequery -sdb -> set default database/host/port#            
            ./simplequery -cleardata -> clear stored credentials
            ./simplequery -cleardb -> clear stored database settings
            ./simplequery -help -> display options
            ./simplequery -version -> print version info

            UPCOMING FEATURES:
            The ability to dynamically set which table attributes are displayed
*/

import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import java.net.*;
import java.sql.*;

public class SimpleQuery
{
    final static String credentialFileName = "SQ_logondata.txt";
    final static String databaseInfoFileName = "SQ_dbdata.txt";

    private static ArrayList<String> queryList;

    static int MAX_ROWS;
    static String datafeed = null;
    //static String hostName = new String("edidluslvd01"); 
    static String hostName = new String("class3.cs.pitt.edu");
    static String hostAddr = null;
    static int hostPort = 1521;
    //static String dbName = new String("SVDEV");
    static String dbName = new String("dbclass.cs.pitt.edu");
    static String queryString; 

    static boolean isCustomQuery = false;
    static boolean isAssertion = false;
    static boolean outputToFile = false;
    static boolean readFromFile = false;

    static File inputFile;
    static File outputFile;
    static File userInfoFile;
    static File databaseInfoFile;
    static BufferedWriter fileWriter;
    static BufferedReader fileReader;

    static ArrayDeque<Query> queryObjectList;
    static ArrayDeque<Query> queryResultList;

    public static void main(String[] args)
    {
        if (args.length == 0)
        {
            System.out.println("Invalid arguments. For usage examples, run \'simplequery -help\'");
            System.exit(1);
        }

        List<String> argList = Arrays.asList(args);
        int validArgsCount = 0;

        userInfoFile = new File(credentialFileName);
        databaseInfoFile = new File(databaseInfoFileName);

        System.out.println("\n**SimpleQuery 0.3**\n");

        //process dynamic arguments
        if (argList.contains("-q"))
        {
            int index = argList.indexOf("-q");
            index++;

            isCustomQuery = true;
            queryString = args[index];
            System.out.println("statement is " + queryString);
            validArgsCount+=2;
        }

        if (argList.contains("-f"))
        {
            int index = argList.indexOf("-f");
            index++;

            outputFile = new File(args[index]);

            try
            {
                fileWriter = new BufferedWriter(new FileWriter(outputFile));

                if (!outputFile.exists())
                {
                    outputFile.createNewFile();
                }           
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            outputToFile = true; 
            validArgsCount+=2;
        }

        if (argList.contains("-exec")) 
        {
            int index = argList.indexOf("-exec");
            index++;

            inputFile = new File(args[index]);
            queryList = new ArrayList<String>();
            queryObjectList = new ArrayDeque<Query>();

            try 
            {
                fileReader = new BufferedReader(new FileReader(inputFile));
                //load queries into queryList:
                String buff = null;
                while ((buff = fileReader.readLine()) != null) {
                    System.out.println("Adding line to list of queries: " + buff);
                    queryList.add(buff);
                }
            }
            catch (FileNotFoundException e) 
            {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            Query theQuery = new Query(queryList); //read in list of queries
            queryObjectList.push(theQuery); //add query object to list of query objects containing lists of queries
            
            readFromFile = true;
            validArgsCount+=2;
        }

        if (argList.contains("-r"))
        {
            int index = argList.indexOf("-r");
            index++;

            MAX_ROWS = Integer.parseInt(args[index]);
            validArgsCount+=2;
        }

        if (argList.contains("-df"))
        {
            int index = argList.indexOf("-df");
            index++;

            datafeed = new String(args[index]);
            validArgsCount+=2;
        }

        if (argList.contains("-assert"))
        {
            isAssertion = true;
            validArgsCount+=1;
        }

        if (argList.contains("-db"))
        {
            int index = argList.indexOf("-db");
            index++;

            dbName = new String(args[index]);
            validArgsCount+=2;
        }

        if (argList.contains("-hn"))
        {
            int index = argList.indexOf("-hn");
            index++;
            
            hostPort = Integer.parseInt(args[index]);
            validArgsCount+=2;
        }

        if (argList.contains("-hp"))
        {
            int index = argList.indexOf("-hp");
            index++;

            hostName = new String(args[index]);
            validArgsCount+=2;
        }

        if (validArgsCount < args.length) //check for bad arguments AND ensure singleton arguments are not mixed with dynamic ones
        {
            if (args.length != 1)
            {
                System.out.println("Invalid arguments. For usage examples, run \'simplequery -help\'");
                System.exit(1);                                
            }
            else if (!argList.contains("-sd") && !argList.contains("-help") && !argList.contains("-version") && !argList.contains("cleardata") && !argList.contains("-sdb") && !argList.contains("-cleardb"))
            {
                System.out.println("Invalid arguments. For usage examples, run \'simplequery -help\'");
                System.exit(1);                      
            }
        }

        //process static arguments
        if (argList.contains("-sd"))
        {
            try
            {
                setDefaultUserData();    
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            
            System.exit(1);
        }
        else if (argList.contains("-help"))
        {
            displayHelp();
            System.exit(1);
        }
        else if (argList.contains("-version"))
        {
            versionInfo();
            System.exit(1);
        }
        else if (argList.contains("-cleardata"))
        {
            try
            {
                if (userInfoFile.exists())
                {
                    Runtime rt = Runtime.getRuntime();
                    Process proc = rt.exec("rm " + credentialFileName);  
                    System.out.println("Stored user credentials have been deleted.");                    
                }
                else
                {
                    System.out.println("Unable to clear data; data does not exist.");
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            System.exit(1);
        }
        else if (argList.contains("-sdb"))
        {
            try 
            {
                setDefaultConnectionData();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            
            System.exit(1);
        }
        else if (argList.contains("-cleardb"))
        {
            try
            {
                if (databaseInfoFile.exists())
                {
                    Runtime rt = Runtime.getRuntime();
                    Process proc = rt.exec("rm " + databaseInfoFileName);  
                    System.out.println("Stored database properties have been deleted.");                    
                }
                else
                {
                    System.out.println("Unable to clear data; data does not exist.");
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            System.exit(1);
        }        

        Scanner passwordScanner = null;
        Scanner usernameScanner = null;
        String username = null;
        String attemptedPassword = null;
        String encryptedPassword = null;
        String saltString = null;
        String input = null;

        if (databaseInfoFile.exists())
        {
            BufferedReader r = null;

            try
            {
                r = new BufferedReader(new FileReader(databaseInfoFileName));    

                int linecount = 0;
                while((input = r.readLine()) != null)
                {
                    if (linecount == 0)
                    {
                        hostName = new String(input.substring(11, input.length()));
                    }
                    else if (linecount == 1)
                    {
                        hostPort = Integer.parseInt(input.substring(11, input.length()));
                    }
                    else if (linecount == 2)
                    {
                        dbName = new String(input.substring(9, input.length()));
                    }

                    linecount++;
                }

                r.close();
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }    
            catch (IOException e)
            {
                e.printStackTrace();
            }       

            //System.out.println("Server data read in: Hostname: " + hostName + " Host port: " + hostPort + " db name: " + dbName);
        } //else use default in-code presets (test server #1)


        if (!userInfoFile.exists())
        {
            usernameScanner = new Scanner(System.in);
            System.out.println("Enter Username: ");
            username = usernameScanner.nextLine();

            passwordScanner = new Scanner(System.in);
            System.out.println("Enter Password: ");
            attemptedPassword = passwordScanner.nextLine();
        }
        else //read stored user info
        {
            PasswordEncryptionService decryptionService = new PasswordEncryptionService();
            BufferedReader r = null;

            try
            {
                r = new BufferedReader(new FileReader(userInfoFile));    

                int linecount = 0;
                while((input = r.readLine()) != null)
                {
                    if (linecount == 0)
                    {
                        username = new String(input.substring(7, input.length()));
                    }
                    else if (linecount == 1)
                    {
                        encryptedPassword = new String(input.substring(5, input.length()));
                    }

                    linecount++;
                }

                attemptedPassword = decryptionService.decrypt(encryptedPassword);

                r.close();
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }    
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (GeneralSecurityException e)
            {
                e.printStackTrace();
            }
        }

        initializeDatabase();

        try
        {
            queryDatabase(username, attemptedPassword);    
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if (outputToFile == true)
        {
            try
            {
                fileWriter.close();    
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        if (!userInfoFile.exists())
        {
            usernameScanner.close();
            passwordScanner.close();                
        }
    }

    private static void setDefaultUserData() throws IOException
    {
        String ans = null;
        String encryptedPassword = null;
        Scanner ansScanner = new Scanner(System.in);
        System.out.println("WARNING: The \"-sd\" option will enable SimpleQuery to retain your encrypted database access credentials locally on disk. No guarantees about security can be made. Continue? (y/n)");
        ans = ansScanner.nextLine();

        if (ans.equals("y"))
        {
            BufferedWriter userDataWriter = null;
            PasswordEncryptionService encryptionService = new PasswordEncryptionService();

            try
            {
                userDataWriter = new BufferedWriter(new FileWriter(userInfoFile));

                if (userInfoFile.exists() == false)
                {
                    userInfoFile.createNewFile();
                }           
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            Scanner newUsernameScanner = new Scanner(System.in);
            System.out.println("Please enter your new default username: ");
            String newUsername = newUsernameScanner.nextLine(); 

            // read in user password
            Scanner newPasswordScanner = new Scanner(System.in);
            System.out.println("Please enter your new password: ");

            try //encrypt user password
            {                
                String newPassword = newPasswordScanner.nextLine();   
                encryptedPassword = encryptionService.encrypt(newPassword);
            }
            catch (GeneralSecurityException e)
            {
                e.printStackTrace();
            }

            userDataWriter.write("_UNAME_" + newUsername + "\r\n");
            userDataWriter.write("_PWD_" + encryptedPassword + "\r\n");

            System.out.println("Username and password have been set. Run \'simplequery -cleardata\' to remove.");

            try
            {
                userDataWriter.close();    
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }            

            newUsernameScanner.close();
            newPasswordScanner.close();

            System.exit(1);
        }
        else
        {
            System.out.println("User data not saved.");
            System.exit(1);
        }
    }

    private static void setDefaultConnectionData() throws IOException
    {
        String ans;
        Scanner ansScanner = new Scanner(System.in);
        System.out.println("WARNING: The \"-sdb\" option will enable SimpleQuery to retain database information locally on disk. No guarantees about security can be made. Continue? (y/n)");
        ans = ansScanner.nextLine();

        if (ans.equals("y"))
        {
            BufferedWriter dbDataWriter = null;

            try
            {
                dbDataWriter = new BufferedWriter(new FileWriter(databaseInfoFile));

                if (databaseInfoFile.exists() == false)
                {
                    databaseInfoFile.createNewFile();
                }           
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            Scanner newHostNameScanner = new Scanner(System.in);
            System.out.println("Please enter a new default database host name: ");
            String newHostName = newHostNameScanner.nextLine();

            Scanner newHostPortScanner = new Scanner(System.in);
            System.out.println("Please enter a new default database host port #: ");
            String newHostPort = newHostPortScanner.nextLine();

            Scanner newDBNameScanner = new Scanner(System.in);
            System.out.println("Please enter a new default database name: ");
            String newDBName = newDBNameScanner.nextLine();

            dbDataWriter.write("_HOST_NAME_" + newHostName + "\r\n");
            dbDataWriter.write("_HOST_PORT_" + newHostPort + "\r\n");
            dbDataWriter.write("_DB_NAME_" + newDBName + "\r\n");

            System.out.println("Host name, host port and DB name have been set. Run \'simplequery -cleardb\' to remove.");

            try
            {
                dbDataWriter.close();    
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }            

            newHostNameScanner.close();
            newHostPortScanner.close();
            newDBNameScanner.close();

            System.exit(1);
        }
        else
        {
            System.out.println("DB data has NOT been saved.");
            System.exit(1);
        }           
    }

    private static void displayHelp()
    {
        System.out.println(
                             "   \nUSAGE:\n" 
                           + "     ./simplequery -q <\"SQL Statement\"> -> outputs the result of a custom SQL statement (NOTICE THE QUOTES!) \n"
                           + "     ./simplequery -assert -> prints only failures \n"
                           + "     ./simplequery -f <filename.txt> -> dump output to file \n"
                           + "     ./simplequery -df <datafeed> -> print table data for specific datafeed \n"
                           + "     ./simplequery -r <# of rows> ->  specify number of rows to output (default is 15) \n"
                           + "     ./simplequery -sd -> set default username/password/dbname/ \n"
                           + "     ./simplequery -cleardata -> clear stored credentials\n"     
                           + "     ./simplequery -help -> display options \n"
                           + "     ./simplequery -version -> print version info \n"
                          );
    }

    private static void versionInfo()
    {
        System.out.println("SimpleQuery BETA v0.2");
        System.out.println("Written by Tyler Raborn");
    }

    private static void initializeDatabase()
    {
        try
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        }
        catch(ClassNotFoundException e)
        {
            System.out.println("ERROR: Unable to load driver class!");
            System.exit(1);
        }
    }

    /* handles querying the database */
    private static void queryDatabase(String username, String password) throws IOException 
    {
        ArrayList<ArrayDeque<String>> resultsList = new ArrayList<ArrayDeque<String>>();
        ConnectionData connectionData = new ConnectionData(hostPort, hostName, dbName, username, password);
        if (readFromFile) {
            for (Query query : queryObjectList) {
                resultsList.add(query.execute(connectionData));
            }

            for (ArrayDeque<String> queryResults : resultsList) {
                //System.out.println("\nResults of query: ");
                for (String line : queryResults) {
                    writeLine(line);
                }
            }
        }
        else {
            try {
                String connectionString = "jdbc:oracle:thin:"+username+"/"+password+"@//" + hostName + ":" + Integer.toString(hostPort) + "/" + dbName;
                System.out.println("Credentials: " + username + ", " + password + ", ConString: " + connectionString);
                Connection con = DriverManager.getConnection(
                                                             connectionString,
                                                             username,
                                                             password
                                                            );

                Statement queryStatement = con.createStatement();
                ResultSet results = queryStatement.executeQuery(queryString);

                ResultSetMetaData theMetaData = results.getMetaData();
                int columnsNumber = theMetaData.getColumnCount();
                while (results.next()) {
                    for (int i = 1; i <= columnsNumber; i++) {
                        if (i > 1) writeLine(",  ");
                        String columnValue = results.getString(i);
                        writeLine(columnValue + " " + theMetaData.getColumnName(i));
                    }
                    writeLine("\n");     
                }           
            }
            catch(java.sql.SQLException e)
            {
                e.printStackTrace();
            }
        }
    }

    private static void writeLine(final String msg) 
    {
        if (outputToFile)
        {
            try 
            {
                fileWriter.write(msg);    
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }   
        else
        {
            System.out.print(msg);
        }
    }
}
