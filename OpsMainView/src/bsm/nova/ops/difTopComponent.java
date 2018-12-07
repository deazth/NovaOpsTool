/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bsm.nova.ops;

import ops.com.PortalConnectionManager;
import ops.com.Utilities;
import ops.com.constant;
import ops.com.dbHandler;
import com.portal.pcm.FList;
import com.portal.pcm.Poid;
import com.portal.pcm.PortalContext;
import com.portal.pcm.PortalOp;
import com.portal.pcm.fields.FldEndT;
import com.portal.pcm.fields.FldFlags;
import com.portal.pcm.fields.FldPoid;
import com.portal.pcm.fields.FldProgramName;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.text.DefaultCaret;
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
        dtd = "-//bsm.nova.ops//dif//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "difTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "bsm.nova.ops.difTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_difAction",
        preferredID = "difTopComponent"
)
@Messages({
  "CTL_difAction=Data Integrity Fix",
  "CTL_difTopComponent=Data Integrity Fix",
  "HINT_difTopComponent=This is a DIF window"
})
public final class difTopComponent extends TopComponent implements PropertyChangeListener {

  public difTopComponent() {
    initComponents();
    customInit();
    setName(Bundle.CTL_difTopComponent());
    setToolTipText(Bundle.HINT_difTopComponent());

  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {

  }
  
  class patchWorker extends SwingWorker<Void, Void>{

    private PreparedStatement psUpdate;
    private PreparedStatement psGet;
    private dbHandler dbh;
    private String newValue = "";
    private String modetext = "";
    
    @Override
    protected Void doInBackground() {
      
      try {
        init();
        processPatch();
        cleanup();
      } catch (Exception e) {
        Utilities.popup(e.toString());
      }
      
      return null;
    }
    
    private void init() throws SQLException{
      removeDuplicate();
      dbh = new dbHandler("apps");

      try {
        dbh.setDBConnInfo(constant.dbConApps);
        dbh.setUserPass(constant.dbConUser, constant.dbConPass);
        dbh.openConnection();
        
        String updateSql = "";
        String getOldSql = "select poid_id0, nvl(account_tag, 'null') from account_t where account_no = ? ";
        
        if(mode == 2){
          String bsinput = JOptionPane.showInputDialog("Enter New Bill Stream");
          if(bsinput.isEmpty()){
            throw new NullPointerException("Empty bill stream input");
          }
          
          String bsmonth = JOptionPane.showInputDialog("For how many month?");
          if(bsmonth.isEmpty()){
            throw new NullPointerException("Invalid month count");
          }
          
          try {
            int number = Integer.parseInt(bsinput);
            int number2 = Integer.parseInt(bsmonth);
          } catch (NumberFormatException e) {
            throw e;
          }
          
          getOldSql = "select obj_id0, tm_fld_bill_stream, tm_fld_bill_stream_counter \n"
                + " from tm_inv_profile_t ttip\n"
                + "  , account_t ta\n"
                + "  , profile_t tpro\n"
                + " where\n"
                + "  ta.account_no = ? \n"
                + "  and ta.poid_Id0 = tpro.account_obj_id0\n"
                + "  and tpro.poid_Id0 = ttip.obj_id0";
          
          updateSql = "update tm_inv_profile_t set tm_fld_bill_stream = " + bsinput + ", tm_fld_bill_stream_counter = " + bsmonth
                + " where obj_id0 = ? ";
          newValue = bsinput;
          modetext = "Patch Bill Stream";
          
        } else if (mode == 3){
          updateSql = "update account_t set account_tag = 'NO_SMS_SR' where poid_Id0 = ? ";
          newValue = "NO_SMS_SR";
          modetext = "Suppress SR SMS";
        } else if (mode == 4){
          updateSql = "update account_t set account_tag = null where poid_Id0 = ? ";
          newValue = "null";
          modetext = "Remove SR SMS suppress";
        }
        
        psGet = dbh.createPS(getOldSql);
        psUpdate = dbh.createPS(updateSql);
        
        
      } catch (SQLException e) {
        Utilities.popup(e.getMessage());
        throw e;
      }
      
      disableBtn();
      
    }
    
    private void processPatch(){
      
      StringBuilder sb = new StringBuilder();
      String output;
      final ProgressHandle pbar = ProgressHandleFactory.createHandle(modetext);
      pbar.start(accno.size());
      int counter = 0;
      for(String ba : accno){
        
        pbar.progress(ba, counter++);
        
        
        try {
          psGet.setString(1, ba);
          
          ResultSet rs = psGet.executeQuery();
          
          if(rs.next()){
            String poid = dbHandler.dbGetString(rs, 1);
            String oldval = dbHandler.dbGetString(rs, 2);
            
            
            psUpdate.setString(1, poid);
            
            int upcount = psUpdate.executeUpdate();
            
            output = ba + " : " + oldval + " -> " + newValue + " (" + upcount + " record updated)" + constant.LINE_SEPARATOR;
            if(mode == 2){
              String oldmonth = dbHandler.dbGetString(rs, 3);
              output = ba + " : " + oldval + "@" + oldmonth + " -> " + newValue + " (" + upcount + " record updated)" + constant.LINE_SEPARATOR;
            }
            
          } else {
            output = ba + " not found" + constant.LINE_SEPARATOR;
          }
          
        } catch (Exception e) {
          output = ba + " error: " + e.toString();
          Utilities.log(me, "error update " + ba, constant.ERROR);
          Utilities.logStack(me,e);
        }
        
        sb.append(output);
        
        txtOutput.setText(sb.toString());
        
      }
      
      pbar.finish();
      
    }
    
    private void cleanup(){
      enableBtn();
      try {
        dbh.closeConnection();
      } catch (Exception e) {
      }
    }
    
    
    
  }

  class DropRCWorker extends SwingWorker<Void, Void> {

    
      PortalContext pcm;
    
    @Override
    protected Void doInBackground() throws Exception {
      
      PropertyConfigurator.configure(getClass().getClassLoader().getResource("../../config/log4j.properties"));
      pcm = PortalConnectionManager.getInstance().getConnection();
      
      disableBtn();
      runstate = true;
      processList();
      runstate = false;
      enableBtn();
      
      pcm.close(true);
      
      return null;
    }

    private void dropTheCharge(String acc_poid, String end_date, StringBuilder sb) throws Exception {
      

      /*
       r << XXX 1
       0 PIN_FLD_POID POID [0] 0.0.0.1 /account 1761354333 0
       0 PIN_FLD_PROGRAM_NAME STR [0] "mnl_cycle_fees"
       0 PIN_FLD_END_T TSTAMP [0] (1438617600)
       0 PIN_FLD_FLAGS INT [0] 256
       XXX

       xop PCM_OP_SUBSCRIPTION_CYCLE_FORWARD 0 1
      
       */
      FList inputf = new FList();
      inputf.set(FldPoid.getInst(), (new Poid(1, Long.parseLong(acc_poid), "/account")));
      inputf.set(FldProgramName.getInst(), "missing_item_rec");
      inputf.set(FldEndT.getInst(), new Date(Long.parseLong(end_date) * 1000));
      inputf.set(FldFlags.getInst(), 256);

      Utilities.log(me, "input", constant.DEBUG);
      Utilities.log(me, inputf.asString(), constant.DEBUG);

      FList output = pcm.opcode(PortalOp.SUBSCRIPTION_CYCLE_FORWARD, inputf);

      Utilities.log(me, "output", constant.DEBUG);
      Utilities.log(me, output.asString(), constant.DEBUG);

      Scanner sc = new Scanner(output.asString());

      while (sc.hasNextLine()) {
        String data = sc.nextLine();
        if (data.contains("/purchased_product")) {
          sb.append(data + constant.LINE_SEPARATOR);
        }
      }

    }

    private void processList() {

      String pbartext = "";

      if (mode == 0) {
        pbartext = "Dropping RC";
      } else if (mode == 1) {
        pbartext = "Fixing unbilled item";
      }

      final ProgressHandle pbar = ProgressHandleFactory.createHandle(pbartext);
      pbar.start(accno.size());

      StringBuilder sb = new StringBuilder();
      Utilities.log(me, "Prepping DB", constant.DEBUG);
      // prep the DB stuffs
      dbHandler dbh = new dbHandler("apps");
      PreparedStatement ps;
      PreparedStatement psUnbilled1;
      PreparedStatement psUnbilledUpdate;

      try {
        dbh.setDBConnInfo(constant.dbConApps);
        dbh.setUserPass(constant.dbConUser, constant.dbConPass);
      } catch (Exception e) {
        Utilities.popup(e.getMessage());
        return;
      }

      // try to open the connection
      try {
        dbh.openConnection();
      } catch (SQLException e) {
        Utilities.popup(e.getMessage());
        return;
      }

      // prep the prepared statement
      Utilities.log(me, "Prepping prep statement", constant.DEBUG);
      try {
        ps = dbh.createPS("select ta.poid_id0, tbi.last_bill_t from account_t ta, billinfo_t tbi "
                + " where ta.account_no = ? and ta.poid_Id0 = tbi.account_obj_id0 ");

        psUnbilled1 = dbh.createPS("select\n"
                + "  ti.poid_Id0, ti.poid_type, ti.item_total, tbi.bill_obj_Id0\n"
                + "from\n"
                + "  item_t ti\n"
                + "  , account_t ta\n"
                + "  , billinfo_t tbi\n"
                + "where\n"
                + "  ta.account_no = ? \n"
                + "  and ti.status = 1\n"
                + "  and ti.bill_obj_id0 != 0\n"
                + "  and ti.bill_obj_id0 != tbi.bill_obj_Id0\n"
                + "  and ti.account_obj_id0 = ta.poid_id0\n"
                + "  and ta.poid_Id0 = tbi.account_obj_Id0");

        psUnbilledUpdate = dbh.createPS("update item_t set bill_obj_id0 = ? where poid_Id0 = ? ");

      } catch (Exception e) {
        Utilities.popup(e.getMessage());
        return;
      }

      // go through each account
      int counter = 0;
      String output = "counter - item poid and type -> new bill poid -> updated row" + constant.LINE_SEPARATOR;
      if (mode == 1) {
        sb.append(output);
      }

      txtOutput.setText(sb.toString());

      for (String aa : accno) {

        if (!runstate) {
          break;
        }

        pbar.progress(aa, counter);
        counter++;
        Utilities.log(me, "Procesing " + aa, constant.DEBUG);

        try {

          if (mode == 0) {
            ps.setString(1, aa);
          } else if (mode == 1) {
            psUnbilled1.setString(1, aa);
          } else {
            ps.setString(1, aa);
          }

          ResultSet rs;

          if (mode == 0) {
            rs = ps.executeQuery();
          } else if (mode == 1) {
            rs = psUnbilled1.executeQuery();
          } else {
            rs = ps.executeQuery();
          }

          if (rs.next()) {

            if (mode == 0) {
              sb.append(aa + constant.LINE_SEPARATOR);
              txtOutput.setText(sb.toString());
              dropTheCharge(dbHandler.dbGetString(rs, 1), dbHandler.dbGetString(rs, 2), sb);
              sb.append(constant.LINE_SEPARATOR);
            } else if (mode == 1) {

              String itempoid = dbHandler.dbGetString(rs, 1);
              String itemtype = dbHandler.dbGetString(rs, 2);
              String itemamt = dbHandler.dbGetString(rs, 3);
              String billpoid = dbHandler.dbGetString(rs, 4);

              psUnbilledUpdate.setString(1, billpoid);
              psUnbilledUpdate.setString(2, itempoid);

              int upcount = psUnbilledUpdate.executeUpdate();

              output = counter + " - " + itempoid + " "
                      + itemtype + " -> " + billpoid + " -> "
                      + upcount + " record updated" + constant.LINE_SEPARATOR;
              sb.append(output);

            } else {
              Utilities.log(me, "Unsupported Mode: " + aa, constant.ERROR);
            }
            txtOutput.setText(sb.toString());
          } else {
            Utilities.log(me, "Account not found: " + aa, constant.WARNING);

          }

        } catch (Exception e) {
          Utilities.log(me, aa + " " + e.getMessage(), constant.ERROR);

        }

      }

      pbar.finish();

      // clean up
      try {
        dbh.closeConnection();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jLabel1 = new javax.swing.JLabel();
    jScrollPane1 = new javax.swing.JScrollPane();
    txtAccNo = new javax.swing.JTextArea();
    jPanel1 = new javax.swing.JPanel();
    btnStop = new javax.swing.JButton();
    btnRemoveDup = new javax.swing.JButton();
    btnDropRC = new javax.swing.JButton();
    btnUnbilled = new javax.swing.JButton();
    btnUpdateBS = new javax.swing.JButton();
    btnStopSR = new javax.swing.JButton();
    btnResumeSR = new javax.swing.JButton();
    jScrollPane2 = new javax.swing.JScrollPane();
    txtOutput = new javax.swing.JTextArea();
    lblReady = new javax.swing.JLabel();

    org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(difTopComponent.class, "difTopComponent.jLabel1.text")); // NOI18N

    txtAccNo.setColumns(20);
    txtAccNo.setRows(5);
    jScrollPane1.setViewportView(txtAccNo);

    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(difTopComponent.class, "difTopComponent.jPanel1.border.title"))); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(btnStop, org.openide.util.NbBundle.getMessage(difTopComponent.class, "difTopComponent.btnStop.text")); // NOI18N
    btnStop.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnStopActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(btnRemoveDup, org.openide.util.NbBundle.getMessage(difTopComponent.class, "difTopComponent.btnRemoveDup.text")); // NOI18N
    btnRemoveDup.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnRemoveDupActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(btnDropRC, org.openide.util.NbBundle.getMessage(difTopComponent.class, "difTopComponent.btnDropRC.text")); // NOI18N
    btnDropRC.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnDropRCActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(btnUnbilled, org.openide.util.NbBundle.getMessage(difTopComponent.class, "difTopComponent.btnUnbilled.text")); // NOI18N
    btnUnbilled.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnUnbilledActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(btnUpdateBS, org.openide.util.NbBundle.getMessage(difTopComponent.class, "difTopComponent.btnUpdateBS.text")); // NOI18N
    btnUpdateBS.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnUpdateBSActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(btnStopSR, org.openide.util.NbBundle.getMessage(difTopComponent.class, "difTopComponent.btnStopSR.text")); // NOI18N
    btnStopSR.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnStopSRActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(btnResumeSR, org.openide.util.NbBundle.getMessage(difTopComponent.class, "difTopComponent.btnResumeSR.text")); // NOI18N
    btnResumeSR.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnResumeSRActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(btnRemoveDup)
          .addComponent(btnStop))
        .addGap(0, 0, Short.MAX_VALUE))
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
          .addComponent(btnDropRC, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btnUnbilled, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btnStopSR, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btnUpdateBS, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btnResumeSR, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
        .addComponent(btnDropRC)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btnUnbilled)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btnUpdateBS)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btnStopSR)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btnResumeSR)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 104, Short.MAX_VALUE)
        .addComponent(btnRemoveDup)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btnStop))
    );

    txtOutput.setColumns(20);
    txtOutput.setRows(5);
    jScrollPane2.setViewportView(txtOutput);

    org.openide.awt.Mnemonics.setLocalizedText(lblReady, org.openide.util.NbBundle.getMessage(difTopComponent.class, "difTopComponent.lblReady.text")); // NOI18N

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel1))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(lblReady)
            .addGap(0, 0, Short.MAX_VALUE))
          .addGroup(layout.createSequentialGroup()
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel1)
          .addComponent(lblReady))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addComponent(jScrollPane1)
          .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(jScrollPane2))
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void btnDropRCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDropRCActionPerformed
    // TODO add your handling code here:
    removeDuplicate();
    Utilities.log(me, "mode 0", constant.DEBUG);
    mode = 0;

    DropRCWorker rcw = new DropRCWorker();
    rcw.addPropertyChangeListener(this);
    rcw.execute();
  }//GEN-LAST:event_btnDropRCActionPerformed

  private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
    // TODO add your handling code here:
    runstate = false;
  }//GEN-LAST:event_btnStopActionPerformed

  private void btnRemoveDupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveDupActionPerformed
    // TODO add your handling code here:
    removeDuplicate();
  }//GEN-LAST:event_btnRemoveDupActionPerformed

  private void btnUnbilledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUnbilledActionPerformed
    // TODO add your handling code here:
    removeDuplicate();
    Utilities.log(me, "mode 1", constant.DEBUG);
    mode = 1;

    DropRCWorker rcw = new DropRCWorker();
    rcw.addPropertyChangeListener(this);
    rcw.execute();
  }//GEN-LAST:event_btnUnbilledActionPerformed

  private void btnUpdateBSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateBSActionPerformed
    // TODO add your handling code here:
    
    mode = 2;

    patchWorker rcw = new patchWorker();
    rcw.addPropertyChangeListener(this);
    rcw.execute();
    
  }//GEN-LAST:event_btnUpdateBSActionPerformed

  private void btnStopSRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopSRActionPerformed
    // TODO add your handling code here:
    mode = 3;

    patchWorker rcw = new patchWorker();
    rcw.addPropertyChangeListener(this);
    rcw.execute();
  }//GEN-LAST:event_btnStopSRActionPerformed

  private void btnResumeSRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResumeSRActionPerformed
    // TODO add your handling code here:
    mode = 4;

    patchWorker rcw = new patchWorker();
    rcw.addPropertyChangeListener(this);
    rcw.execute();
  }//GEN-LAST:event_btnResumeSRActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton btnDropRC;
  private javax.swing.JButton btnRemoveDup;
  private javax.swing.JButton btnResumeSR;
  private javax.swing.JButton btnStop;
  private javax.swing.JButton btnStopSR;
  private javax.swing.JButton btnUnbilled;
  private javax.swing.JButton btnUpdateBS;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JLabel lblReady;
  private javax.swing.JTextArea txtAccNo;
  private javax.swing.JTextArea txtOutput;
  // End of variables declaration//GEN-END:variables

  // custom vars
  private String me = "Data Integrity Fix";
  private ArrayList<String> accno;
  private boolean runstate;
  private int mode;
  private DefaultCaret caret;

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

  // custom functions
  private void removeDuplicate() {
    Utilities.log(me, "Removing duplicate", constant.DEBUG);
    Scanner sc = new Scanner(txtAccNo.getText());

    accno = new ArrayList<String>();

    // remove duplicate
    while (sc.hasNextLine()) {
      String data = sc.nextLine().trim();

      if (data.isEmpty()) {
        continue;
      }

      if (data.toLowerCase().contains("acc")) {
        continue;
      }

      if (!accno.contains(data)) {
        accno.add(data);
      }

    }

    // add back to the GUI
    String output = "";

    for (String aa : accno) {
      output += aa + constant.LINE_SEPARATOR;
    }

    txtAccNo.setText(output);

    lblReady.setText("Total record to process: " + accno.size());

  }

  private void disableBtn() {
    btnStop.setEnabled(true);
    btnDropRC.setEnabled(false);
    btnRemoveDup.setEnabled(false);
    btnUnbilled.setEnabled(false);
    txtAccNo.setEditable(false);
    btnUpdateBS.setEnabled(false);
    btnStopSR.setEnabled(false);
    btnResumeSR.setEnabled(false);
  }

  private void enableBtn() {
    btnStop.setEnabled(false);
    btnDropRC.setEnabled(true);
    btnRemoveDup.setEnabled(true);
    btnUnbilled.setEnabled(true);
    txtAccNo.setEditable(true);
    btnUpdateBS.setEnabled(true);
    btnStopSR.setEnabled(true);
    btnResumeSR.setEnabled(true);
  }

  private void customInit() {

    caret = (DefaultCaret) txtOutput.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

  }

}
