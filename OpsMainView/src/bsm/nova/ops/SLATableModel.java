/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bsm.nova.ops;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author S53788
 */
public class SLATableModel extends AbstractTableModel {

  private ArrayList<ArrayList<Object>> tableCOntent;
  private String[] columnNames = {"Account No", "Name", "Service Login", "Svc Status", "Current SLG Value", "Patch?"};
  
  Class[] types = new Class [] {
    java.lang.String.class, java.lang.String.class, java.lang.String.class
          , java.lang.String.class, java.lang.String.class, java.lang.Boolean.class
  };
  
  boolean[] canEdit = new boolean [] {
    false, false, false, false, false, true
  };

  public SLATableModel(){
    tableCOntent = new ArrayList<ArrayList<Object>>();
  }
  
  public void clearContent(){
    tableCOntent.clear();
  }
  
  public void add(String bano, String namae, String login, String svcstatus
          , String slavalue, boolean patch, String poid, String recid){
    
    ArrayList<Object> data = new ArrayList<Object>();
    data.add(bano);
    data.add(namae);
    data.add(login);
    data.add(svcstatus);
    data.add(slavalue);
    data.add(patch);
    data.add(poid);
    data.add(recid);
    
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
    return columnNames.length;
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
