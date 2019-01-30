/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package playground;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Wrapper for Connection class
 * Supports connection pooling, without having the need to care about it at the ground level
 * @author amer
 */
public class DBConnection {
  private final Connection conn;
  
  public DBConnection() throws SQLException{
    conn = kolamdb.getConnection();
  }
  
  public void close(){
    kolamdb.returnConn(conn);
  }
  
  public ResultSet executeQuery(String sql) throws SQLException{
    return conn.createStatement().executeQuery(sql);
  }
  
  public PreparedStatement createPS(String sql) throws SQLException{
    return conn.prepareStatement(sql);
  }
  
}
