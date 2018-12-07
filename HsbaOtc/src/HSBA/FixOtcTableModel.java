/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HSBA;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author S53788
 */
public class FixOtcTableModel extends AbstractTableModel {
  
  private ArrayList<ArrayList<Object>> tableCOntent;
  private String[] columnNames = {"Bill Poid", "Event Poid", "EAI descr", "BRM descr"
          , "OTC Date", "Order Date", "Fix?"};
  Class[] types = new Class [] {
    java.lang.String.class, java.lang.String.class, java.lang.String.class
          , java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class
  };
  boolean[] canEdit = new boolean [] {
    false, false, false, false, false, false, true
  };

  public FixOtcTableModel(){
    tableCOntent = new ArrayList<ArrayList<Object>>();
  }
  
  public void clearContent(){
    tableCOntent.clear();
  }
  
  public synchronized void add(String billpoid, String eventpoid, 
          String eaid, String brmd, String otcdate, String orderdate, boolean fix){
    
    ArrayList<Object> data = new ArrayList<Object>();
    data.add(billpoid);
    data.add(eventpoid);
    data.add(eaid);
    data.add(brmd);
    data.add(otcdate);
    data.add(orderdate);
    data.add(fix);
    
    tableCOntent.add(data);
  }

  public void refire(){
    fireTableDataChanged();
  }
  
  @Override
  public void setValueAt(Object value, int row, int column) {
    tableCOntent.get(row).set(column, value);
  }


  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    return tableCOntent.get(rowIndex).get(columnIndex);
  }
  
  @Override
  public String getColumnName(int col) {
    return columnNames[col];
  }

  @Override
  public int getColumnCount() {
    return 7;
  }
 
  @Override
  public int getRowCount() {
    return tableCOntent.size();
  }

  @Override
  public Class getColumnClass(int columnIndex) {
    return types [columnIndex];
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return canEdit [columnIndex];
  }
  
}
