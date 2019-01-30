/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bsm.nova.ops;

import com.portal.pcm.FList;
import com.portal.pcm.PortalContext;
import com.portal.pcm.PortalOp;
import ops.com.Utilities;
import ops.com.constant;
import ops.com.dbHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.text.DefaultCaret;
import ops.com.MtaWannabeManager;
import ops.com.PortalConnectionManager;
import org.apache.log4j.PropertyConfigurator;
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
        dtd = "-//bsm.nova.ops//fbc//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "fbcTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "bsm.nova.ops.fbcTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_fbcAction",
        preferredID = "fbcTopComponent"
)
@Messages({
  "CTL_fbcAction=Bulk Clost Flist",
  "CTL_fbcTopComponent=Bulk Clost Flist Window",
  "HINT_fbcTopComponent=This is a fbc window"
})
public final class fbcTopComponent extends TopComponent implements PropertyChangeListener {

  public fbcTopComponent() {
    initComponents();
    getProgramName();
    setName(Bundle.CTL_fbcTopComponent());
    setToolTipText(Bundle.HINT_fbcTopComponent());

  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {

  }

  public class TerminateSvcWorker extends SwingWorker<Void, Void> {

    private dbHandler dbh;
    private PreparedStatement psTerminateSvc;


    private void initWorker() throws Exception {
      dbh = new dbHandler("apps");
      dbh.setDBConnInfo(constant.dbConApps);
      dbh.setUserPass(constant.dbConUser, constant.dbConPass);

      dbh.openConnection();

      psTerminateSvc = dbh.createPS("select poid_type, account_obj_Id0, status from service_t where poid_Id0 = ? ");

      PropertyConfigurator.configure(getClass().getClassLoader().getResource("../../config/log4j.properties"));
      
      oputSB = new StringBuilder();

    }

    @Override
    protected Void doInBackground() {
      pbar = ProgressHandleFactory.createHandle("Terminate service");
      pbar.start();

      
      
      try {
        initWorker();
        SvcKillWorker skw = new SvcKillWorker();
        skw.loadToQueue(jTextArea1.getText());

        MtaWannabeManager mwm = new MtaWannabeManager(5);
        mwm.addJob(skw);
        mwm.addJob(skw);
        mwm.addJob(skw);
        mwm.addJob(skw);
        mwm.addJob(skw);

        mwm.waitJobToComplete(3, TimeUnit.DAYS);
        cleanup();

      } catch (Exception e) {
        Utilities.logStack(me, e);
      }

      pbar.finish();
      return null;
    }

    private void cleanup() {

      try {
        psTerminateSvc.close();
        dbh.closeConnection();
      } catch (Exception e) {

        e.printStackTrace();
      }

    }

  }

  public class FlistWorker extends SwingWorker<Void, Void> {

    private dbHandler dbh;
    private PreparedStatement ps;

    private String accpoid;
    private String status;
    private int svc_count;
    boolean ignoreActSvc;

    private void initWorker() throws Exception {
      dbh = new dbHandler("apps");
      dbh.setDBConnInfo(constant.dbConApps);
      dbh.setUserPass(constant.dbConUser, constant.dbConPass);

      dbh.openConnection();

      ps = dbh.createPS("select poid_Id0, status"
              + ", (select count(1) from service_t ts where ts.account_obj_Id0 = ta.poid_Id0 and ts.status != 10103) svc_count "
              + "from account_T ta where account_no = ? ");

      ignoreActSvc = chkIgnoreActiveSvc.isSelected();
      
    }

    private String getFlist(String accno, String terminatedate) {

      getAccPoid(accno);

      // check for YT's requirement
      // 1. account with active service
      if (svc_count > 0 && !ignoreActSvc) {
        Utilities.log(me, accno + " got " + svc_count + " active service(s)", 1);
        return "";
      }

      // 2. account already closed
      if (status.equals("10103")) {
        Utilities.log(me, accno + " already closed", 1);
        return "";
      }

      String tdate = translateDate(terminatedate);

      if (accpoid.length() == 0) {
        System.err.println("Acc not found: " + accno);
        return "";
      }

      String retval = "r << XXX 1\n"
              + "0 PIN_FLD_POID           POID [0] 0.0.0.1 /account " + accpoid + " 12\n"
              + "0 PIN_FLD_END_T             TSTAMP [0] (" + tdate + ")\n"
              + "0 PIN_FLD_PROGRAM_NAME    STR [0] \"" + programName + "\"\n"
              + "0 PIN_FLD_DESCR           STR [0] \"" + reason + "\"\n"
              + "0 PIN_FLD_STATUSES      ARRAY [0] allocated 20, used 2\n"
              + "1     PIN_FLD_STATUS_FLAGS    INT [0] 4\n"
              + "1     PIN_FLD_STATUS         ENUM [0] 10103\n"
              + "XXX\n\n"
              + "xop PCM_OP_CUST_SET_STATUS 0 1\n\n";

//    String retval = accpoid + " - " + tdate;
      return retval;
    }

    private boolean getAccPoid(String accno) {
      boolean retval = false;

      try {
        ps.setString(1, accno);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
          accpoid = dbHandler.dbGetString(rs, 1);
          status = dbHandler.dbGetString(rs, 2);
          svc_count = rs.getInt(3);
          retval = true;
        }

      } catch (Exception e) {
        e.printStackTrace();
      }

      return retval;

    }

    private String translateDate(String date) {

      return Utilities.dateToTS(date, "M/d/yyyy") + "";

    }

    private void cleanup() {

      try {
        ps.close();
        dbh.closeConnection();
      } catch (Exception e) {

        e.printStackTrace();
      }

    }

    @Override
    protected Void doInBackground() {
      reason = jTextField1.getText().trim();

      if (reason.isEmpty()) {
        Utilities.popup("Reason cannot be empty");
        jTextField1.requestFocus();
        return null;
      }

      Scanner sc = new Scanner(jTextArea1.getText());
      String output = "";
      String pbartext = "";
      int workcount = 0;
      int count = 0;
      StringBuilder sb = new StringBuilder();

      jButton1.setEnabled(false);
      jButton2.setEnabled(false);
      jButton3.setEnabled(false);

      pbar = ProgressHandleFactory.createHandle(pbartext);
      pbar.start();

      while (sc.hasNextLine()) {
        sc.nextLine();
        workcount++;
      }

      sc.close();
      sc = new Scanner(jTextArea1.getText());

      txtOutputArea.setText(output);

      jButton3.setEnabled(true);

      try {
        initWorker();
      } catch (Exception e) {
        Utilities.log(me, e.getMessage(), 1);
        return null;
      }

      pbar.switchToDeterminate(workcount);

      while (sc.hasNextLine()) {
        if (allowRun == false) {
          break;
        }
        pbar.progress(count++);
        String data = sc.nextLine().trim();

        if (data.isEmpty()) {
          continue;
        }

        if (!data.contains("|")) {
          Utilities.log(me, "Invalid line: " + data, 2);
          continue;
        }

        String[] input = data.split("\\|");

        if (input.length < 2) {
          Utilities.log(me, "Invalid line: " + data, 2);
          continue;
        }

        output += getFlist(input[0], input[1]);
        txtOutputArea.setText(output);

//        output += constant.LINE_SEPARATOR + constant.LINE_SEPARATOR;
      }

      cleanup();

      pbar.finish();

      jButton1.setEnabled(true);
      jButton2.setEnabled(true);
      jButton3.setEnabled(false);

      return null;
    }

  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jScrollPane2 = new javax.swing.JScrollPane();
    jTextArea2 = new javax.swing.JTextArea();
    jLabel1 = new javax.swing.JLabel();
    jScrollPane1 = new javax.swing.JScrollPane();
    jTextArea1 = new javax.swing.JTextArea();
    jScrollPane3 = new javax.swing.JScrollPane();
    txtOutputArea = new javax.swing.JTextArea();
    jLabel2 = new javax.swing.JLabel();
    jPanel1 = new javax.swing.JPanel();
    jButton1 = new javax.swing.JButton();
    jButton3 = new javax.swing.JButton();
    jButton2 = new javax.swing.JButton();
    jTextField1 = new javax.swing.JTextField();
    jLabel3 = new javax.swing.JLabel();
    chkIgnoreActiveSvc = new javax.swing.JCheckBox();

    jTextArea2.setColumns(20);
    jTextArea2.setRows(5);
    jScrollPane2.setViewportView(jTextArea2);

    org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(fbcTopComponent.class, "fbcTopComponent.jLabel1.text")); // NOI18N

    jTextArea1.setColumns(20);
    jTextArea1.setRows(5);
    jScrollPane1.setViewportView(jTextArea1);

    txtOutputArea.setColumns(20);
    txtOutputArea.setRows(5);
    jScrollPane3.setViewportView(txtOutputArea);

    org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(fbcTopComponent.class, "fbcTopComponent.jLabel2.text")); // NOI18N

    jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

    org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(fbcTopComponent.class, "fbcTopComponent.jButton1.text")); // NOI18N
    jButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton1ActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(jButton3, org.openide.util.NbBundle.getMessage(fbcTopComponent.class, "fbcTopComponent.jButton3.text")); // NOI18N
    jButton3.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton3ActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(jButton2, org.openide.util.NbBundle.getMessage(fbcTopComponent.class, "fbcTopComponent.jButton2.text")); // NOI18N
    jButton2.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton2ActionPerformed(evt);
      }
    });

    jTextField1.setText(org.openide.util.NbBundle.getMessage(fbcTopComponent.class, "fbcTopComponent.jTextField1.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(fbcTopComponent.class, "fbcTopComponent.jLabel3.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(chkIgnoreActiveSvc, org.openide.util.NbBundle.getMessage(fbcTopComponent.class, "fbcTopComponent.chkIgnoreActiveSvc.text")); // NOI18N

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(jPanel1Layout.createSequentialGroup()
            .addComponent(jLabel3)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jTextField1))
          .addGroup(jPanel1Layout.createSequentialGroup()
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(chkIgnoreActiveSvc)
              .addComponent(jButton1)
              .addComponent(jButton3)
              .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(0, 0, Short.MAX_VALUE)))
        .addContainerGap())
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel3))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jButton1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(chkIgnoreActiveSvc)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jButton2)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(jButton3)
        .addContainerGap())
    );

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
          .addComponent(jScrollPane1)
          .addComponent(jLabel1)
          .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel2)
            .addGap(0, 0, Short.MAX_VALUE))
          .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 633, Short.MAX_VALUE))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel1)
          .addComponent(jLabel2))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
          .addComponent(jScrollPane3))
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    // TODO add your handling code here:
    mode = 0;
    allowRun = true;
    FlistWorker fw = new FlistWorker();
    fw.addPropertyChangeListener(this);
    fw.execute();

  }//GEN-LAST:event_jButton1ActionPerformed

  private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
    // TODO add your handling code here:

    reason = jTextField1.getText().trim();

    if (reason.isEmpty()) {
      Utilities.popup("Reason cannot be empty");
      jTextField1.requestFocus();
      return;
    }

    int firstconfirm = JOptionPane.showConfirmDialog(this, "This will straight away terminate the service poid listed. Proceed?", "Are you sure?", JOptionPane.OK_CANCEL_OPTION);

    if (firstconfirm == JOptionPane.OK_OPTION) {
      int secondconfirm = JOptionPane.showConfirmDialog(this, "Are you really sure?", "o.O!!", JOptionPane.OK_CANCEL_OPTION);

      if (secondconfirm == JOptionPane.OK_OPTION) {
        mode = 1;
        allowRun = true;
        TerminateSvcWorker fw = new TerminateSvcWorker();
        fw.addPropertyChangeListener(this);
        fw.execute();

      }

    }


  }//GEN-LAST:event_jButton2ActionPerformed

  private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
    // TODO add your handling code here:
//    Utilities.popup(programName);
    allowRun = false;
  }//GEN-LAST:event_jButton3ActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JCheckBox chkIgnoreActiveSvc;
  private javax.swing.JButton jButton1;
  private javax.swing.JButton jButton2;
  private javax.swing.JButton jButton3;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JScrollPane jScrollPane3;
  private javax.swing.JTextArea jTextArea1;
  private javax.swing.JTextArea jTextArea2;
  private javax.swing.JTextField jTextField1;
  private javax.swing.JTextArea txtOutputArea;
  // End of variables declaration//GEN-END:variables

  private String me = "Bulk Close Acc Flist Generator";
  DefaultCaret caret;
  int mode = 0;
  boolean allowRun = true;
  String reason = "";
  String programName;
  StringBuilder oputSB;

  ProgressHandle pbar;

  private void getProgramName() {
    programName = System.getProperty("user.name") + " - NovaOpsTool";
  }

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

  class SvcKillWorker implements Runnable {

    private dbHandler dbh;
    private String me = "Thread worker";
    ConcurrentLinkedQueue<String> svcQueue;
    int workcount;
//  private String programName;
//  private String reason;

    public SvcKillWorker() throws SQLException {

//    programName = pn;
//    reason = r;
      dbh = new dbHandler("apps");
      dbh.setDBConnInfo(constant.dbConApps);
      dbh.setUserPass(constant.dbConUser, constant.dbConPass);

      dbh.openConnection();

      PropertyConfigurator.configure(getClass().getClassLoader().getResource("../../config/log4j.properties"));

      svcQueue = new ConcurrentLinkedQueue<String>();

      workcount = 0;
    }

    public void loadToQueue(String input) {
      Scanner sc = new Scanner(input);

      while (sc.hasNextLine()) {
        String data = sc.nextLine().trim();
        if (data.isEmpty()) {
          continue;
        }

        if (data.startsWith("#")) {
          continue;
        }

        if (data.toLowerCase().contains("poid")) {
          continue;
        }

        svcQueue.add(data);

      }

      Utilities.log(me, "DOne loading the input", 3);

      pbar.switchToDeterminate(svcQueue.size());

    }

    private synchronized String getNextSvc() {
      pbar.progress(workcount++);
      return svcQueue.poll();
    }

    private synchronized void printOut(String txt) {
      oputSB.append(txt);
      txtOutputArea.setText(oputSB.toString());
    }

    @Override
    public void run() {
      int process_count = 0;
      String ctid = Thread.currentThread().getName();
      PortalContext pcm = null;
      PreparedStatement psTerminateSvc = null;

      try {
        pcm = PortalConnectionManager.getInstance().getConnection();
        psTerminateSvc = dbh.createPS("select poid_type, account_obj_Id0, status from service_t where poid_Id0 = ? ");
      } catch (Exception e) {
        Utilities.log(me, "Error init worker thread", 1);
        return;
      }

      while (allowRun) {
        
        process_count++;
        String data_to_process = getNextSvc();
        if (data_to_process == null) {
          break;
        }

        String status_before = "null";
        String status_after = "null";
        String process_status = "N/A";

        try {
          // find the infos
          psTerminateSvc.setString(1, data_to_process);

          ResultSet rs = psTerminateSvc.executeQuery();
          if (rs.next()) {
            String svcaccpoid = dbHandler.dbGetString(rs, "account_obj_id0");
            String svctype = dbHandler.dbGetString(rs, "poid_type");
            status_before = dbHandler.dbGetString(rs, "status");

            if (status_before.equals("10100")) {
              String flist = "0 PIN_FLD_POID           POID [0] 0.0.0.1 /account " + svcaccpoid + "\n"
                      + "0 PIN_FLD_PROGRAM_NAME    STR [0] \"" + programName + "\"\n"
                      + "0 PIN_FLD_DESCR           STR [0] \"" + reason + "\"\n"
                      + "0 PIN_FLD_SERVICES      ARRAY [0] allocated 20, used 3\n"
                      + "1 PIN_FLD_AAC_ACCESS                 STR [0] \"N\"\n"
                      + "1     PIN_FLD_STATUS_FLAGS    INT [0] 4\n"
                      + "1     PIN_FLD_POID           POID [0] 0.0.0.1 " + svctype + " " + data_to_process + "\n"
                      + "1     PIN_FLD_STATUS         ENUM [0] 10103";

              FList inputflist = FList.createFromString(flist);

              FList outputflist = pcm.opcode(PortalOp.CUST_UPDATE_SERVICES, inputflist);

              ResultSet rs2 = psTerminateSvc.executeQuery();
              if (rs2.next()) {
                status_after = dbHandler.dbGetString(rs2, "status");
                process_status = "done";
              } else {
                process_status = "kenot find svc after terminate lol";
              }
            } else {
              process_status = "Service currently not 10100";
            }

          } else {
            process_status = "Service poid doesnt exist";
          }

        } catch (Exception e) {
          Utilities.logStack(me, e);
          process_status = e.getMessage();
        }

        String out = ctid + " " + process_count + " - " + data_to_process + " : from " + status_before + " to "
                + status_after + ". operation: " + process_status + constant.LINE_SEPARATOR;

        printOut(out);

      }

      Utilities.log(me, ctid + " completed: " + process_count, 3);

    }
  }

}
