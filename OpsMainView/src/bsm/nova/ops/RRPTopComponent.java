/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bsm.nova.ops;

import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
        dtd = "-//bsm.nova.ops//RRP//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "RRPTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "bsm.nova.ops.RRPTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_RRPAction",
        preferredID = "RRPTopComponent"
)
@Messages({
  "CTL_RRPAction=RR Tax Profile Patch",
  "CTL_RRPTopComponent=RRP Window",
  "HINT_RRPTopComponent=This is a RRP window"
})
public final class RRPTopComponent extends TopComponent implements PropertyChangeListener {

  class BgWorker extends SwingWorker<Void, Void> {

    private dbHandler apps;
    private PreparedStatement psUpdateAcctPro;
    private PreparedStatement psInsertAcctPro;
    private PreparedStatement psUpdateProMod;
    private PreparedStatement psSearchAcctPro;
    private ProgressHandle pbar;

    @Override
    protected Void doInBackground() throws Exception {

      pbar = ProgressHandleFactory.createHandle("RR Profile");
      pbar.start();
      Utilities.log(me, "Started", constant.DEBUG);

      try {
        initconn();
      } catch (SQLException e) {
        Utilities.log(me, "Error init DB", constant.ERROR);
        Utilities.logStack(me, e);
        cleanup();
        return null;
      }

      if (mode == 1) {
        scanDB();
      } else if (mode == 2) {
        patchRecords();
      }

      cleanup();
      return null;
    }

    private void scanDB() {
      tablemodel.clearContent();
      Utilities.log(me, "Scanning DB for list of account", constant.DEBUG);
      int reccount = 0;
      boolean filter = chkFilter.isSelected();

      try {
        pbar.progress("Counting beans");
        ResultSet rs = apps.executeSelect("select count(1) from tmpin_batch.novabrm_taxprofile@custom_to_stg");

        if (rs.next()) {
          reccount = rs.getInt(1);
        }
        Utilities.log(me, "Total record: " + reccount, constant.DEBUG);

        pbar.progress("Scooping beans");
        rs = apps.executeSelect("select * from tmpin_batch.novabrm_taxprofile@custom_to_stg");

        pbar.switchToDeterminate(reccount);
        int counter = 0;

        while (rs.next()) {
          if (!isRunningState) {
            break;
          }
          pbar.progress(counter++);

          String bano = dbHandler.dbGetString(rs, "account_no");
          String propoid = dbHandler.dbGetString(rs, "profile poid");
          String BP = dbHandler.dbGetString(rs, "bp");

          // get current value in DB
          psSearchAcctPro.setString(1, propoid);
          ResultSet rs2 = psSearchAcctPro.executeQuery();
          if (rs2.next()) {
            String recid = dbHandler.dbGetString(rs2, "rec_id");
            String curr = dbHandler.dbGetString(rs2, "value");

            long valfrom = rs2.getLong("valid_from");

            if (filter && curr.equals("RR")) {
              continue;
            }

            ArrayList<Object> data = new ArrayList<>();
            data.add(bano);
            data.add(BP);
            data.add(propoid);
            data.add(recid);
            data.add(curr);
            data.add(Utilities.tsToDate(valfrom, "dd/MM/yyyy"));
            data.add(!curr.equals("RR"));

            tablemodel.add(data);
            tablemodel.refire();

          } else {
            // no data for this 
            Utilities.log(me, "profile not found: " + bano + " - /profile " + propoid, constant.ERROR);
          }

        }

      } catch (Exception e) {
        Utilities.logStack(me, e);
      }

    }

    private void patchRecords() {
      int totalrecord = tablemodel.getRowCount();
      pbar.progress("patching");
      pbar.switchToDeterminate(totalrecord);
      if (totalrecord == 0) {
        return;
      }
      jTable1.changeSelection(0, 0, false, false);

      for (int i = 0; i < totalrecord; i++) {
        pbar.progress(i);
        if (!isRunningState) {
          break;
        }

        jTable1.changeSelection(i, 0, false, false);

//        Utilities.log(me, tablemodel.getValueAt(i, 6).toString(), constant.DEBUG);
        if (tablemodel.getValueAt(i, 6).toString().equals("true")) {
          try {
            String propoid = tablemodel.getValueAt(i, 2).toString();
            int recid = Integer.parseInt(tablemodel.getValueAt(i, 3).toString());
            Utilities.log(me, recid + " " + propoid , constant.ERROR);
            updateAnyExtrating(propoid, recid);
          } catch (SQLException e) {
            Utilities.log(me, "Error updating " + tablemodel.getValueAt(i, 0).toString(), constant.ERROR);
            Utilities.logStack(me, e);
          }
        }

      }

    }

    private void initconn() throws SQLException {
//      jTable1.setModel(tablemodel);
//      Utilities.log(me, "Table model set", constant.DEBUG);
      isRunningState = true;
      lockButton();
      long NEW_PROFILE_DATE = Utilities.dateToTS(Utilities.tsToDateNow("ddMMyyyy"), "ddMMyyyy");
      apps = new dbHandler("apps");
      apps.setDBConnInfo(constant.dbConApps);
      apps.setUserPass(constant.dbConUser, constant.dbConPass);
//      Utilities.log(me, "Opening connection to DB: " + constant.dbConApps, constant.DEBUG);
      pbar.progress("Connecting to DB");
      apps.openConnection();

//      Utilities.log(me, "creating PSes", constant.DEBUG);
      pbar.progress("Creating PS");
      psSearchAcctPro = apps.createPS("select rec_id, value, valid_from from PROFILE_ACCT_EXTRATING_data_T where obj_Id0 = ? "
              + "and valid_to = 0 and name = 'AcctTaxCode' ");

      psUpdateAcctPro = apps.createPS("update PROFILE_ACCT_EXTRATING_data_T set valid_to = " + NEW_PROFILE_DATE + " where obj_id0 = ? "
              + "and rec_id = ? ");
      psInsertAcctPro = apps.createPS("insert into PROFILE_ACCT_EXTRATING_data_T (obj_Id0, rec_id, name, value, valid_to, valid_from) "
              + "values (?, ?, 'AcctTaxCode', 'RR' , 0, " + NEW_PROFILE_DATE + " )");
      psUpdateProMod = apps.createPS("update profile_t set mod_t = ? where poid_id0 = ? ");

    }

    private void cleanup() {
      pbar.finish();
      isRunningState = false;
      lockButton();
      try {
        apps.closeConnection();
      } catch (Exception e) {
      }
    }

    private void updateAnyExtrating(String propoid, int recid) throws SQLException {

      // update the existing profile
      psUpdateAcctPro.setString(1, propoid);
      psUpdateAcctPro.setInt(2, recid);
      psUpdateAcctPro.executeUpdate();
      recid++;

      // then add the new line
      psInsertAcctPro.setString(1, propoid);
      psInsertAcctPro.setInt(2, recid);
      psInsertAcctPro.executeUpdate();

      long currts = System.currentTimeMillis() / 1000;
      psUpdateProMod.setLong(1, currts);
      psUpdateProMod.setString(2, propoid);
      psUpdateProMod.executeUpdate();

    }

  }

  public RRPTopComponent() {
    initComponents();
    setName(Bundle.CTL_RRPTopComponent());
    setToolTipText(Bundle.HINT_RRPTopComponent());
    customInit();
  }

  private void customInit() {
    isRunningState = false;
    lockButton();

    jTable1.setModel(tablemodel);
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jPanel1 = new javax.swing.JPanel();
    btnScan = new javax.swing.JButton();
    BtnPatch = new javax.swing.JButton();
    BtnStop = new javax.swing.JButton();
    chkFilter = new javax.swing.JCheckBox();
    jScrollPane1 = new javax.swing.JScrollPane();
    jTable1 = new javax.swing.JTable();

    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(RRPTopComponent.class, "RRPTopComponent.jPanel1.border.title"))); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(btnScan, org.openide.util.NbBundle.getMessage(RRPTopComponent.class, "RRPTopComponent.btnScan.text")); // NOI18N
    btnScan.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnScanActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(BtnPatch, org.openide.util.NbBundle.getMessage(RRPTopComponent.class, "RRPTopComponent.BtnPatch.text")); // NOI18N
    BtnPatch.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        BtnPatchActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(BtnStop, org.openide.util.NbBundle.getMessage(RRPTopComponent.class, "RRPTopComponent.BtnStop.text")); // NOI18N
    BtnStop.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        BtnStopActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(chkFilter, org.openide.util.NbBundle.getMessage(RRPTopComponent.class, "RRPTopComponent.chkFilter.text")); // NOI18N

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(btnScan)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(chkFilter)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(BtnPatch)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(BtnStop))
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
        .addComponent(btnScan)
        .addComponent(BtnPatch)
        .addComponent(BtnStop)
        .addComponent(chkFilter))
    );

    jTable1.setModel(new javax.swing.table.DefaultTableModel(
      new Object [][] {
        {null, null},
        {null, null}
      },
      new String [] {
        "Title 1", "Title 2"
      }
    ));
    jTable1.setCellSelectionEnabled(true);
    jTable1.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    jScrollPane1.setViewportView(jTable1);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 772, Short.MAX_VALUE)
          .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void BtnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnStopActionPerformed
    // TODO add your handling code here:
    isRunningState = false;
  }//GEN-LAST:event_BtnStopActionPerformed

  private void BtnPatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnPatchActionPerformed
    // TODO add your handling code here:
    mode = 2;

    BgWorker nw = new BgWorker();
    nw.addPropertyChangeListener(this);
    nw.execute();
  }//GEN-LAST:event_BtnPatchActionPerformed

  private void btnScanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnScanActionPerformed
    // TODO add your handling code here:
    mode = 1;

    BgWorker nw = new BgWorker();
    nw.addPropertyChangeListener(this);
    nw.execute();

  }//GEN-LAST:event_btnScanActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton BtnPatch;
  private javax.swing.JButton BtnStop;
  private javax.swing.JButton btnScan;
  private javax.swing.JCheckBox chkFilter;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JTable jTable1;
  // End of variables declaration//GEN-END:variables

  private final RRPTableModel tablemodel = new RRPTableModel();
  private final String me = "RR tax profile patcher";
  private boolean isRunningState;
  private int mode;

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

  @Override
  public void propertyChange(PropertyChangeEvent evt) {

  }

  private void lockButton() {
    btnScan.setEnabled(!isRunningState);
    BtnStop.setEnabled(isRunningState);
    BtnPatch.setEnabled(!isRunningState);
  }
}
