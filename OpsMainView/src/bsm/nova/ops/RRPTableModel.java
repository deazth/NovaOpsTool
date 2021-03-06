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
public class RRPTableModel extends AbstractTableModel {

  private final ArrayList<ArrayList<Object>> tableCOntent;
  private final String[] columnNames = {"Account No", "BP", "Profile POID", "Rec_ID", "Current Value", "Valid From", "Patch?"};
  Class[] types = new Class[]{
    java.lang.String.class, java.lang.String.class, java.lang.String.class,
    java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class
  };
  boolean[] canEdit = new boolean[]{
    false, false, false, false, false, false, true
  };

  public RRPTableModel() {
    tableCOntent = new ArrayList<ArrayList<Object>>();
  }

  public void clearContent() {
    tableCOntent.clear();
  }

  public synchronized void add(ArrayList<Object> data) {
    tableCOntent.add(data);
  }

  public void refire() {
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
    return types[columnIndex];
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return canEdit[columnIndex];
  }

}
