/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ops.mawa;

import com.portal.pcm.EBufException;
import com.portal.pcm.FList;
import com.portal.pcm.Poid;
import com.portal.pcm.PortalContext;
import com.portal.pcm.PortalOp;
import com.portal.pcm.fields.FldAccountObj;
import com.portal.pcm.fields.FldAddress;
import com.portal.pcm.fields.FldBillinfo;
import com.portal.pcm.fields.FldCity;
import com.portal.pcm.fields.FldCountry;
import com.portal.pcm.fields.FldInheritedInfo;
import com.portal.pcm.fields.FldInvType;
import com.portal.pcm.fields.FldName;
import com.portal.pcm.fields.FldPayType;
import com.portal.pcm.fields.FldPayinfo;
import com.portal.pcm.fields.FldPoid;
import com.portal.pcm.fields.FldProgramName;
import com.portal.pcm.fields.FldState;
import com.portal.pcm.fields.FldZip;
import com.portal.pcm.fields.TmFldBauKodHasil;
import com.portal.pcm.fields.TmFldJanCode;
import com.portal.pcm.fields.TmFldPocCode;
import com.portal.pcm.fields.TmFldPukalInfo;
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
        dtd = "-//ops.mawa//pkpay//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "pkpayTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "ops.mawa.pkpayTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_pkpayAction",
        preferredID = "pkpayTopComponent"
)
@Messages({
  "CTL_pkpayAction=TMPukal Payinfo",
  "CTL_pkpayTopComponent=TMPukal Payinfo Window",
  "HINT_pkpayTopComponent=This is a pkpay window"
})
public final class pkpayTopComponent extends TopComponent implements PropertyChangeListener {

  public pkpayTopComponent() {
    System.out.println("before init");
    initComponents();
    System.out.println("after init");
    customInit();
    System.out.println("after custom");

    setName(Bundle.CTL_pkpayTopComponent());
    setToolTipText(Bundle.HINT_pkpayTopComponent());

  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {

  }

  private class FireFlist extends SwingWorker<Void, Void> {

    final ProgressHandle pbar = ProgressHandleFactory.createHandle("Firing Flist");
    private PortalContext pcm;

    @Override
    protected Void doInBackground() {
      allowRunningState = true;
      disableBtn();
      pbar.start();

      if (F_list.isEmpty()) {
        txtOutput.setText("Nothing to process");
        pbar.finish();
        allowRunningState = false;
        enableBtn();
        return null;
      }

      try {
        init();
        process();
      } catch (Exception e) {
        Utilities.popup(e.toString());
        Utilities.logStack(me, e);
      }

      pbar.finish();
      allowRunningState = false;
      enableBtn();
      return null;
    }

    private void init() throws Exception {

      pcm = PortalConnectionManager.getInstance().getConnection();
      sb = new StringBuilder();
    }

    private void process() {

      pbar.switchToDeterminate(F_list.size());
      int counter = 0;

      for (FList input : F_list) {

        if (allowRunningState == false) {
          break;
        }

        String ba = banos.get(counter);
        String checked = ptable.getValueAt(counter, 3).toString();
        pbar.progress(ba, counter);
        counter++;

        if (checked.equals("false") || input.isEmpty()) {
          sb.append("Skipping ba#" + ba);
          sb.append(constant.LINE_SEPARATOR);
          txtOutput.setText(sb.toString());
          continue;
        }

        sb.append("Firing " + ba);
        sb.append(constant.LINE_SEPARATOR);
        sb.append(input.asString());
        sb.append(constant.LINE_SEPARATOR);
        txtOutput.setText(sb.toString());

        try {
          FList output = pcm.opcode(PortalOp.CUST_UPDATE_CUSTOMER, 32, input);
          sb.append(output.asString());

        } catch (EBufException ebe) {
          sb.append(ebe.toString());

          Utilities.log(me, "error firing testnap for ba#" + ba, 1);
          Utilities.logStack(me, ebe);
        }

        sb.append(constant.LINE_SEPARATOR);
        sb.append("----");
        sb.append(constant.LINE_SEPARATOR);
        txtOutput.setText(sb.toString());

      }

    }

  }

  private class FlistBuilder extends SwingWorker<Void, Void> {

    dbHandler dbh;
    final ProgressHandle pbar = ProgressHandleFactory.createHandle("Building Flist");
    PreparedStatement psGetInv;
    PreparedStatement psGetPukal;
    PreparedStatement psCheckPayinfo;

    @Override
    protected Void doInBackground() {
      allowRunningState = true;
      disableBtn();
      pbar.start();

      pbar.progress("Discarding impurities");
      removeDuplicate();

      try {
        init();
        process();

      } catch (Exception e) {

      }

      allowRunningState = false;
      enableBtn();
      cleanup();
      return null;

    }

    private void init() throws SQLException {
      pbar.progress("Hiring clerks");
      txtOutput.setText("Working on it");

      dbh = new dbHandler("apps");
      dbh.setDBConnInfo(constant.dbConApps);
      dbh.setUserPass(constant.dbConUser, constant.dbConPass);

      dbh.openConnection();

      psGetInv = dbh.createPS("select a.POID_ID0 as acct_poid,b.POID_ID0 as billinfo_poid,\n"
              + " e.address,e.CITY,e.COUNTRY,e.name,e.STATE,e.ZIP\n"
              + " from account_t a,pin.billinfo_t b,\n"
              + " payinfo_t c, PAYINFO_INV_T e\n"
              + " where b.ACCOUNT_OBJ_ID0=a.POID_ID0\n"
              + " and c.ACCOUNT_OBJ_ID0 = b.ACCOUNT_OBJ_ID0\n"
              + " and b.PAYINFO_OBJ_ID0 = c.POID_ID0\n"
              + " and c.ACCOUNT_OBJ_ID0 = a.POID_ID0\n"
              + " and e.obj_id0 = c.poid_id0\n"
              + " and a.ACCOUNT_NO = ? ");

      psGetPukal = dbh.createPS("select a.POID_ID0 as acct_poid,b.POID_ID0 as billinfo_poid,\n"
              + " e.address,e.CITY,e.COUNTRY,e.name,e.STATE,e.ZIP\n"
              + " from account_t a, pin.billinfo_t b,\n"
              + " payinfo_t c, TM_PAYINFO_PUKAL_T e\n"
              + " where b.ACCOUNT_OBJ_ID0=a.POID_ID0\n"
              + " and c.ACCOUNT_OBJ_ID0 = b.ACCOUNT_OBJ_ID0\n"
              + " and b.PAYINFO_OBJ_ID0 = c.POID_ID0\n"
              + " and c.ACCOUNT_OBJ_ID0 = a.POID_ID0\n"
              + " and e.obj_id0 = c.poid_id0\n"
              + " and a.ACCOUNT_NO = ? ");
      
      psCheckPayinfo = dbh.createPS("select tbi.payinfo_obj_type from account_T ta, billinfo_t tbi "
              + "where ta.account_no = ? and ta.poid_Id0 = tbi.account_obj_id0");

      F_list = new ArrayList<FList>();
      banos = new ArrayList<String>();
      ptable.clearContent();
      sb = new StringBuilder();

    }

    private void process() {
      pbar.switchToDeterminate(inputs.size());
      int progress = 0;
      for (String aa : inputs) {
        if (allowRunningState == false) {
          break;
        }

        boolean gotdata = true;
        String[] data = aa.split("\\|");

        if (data.length != 3) {
          Utilities.log(me, "bad input: " + aa, 1);
          continue;
        }

        pbar.progress(data[0], progress++);

        FList input = buildTheFlist(data[0], data[1], data[2]);
        if (input.isEmpty()) {
          gotdata = false;
        } else {
          sb.append(input.asString());
          sb.append(constant.LINE_SEPARATOR);
          sb.append(constant.LINE_SEPARATOR);
        }

        F_list.add(input);
        banos.add(data[0]);

        ptable.add(data[0], data[1], data[2], gotdata);
        txtOutput.setText(sb.toString());
        ptable.fireTableDataChanged();

      }

    }

    private FList buildTheFlist(String ba, String jan, String poc) {

      FList output = new FList();

      try {
        
        // check the payinfo type
        psCheckPayinfo.setString(1, ba);
        ResultSet cpay = psCheckPayinfo.executeQuery();
        ResultSet rs;
        if(cpay.next()){
          String type = dbHandler.dbGetString(cpay, 1);
          if(type.equals("/payinfo/invoice")){
            psGetInv.setString(1, ba);
            rs = psGetInv.executeQuery();
          } else {
            psGetPukal.setString(1, ba);
            rs = psGetPukal.executeQuery();
          }
        } else {
          Utilities.log(me, "Record not found: " + ba, 1);
          return output;
        }
        
        if (rs.next()) {
          String accpoid = dbHandler.dbGetString(rs, 1);
          String tbipoid = dbHandler.dbGetString(rs, 2);
          String addr = dbHandler.dbGetString(rs, 3);
          String city = dbHandler.dbGetString(rs, 4);
          String country = dbHandler.dbGetString(rs, 5);
          String namae = dbHandler.dbGetString(rs, 6);
          String state = dbHandler.dbGetString(rs, 7);
          String zip = dbHandler.dbGetString(rs, 8);

          // inputf.set(FldPoid.getInst(), (new Poid(1, Long.parseLong(acc_poid), "/account")));
          output.set(FldAccountObj.getInst(), (new Poid(1, Long.parseLong(accpoid), "/account")));
          output.set(FldPoid.getInst(), (new Poid(1, Long.parseLong(accpoid), "/account")));
          output.set(FldProgramName.getInst(), "Operate - GUI");

          FList billinfo = new FList();
          billinfo.set(FldPoid.getInst(), (new Poid(1, Long.parseLong(tbipoid), "/billinfo")));
          billinfo.set(FldPayType.getInst(), 10100);
          output.setElement(FldBillinfo.getInst(), 0, billinfo);

          FList payinfo = new FList();
          payinfo.set(FldInvType.getInst(), 0);
          payinfo.set(FldPoid.getInst(), (new Poid(1, -1, "/tm_pukal")));
          payinfo.set(FldPayType.getInst(), 10100);

          FList inherit = new FList();
          FList pukalInfo = new FList();
          pukalInfo.set(FldAddress.getInst(), addr);
          pukalInfo.set(FldCity.getInst(), city);
          pukalInfo.set(FldCountry.getInst(), country);
          pukalInfo.set(FldName.getInst(), namae);
          pukalInfo.set(FldState.getInst(), state);
          pukalInfo.set(FldZip.getInst(), zip);
          pukalInfo.set(TmFldJanCode.getInst(), jan);
          pukalInfo.set(TmFldPocCode.getInst(), poc);
          pukalInfo.set(TmFldBauKodHasil.getInst(), "0");

          inherit.setElement(TmFldPukalInfo.getInst(), 0, pukalInfo);
          payinfo.set(FldInheritedInfo.getInst(), inherit);
          output.setElement(FldPayinfo.getInst(), 0, payinfo);

          /*
           0 PIN_FLD_ACCOUNT_OBJ               POID [0] 0.0.0.1 /account 4909118220
           0 PIN_FLD_POID                      POID [0] 0.0.0.1 /account 4909118220
           0 PIN_FLD_BILLINFO                 ARRAY [0] allocated 5, used 5
           1   PIN_FLD_POID                    POID [0] 0.0.0.1 /billinfo 4909118223
           1   PIN_FLD_PAY_TYPE                ENUM [0] 10100
           0 PIN_FLD_PROGRAM_NAME               STR [0] "Operate"
           0 PIN_FLD_PAYINFO                  ARRAY [0] allocated 4, used 4
           1   PIN_FLD_INV_TYPE                ENUM [0] 0
           1   PIN_FLD_POID                    POID [0] 0.0.0.1 /payinfo/tm_pukal -1 0
           1   PIN_FLD_INHERITED_INFO     SUBSTRUCT [0] allocated 1, used 1
           2     TM_FLD_PUKAL_INFO            ARRAY [0] allocated 20, used 10
           3       PIN_FLD_ADDRESS              STR [0] ""
           3       PIN_FLD_CITY                 STR [0] "KUCHING"
           3       PIN_FLD_COUNTRY              STR [0] "MALAYSIA"
           3       PIN_FLD_NAME                 STR [0] "SK SG TISANG - YBB9110"
           3       PIN_FLD_STATE                STR [0] "SARAWAK"
           3       PIN_FLD_ZIP                  STR [0] "93200"
           3       TM_FLD_JAN_CODE              STR [0] "4617"
           3       TM_FLD_POC_CODE              STR [0] "097"
           3       TM_FLD_BAU_KOD_HASIL         STR [0] "0"
           1   PIN_FLD_PAY_TYPE                ENUM [0] 10100  
           */
        } else {
          Utilities.log(me, "Record not found: " + ba, 1);
        }
      } catch (SQLException e) {
        Utilities.log(me, "error building flist: " + ba, 1);
        Utilities.logStack(me, e);
      }

      return output;

    }

    private void cleanup() {
      try {
        dbh.closeConnection();
      } catch (Exception e) {
        Utilities.logStack(me, e);
      }

      pbar.finish();

    }

  }

  private void customInit() {
    caret = (DefaultCaret) txtOutput.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

    F_list = new ArrayList<FList>();
    enableBtn();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jLabel2 = new javax.swing.JLabel();
    jLabel1 = new javax.swing.JLabel();
    jScrollPane1 = new javax.swing.JScrollPane();
    txtInput = new javax.swing.JTextArea();
    jPanel1 = new javax.swing.JPanel();
    btnBuild = new javax.swing.JButton();
    btnFira = new javax.swing.JButton();
    btnStop = new javax.swing.JButton();
    jScrollPane2 = new javax.swing.JScrollPane();
    jTable1 = new javax.swing.JTable();
    jLabel3 = new javax.swing.JLabel();
    jScrollPane3 = new javax.swing.JScrollPane();
    txtOutput = new javax.swing.JTextArea();
    lblReady = new javax.swing.JLabel();

    org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(pkpayTopComponent.class, "pkpayTopComponent.jLabel2.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(pkpayTopComponent.class, "pkpayTopComponent.jLabel1.text")); // NOI18N

    txtInput.setColumns(20);
    txtInput.setRows(5);
    jScrollPane1.setViewportView(txtInput);

    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(pkpayTopComponent.class, "pkpayTopComponent.jPanel1.border.title"))); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(btnBuild, org.openide.util.NbBundle.getMessage(pkpayTopComponent.class, "pkpayTopComponent.btnBuild.text")); // NOI18N
    btnBuild.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnBuildActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(btnFira, org.openide.util.NbBundle.getMessage(pkpayTopComponent.class, "pkpayTopComponent.btnFira.text")); // NOI18N
    btnFira.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnFiraActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(btnStop, org.openide.util.NbBundle.getMessage(pkpayTopComponent.class, "pkpayTopComponent.btnStop.text")); // NOI18N
    btnStop.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnStopActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
          .addComponent(btnBuild, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btnFira, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btnStop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addComponent(btnBuild)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btnFira)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 43, Short.MAX_VALUE)
        .addComponent(btnStop)
        .addContainerGap())
    );

    jTable1.setModel(ptable);
    jScrollPane2.setViewportView(jTable1);

    org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(pkpayTopComponent.class, "pkpayTopComponent.jLabel3.text")); // NOI18N

    txtOutput.setColumns(20);
    txtOutput.setRows(5);
    jScrollPane3.setViewportView(txtOutput);

    org.openide.awt.Mnemonics.setLocalizedText(lblReady, org.openide.util.NbBundle.getMessage(pkpayTopComponent.class, "pkpayTopComponent.lblReady.text")); // NOI18N

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane3)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jLabel1)
              .addComponent(jLabel3))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lblReady))
          .addGroup(layout.createSequentialGroup()
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 367, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 312, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel1)
          .addComponent(lblReady))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
          .addComponent(jScrollPane1)
          .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
          .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabel3)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
    // TODO add your handling code here:
    allowRunningState = false;
  }//GEN-LAST:event_btnStopActionPerformed

  private void btnBuildActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuildActionPerformed
    // TODO add your handling code here:
    FlistBuilder fb = new FlistBuilder();
    fb.addPropertyChangeListener(this);
    fb.execute();

  }//GEN-LAST:event_btnBuildActionPerformed

  private void btnFiraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFiraActionPerformed
    // TODO add your handling code here:
    FireFlist fb = new FireFlist();
    fb.addPropertyChangeListener(this);
    fb.execute();
  }//GEN-LAST:event_btnFiraActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton btnBuild;
  private javax.swing.JButton btnFira;
  private javax.swing.JButton btnStop;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JScrollPane jScrollPane3;
  private javax.swing.JTable jTable1;
  private javax.swing.JLabel lblReady;
  private javax.swing.JTextArea txtInput;
  private javax.swing.JTextArea txtOutput;
  // End of variables declaration//GEN-END:variables

  private String me = "Pukal Payinfo Flist";
  pymtTblModel ptable = new pymtTblModel();
  private boolean allowRunningState;
  DefaultCaret caret;
  private StringBuilder sb;

  private ArrayList<FList> F_list;
  private ArrayList<String> inputs;
  private ArrayList<String> banos;

  private void removeDuplicate() {
    Utilities.log(me, "Removing duplicate", constant.DEBUG);
    Scanner sc = new Scanner(txtInput.getText());

    inputs = new ArrayList<String>();

    // remove duplicate
    while (sc.hasNextLine()) {
      String data = sc.nextLine().trim();

      if (data.isEmpty()) {
        continue;
      }

      if (data.toLowerCase().contains("acc")) {
        continue;
      }

      if (!inputs.contains(data)) {
        inputs.add(data);
      }

    }

    // add back to the GUI
    String output = "";

    for (String aa : inputs) {
      output += aa + constant.LINE_SEPARATOR;
    }

    txtInput.setText(output);

    lblReady.setText("Total record to process: " + inputs.size());

  }

  private void enableBtn() {
    btnBuild.setEnabled(true);
    btnFira.setEnabled(true);
    btnStop.setEnabled(false);
  }

  private void disableBtn() {
    btnBuild.setEnabled(false);
    btnFira.setEnabled(false);
    btnStop.setEnabled(true);
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
}
