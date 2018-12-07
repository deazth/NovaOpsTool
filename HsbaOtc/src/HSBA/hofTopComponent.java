/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HSBA;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import ops.com.Utilities;
import ops.com.constant;
import ops.com.dbHandler;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.SwingWorker;
import javax.swing.text.DefaultCaret;
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
        dtd = "-//HSBA//hof//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "hofTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "HSBA.hofTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_hofAction",
        preferredID = "hofTopComponent"
)
@Messages({
  "CTL_hofAction=HSBA OTC Fix",
  "CTL_hofTopComponent=HSBA OTC Fix Window",
  "HINT_hofTopComponent=This is a hof window"
})
public final class hofTopComponent extends TopComponent implements PropertyChangeListener {

  @Override
  public void propertyChange(PropertyChangeEvent evt) {

  }

  class bgWorker extends SwingWorker<Void, Void> {

    @Override
    protected Void doInBackground() {

      disableBtn();
      allowRunningState = true;

      if (mode.equals("getOSMID")) {
        try {
          searchOrderFromAcc();
        } catch (Exception e) {
          Utilities.log(me, "Error while searching for OSM order ID", constant.ERROR);
          Utilities.logStack(me, e);
        }

      } else if (mode.equals("processOSMID")) {
        try {
          bootUpOSMWorker();
          loadOSMToJob();
          shutDownOSMWorker();
        } catch (Exception e) {
          Utilities.popup(e.toString());
          Utilities.logStack(me, e);
        }

      } else if (mode.equals("patchOTC")) {
        patchOTC();
      }

      enableBtn();
      closeConnections();
      return null;
    }

  }

  class OSMWorker implements Runnable {

    private PreparedStatement psEaiGetEaiID;
    private PreparedStatement psEaiGetReq;
    private PreparedStatement psEaiGetResp;
    private PreparedStatement psEaiGetOSMID;
    private PreparedStatement psEaiCheckStatus;

    String event_poid;
    String event_descr = null;
    String eai_descr;
    String bill_poid = null;
    String accno;
    String otc_date;
    String orderdate;

    private PreparedStatement psBrmGetEvent;

    public OSMWorker() throws SQLException {

      ohsemqueue = new ConcurrentLinkedQueue<String>();

      connectEAI();
      connectApps();

      psEaiGetEaiID = dbhEAI.createPS("Select \n"
              + " a.Int_Msg_Id\n"
              + " From Wliprd_Custom.Eai_Audit_Log A left join Wliprd_Custom.Eai_Error_Log B\n"
              + "    on B.Integration_Msg_Id In A.Int_Msg_Id\n"
              + "  Where A.Ext_Msg_Id = ? \n"
              + "  And A.Event_Name in ('BillingActivate')\n"
              + " Order By Ext_Msg_Id desc, Audit_Date_Time desc");

      psEaiGetOSMID = dbhEAI.createPS("select  b.ext_msg_id as OSM_ID\n"
              + "from (\n"
              + " select * from wliprd_custom.EAI_AUDIT_LOG where event_name='OrderCreate' and AUDIT_TYPE='RQI') a left outer join \n"
              + " (select * from wliprd_custom.EAI_AUDIT_LOG where EVENT_NAME='OrderCreate'\n"
              + " and AUDIT_TYPE in ('WSResponse', 'ERROR')) b on a.int_msg_id = b.int_msg_id\n"
              + " where a.EXT_MSG_ID = ? and b.ext_msg_id is not null");

      psEaiGetReq = dbhEAI.createPS("Select payload, audit_date_time From Wliprd_Custom.Eai_Audit_Log Where Int_Msg_Id = ? \n"
              + " and audit_type = 'WSRequest' and audit_param_3 = '40009' \n"
              + " Order By Audit_Date_Time Desc");

      psEaiGetResp = dbhEAI.createPS("Select payload From Wliprd_Custom.Eai_Audit_Log Where Int_Msg_Id = ? \n"
              + " and audit_type = 'WSResponse' \n"
              + " Order By Audit_Date_Time Desc");

      psEaiCheckStatus = dbhEAI.createPS("Select * From Wliprd_Custom.Eai_Audit_Log "
              + " Where Int_Msg_Id = ? and audit_type = 'JMSRequest'"
              + " and audit_param_3 = 'Success' ");

      psBrmGetEvent = dbhBRM.createPS("select te.sys_descr, ti.bill_obj_id0, ta.account_no, unix_ora_ts_conv(te.created_t) \n"
              + " from pin.event_t te, pin.item_t ti, pin.account_t ta \n"
              + " where te.poid_Id0 = ? \n"
              + " and te.item_obj_id0 = ti.poid_id0 "
              + " and ti.account_obj_id0 = ta.poid_Id0");

    }

    private void processOsmID(String passed_id) throws SQLException {
      event_poid = "";
      event_descr = "";
      eai_descr = "";
      bill_poid = "";
      accno = "";
      otc_date = "";
      orderdate = "";
      boolean do_listing = false;
      Utilities.log(me, "OSM Slave processing - " + passed_id, 3);

      String osmid;
      String eaiid = "";

      if (passed_id.contains("-")) {
        psEaiGetOSMID.setString(1, passed_id);

        ResultSet rs = psEaiGetOSMID.executeQuery();
        if (rs.next()) {
          osmid = dbHandler.dbGetString(rs, 1);
          Utilities.log(me, "OSM ID for order# " + passed_id + " is " + osmid, 3);
        } else {
          Utilities.log(me, "OSM ID not found for Order " + passed_id, 1);
          return;
        }

      } else {
        osmid = passed_id;
      }

      // get the EAI ID
//      Utilities.log(me, "Getting EAI ID", 0);
      psEaiGetEaiID.setString(1, osmid);
      ResultSet rs = psEaiGetEaiID.executeQuery();
      while (rs.next()) {
        eaiid = dbHandler.dbGetString(rs, 1);

//        Utilities.log(me, "Getting EAI DESC", 0);
        eai_descr = getEaiDesc(eaiid, do_listing);
        
        Utilities.log(me, osmid + "  eai desc> " + eai_descr, 3);

        if (!eai_descr.isEmpty() && !do_listing) {
          break;
        }
      }

      if (eaiid.isEmpty()) {
        Utilities.log(me, "EAI not found for OSM ID: " + osmid, 1);
        return;
      }

      // check whether or not this order is a success order
      psEaiCheckStatus.setString(1, eaiid);
      rs = psEaiCheckStatus.executeQuery();
      if (!rs.next()) {
        Utilities.log(me, "exception EAI ID " + eaiid + " - " + eai_descr, 1);
        String oo = passed_id + " Exception: " + eai_descr + constant.LINE_SEPARATOR;
//        sb.append(oo);
        return;
      } else {
        Utilities.log(me, eaiid + "  success ", 3);
      }

      if (eai_descr.isEmpty()) {
        Utilities.log(me, "EAI ID not found for OSM ID " + osmid, 1);
        return;
      }

//      Utilities.log(me, "Getting EAI Event " + eaiid, 0);
      event_poid = getEventFromEai(eaiid);

//      Utilities.log(me, "Getting BRM event " + event_poid, 0);
      getBRMDesc();

//      if(event_descr.trim().equals("MAXIS - BTU Port Modify Bandwidth")){
//        return;
//      }
      if (!event_descr.equals(eai_descr)) {
        //String oput = event_descr + "@" + bill_poid + "@" + eai_descr + "@" + event_poid + constant.LINE_SEPARATOR;

        boolean tick = true;

        if (event_descr.length() > 5) {
          if (event_descr.substring(0, 4).equals(eai_descr.substring(0, 4))) {
            if (event_descr.substring(event_descr.length() - 4).equals(eai_descr.substring(eai_descr.length() - 4))) {
              tick = false;
            }
          }
        }

        otctable.add(bill_poid, event_poid, eai_descr, event_descr, otc_date, orderdate, tick);
        otctable.refire();
      } else {
        Utilities.log(me, passed_id + "  same desc between eai and brm " + eaiid, 3);
        otctable.add(bill_poid, event_poid, eai_descr, event_descr, otc_date, orderdate, false);
        otctable.refire();
      }

    }

    private void getBRMDesc() throws SQLException {
      String evd = "";
      psBrmGetEvent.setString(1, event_poid);
      ResultSet rs = psBrmGetEvent.executeQuery();
      if (rs.next()) {
//        Utilities.log(me, "got data Getting BRM Event " + event_poid, 0);
        event_descr = dbHandler.dbGetString(rs, 1).trim();
        bill_poid = dbHandler.dbGetString(rs, 2);
        accno = dbHandler.dbGetString(rs, 3);
        otc_date = dbHandler.dbGetString(rs, 4);
      }

      rs.close();

    }

    private String getEaiDesc(String eaiid, boolean do_listing) throws SQLException {
      String event_poid = "";
      String date = "";
// get wsreq payload
      psEaiGetReq.setString(1, eaiid);

      ResultSet rs = psEaiGetReq.executeQuery();

      if (rs.next()) {
        Clob b = rs.getClob(1);
        orderdate = rs.getString(2);

        if (b != null) {
          Scanner sc = new Scanner(b.getAsciiStream());

          while (sc.hasNextLine()) {
            String data = sc.nextLine();

            if (data.contains("</DESCR>")) {
              event_poid = getFullPoid(data);
//              break;
            } else if(data.contains("</SERVICE_OBJ>")){
              Utilities.log(me, eaiid + " target service: " + getFullPoid(data), 0);
            }
          }

          sc.close();

        }
      } else {

      }
      rs.close();

      if (do_listing && !event_poid.isEmpty()
              && !event_poid.equals("HSBA Port Contract Product Purchase")
              && !event_poid.equals("MAXIS - BTU Port Modify Bandwidth")) {
        String temp = date + " - " + event_poid + constant.LINE_SEPARATOR;
//        sb.append(temp);
      }

      return event_poid.trim();
    }

    private String getEventFromEai(String eaiid) throws SQLException {

      String event_poid = "";
// get wsreq payload
      psEaiGetResp.setString(1, eaiid);

      ResultSet rs = psEaiGetResp.executeQuery();

      if (rs.next()) {
//        Utilities.log(me, "got data Getting EAI Event " + eaiid, 0);
        Clob b = rs.getClob(1);

        if (b != null) {
          Scanner sc = new Scanner(b.getAsciiStream());

          while (sc.hasNextLine()) {
            String data = sc.nextLine();
//            System.out.println(data);

            if (data.contains("/event/billing/product/fee/purchase")) {
              event_poid = getPoid(data);
              break;
            }
          }

          sc.close();

        }
      }
      rs.close();

      return event_poid;
    }

    @Override
    public void run() {

      while (true) {

        if (!allowRunningState) {
          break;
        }

        String data_to_process = ohsemqueue.poll();

        if (data_to_process == null) {
          if (gotAllAccount) {
            break;
          } else {
            try {
              Thread.sleep(2000);
              continue;
            } catch (Exception e) {
            }
          }

        }

        if (gotAllAccount) {
          osmpbar.progress(data_to_process, processedCount);
        } else {
          osmpbar.progress(data_to_process);
        }
        processedCount++;

        // ok got data to process
        try {
          processOsmID(data_to_process);
        } catch (SQLException e) {
          Utilities.log(me, "error processing OSM ID " + data_to_process, 1);
          Utilities.logStack(me, e);
        }

      }

    }

  }

  public hofTopComponent() {
    initComponents();
    customInit();
    setName(Bundle.CTL_hofTopComponent());
    setToolTipText(Bundle.HINT_hofTopComponent());

  }

  private void customInit() {
    caret = (DefaultCaret) txtOsmInput.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

    btnStop.setEnabled(false);

    jdtToDate.setEnabled(false);

  }

  private void enableBtn() {
    btnStop.setEnabled(false);
    btnAnalyse.setEnabled(true);
    btnFind.setEnabled(true);
    btnFix.setEnabled(true);

    jdtFromDate.setEnabled(true);
    jdtToDate.setEnabled(chkToDate.isSelected());

    chkAnalyseNow.setEnabled(true);
    chkToDate.setEnabled(true);
    jTable1.setEnabled(true);

  }

  private void disableBtn() {
    btnStop.setEnabled(true);
    btnAnalyse.setEnabled(false);
    btnFind.setEnabled(false);
    btnFix.setEnabled(false);

    jdtFromDate.setEnabled(false);
    jdtToDate.setEnabled(false);

    chkAnalyseNow.setEnabled(false);
    chkToDate.setEnabled(false);
    jTable1.setEnabled(false);

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
    txtAccInput = new javax.swing.JTextArea();
    jLabel3 = new javax.swing.JLabel();
    jScrollPane2 = new javax.swing.JScrollPane();
    txtOsmInput = new javax.swing.JTextArea();
    jPanel1 = new javax.swing.JPanel();
    btnFind = new javax.swing.JButton();
    btnAnalyse = new javax.swing.JButton();
    jdtFromDate = new org.jdesktop.swingx.JXDatePicker();
    jLabel5 = new javax.swing.JLabel();
    jdtToDate = new org.jdesktop.swingx.JXDatePicker();
    chkAnalyseNow = new javax.swing.JCheckBox();
    btnFix = new javax.swing.JButton();
    btnStop = new javax.swing.JButton();
    chkToDate = new javax.swing.JCheckBox();
    jScrollPane3 = new javax.swing.JScrollPane();
    txtBillOutput = new javax.swing.JTextArea();
    jLabel7 = new javax.swing.JLabel();
    jScrollPane4 = new javax.swing.JScrollPane();
    jTable1 = new javax.swing.JTable();
    lblRecordCount = new javax.swing.JLabel();

    org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(hofTopComponent.class, "hofTopComponent.jLabel1.text")); // NOI18N

    txtAccInput.setColumns(20);
    txtAccInput.setRows(5);
    jScrollPane1.setViewportView(txtAccInput);

    org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(hofTopComponent.class, "hofTopComponent.jLabel3.text")); // NOI18N

    txtOsmInput.setColumns(20);
    txtOsmInput.setRows(5);
    jScrollPane2.setViewportView(txtOsmInput);

    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(hofTopComponent.class, "hofTopComponent.jPanel1.border.title"))); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(btnFind, org.openide.util.NbBundle.getMessage(hofTopComponent.class, "hofTopComponent.btnFind.text")); // NOI18N
    btnFind.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnFindActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(btnAnalyse, org.openide.util.NbBundle.getMessage(hofTopComponent.class, "hofTopComponent.btnAnalyse.text")); // NOI18N
    btnAnalyse.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnAnalyseActionPerformed(evt);
      }
    });

    jdtFromDate.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jdtFromDateActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(hofTopComponent.class, "hofTopComponent.jLabel5.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(chkAnalyseNow, org.openide.util.NbBundle.getMessage(hofTopComponent.class, "hofTopComponent.chkAnalyseNow.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(btnFix, org.openide.util.NbBundle.getMessage(hofTopComponent.class, "hofTopComponent.btnFix.text")); // NOI18N
    btnFix.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnFixActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(btnStop, org.openide.util.NbBundle.getMessage(hofTopComponent.class, "hofTopComponent.btnStop.text")); // NOI18N
    btnStop.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnStopActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(chkToDate, org.openide.util.NbBundle.getMessage(hofTopComponent.class, "hofTopComponent.chkToDate.text")); // NOI18N
    chkToDate.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        chkToDateActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addComponent(btnAnalyse)
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addComponent(btnFix)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(btnStop))
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(jPanel1Layout.createSequentialGroup()
            .addComponent(btnFind)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(chkAnalyseNow))
          .addGroup(jPanel1Layout.createSequentialGroup()
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jLabel5)
              .addComponent(chkToDate))
            .addGap(18, 18, 18)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jdtFromDate, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(jdtToDate, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE))))
        .addGap(0, 0, Short.MAX_VALUE))
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jdtFromDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel5))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jdtToDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(chkToDate))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(btnFind)
          .addComponent(chkAnalyseNow))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btnAnalyse)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(btnFix)
          .addComponent(btnStop)))
    );

    txtBillOutput.setColumns(20);
    txtBillOutput.setRows(5);
    jScrollPane3.setViewportView(txtBillOutput);

    org.openide.awt.Mnemonics.setLocalizedText(jLabel7, org.openide.util.NbBundle.getMessage(hofTopComponent.class, "hofTopComponent.jLabel7.text")); // NOI18N

    jTable1.setModel(otctable);
    jScrollPane4.setViewportView(jTable1);

    org.openide.awt.Mnemonics.setLocalizedText(lblRecordCount, org.openide.util.NbBundle.getMessage(hofTopComponent.class, "hofTopComponent.lblRecordCount.text")); // NOI18N

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
          .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel1)
            .addGap(75, 75, 75)
            .addComponent(jLabel3)))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(lblRecordCount)
            .addGap(0, 0, Short.MAX_VALUE))
          .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 465, Short.MAX_VALUE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel7))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel1)
          .addComponent(jLabel3)
          .addComponent(jLabel7)
          .addComponent(lblRecordCount))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jScrollPane1)
              .addComponent(jScrollPane2))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
          .addComponent(jScrollPane3)
          .addComponent(jScrollPane4))
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void jdtFromDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jdtFromDateActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_jdtFromDateActionPerformed

  private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
    // TODO add your handling code here:
    allowRunningState = false;
  }//GEN-LAST:event_btnStopActionPerformed

  private void chkToDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkToDateActionPerformed
    // TODO add your handling code here:

    jdtToDate.setEnabled(chkToDate.isSelected());

  }//GEN-LAST:event_chkToDateActionPerformed

  private void btnFindActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFindActionPerformed
    // TODO add your handling code here:

    mode = "getOSMID";

    bgWorker bw = new bgWorker();
    bw.addPropertyChangeListener(this);
    bw.execute();

  }//GEN-LAST:event_btnFindActionPerformed

  private void btnAnalyseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAnalyseActionPerformed
    // TODO add your handling code here:
    mode = "processOSMID";

    bgWorker bw = new bgWorker();
    bw.addPropertyChangeListener(this);
    bw.execute();
  }//GEN-LAST:event_btnAnalyseActionPerformed

  private void btnFixActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFixActionPerformed
    // TODO add your handling code here:
    mode = "patchOTC";

    bgWorker bw = new bgWorker();
    bw.addPropertyChangeListener(this);
    bw.execute();
  }//GEN-LAST:event_btnFixActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton btnAnalyse;
  private javax.swing.JButton btnFind;
  private javax.swing.JButton btnFix;
  private javax.swing.JButton btnStop;
  private javax.swing.JCheckBox chkAnalyseNow;
  private javax.swing.JCheckBox chkToDate;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JLabel jLabel7;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JScrollPane jScrollPane3;
  private javax.swing.JScrollPane jScrollPane4;
  private javax.swing.JTable jTable1;
  private org.jdesktop.swingx.JXDatePicker jdtFromDate;
  private org.jdesktop.swingx.JXDatePicker jdtToDate;
  private javax.swing.JLabel lblRecordCount;
  private javax.swing.JTextArea txtAccInput;
  private javax.swing.JTextArea txtBillOutput;
  private javax.swing.JTextArea txtOsmInput;
  // End of variables declaration//GEN-END:variables

  // custom vars
  FixOtcTableModel otctable = new FixOtcTableModel();
  ArrayList<String> output = new ArrayList<String>();
  ArrayList<String> accnos = new ArrayList<String>();
  private boolean allowRunningState;
  DefaultCaret caret;
  String mode;
  String me = "HSBA";

  dbHandler dbhBRM;
  dbHandler dbhEAI;
  dbHandler dbhSBL;

  // vars to handle OSM ID processing
  boolean gotAllAccount;
  int processedCount;
  ProgressHandle osmpbar;
  ArrayList<String> ohsem_id = new ArrayList<String>();
  ConcurrentLinkedQueue<String> ohsemqueue;
  Thread theActualThread;

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

  // functions
  // <editor-fold defaultstate="collapsed" desc="connections">  
  void connectApps() throws SQLException {
    dbhBRM = new dbHandler("apps");
    dbhBRM.setDBConnInfo(constant.dbConApps);
    dbhBRM.setUserPass(constant.dbConUser, constant.dbConPass);
    dbhBRM.openConnection();
  }

  void connectEAI() throws SQLException {
    dbhEAI = new dbHandler("EAI");
    dbhEAI.setDBConnInfo(constant.dbConEAI);
    dbhEAI.setUserPass(constant.dbEAIUser, constant.dbEAIPass);
    dbhEAI.openConnection();
  }

  void connectSBL() throws SQLException {
    dbhSBL = new dbHandler("SBL");
    dbhSBL.setDBConnInfo(constant.dbConSBL);
    dbhSBL.setUserPass(constant.dbSBLUser, constant.dbSBLPass);
    dbhSBL.openConnection();
  }

  void closeConnections() {
    try {
      if (dbhBRM != null) {
        dbhBRM.closeConnection();
      }
      if (dbhSBL != null) {
        dbhSBL.closeConnection();
      }

      if (dbhEAI != null) {
        dbhEAI.closeConnection();
      }

    } catch (Exception e) {
      Utilities.logStack(me, e);
    }

  }
  //</editor-fold>

  private void removeDuplicateAcc() {
    Utilities.log(me, "Removing duplicate", constant.DEBUG);
    Scanner sc = new Scanner(txtAccInput.getText());

    accnos = new ArrayList<String>();

    // remove duplicate
    while (sc.hasNextLine()) {
      String data = sc.nextLine().trim();

      if (data.isEmpty()) {
        continue;
      }

      if (data.toLowerCase().contains("acc")) {
        continue;
      }

      if (!accnos.contains(data)) {
        accnos.add(data);
      }

    }

    // add back to the GUI
    String outputtxt = "";

    for (String aa : accnos) {
      outputtxt += aa + constant.LINE_SEPARATOR;
    }

    txtAccInput.setText(outputtxt);

    lblRecordCount.setText("Total record to process: " + accnos.size());

  }

  private void removeDuplicateOSM() {
    Utilities.log(me, "Removing duplicate OSM", constant.DEBUG);
    Scanner sc = new Scanner(txtOsmInput.getText());

    ohsem_id = new ArrayList<String>();

    // remove duplicate
    while (sc.hasNextLine()) {
      String data = sc.nextLine().trim();

      if (data.isEmpty()) {
        continue;
      }

      if (data.toLowerCase().contains("acc")) {
        continue;
      }

      if (!ohsem_id.contains(data)) {
        ohsem_id.add(data);
      }

    }

    // add back to the GUI
    String outputtxt = "";

    for (String aa : ohsem_id) {
      outputtxt += aa + constant.LINE_SEPARATOR;
    }

    txtOsmInput.setText(outputtxt);

    lblRecordCount.setText("Total record to process: " + ohsem_id.size());

  }

  private void bootUpOSMWorker() throws SQLException {

    otctable.clearContent();
    gotAllAccount = false;
    processedCount = 0;
    theActualThread = new Thread(new OSMWorker());

    theActualThread.start();
    Utilities.log(me, "OSM Worker Booted", 0);
    osmpbar = ProgressHandleFactory.createHandle("Processing OSM IDs");
    osmpbar.start();

  }

  private void loadOSMToJob() {

    removeDuplicateOSM();

    if (ohsem_id.isEmpty()) {
      Utilities.popup("OSM ID input is empty");
    }

    for (String osid : ohsem_id) {
      ohsemqueue.add(osid);
    }
  }

  private void shutDownOSMWorker() {
    try {
      osmpbar.switchToDeterminate(ohsem_id.size());
      gotAllAccount = true;
      theActualThread.join();
      osmpbar.finish();
    } catch (Exception e) {
      Utilities.logStack(me, e);
    }

  }

  private void searchOrderFromAcc() throws SQLException {

    Utilities.log(me, "Searching for OSM ID", constant.DEBUG);
    removeDuplicateAcc();

    long start_date;
    try {
      start_date = jdtFromDate.getDate().getTime() / 1000;
    } catch (NullPointerException e) {
      Utilities.popup("From date is not set");
      return;
    }

    String sdatestr = Utilities.tsToDate(start_date, "dd-MM-yyyy");
    Utilities.log(me, "From date: " + sdatestr, constant.DEBUG);
    long end_date = System.currentTimeMillis() / 1000;
    StringBuilder sb = new StringBuilder();

    connectSBL();

    String to_date_condition = "";

    if (chkToDate.isSelected()) {
      try {
        end_date = jdtToDate.getDate().getTime() / 1000;
      } catch (NullPointerException e) {
        Utilities.popup("To date is not set");
        return;
      }

      String edatestr = Utilities.tsToDate(end_date, "dd-MM-yyyy");
      Utilities.log(me, "To date: " + edatestr, constant.DEBUG);
      to_date_condition = " and (a.created+1/3)  < to_date( '" + edatestr + "' ,'DD-MM-YYYY')\n";
    }

    if (end_date < start_date) {
      Utilities.popup("From date is either in the future, or more recent compared to end date");
      return;
    }

    if (end_date - start_date > 5184000) {
      Utilities.popup("Date range gap is too big. Set it to less than 2 month");
      return;
    }

    String getOrderSql = "SELECT \n"
            + " distinct\n"
            + " order_num\n"
            + " FROM siebel.s_order a, siebel.s_order_type b, siebel.s_order_item c, siebel.s_prod_int d, siebel.s_org_ext e,\n"
            + " siebel.s_order_x f,\n"
            + " siebel.s_order_item_x g\n"
            + " WHERE a.order_type_id = b.row_id\n"
            + " AND c.order_id = a.row_id\n"
            + " AND d.row_id = c.prod_id\n"
            + " AND e.par_row_id = a.bill_accnt_id\n"
            + " AND f.par_row_id = a.row_id\n"
            + " AND g.par_row_id = c.row_id\n"
            + " and (a.created+1/3) >= to_date( '" + sdatestr + "' ,'DD-MM-YYYY')\n"
            + to_date_condition
            + " and ou_num = ? \n"
            + " and x_service_inst_id is null";

    Utilities.log(me, "SQL: " + getOrderSql, constant.DEBUG);

    PreparedStatement psSblGetOrder = dbhSBL.createPS(getOrderSql);

    // display the progress bar
    final ProgressHandle pbar = ProgressHandleFactory.createHandle("Getting OSM IDs");
    pbar.start(accnos.size());

    // reset OSM id array
    ohsem_id = new ArrayList<String>();

    if (chkAnalyseNow.isSelected()) {
      bootUpOSMWorker();
    }

    int counter = 0;
    for (String bano : accnos) {
      if (!allowRunningState) {
        break;
      }

      Utilities.log(me, "Processing: " + bano, constant.DEBUG);
      pbar.progress(bano, counter++);
      psSblGetOrder.setString(1, bano);

      ResultSet rs = psSblGetOrder.executeQuery();
      while (rs.next()) {
        String osmid = dbHandler.dbGetString(rs, 1);

        if (chkAnalyseNow.isSelected()) {
          ohsemqueue.add(osmid);
        }

        ohsem_id.add(osmid);
//        Utilities.log(me, bano + " osmid: " + osmid, constant.DEBUG);
        sb.append(osmid);
        sb.append(constant.LINE_SEPARATOR);
      }

      txtOsmInput.setText(sb.toString());

    }

    pbar.finish();

    if (chkAnalyseNow.isSelected()) {
      shutDownOSMWorker();
    }

  }

  private void patchOTC() {
    if (otctable.getRowCount() == 0) {
      Utilities.popup("No data to patch");
      return;
    }

    ArrayList<String> billpoids = new ArrayList<String>();
    StringBuilder sb = new StringBuilder();
    PreparedStatement psBrmUpdate;
    try {
      connectApps();
      psBrmUpdate = dbhBRM.createPS("update event_t set sys_descr = ? where poid_id0 = ? ");
    } catch (SQLException e) {
      Utilities.popup(e.toString());
      Utilities.logStack(me, e);
      return;
    }

    final ProgressHandle pbar = ProgressHandleFactory.createHandle("Patching OTC Desc");
    pbar.start(otctable.getRowCount());

    for (int i = 0; i < otctable.getRowCount(); i++) {
      
      if(!allowRunningState){
        break;
      }
      
      if (otctable.getValueAt(i, 4).toString().equals("false")) {
        continue;
      }

      String eaidesc = otctable.getValueAt(i, 2).toString();
      String billpoid = otctable.getValueAt(i, 0).toString();
      String eventpoid = otctable.getValueAt(i, 1).toString();

      try {
        psBrmUpdate.setString(1, eaidesc);
        psBrmUpdate.setString(2, eventpoid);

        psBrmUpdate.executeUpdate();

      } catch (SQLException sqle) {
        Utilities.log(me, "error updating " + eventpoid + " - " + sqle.toString(), 1);
      }

      if (!billpoids.contains(billpoid)) {
        billpoids.add(billpoid);
        sb.append(billpoid);
        sb.append(constant.LINE_SEPARATOR);
        txtBillOutput.setText(sb.toString());
      }

      
    }

    txtBillOutput.setText(sb.toString());

    pbar.finish();

  }

  private synchronized void append(String data) {

  }

  private String getPoid(String xmlLine) {
    String right = xmlLine.substring(xmlLine.indexOf(">"));
    String poid = right.substring(1, right.indexOf("<"));

    return poid.split(" ")[2];

  }

  private String getFullPoid(String xmlLine) {
    String right = xmlLine.substring(xmlLine.indexOf(">"));
    return right.substring(1, right.indexOf("<"));
  }

}
