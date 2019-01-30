/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bsm.nova.ops;

import com.portal.pcm.EBufException;
import com.portal.pcm.FList;
import com.portal.pcm.Poid;
import com.portal.pcm.PortalContext;
import com.portal.pcm.PortalOp;
import com.portal.pcm.fields.FldAccountObj;
import com.portal.pcm.fields.FldDealInfo;
import com.portal.pcm.fields.FldEndT;
import com.portal.pcm.fields.FldPoid;
import com.portal.pcm.fields.FldProducts;
import com.portal.pcm.fields.FldServiceObj;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import javax.swing.SwingWorker;
import ops.com.PortalConnectionManager;
import ops.com.Utilities;
import ops.com.constant;
import ops.com.dbHandler;
import org.apache.log4j.PropertyConfigurator;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Cancellable;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//bsm.nova.ops//Ata//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "AtaTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "bsm.nova.ops.AtaTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_AtaAction",
        preferredID = "AtaTopComponent"
)
@Messages({
  "CTL_AtaAction=Ata",
  "CTL_AtaTopComponent=Ata Window",
  "HINT_AtaTopComponent=This is a Ata window"
})
public final class AtaTopComponent extends TopComponent implements PropertyChangeListener {

  class CancelMyTask implements Cancellable {

    @Override
    public boolean cancel() {
      isRunningState = false;

      return doneConnectings;
    }

  }

  class bgWorker extends SwingWorker<Void, Void> {

    private String descr;
    private long prodpoid;
    private ProgressHandle pbar;
    private dbHandler dbh;
    private PortalContext pc;
    private PreparedStatement psSearch;
    private PreparedStatement psGetResult;
    private StringBuilder sb;
    private FList inputfl;

    public bgWorker() {
      PropertyConfigurator.configure(getClass().getClassLoader().getResource("../../config/log4j.properties"));
      dbh = new dbHandler("apps");
      dbh.setDBConnInfo(constant.dbConApps);
      dbh.setUserPass(constant.dbConUser, constant.dbConPass);
    }

    @Override
    protected Void doInBackground() throws Exception {
      isRunningState = true;
      sb = new StringBuilder();
      setButton();

      pbar = ProgressHandle.createHandle("ATA Product Purchaser");
      pbar.start();

      pbar.progress("Removing duplicates");
      removeDuplicate();

      if (servpoid.size() > 0) {
        pbar.progress("checking for selected product");
        if (!checkSelectedProduct()) {

          Utilities.popup("Please select a product to purchase");

          pbar.finish();
          isRunningState = false;
          setButton();
          return null;
        }

        pbar.progress("connecting to DB");
        try {
          connectThemAll();
        } catch (EBufException e) {
          Utilities.popup("Error connecting to CM");
          Utilities.logStack(me, e);
          pbar.finish();
          isRunningState = false;
          setButton();
          return null;
        } catch (SQLException e) {
          Utilities.popup("Error connecting to database");
          Utilities.logStack(me, e);
          pbar.finish();
          isRunningState = false;
          setButton();
          return null;
        }

        pbar.progress("Processing input");
        pbar.switchToDeterminate(servpoid.size());
        int counter = 0;

        for (String serv : servpoid) {
          pbar.progress(counter++);
          if (serv.contains("|")) {
            process(serv);
          } else {
            outPut("Bad input: " + serv);
          }

        }

      }

      cleanup();

      pbar.finish();
      isRunningState = false;
      setButton();
      return null;
    }

    private void process(String line) {

      String[] input = line.split("\\|");
      long svc;
      long datets;
      long acc;

      try {
        svc = Long.parseLong(input[0]);
        datets = Long.parseLong(input[1]);
      } catch (NumberFormatException e) {
        outPut("Bad input: " + line);
        return;
      }

      Date enddate = new Date(datets * 1000);

      // get the account poid
      try {
        psSearch.setLong(1, svc);
        ResultSet rs = psSearch.executeQuery();

        if (rs.next()) {
          acc = rs.getLong(1);
        } else {
          outPut("Service not found " + svc);
          return;
        }

      } catch (SQLException e) {
        outPut("bad input? " + svc + " - " + e.toString());
        return;
      }

      // set the end date
      inputfl.set(FldEndT.getInst(), enddate);

      // set the service poid
      Poid svcpoid = new Poid(1, svc, "/service/telephony");
      inputfl.set(FldServiceObj.getInst(), svcpoid);

      // set the account poid
      Poid accpoid = new Poid(1, acc, "/account");
      inputfl.set(FldPoid.getInst(), accpoid);

      // fire the opcode
      try {
        FList out = pc.opcode(PortalOp.SUBSCRIPTION_PURCHASE_DEAL, inputfl);
      } catch (EBufException e) {
        outPut("Error fire opcode: " + svc + " - " + e.toString());
        Utilities.log(me, "Error fire opcode: " + svc, constant.ERROR);
        Utilities.logStack(me, e);
        Utilities.log(me, inputfl.asString(), constant.ERROR);
      }

      // search the output?
      try {
        psGetResult.setLong(1, svc);
        
        ResultSet rs = psGetResult.executeQuery();
        int resultcounter = 0;
        String d = "|";
        while(rs.next()){
          resultcounter++;
          String out = dbHandler.dbGetString(rs, 1) + d
                  + dbHandler.dbGetString(rs, 2) + d
                  + dbHandler.dbGetString(rs, 3) + d
                  + dbHandler.dbGetString(rs, 4) + d
                  + dbHandler.dbGetString(rs, 5) + d
                  + dbHandler.dbGetString(rs, 6) + d
                  + dbHandler.dbGetString(rs, 7) + d
                  + dbHandler.dbGetString(rs, 8) + d
                  + dbHandler.dbGetString(rs, 9) + d
                  + dbHandler.dbGetString(rs, 10);
          outPut(out);
        }
        
        if(resultcounter == 0){
          outPut("No purchased product found for service " + svc);
        } else if(resultcounter > 1){
          outPut("Multiple same product active for this service " + svc);
        }
        
      } catch (SQLException e) {
        outPut("Error finding product for service " + svc + " - " + e.toString());
      }

    }

    private boolean checkSelectedProduct() {
      if (rbBizVoice45.isSelected()) {
        descr = "BIZ Voice Plan (Multiple Voice Line RM45)";
        prodpoid = 519519003;
      } else if (rbBizVoice78.isSelected()) {
        descr = "BIZ Voice Plan (Multiple Voice Line RM78)";
        prodpoid = 519519771;
      } else if (rbClip.isSelected()) {
        descr = "Voice Call Line Identification Presentation (CLIP) Monthly Fee";
        prodpoid = 44021344;
      } else if (rbEarlyTerminateFee.isSelected()) {
        descr = "Business Voice Early Termination Penalty Fee";
        prodpoid = 519518571;
      } else if (rbFreeCredit.isSelected()) {
        descr = "Free Credits RM78";
        prodpoid = 519516395;
      } else if (rbMUltiVoice45.isSelected()) {
        descr = "Multiple Voice Line (Business) Monthly Fee RM45";
        prodpoid = 519519147;
      } else if (rbMultiVoice78.isSelected()) {
        descr = "Multiple Voice Line (Business) Monthly Fee RM78";
        prodpoid = 519519339;
      } else {
        return false;
      }

      outPut("Selected product: " + prodpoid + " - " + descr);

      return true;
    }

    private void connectThemAll() throws EBufException, SQLException {
      pc = PortalConnectionManager.getInstance().getConnection();

      dbh.openConnection();

      psSearch = dbh.createPS("select account_obj_Id0 from service_T where poid_Id0 = ? ");
      psGetResult = dbh.createPS("select\n"
              + "    ta.account_no\n"
              + "    , ts.login\n"
              + "    , tpp.descr\n"
              + "    , tpp.service_obj_type\n"
              + "    , tpp.service_obj_id0\n"
              + "    , tpp.product_obj_type\n"
              + "    , tpp.product_obj_id0\n"
              + "    , tpp.poid_type\n"
              + "    , tpp.poid_Id0\n"
              + "    , unix_ora_Ts_conv(tpp.effective_t)\n"
              + " from\n"
              + "    account_T ta\n"
              + "    , service_t ts\n"
              + "    , purchased_product_t tpp\n"
              + " where\n"
              + "    ts.poid_Id0 = ? \n"
              + "    and tpp.account_obj_Id0 = ts.account_obj_Id0\n"
              + "    and tpp.service_obj_id0 = ts.poid_Id0\n"
              + "    and tpp.product_obj_Id0 = " + prodpoid + " and tpp.status = 1 \n"
              + "    and ts.account_obj_Id0 = ta.poid_Id0");

      inputfl = FList.createFromString(
              "0 PIN_FLD_POID           POID [0] 0.0.0.1 /account 4370538694 0\n"
              + "0 PIN_FLD_PROGRAM_NAME    STR [0] \"ATA OpsTool\"\n"
              + "0 PIN_FLD_SERVICE_OBJ    POID [0] 0.0.0.1 /service/telephony 6601083185\n"
              + "0 PIN_FLD_END_T        TSTAMP [0] (1542902401)\n"
              + "0 PIN_FLD_DEAL_INFO    SUBSTRUCT [0] allocated 20, used 8\n"
              + "1     PIN_FLD_NAME            STR [0] \"Dummy\"\n"
              + "1     PIN_FLD_POID           POID [0] 0.0.0.1 /deal -1 0\n"
              + "1     PIN_FLD_END_T        TSTAMP [0] (0)\n"
              + "1     PIN_FLD_FLAGS           INT [0] 0\n"
              + "1     PIN_FLD_START_T      TSTAMP [0] (0)\n"
              + "1     PIN_FLD_DESCR           STR [0] \"\"\n"
              + "1     PIN_FLD_PRODUCTS      ARRAY [0] allocated 24, used 24\n"
              + "2         PIN_FLD_PURCHASE_END_T TSTAMP [0] (0)\n"
              + "2         PIN_FLD_PURCHASE_START_T TSTAMP [0] (0)\n"
              + "2         PIN_FLD_USAGE_END_DETAILS    INT [0] 0\n"
              + "2         PIN_FLD_USAGE_START_UNIT    INT [0] 0\n"
              + "2         PIN_FLD_CYCLE_END_DETAILS    INT [0] 0\n"
              + "2         PIN_FLD_QUANTITY     DECIMAL [0] 1\n"
              + "2         PIN_FLD_PURCHASE_END_DETAILS    INT [0] 0\n"
              + "2         PIN_FLD_CYCLE_START_UNIT    INT [0] 0\n"
              + "2         PIN_FLD_USAGE_START_DETAILS    INT [0] 1\n"
              + "2         PIN_FLD_CYCLE_START_DETAILS    INT [0] 1\n"
              + "2         PIN_FLD_PURCHASE_START_DETAILS    INT [0] 1\n"
              + "2         PIN_FLD_PURCHASE_START_UNIT    INT [0] 0\n"
              + "2         PIN_FLD_PRODUCT_OBJ    POID [0] 0.0.0.1 /product " + prodpoid + " 0\n"
              + "2         PIN_FLD_DESCR           STR [0] \"" + descr + "\"\n"
              + "2         PIN_FLD_USAGE_DISCOUNT DECIMAL [0] 0\n"
              + "2         PIN_FLD_CYCLE_DISCOUNT DECIMAL [0] 0\n"
              + "2         PIN_FLD_PURCHASE_DISCOUNT DECIMAL [0] 0\n"
              + "2         PIN_FLD_STATUS         ENUM [0] 1\n"
              + "2         PIN_FLD_STATUS_FLAGS    INT [0] 0\n"
              + "2         PIN_FLD_USAGE_END_T  TSTAMP [0] (0)\n"
              + "2         PIN_FLD_USAGE_START_T TSTAMP [0] (0)\n"
              + "2         PIN_FLD_CYCLE_END_T  TSTAMP [0] (0)\n"
              + "2         PIN_FLD_CYCLE_START_T TSTAMP [0] (0)");

    }

    private void cleanup() {
      try {
        pc.close(true);
        dbh.closeConnection();
      } catch (EBufException | SQLException e) {
        Utilities.logStack(me, e);
      }
    }

    private void outPut(String line) {
      sb.append(line);
      sb.append(constant.LINE_SEPARATOR);

      txtOutput.setText(sb.toString());
    }

  }

  public AtaTopComponent() {
    initComponents();
    setName(Bundle.CTL_AtaTopComponent());
    setToolTipText(Bundle.HINT_AtaTopComponent());
    custominit();
  }

  private void custominit() {
    isRunningState = false;
    doneConnectings = true;
    setButton();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    bgAtaGroup = new javax.swing.ButtonGroup();
    jPanel1 = new javax.swing.JPanel();
    rbClip = new javax.swing.JRadioButton();
    rbFreeCredit = new javax.swing.JRadioButton();
    rbEarlyTerminateFee = new javax.swing.JRadioButton();
    rbBizVoice45 = new javax.swing.JRadioButton();
    rbMUltiVoice45 = new javax.swing.JRadioButton();
    rbMultiVoice78 = new javax.swing.JRadioButton();
    rbBizVoice78 = new javax.swing.JRadioButton();
    jButton1 = new javax.swing.JButton();
    jButton2 = new javax.swing.JButton();
    jScrollPane1 = new javax.swing.JScrollPane();
    txtInput = new javax.swing.JTextArea();
    jLabel1 = new javax.swing.JLabel();
    jScrollPane2 = new javax.swing.JScrollPane();
    txtOutput = new javax.swing.JTextArea();
    jLabel2 = new javax.swing.JLabel();

    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.jPanel1.border.title"))); // NOI18N

    bgAtaGroup.add(rbClip);
    org.openide.awt.Mnemonics.setLocalizedText(rbClip, org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.rbClip.text")); // NOI18N

    bgAtaGroup.add(rbFreeCredit);
    org.openide.awt.Mnemonics.setLocalizedText(rbFreeCredit, org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.rbFreeCredit.text")); // NOI18N

    bgAtaGroup.add(rbEarlyTerminateFee);
    org.openide.awt.Mnemonics.setLocalizedText(rbEarlyTerminateFee, org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.rbEarlyTerminateFee.text")); // NOI18N

    bgAtaGroup.add(rbBizVoice45);
    org.openide.awt.Mnemonics.setLocalizedText(rbBizVoice45, org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.rbBizVoice45.text")); // NOI18N
    rbBizVoice45.setToolTipText(org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.rbBizVoice45.toolTipText")); // NOI18N

    bgAtaGroup.add(rbMUltiVoice45);
    org.openide.awt.Mnemonics.setLocalizedText(rbMUltiVoice45, org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.rbMUltiVoice45.text")); // NOI18N

    bgAtaGroup.add(rbMultiVoice78);
    org.openide.awt.Mnemonics.setLocalizedText(rbMultiVoice78, org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.rbMultiVoice78.text")); // NOI18N

    bgAtaGroup.add(rbBizVoice78);
    org.openide.awt.Mnemonics.setLocalizedText(rbBizVoice78, org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.rbBizVoice78.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.jButton1.text")); // NOI18N
    jButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton1ActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(jButton2, org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.jButton2.text")); // NOI18N
    jButton2.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton2ActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(rbClip)
          .addComponent(rbFreeCredit)
          .addComponent(rbEarlyTerminateFee))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(rbBizVoice78)
          .addComponent(rbBizVoice45)
          .addComponent(rbMultiVoice78)
          .addComponent(rbMUltiVoice45))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 80, Short.MAX_VALUE)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
          .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(rbClip)
          .addComponent(rbMUltiVoice45))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(rbFreeCredit)
          .addComponent(rbMultiVoice78))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(rbEarlyTerminateFee)
          .addComponent(rbBizVoice45))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(rbBizVoice78))
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addComponent(jButton2)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jButton1))
    );

    txtInput.setColumns(20);
    txtInput.setRows(5);
    jScrollPane1.setViewportView(txtInput);

    org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.jLabel1.text")); // NOI18N

    txtOutput.setColumns(20);
    txtOutput.setRows(5);
    jScrollPane2.setViewportView(txtOutput);

    org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(AtaTopComponent.class, "AtaTopComponent.jLabel2.text")); // NOI18N

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 254, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel1)))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jLabel2)
              .addComponent(jScrollPane2))))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel1)
          .addComponent(jLabel2))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
          .addComponent(jScrollPane1))
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    // TODO add your handling code here:
    isRunningState = false;
  }//GEN-LAST:event_jButton1ActionPerformed

  private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
    // TODO add your handling code here:
    bgWorker bw = new bgWorker();
    bw.addPropertyChangeListener(this);
    bw.execute();
  }//GEN-LAST:event_jButton2ActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.ButtonGroup bgAtaGroup;
  private javax.swing.JButton jButton1;
  private javax.swing.JButton jButton2;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JRadioButton rbBizVoice45;
  private javax.swing.JRadioButton rbBizVoice78;
  private javax.swing.JRadioButton rbClip;
  private javax.swing.JRadioButton rbEarlyTerminateFee;
  private javax.swing.JRadioButton rbFreeCredit;
  private javax.swing.JRadioButton rbMUltiVoice45;
  private javax.swing.JRadioButton rbMultiVoice78;
  private javax.swing.JTextArea txtInput;
  private javax.swing.JTextArea txtOutput;
  // End of variables declaration//GEN-END:variables

  private boolean isRunningState;
  private boolean doneConnectings;
  private ArrayList<String> servpoid;
  private final String me = "ATA purchaser";

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

  private void setButton() {
    jButton1.setEnabled(isRunningState);
    jButton2.setEnabled(!isRunningState);

    rbBizVoice45.setEnabled(!isRunningState);
    rbBizVoice78.setEnabled(!isRunningState);
    rbClip.setEnabled(!isRunningState);
    rbEarlyTerminateFee.setEnabled(!isRunningState);
    rbFreeCredit.setEnabled(!isRunningState);
    rbMUltiVoice45.setEnabled(!isRunningState);
    rbMultiVoice78.setEnabled(!isRunningState);

  }

  private void removeDuplicate() {
    Scanner sc = new Scanner(txtInput.getText());

    servpoid = new ArrayList<String>();

    // remove duplicate
    while (sc.hasNextLine()) {
      String data = sc.nextLine().trim();

      if (data.isEmpty()) {
        continue;
      }

      if (!servpoid.contains(data)) {
        servpoid.add(data);
      }

    }

    // add back to the GUI
    String uiout = "";

    for (String aa : servpoid) {
      uiout += aa + constant.LINE_SEPARATOR;
    }

    txtInput.setText(uiout);

    jLabel2.setText("Total record: " + servpoid.size());

  }
}
