/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ops.com;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.PooledConnection;
import oracle.jdbc.pool.OracleConnectionPoolDataSource;

/**
 *
 * @author S53788
 */
public class dbHandler {

  private String DriverType;
  private String ServerName;
  private int PortNumber;
  private String ServiceName;
  private String User;
  private String Password;
  private boolean isConnected;
  private Connection conn;
  private int queryCount;
  private String connString;
  // controller booleans
  private boolean useConnString;
  private boolean usernameSet;
  private boolean hasError;
  private Statement stmt;
  private String dbname;

  public dbHandler(String database_name) {
    isConnected = false;
    usernameSet = false;
    hasError = true;
    dbname = database_name;
  }

  /**
   * Manually set the required information for this oracle connection
   *
   * @param driver_type The type of driver to use
   * @param server_name IP / host name of the db server
   * @param port_no Port no for that database
   * @param service_name Service Name / SID
   */
  public void setDBConnInfo(String driver_type, String server_name, int port_no, String service_name) {
    this.DriverType = driver_type;
    this.PortNumber = port_no;
    this.ServerName = server_name;
    this.ServiceName = service_name;
    this.useConnString = false;
    this.hasError = false;
  }

  /**
   * Set the connection info using the connection string
   *
   * @param connection_string
   */
  public void setDBConnInfo(String connection_string) {
    this.connString = connection_string;
    this.useConnString = true;
    this.hasError = false;

  }

  /**
   * Set the username and password for this connection
   *
   * @param user
   * @param pass
   */
  public void setUserPass(String user, String pass) {
    this.User = user;
    this.Password = pass;
    this.usernameSet = true;
  }

  /**
   * Try to open the connection to the database
   *
   * @throws SQLException This exception is thrown when the attempt to connect exceed 20 times
   */
  public void openConnection() throws SQLException {
    //CommonUtility.println("Connecting to " + dbname);
    if (!usernameSet) {
      System.err.println("Username and password is not set");
      throw new SQLException("Username / pass is not set");
    }

    if (hasError) {
      System.err.println("Connection info is not set");
      throw new SQLException("Connection info is not set");
    }

    int attemptleft = 20;

    OracleConnectionPoolDataSource asd = new OracleConnectionPoolDataSource();
    if (this.useConnString) {
      asd.setURL(connString);
      //CommonUtility.println(connString);
    } else {
      asd.setDriverType(DriverType);
      asd.setServerName(ServerName);
      asd.setPortNumber(PortNumber);
      asd.setServiceName(ServiceName);
    }

    asd.setUser(User);
    asd.setPassword(Password);

    while (!isConnected) {
      try {
        PooledConnection pc = asd.getPooledConnection();
        conn = pc.getConnection();

        if (conn != null) {
          //CommonUtility.println("Connected to: " + dbname);
          stmt = conn.createStatement();
          isConnected = true;
          queryCount = 0;
        }

      } catch (SQLException se) {
        isConnected = false;
        attemptleft--;
        
        if(se.getMessage().contains("logon") || se.getMessage().contains("username")){
          System.err.println("Invalid login");
          System.exit(1);
        }
        
        if (attemptleft == 0) {

          System.err.println("Failed to connect: " + dbname);
          throw se;
        }
      }
    }
  }

  /**
   * Try to close the active connection
   *
   * @throws SQLException
   */
  public void closeConnection() throws SQLException {
    if(isConnected){
      stmt.close();
    conn.close();
    }
    
    isConnected = false;
    queryCount = 0;
    //CommonUtility.println("Connection closed: " + dbname);
  }

  /**
   * Try to re-establish the connection
   *
   * @throws SQLException
   */
  public void reconnect() throws SQLException {
    closeConnection();
    openConnection();
  }

  /**
   * Execute the given select query and return the result
   * It is not recommended to use the same connection to query other result while the resultset is still being used
   * @param query SQL select query
   * @return Resultset for the query
   * @throws SQLException
   */
  public ResultSet executeSelect(String query) throws SQLException {

    if (!isConnected) {
      openConnection();
    }

    queryCount++;

    try {
      return stmt.executeQuery(query);
    } catch (SQLException sqle) {
      System.err.println("Error - " + query);
      throw sqle;
    }
  }

  public boolean executeUpdate(String query) throws SQLException {

    if (!isConnected) {
      openConnection();
    }

    queryCount++;

    try {
      return stmt.executeUpdate(query) == 0;
    } catch (SQLException e) {
      System.err.println("Error - " + query);
      throw e;
    }
    
  }

  public static String dbGetString(ResultSet rs, int pos) throws SQLException{
    String ret = "";

    if(rs.getString(pos) == null) ret = "";
    else ret = rs.getString(pos);

    return ret;
  }
  
  public static String dbGetString(ResultSet rs, String pos) throws SQLException{
    String ret = "";

    if(rs.getString(pos) == null) ret = "";
    else ret = rs.getString(pos);

    return ret;
  }

  public boolean isCOnnected(){
    return isConnected;
  }
  
  public PreparedStatement createPS(String sql) throws SQLException{
    return conn.prepareStatement(sql);
  }
  
}
