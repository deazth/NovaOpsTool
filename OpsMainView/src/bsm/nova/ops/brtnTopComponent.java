/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bsm.nova.ops;

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
import javax.swing.text.DefaultCaret;
import ops.com.PortalConnectionManager;
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
        dtd = "-//bsm.nova.ops//brtn//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "brtnTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "bsm.nova.ops.brtnTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_brtnAction",
        preferredID = "brtnTopComponent"
)
@Messages({
  "CTL_brtnAction=Bulk RTN",
  "CTL_brtnTopComponent=Bulk RTN Window",
  "HINT_brtnTopComponent=This is a brtn window"
})
public final class brtnTopComponent extends TopComponent implements PropertyChangeListener {

  public brtnTopComponent() {
    initComponents();
    btnStop.setEnabled(false);
    setName(Bundle.CTL_brtnTopComponent());
    setToolTipText(Bundle.HINT_brtnTopComponent());

  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {

  }

  class RTNWorker extends SwingWorker<Void, Void> {

//    private ArrayList<RtnObj> inputlist;
    private dbHandler dbh;
    private PortalContext pc;

    @Override
    protected Void doInBackground() {

      lockButton();

      try {
        init();
      } catch (Exception e) {
        Utilities.popup(e.getMessage());
        releaseButton();
        return null;
      }

      countline();

      processInput();

      try {
        dbh.closeConnection();

        if (fireToProd) {
          pc.cloneConnection();
        }

      } catch (Exception e) {
      }
      releaseButton();
      return null;
    }

    private void processInput() {
      Scanner sc = new Scanner(jTextArea1.getText());
//      inputlist = new ArrayList<RtnObj>();
      PreparedStatement ps;
      pbar.setDisplayName("Processing data");

      try {
        ps = dbh.createPS("select\n"
                + "  ta.poid_Id0 acc_poid, ts.poid_Id0 svc_poid, ts.poid_type svc_type\n"
                + " from\n"
                + "  account_T ta\n"
                + "  , service_t ts\n"
                + " where\n"
                + "  ta.account_no = ? \n"
                + "  and ts.login = ? \n"
                + "  and ta.status = 10100\n"
                + "  and ts.status = 10102\n"
                + "  and ta.poid_Id0 = ts.account_obj_Id0");
      } catch (Exception e) {
        Utilities.popup("Error prepping query: " + e.getMessage());
        return;
      }

      int counter = 0;
      while (sc.hasNextLine()) {

        if (!isRunning) {
          break;
        }
        pbar.progress(counter++);
        String line = sc.nextLine().trim();

        if (line.isEmpty()) {
          continue;
        }

        String[] input = line.split("\\|");

        if (input.length != 3) {
          output("invalid input: " + line, 1);
          continue;
        }

        // find in db
        RtnObj ro;
        try {
          ps.setString(1, input[0]);
          pbar.progress(input[0]);
          ps.setString(2, input[1]);

          ResultSet rs = ps.executeQuery();

          if (rs.next()) {
            ro = new RtnObj(rs.getLong("acc_poid"), rs.getLong("svc_poid"), rs.getString("svc_type"), Utilities.dateToTS(input[2], "dd/MM/yyyy"));
          } else {
            output("Record not found: " + line, 1);
            continue;
          }

        } catch (Exception e) {
          output("DB Error processing line : " + line + ". " + e.getMessage(), 1);
          continue;
        }

        try {
          FList inputflist = ro.getInputFlist();

          output("r << XXX 1");
          output(inputflist.asString());
          output("XXX");
          output("");
          output("xop PCM_OP_CUST_SET_STATUS 0 1");
          output("");

          if (fireToProd) {
            FList ouput = pc.opcode(PortalOp.CUST_SET_STATUS, inputflist);
            output(ouput.asString());
          }

          output("");
          output("");

        } catch (Exception e) {
          output("Error processing line : " + line + ". " + e.getMessage(), 1);
        }

      }

    }

    private void init() throws SQLException, Exception {
      pbar.setDisplayName("Initializing...");
      recordcount = 0;
      fireToProd = chkFireProd.isSelected();
      DefaultCaret caret = (DefaultCaret) jTextArea1.getCaret();
      caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
      sb = new StringBuilder();

      output("Bulk RTN started", 0);

      if (fireToProd) {
        pc = PortalConnectionManager.getInstance().getConnection();
      }

      dbh = new dbHandler("apps");

      dbh.setDBConnInfo(constant.dbConApps);
      dbh.setUserPass(constant.dbConUser, constant.dbConPass);
      dbh.openConnection();
    }

  }

  class CEMTWorker extends SwingWorker<Void, Void> {

    private dbHandler dbh;

    @Override
    protected Void doInBackground() {
      lockButton();

      try {
        init();
      } catch (Exception e) {
        Utilities.popup(e.getMessage());
        releaseButton();
        return null;
      }

      countline();

      processInput();

      try {
        dbh.closeConnection();

      } catch (Exception e) {
      }
      releaseButton();
      return null;
    }

    private void processInput() {
      Scanner sc = new Scanner(jTextArea1.getText());
//      inputlist = new ArrayList<RtnObj>();
      PreparedStatement psacc;
      PreparedStatement pschg;
      pbar.setDisplayName("Processing data");

      try {
        psacc = dbh.createPS("select\n"
                + "  Tbi.Actg_Cycle_Dom\n"
                + "  , Tan.Last_Name\n"
                + "  , Ts.Service_Id\n"
                + "  , Ttcp.Tm_Segment_Code\n"
                + "  , Tan.State\n"
                + "  , decode(tbi.payinfo_obj_type, '/payinfo/cc', 'Y', 'N') otopay\n"
                + "  , Ttip.Mobile_Number\n"
                + "  , Ttip.Delivery_Descr\n"
                + " from\n"
                + "  account_t ta\n"
                + "  , billinfo_t tbi\n"
                + "  , account_nameinfo_t tan\n"
                + "  , service_t ts\n"
                + "  , profile_T tpro1\n"
                + "  , tm_cust_profile_t ttcp\n"
                + "  , profile_t tpro2\n"
                + "  , tm_inv_profile_t ttip\n"
                + " where\n"
                + "  ta.account_no = ? \n"
                + "  and ts.login = ? \n"
                + "  and ta.poid_id0 =  tbi.account_obj_Id0\n"
                + "  and ta.poid_id0 =  ts.account_obj_Id0\n"
                + "  and ta.poid_id0 =  tpro1.account_obj_Id0\n"
                + "  and ta.poid_id0 =  tpro2.account_obj_Id0\n"
                + "  and ta.poid_id0 =  tan.obj_Id0\n"
                + "  and tpro1.poid_Id0 = ttcp.obj_id0\n"
                + "  and tpro2.poid_Id0 = ttip.obj_id0 ");

        pschg = dbh.createPS("select\n"
                + "  sum(ti.item_total) tot_charge, tb.end_t\n"
                + " from\n"
                + "  account_t ta\n"
                + "  , service_t ts\n"
                + "  , item_t ti\n"
                + "  , event_t te\n"
                + "  , bill_t tb\n"
                + " where\n"
                + "  ta.account_no = ? \n"
                + "  and ts.login = ? \n"
                + "  and Te.Earned_Start_T = ? \n"
                + "  and ta.poid_id0 = ts.account_obj_id0\n"
                + "  and ta.poid_id0 = ti.account_obj_id0\n"
                + "  and ti.poid_id0 = te.item_obj_Id0\n"
                + "  and ti.service_obj_Id0 = ts.poid_Id0\n"
                + "  and ti.bill_obj_id0 = tb.poid_id0\n"
                + " group by tb.end_t");

      } catch (Exception e) {
        Utilities.popup("Error prepping query: " + e.getMessage());
        return;
      }

      int counter = 0;
      while (sc.hasNextLine()) {
        if (!isRunning) {
          break;
        }
        pbar.progress(counter++);
        String line = sc.nextLine().trim();

        if (line.isEmpty()) {
          continue;
        }

        String[] input = line.split("\\|");

        if (input.length != 3) {
          output("invalid input: " + line, 1);
          continue;
        }

        // cari acc level info
        try {
          psacc.setString(1, input[0]);
          psacc.setString(2, input[1]);

          ResultSet rs = psacc.executeQuery();

          if (rs.next()) {

            long rtndate = Utilities.dateToTS(input[2], "dd/MM/yyyy");

            String d = "|";
            String Actg_Cycle_Dom = dbHandler.dbGetString(rs, "Actg_Cycle_Dom");
            String Last_Name = dbHandler.dbGetString(rs, "Last_Name");
            String Service_Id = dbHandler.dbGetString(rs, "Service_Id");
            String Tm_Segment_Code = dbHandler.dbGetString(rs, "Tm_Segment_Code");
            String State = dbHandler.dbGetString(rs, "State");
            String otopay = dbHandler.dbGetString(rs, "otopay");
            String Mobile_Number = dbHandler.dbGetString(rs, "Mobile_Number");
            String Delivery_Descr = dbHandler.dbGetString(rs, "Delivery_Descr");

            String chargeamt = "N/A";
            String billdate = "N/A";

            pschg.setString(1, input[0]);
            pschg.setString(2, input[1]);
            pschg.setLong(3, rtndate);
            
            ResultSet rs2 = pschg.executeQuery();
            
            if(rs2.next()){
              chargeamt = dbHandler.dbGetString(rs2, "tot_charge");
              billdate = Utilities.tsToDate(rs2.getLong("end_t"), "dd/MM/yyyy");
            }

            output(input[0] + d
                    + Actg_Cycle_Dom + d
                    + billdate + d
                    + Last_Name + d
                    + input[1] + d
                    + Service_Id + d
                    + Tm_Segment_Code + d
                    + State + d
                    + otopay + d
                    + Mobile_Number + d
                    + Delivery_Descr + d
                    + chargeamt
            );

          } else {
            output("combo not found: " + input[0] + " " + input[1]);
          }

        } catch (Exception e) {
          output("error searching for ba#" + input[0] + " : " + e.getMessage(), 1);
          Utilities.logStack(me, e);
        }

      }
    }

    private void init() throws SQLException, Exception {
      pbar.setDisplayName("Initializing...");
      recordcount = 0;
      DefaultCaret caret = (DefaultCaret) jTextArea1.getCaret();
      caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
      sb = new StringBuilder();

      output("Bulk CEMT started", 0);
      output("ACCOUNT_NO|BP|IMPACTED_BILL_MONTH|CUSTOMER_NAME|SERVICE_LOGIN|SERVICE_TYPE|SEGMENT_CODE|STATE|AUTOPAY|MOBILE_NO|EMAIL_ADDR|LATE_CHARGES_AMT");

      dbh = new dbHandler("apps");

      dbh.setDBConnInfo(constant.dbConApps);
      dbh.setUserPass(constant.dbConUser, constant.dbConPass);
      dbh.openConnection();
    }

  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jScrollPane1 = new javax.swing.JScrollPane();
    jTextArea1 = new javax.swing.JTextArea();
    jPanel1 = new javax.swing.JPanel();
    chkFireProd = new javax.swing.JCheckBox();
    btnBulkRtn = new javax.swing.JButton();
    btnStop = new javax.swing.JButton();
    jLabel1 = new javax.swing.JLabel();
    jScrollPane2 = new javax.swing.JScrollPane();
    jTextArea2 = new javax.swing.JTextArea();
    jLabel2 = new javax.swing.JLabel();
    jPanel2 = new javax.swing.JPanel();
    btnSearchCEMT = new javax.swing.JButton();

    jTextArea1.setColumns(20);
    jTextArea1.setRows(5);
    jScrollPane1.setViewportView(jTextArea1);

    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(brtnTopComponent.class, "brtnTopComponent.jPanel1.border.title"))); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(chkFireProd, org.openide.util.NbBundle.getMessage(brtnTopComponent.class, "brtnTopComponent.chkFireProd.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(btnBulkRtn, org.openide.util.NbBundle.getMessage(brtnTopComponent.class, "brtnTopComponent.btnBulkRtn.text")); // NOI18N
    btnBulkRtn.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnBulkRtnActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap(10, Short.MAX_VALUE)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(chkFireProd, javax.swing.GroupLayout.Alignment.TRAILING)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
            .addComponent(btnBulkRtn)
            .addContainerGap())))
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addComponent(chkFireProd)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(btnBulkRtn)
        .addContainerGap())
    );

    org.openide.awt.Mnemonics.setLocalizedText(btnStop, org.openide.util.NbBundle.getMessage(brtnTopComponent.class, "brtnTopComponent.btnStop.text")); // NOI18N
    btnStop.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnStopActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(brtnTopComponent.class, "brtnTopComponent.jLabel1.text")); // NOI18N

    jTextArea2.setColumns(20);
    jTextArea2.setRows(5);
    jScrollPane2.setViewportView(jTextArea2);

    org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(brtnTopComponent.class, "brtnTopComponent.jLabel2.text")); // NOI18N

    jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(brtnTopComponent.class, "brtnTopComponent.jPanel2.border.title"))); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(btnSearchCEMT, org.openide.util.NbBundle.getMessage(brtnTopComponent.class, "brtnTopComponent.btnSearchCEMT.text")); // NOI18N
    btnSearchCEMT.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnSearchCEMTActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
    jPanel2.setLayout(jPanel2Layout);
    jPanel2Layout.setHorizontalGroup(
      jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(btnSearchCEMT)
        .addContainerGap())
    );
    jPanel2Layout.setVerticalGroup(
      jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(btnSearchCEMT)
        .addContainerGap())
    );

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 249, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel1))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel2)
            .addGap(0, 0, Short.MAX_VALUE))
          .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
          .addComponent(btnStop, javax.swing.GroupLayout.Alignment.TRAILING)
          .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
              .addComponent(jLabel1)
              .addComponent(jLabel2))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
              .addComponent(jScrollPane2)))
          .addGroup(layout.createSequentialGroup()
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(btnStop)))
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void btnBulkRtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBulkRtnActionPerformed
    // TODO add your handling code here:

    RTNWorker rw = new RTNWorker();
    rw.addPropertyChangeListener(this);
    rw.execute();


  }//GEN-LAST:event_btnBulkRtnActionPerformed

  private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
    // TODO add your handling code here:

    isRunning = false;
  }//GEN-LAST:event_btnStopActionPerformed

  private void btnSearchCEMTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchCEMTActionPerformed
    // TODO add your handling code here:
    CEMTWorker cw = new CEMTWorker();
    cw.addPropertyChangeListener(this);
    cw.execute();
  }//GEN-LAST:event_btnSearchCEMTActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton btnBulkRtn;
  private javax.swing.JButton btnSearchCEMT;
  private javax.swing.JButton btnStop;
  private javax.swing.JCheckBox chkFireProd;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JTextArea jTextArea1;
  private javax.swing.JTextArea jTextArea2;
  // End of variables declaration//GEN-END:variables

  private boolean isRunning = false;
  private String me = "Bulk RTN";
  private StringBuilder sb;
  private boolean fireToProd;
  private ProgressHandle pbar;
  private int recordcount;

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

  private void output(String line, int mode) {
    Utilities.log(me, line, mode);
  }

  private void output(String line) {
    sb.append(line);
    sb.append(constant.LINE_SEPARATOR);
    jTextArea2.setText(sb.toString());
  }

  private void lockButton() {
    pbar = ProgressHandleFactory.createHandle("Bulk RTN");
    pbar.start();
    isRunning = true;
    btnStop.setEnabled(true);
    btnBulkRtn.setEnabled(false);
    btnSearchCEMT.setEnabled(false);
  }

  private void releaseButton() {
    pbar.finish();
    isRunning = false;
    btnStop.setEnabled(false);
    btnBulkRtn.setEnabled(true);
    btnSearchCEMT.setEnabled(true);
  }

  private void countline() {
    Scanner sc = new Scanner(jTextArea1.getText());
    while (sc.hasNextLine()) {
      sc.nextLine();
      recordcount++;
    }

    pbar.switchToDeterminate(recordcount);

  }
}
