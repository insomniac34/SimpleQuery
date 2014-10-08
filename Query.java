/**
 * Created by Tyler Raborn on 10/6/14.
 * A query (or series of queries) to be executed on the DB
 */

import java.sql.*;
import java.util.*;
import java.io.*;

public class Query {

    private ArrayList<String> queryList;

    public Query() {}

    public Query(ArrayList<String> queryList) {
        this.queryList = new ArrayList<String>(queryList);
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        }
        catch (ClassNotFoundException e) {
            System.out.println("ERROR: Unable to load driver class!");
            System.exit(1);
        }
    }

    /* prints result to stdout */
    public ArrayDeque<String> execute(ConnectionData connectionData) {
        
        ArrayDeque<String> ret = new ArrayDeque<String>();

        String connectionString = "jdbc:oracle:thin:"+
                connectionData.getUsername()+
                "/"+connectionData.getPassword()+"@//"+
                connectionData.getHostName()+
                ":"+Integer.toString(connectionData.getPortNumber())+
                "/"+connectionData.getDbName();
        try {
            for (String query : queryList) {

                Connection theConnection = DriverManager.getConnection(
                        connectionString,
                        connectionData.getUsername(),
                        connectionData.getPassword()
                );

                if (query.charAt(query.length()-1) == ';')
                    query = query.substring(0, query.length()-1);
                //System.out.println("Query.java: executing string: " + query);
                Statement queryStatement = theConnection.createStatement();
                ResultSet theResults = queryStatement.executeQuery(query);
                ResultSetMetaData theMetaData = theResults.getMetaData();

                while(theResults.next()) {
                    for (int i = 1; i < theMetaData.getColumnCount(); i++) {
                        if (i > 1) {
                            //System.out.print(", ");
                            ret.push(", ");
                        }
                        String columnValue = theResults.getString(i);
                        ret.push((columnValue + " " + theMetaData.getColumnName(i) + "\n"));
                    }
                    ret.push("\n");
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return ret;
    }
}
