/**
 * Created by tyler on 10/6/14.
 */

import javax.crypto.*;
import java.security.*;

public class ConnectionData {

    private int portNumber;
    private String hostName;
    private String dbName;
    private String username;
    private String password;

    public ConnectionData() {}

    public ConnectionData(
                          int portNumber,
                          String hostName,
                          String dbName,
                          String username,
                          String password
                         ) {
        this.portNumber = portNumber;
        this.hostName = hostName;
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    public int getPortNumber() {
        return this.portNumber;
    }

    public String getHostName() {
        return this.hostName;
    }

    public String getDbName() {
        return this.dbName;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }
}
