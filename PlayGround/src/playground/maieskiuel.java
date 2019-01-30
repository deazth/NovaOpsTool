/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package playground;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author amer
 */
public class maieskiuel {
  
  private DBConnection conn = null;
  
  public void readDB(){
    
    try {
      conn = new DBConnection();
      
      System.out.println("Conencted to DB");
    } catch (SQLException e) {
      System.err.println("Error establishing connection to DB");
      e.printStackTrace();
    }
    
    try {
      ResultSet rs = conn.executeQuery("select * from stores");
      if(rs.next()){
        
        System.out.println("ID: " + rs.getInt("id"));
        System.out.println("tag_no: " + rs.getString("tag_no"));
        System.out.println("status: " + rs.getString("status"));
        
        
      }
    } catch (Exception e) {
    }
    
    try {
      if(conn != null){
        conn.close();
      }
    } catch (Exception e) {
    }
    
    
  }
}
