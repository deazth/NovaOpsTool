/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package playground;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author amer
 */
public class kolamdb {

  private static final String CONN_STR = "jdbc:mysql://127.0.0.1:33060/homestead?useSSL=false";
  private static final String DB_USER = "homestead";
  private static final String DB_PASS = "secret";

  private static final int MAX_IDLE_CONN = 5;
  private static final ArrayList<Connection> CONN_IDLE = new ArrayList<>();

  /**
   * give the connection back to the pool
   * or destroys it, if the pool is full
   * @param conn 
   */
  public static void returnConn(Connection conn) {
    if (CONN_IDLE.size() >= MAX_IDLE_CONN) {
      // if current idle pool size already max, just close the connection
      try {
        conn.close();
      } catch (Exception e) {
      }
    } else {
      // return the connection to the pool
      CONN_IDLE.add(conn);
    }
  }

  /**
   * Get the connection from the pool
   * to do: implement max number of connection?
   * @return
   * @throws SQLException 
   */
  public static Connection getConnection() throws SQLException {
    if (CONN_IDLE.isEmpty()) {
      // if the pool is empty, create new connection
      return openConenction();
    } else {
      // give the connection from the pool
      return CONN_IDLE.remove(0);
    }
  }

  private static Connection openConenction() throws SQLException {
    try {
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      System.err.println("JDBC driver not found");
      throw new SQLException("Unable to connect: " + e.getMessage());
    }

    return DriverManager.getConnection(CONN_STR, DB_USER, DB_PASS);

  }

  private static void shutdown() {
    CONN_IDLE.forEach((c) -> {
      try {
        c.close();
      } catch (SQLException e) {
      }
    });

  }

  private kolamdb() {
  }
}
