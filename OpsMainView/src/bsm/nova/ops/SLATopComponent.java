/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bsm.nova.ops;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import ops.com.Utilities;
import ops.com.constant;
import ops.com.dbHandler;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//bsm.nova.ops//SLA//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "SLATopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "bsm.nova.ops.SLATopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_SLAAction",
        preferredID = "SLATopComponent"
)
@Messages({
  "CTL_SLAAction=SLG Patcher",
  "CTL_SLATopComponent=SLG Patch",
  "HINT_SLATopComponent=This is a SLG Patch window"
})
public final class SLATopComponent extends TopComponent implements PropertyChangeListener {

  @Override
  public void propertyChange(PropertyChangeEvent evt) {

  }

  class bgWorker extends SwingWorker<Void, Void> {

    private ProgressHandle pbar;
    private dbHandler dbhBRM;

    @Override
    protected Void doInBackground() throws Exception {

      removeDuplicate();

      if (ohsem_id.isEmpty()) {
        Utilities.popup("Nothing to process");
        return null;
      }

      try {
        connectApps();
      } catch (SQLException e) {
        Utilities.logStack(me, e);
        Utilities.popup("Unable to connect to DB: " + e.getMessage());
        return null;
      }

      Utilities.log(me, "Mode: " + mode, 0);

      if (mode.equals("search")) {
        slatable.clearContent();
        search();
      } else if (mode.equals("update")) {

        if (slatable.getRowCount() == 0) {
          Utilities.popup("Nothing to process");
          return null;
        }
        update();
      }

      Utilities.log(me, "completed: " + mode, 0);
      cleanup();
      return null;
    }

    private void search() {
      pbar = ProgressHandleFactory.createHandle("Search Service SLG");
      pbar.start();

      // prepare the statement
      try {
        PreparedStatement pscari = dbhBRM.createPS("select\n"
                + "  ta.account_no, tan.last_name\n"
                + "  , ts.status, tcs.value, tcs.rec_id, tcs.obj_Id0\n"
                + "from\n"
                + "  service_t ts\n"
                + "  , profile_t tpro\n"
                + "  , Tm_Cust_Srv_Profile_T tcs\n"
                + "  , account_T ta\n"
                + "  , account_nameinfo_t tan\n"
                + "where\n"
                + "  ts.login = ? \n"
                + "  and tcs.name = 'SERVICE_LEVEL'\n"
                + "  and ts.poid_Id0 = tpro.service_obj_id0\n"
                + "  and tpro.poid_Id0 = tcs.obj_Id0\n"
                + "  and ts.account_obj_Id0 = ta.poid_Id0\n"
                + "  and ta.poid_Id0 = tan.obj_id0");

        pbar.switchToDeterminate(ohsem_id.size());
        int counter = 0;

        for (String login : ohsem_id) {
          counter++;
          pbar.progress(login, counter);

          pscari.setString(1, login);
          try {
            ResultSet rs = pscari.executeQuery();

            if (rs.next()) {
              slatable.add(dbHandler.dbGetString(rs, "account_no"), dbHandler.dbGetString(rs, "last_name"), login, dbHandler.dbGetString(rs, "status"), dbHandler.dbGetString(rs, "value"), true, dbHandler.dbGetString(rs, "obj_Id0"), dbHandler.dbGetString(rs, "rec_id"));
            } else {
              slatable.add("", "Service profile not found", login, "", "", false, "", "");
            }

          } catch (SQLException e) {
            Utilities.popup("Error while processing " + login + ": " + e.getMessage());
            Utilities.logStack(me, e);
          }

          slatable.refire();

        }

      } catch (Exception e) {
        Utilities.popup("Error while processing data: " + e.getMessage());
        Utilities.logStack(me, e);
      }

    }

    private void update() {

      String inputval = "";

      while (true) {

        String bsinput = JOptionPane.showInputDialog("Enter new SLG %");
        if (bsinput.isEmpty()) {
          Utilities.popup("No new SLG text entered");
          return;
        }

        try {
          double num = Double.parseDouble(bsinput);
          // no error? then concat it
          inputval = "SLG " + bsinput.trim() + "%";
        } catch (Exception e) {
          Utilities.popup("Invalid number: " + bsinput);
          continue;
        }

        int choice = JOptionPane.showConfirmDialog(null, "Confirm update to : " + inputval + " ?");

        if (choice == JOptionPane.YES_OPTION) {
          break;
        } else if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
          Utilities.popup("SLG update cancelled");
          return;
        }

      }

      pbar = ProgressHandleFactory.createHandle("Update Service SLG");
      pbar.start();

      try {
        PreparedStatement psupd = dbhBRM.createPS("update Tm_Cust_Srv_Profile_T set value = '"
                + inputval + "' where obj_id0 = ? and rec_id = ? ");

        int tablesize = slatable.getRowCount();
        pbar.switchToDeterminate(tablesize);

        for (int i = 0; i < tablesize; i++) {
          String login = (String) slatable.getValueAt(i, 2);

          pbar.progress(login, i);

          String bano = (String) slatable.getValueAt(i, 0);
          String poid = (String) slatable.getValueAt(i, 6);
          String recid = (String) slatable.getValueAt(i, 7);
          String value = (String) slatable.getValueAt(i, 4);
          String tick = slatable.getValueAt(i, 5).toString();

          if (tick.equals("false") || poid.isEmpty()) {
            Utilities.log(me, login + " skipped", 0);
            continue;
          }

          psupd.setString(1, poid);
          psupd.setString(2, recid);

          int upc = psupd.executeUpdate();

          String display = bano + " : " + login + " " + value + " -> " + inputval + " @ " + poid + "." + recid + ". record updated: " + upc;
          Utilities.log(me, display, 0);
        }

      } catch (Exception e) {
        Utilities.popup("Error while processing data: " + e.getMessage());
        Utilities.logStack(me, e);
      }

    }

    private void connectApps() throws SQLException {
      dbhBRM = new dbHandler("apps");

      dbhBRM.setDBConnInfo(constant.dbConApps);
      dbhBRM.setUserPass(constant.dbConUser, constant.dbConPass);
//      dbhBRM.setDBConnInfo("jdbc:oracle:thin:@(DESCRIPTION =\n"
//              + "    (ADDRESS = (PROTOCOL = TCP)(HOST = 10.14.43.49)(PORT = 1521))\n"
//              + "    (CONNECT_DATA =\n"
//              + "      (SERVER = DEDICATED)\n"
//              + "      (SERVICE_NAME = HBRMSIT2)\n"
//              + "    )\n"
//              + "  )");
//      dbhBRM.setUserPass("pin", "pin123");

      dbhBRM.openConnection();
    }

    private void cleanup() {
      try {
        dbhBRM.closeConnection();
      } catch (Exception e) {
      }

      pbar.finish();
    }

  }

  public SLATopComponent() {
    initComponents();
    setName(Bundle.CTL_SLATopComponent());
    setToolTipText(Bundle.HINT_SLATopComponent());

  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jScrollPane1 = new javax.swing.JScrollPane();
    txtInput = new javax.swing.JTextArea();
    jPanel1 = new javax.swing.JPanel();
    btnSearch = new javax.swing.JButton();
    btnUpdate = new javax.swing.JButton();
    jLabel1 = new javax.swing.JLabel();
    jScrollPane2 = new javax.swing.JScrollPane();
    jTable1 = new javax.swing.JTable();

    txtInput.setColumns(20);
    txtInput.setRows(5);
    jScrollPane1.setViewportView(txtInput);

    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SLATopComponent.class, "SLATopComponent.jPanel1.border.title"))); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(btnSearch, org.openide.util.NbBundle.getMessage(SLATopComponent.class, "SLATopComponent.btnSearch.text")); // NOI18N
    btnSearch.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnSearchActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(btnUpdate, org.openide.util.NbBundle.getMessage(SLATopComponent.class, "SLATopComponent.btnUpdate.text")); // NOI18N
    btnUpdate.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnUpdateActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
          .addComponent(btnUpdate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btnSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(btnSearch)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btnUpdate))
    );

    org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(SLATopComponent.class, "SLATopComponent.jLabel1.text")); // NOI18N

    jTable1.setModel(slatable);
    jScrollPane2.setViewportView(jTable1);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel1)
            .addGap(0, 0, Short.MAX_VALUE))
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jLabel1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jScrollPane1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
          .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE))
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
    // TODO add your handling code here:
    mode = "search";

    bgWorker bw = new bgWorker();
    bw.addPropertyChangeListener(this);
    bw.execute();

  }//GEN-LAST:event_btnSearchActionPerformed

  private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
    // TODO add your handling code here:
    mode = "update";

    bgWorker bw = new bgWorker();
    bw.addPropertyChangeListener(this);
    bw.execute();
  }//GEN-LAST:event_btnUpdateActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton btnSearch;
  private javax.swing.JButton btnUpdate;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JTable jTable1;
  private javax.swing.JTextArea txtInput;
  // End of variables declaration//GEN-END:variables

  // custom vars
  SLATableModel slatable = new SLATableModel();
  ArrayList<String> ohsem_id;
  private String me = "SLA Patcher";
  private String mode;

  @Override
  public void componentOpened() {
    // TODO add custom code on component opening
  }

  @Override
  public void componentClosed() {
    // TODO add custom code on component closing
  }

  void writeProperties(java.util.Properties p) {
    // better to version settings since initial version as advocated at
    // http://wiki.apidesign.org/wiki/PropertyFiles
    p.setProperty("version", "1.0");
    // TODO store your settings
  }

  void readProperties(java.util.Properties p) {
    String version = p.getProperty("version");
    // TODO read your settings according to their version
  }

  private void removeDuplicate() {
    Scanner sc = new Scanner(txtInput.getText());

    ohsem_id = new ArrayList<String>();

    // remove duplicate
    while (sc.hasNextLine()) {
      String data = sc.nextLine().trim();

      if (data.isEmpty()) {
        continue;
      }

      if (!ohsem_id.contains(data)) {
        ohsem_id.add(data);
      }

    }

    // add back to the GUI
    String uiout = "";

    for (String aa : ohsem_id) {
      uiout += aa + constant.LINE_SEPARATOR;
    }

    txtInput.setText(uiout);
  }

}
