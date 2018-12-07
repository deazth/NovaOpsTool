/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bsm.nova.ops;

import com.portal.pcm.EBufException;
import com.portal.pcm.FList;
import com.portal.pcm.PortalContext;
import com.portal.pcm.PortalOp;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.SwingWorker;
import ops.com.PortalConnectionManager;
import ops.com.Utilities;
import ops.com.constant;
import ops.com.dbHandler;
import org.apache.log4j.PropertyConfigurator;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//bsm.nova.ops//BCL//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "BCLTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "bsm.nova.ops.BCLTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_BCLAction",
        preferredID = "BCLTopComponent"
)
@Messages({
  "CTL_BCLAction=Bulk Change Login",
  "CTL_BCLTopComponent=Bulk Change Login Window",
  "HINT_BCLTopComponent=This is a BCL window"
})
public final class BCLTopComponent extends TopComponent implements PropertyChangeListener {

  class PatchWorker extends SwingWorker<Void, Void> {

    private dbHandler dbhapps;
    private PreparedStatement psSearch;
    private PortalContext pcm;
    private StringBuilder sb;

    @Override
    protected Void doInBackground() {
      ProgressHandle pbar = ProgressHandleFactory.createHandle("BCL");
      pbar.start();
      btnRunState();
      pbar.progress("removing duplicate");
      sb = new StringBuilder();
      removeDuplicate();

      try {
        pbar.progress("connecting to DB");
        connectDB();
        pbar.progress("connecting to CM");
        connectCM();
      } catch (EBufException | SQLException e) {
        Utilities.log(me, "Error init connections", constant.ERROR);
        Utilities.logStack(me, e);
        pbar.finish();
        cleanup();
        return null;
      }

      pbar.switchToDeterminate(inputs.size());
      int counter = 0;

      for (String in : inputs) {
        pbar.progress(counter++);
        process(in);
      }

      cleanup();
      pbar.finish();
      return null;
    }

    private void process(String line) {

      String[] input = line.split("\\|");
      String accpoid = "";
      String oldlogin = "";

      if (input.length != 3) {
        addOutput("invalid input: " + line);
        return;
      }

      // get the acc poid and old login
      try {
        psSearch.setString(1, input[0]);
        ResultSet rs = psSearch.executeQuery();

        if (rs.next()) {
          accpoid = dbHandler.dbGetString(rs, 1);
          oldlogin = dbHandler.dbGetString(rs, 2);
        } else {
          addOutput("service not found: " + input[0]);
          return;
        }

      } catch (SQLException e) {
        addOutput("Error finding service: " + input[0] + " - " + e.getMessage());
        return;
      }

      // fire the testnap
      long indate;
      try {
        indate = Long.parseLong(input[2]);
      } catch (NumberFormatException e) {
        addOutput("invalid timestamp: " + input[0] + " -> " + input[2]);
        return;
      }
      String out = fireNap(accpoid, input[0], input[1], input[2]);
      if (out.equals("success")) {
        addOutput(input[0] + " changed from " + oldlogin + " to " + input[1] + " date " + Utilities.tsToDate(indate, "dd/MM/yyyy"));
      } else {
        addOutput(input[0] + " " + out);
      }

    }

    private String fireNap(String accpoid, String svcpoid, String newlogin, String newts) {
      String ret = "";
      String input
              = "0 PIN_FLD_POID               POID [0] 0.0.0.1 /account " + accpoid + " 0\n"
              + "0 PIN_FLD_PROGRAM_NAME        STR [0] \"Opstool BCL\"\n"
              + "0 PIN_FLD_END_T            TSTAMP [0]  (" + newts + ")\n"
              + "0 PIN_FLD_SERVICES          ARRAY [0] allocated 20, used 3\n"
              + "1 PIN_FLD_POID               POID [0] 0.0.0.1 /service/telephony " + svcpoid + " 0\n"
              + "1 PIN_FLD_LOGIN               STR [0] \"" + newlogin + "\"\n"
              + "1 PIN_FLD_ALIAS_LIST        ARRAY [0] allocated 20, used 1\n"
              + "2 PIN_FLD_NAME                STR [0] \"" + newlogin + "\"";

      try {
        FList infl = FList.createFromString(input);
        FList output = pcm.opcode(PortalOp.CUST_UPDATE_SERVICES, infl);
//        Utilities.log(me, infl.asString(), 1);
        ret = "success";
      } catch (EBufException e) {
        ret = e.getMessage();
      }

      return ret;
    }

    private void connectDB() throws SQLException {
      dbhapps = new dbHandler("apps");
      dbhapps.setDBConnInfo(constant.dbConApps);
      dbhapps.setUserPass(constant.dbConUser, constant.dbConPass);
      dbhapps.openConnection();

      psSearch = dbhapps.createPS("select account_obj_Id0, login from pin.service_t where poid_Id0 = ? ");

    }

    private void connectCM() throws EBufException {
      PropertyConfigurator.configure(getClass().getClassLoader().getResource("../../config/log4j.properties"));
      pcm = PortalConnectionManager.getInstance().getConnection();
    }

    private void cleanup() {
      btnStopState();
      try {
        dbhapps.closeConnection();
        pcm.close(true);

      } catch (SQLException e) {
      } catch (EBufException ex) {

      }
    }

    private void addOutput(String line) {
      sb.append(line);
      sb.append(constant.LINE_SEPARATOR);

      txtOutput.setText(sb.toString());
    }

  }

  public BCLTopComponent() {
    initComponents();
    setName(Bundle.CTL_BCLTopComponent());
    setToolTipText(Bundle.HINT_BCLTopComponent());
    customInit();
  }

  private void customInit() {
    btnStopState();
  }

  private void btnRunState() {
    isRunningState = true;
    btnPatch.setEnabled(false);
    btnStop.setEnabled(true);
  }

  private void btnStopState() {
    btnPatch.setEnabled(true);
    btnStop.setEnabled(false);
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
    jLabel1 = new javax.swing.JLabel();
    jScrollPane2 = new javax.swing.JScrollPane();
    txtOutput = new javax.swing.JTextArea();
    lblResult = new javax.swing.JLabel();
    btnPatch = new javax.swing.JButton();
    btnStop = new javax.swing.JButton();

    txtInput.setColumns(20);
    txtInput.setRows(5);
    jScrollPane1.setViewportView(txtInput);

    org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(BCLTopComponent.class, "BCLTopComponent.jLabel1.text")); // NOI18N

    txtOutput.setColumns(20);
    txtOutput.setRows(5);
    jScrollPane2.setViewportView(txtOutput);

    org.openide.awt.Mnemonics.setLocalizedText(lblResult, org.openide.util.NbBundle.getMessage(BCLTopComponent.class, "BCLTopComponent.lblResult.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(btnPatch, org.openide.util.NbBundle.getMessage(BCLTopComponent.class, "BCLTopComponent.btnPatch.text")); // NOI18N
    btnPatch.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnPatchActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(btnStop, org.openide.util.NbBundle.getMessage(BCLTopComponent.class, "BCLTopComponent.btnStop.text")); // NOI18N
    btnStop.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnStopActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jLabel1)
          .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(lblResult)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(btnPatch)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(btnStop))
          .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 411, Short.MAX_VALUE))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel1)
          .addComponent(btnPatch)
          .addComponent(btnStop)
          .addComponent(lblResult))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
          .addComponent(jScrollPane1))
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void btnPatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPatchActionPerformed
    // TODO add your handling code here:

    PatchWorker pw = new PatchWorker();
    pw.addPropertyChangeListener(this);
    pw.execute();

  }//GEN-LAST:event_btnPatchActionPerformed

  private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
    // TODO add your handling code here:
    isRunningState = false;
  }//GEN-LAST:event_btnStopActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton btnPatch;
  private javax.swing.JButton btnStop;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JLabel lblResult;
  private javax.swing.JTextArea txtInput;
  private javax.swing.JTextArea txtOutput;
  // End of variables declaration//GEN-END:variables

  private boolean isRunningState;
  private ArrayList<String> inputs;
  private String me = "Bulk Change Login";

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

  private void removeDuplicate() {
    Scanner sc = new Scanner(txtInput.getText());

    inputs = new ArrayList<String>();

    // remove duplicate
    while (sc.hasNextLine()) {
      String data = sc.nextLine().trim();

      if (data.isEmpty()) {
        continue;
      }

      if (!inputs.contains(data)) {
        inputs.add(data);
      }

    }

    // add back to the GUI
    String uiout = "";

    for (String aa : inputs) {
      uiout += aa + constant.LINE_SEPARATOR;
    }

    txtInput.setText(uiout);
    lblResult.setText("Record to process: " + inputs.size());
  }
}
