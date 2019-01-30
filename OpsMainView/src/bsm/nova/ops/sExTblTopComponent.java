/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bsm.nova.ops;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import ops.com.Utilities;
import ops.com.constant;
import ops.com.dFileWriter;
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
        dtd = "-//bsm.nova.ops//sExTbl//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "sExTblTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "bsm.nova.ops.sExTblTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_sExTblAction",
        preferredID = "sExTblTopComponent"
)
@Messages({
  "CTL_sExTblAction=Export IPTV table",
  "CTL_sExTblTopComponent=Export IPTV table Window",
  "HINT_sExTblTopComponent=This is a sExTbl window"
})
public final class sExTblTopComponent extends TopComponent implements PropertyChangeListener {

  public sExTblTopComponent() {
    initComponents();
    setName(Bundle.CTL_sExTblTopComponent());
    setToolTipText(Bundle.HINT_sExTblTopComponent());

  }

  class exportWorker extends SwingWorker<Void, Void> {

    String tablename;
    dbHandler dbh;
    dFileWriter dfw;
    int fcount;
    int reccount;
    File saveDir;

    @Override
    protected Void doInBackground() {

      jButton1.setEnabled(false);

      tablename = jTextField1.getText().trim();

      if (tablename.isEmpty()) {
        Utilities.popup("Tablename is empty");
        jTextField1.requestFocus();
        jButton1.setEnabled(true);
        return null;
      }

      JFileChooser jfc = new JFileChooser(".");
      jfc.setDialogTitle("Choose output folder");
      jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      jfc.setAcceptAllFileFilterUsed(false);

      if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        saveDir = jfc.getSelectedFile();
      } else {
        Utilities.popup("No output folder selected");
        jButton1.requestFocus();
        jButton1.setEnabled(true);
        return null;
      }

      dbh = new dbHandler("apps");
      dbh.setDBConnInfo(constant.dbConApps);
      dbh.setUserPass(constant.dbConUser, constant.dbConPass);
      fcount = 1;

      ProgressHandle pbar = ProgressHandleFactory.createHandle("Export table");
      pbar.start();

      process();

      pbar.finish();
      jButton1.setEnabled(true);
      return null;
    }

    private void process() {
      Utilities.log(me, "Started", 3);
      try {
        dbh.openConnection();
      } catch (Exception e) {
        Utilities.logStack(me, e);
        return;
      }

      try {
        prepWriter();
      } catch (Exception e) {
        Utilities.logStack(me, e);
        return;
      }

      try {
        ResultSet rs = dbh.executeSelect("select * from pin." + tablename
                + " order by bill_date,ba_number");

        Utilities.log(me, "data obtained. spooling", 3);

        while (rs.next()) {
          String accname = dbHandler.dbGetString(rs, "CUST_NAME");
          String bano = dbHandler.dbGetString(rs, "BA_NUMBER");
          String login = dbHandler.dbGetString(rs, "LOGIN_ID");
          String amt = dbHandler.dbGetString(rs, "AMOUNT");
          String descr = dbHandler.dbGetString(rs, "DESCR");
          String segcode = dbHandler.dbGetString(rs, "SEGMENT_CODE");
          String purchase_start = dbHandler.dbGetString(rs, "PURCHASE_START");
          String billdate = dbHandler.dbGetString(rs, "BILL_DATE");
          String type = dbHandler.dbGetString(rs, "CUST_TYPE");

          String output = cleanStr(accname) + ","
                  + bano + ","
                  + cleanStr(login) + ","
                  + amt + ","
                  + cleanStr(descr) + ","
                  + segcode + ","
                  + purchase_start + ","
                  + billdate + ","
                  + type;
          try {
            dfw.println(output);
            reccount++;
          } catch (IOException ioe) {
            Utilities.log(me, "err - " + bano, 1);
            Utilities.logStack(me, ioe);
          }

          if (reccount % 300000 == 0) {
            try {
              flush();
            } catch (IOException ioe) {
              Utilities.log(me, "unable to close writer", 1);
              Utilities.logStack(me, ioe);
            }

            try {
              prepWriter();
            } catch (IOException ioe) {
              Utilities.log(me, "unable to create new writer", 1);
              Utilities.logStack(me, ioe);
              return;
            }

          }

        }

        rs.close();
        dbh.closeConnection();

      } catch (SQLException sqle) {
        Utilities.popup(sqle.getMessage());
        Utilities.logStack(me, sqle);
        return;
      }

      try {
        flush();
      } catch (Exception e) {
        Utilities.logStack(me, e);
      }

      Utilities.log(me, "done", 3);

    }

    private void prepWriter() throws IOException {
      String nfname = tablename + "_" + fcount + ".csv";
      System.out.println(nfname);
      dfw = new dFileWriter(nfname);
      dfw.println("CUST_NAME,BA_NUMBER,LOGIN_ID,AMOUNT,DESCR,SEGMENT_CODE,PURCHASE_START,BILL_DATE,CUST_TYPE");
      reccount = 0;

    }

    private void flush() throws IOException {
      dfw.flush(saveDir.getAbsolutePath());
      fcount++;
    }

    private String cleanStr(String input) {
      return "\"" + input.replace("\"", "") + "\"";
    }

  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jLabel5 = new javax.swing.JLabel();
    jLabel1 = new javax.swing.JLabel();
    jTextField1 = new javax.swing.JTextField();
    jButton1 = new javax.swing.JButton();
    jLabel2 = new javax.swing.JLabel();
    jLabel3 = new javax.swing.JLabel();
    jLabel4 = new javax.swing.JLabel();
    jLabel6 = new javax.swing.JLabel();
    jLabel7 = new javax.swing.JLabel();
    jLabel8 = new javax.swing.JLabel();
    jLabel9 = new javax.swing.JLabel();
    jLabel10 = new javax.swing.JLabel();
    jLabel11 = new javax.swing.JLabel();
    jLabel12 = new javax.swing.JLabel();

    org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jLabel5.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jLabel1.text")); // NOI18N

    jTextField1.setText(org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jTextField1.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jButton1.text")); // NOI18N
    jButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton1ActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jLabel2.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jLabel3.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jLabel4.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jLabel6.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel7, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jLabel7.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel8, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jLabel8.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel9, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jLabel9.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel10, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jLabel10.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel11, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jLabel11.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel12, org.openide.util.NbBundle.getMessage(sExTblTopComponent.class, "sExTblTopComponent.jLabel12.text")); // NOI18N

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jTextField1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jButton1))
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel2)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jLabel4)
              .addComponent(jLabel3)
              .addComponent(jLabel6)
              .addComponent(jLabel7)
              .addComponent(jLabel8)
              .addComponent(jLabel9)
              .addComponent(jLabel10)
              .addComponent(jLabel11)
              .addComponent(jLabel12))
            .addGap(0, 168, Short.MAX_VALUE)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel1)
          .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jButton1))
        .addGap(18, 18, 18)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel2)
          .addComponent(jLabel3))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabel4)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabel6)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabel7)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabel8)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabel9)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabel10)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabel11)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabel12)
        .addContainerGap(74, Short.MAX_VALUE))
    );
  }// </editor-fold>//GEN-END:initComponents

  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    // TODO add your handling code here:

    exportWorker we = new exportWorker();
    we.addPropertyChangeListener(this);
    we.execute();

  }//GEN-LAST:event_jButton1ActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton jButton1;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel10;
  private javax.swing.JLabel jLabel11;
  private javax.swing.JLabel jLabel12;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JLabel jLabel6;
  private javax.swing.JLabel jLabel7;
  private javax.swing.JLabel jLabel8;
  private javax.swing.JLabel jLabel9;
  private javax.swing.JTextField jTextField1;
  // End of variables declaration//GEN-END:variables

  String me = "Export Shina";

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
}
