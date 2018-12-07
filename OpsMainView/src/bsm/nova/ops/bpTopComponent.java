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
        dtd = "-//bsm.nova.ops//bp//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "bpTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "bsm.nova.ops.bpTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_bpAction",
        preferredID = "bpTopComponent"
)
@Messages({
  "CTL_bpAction=Bulk Patcher",
  "CTL_bpTopComponent=Bulk Patcher",
  "HINT_bpTopComponent=This is a bp window"
})
public final class bpTopComponent extends TopComponent implements PropertyChangeListener {

  public bpTopComponent() {
    initComponents();
    customInit();
    setName(Bundle.CTL_bpTopComponent());
    setToolTipText(Bundle.HINT_bpTopComponent());

  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {

  }

  class bgWorker extends SwingWorker<Void, Void> {

    private PreparedStatement psUpdate;
    private PreparedStatement psUpdate2;
    private PreparedStatement psGet;
    private dbHandler dbh;
    private String newValue = "";
    private String modetext = "";
    ProgressHandle pbar;
    private int counter;

    private String irisNo = "";
    private String patchvalue = "";
    private PortalContext pcm;

    @Override
    protected Void doInBackground() {
      try {
        init();
        patch();
        cleanup();
      } catch (SQLException e) {
        Utilities.logStack(me, e);
      } catch (NullPointerException npe) {
        Utilities.popup(npe.getMessage());
        Utilities.logStack(me, npe);
      } catch (EBufException b) {
        Utilities.logStack(me, b);
      }

      return null;
    }

    private void cleanup() {
      enableBtn();
      try {
        dbh.closeConnection();
        pcm.close(true);
      } catch (Exception e) {
      }
    }

    private void init() throws SQLException, EBufException {
      removeDuplicate();
      dbh = new dbHandler("apps");

      try {
//        dbh.setDBConnInfo("jdbc:oracle:thin:@(DESCRIPTION =\n"
//                + "    (ADDRESS = (PROTOCOL = TCP)(HOST = 10.14.43.56)(PORT = 1521))\n"
//                + "    (CONNECT_DATA =\n"
//                + "       (SERVER = DEDICATED)\n"
//                + "       (SERVICE_NAME = HBRMUAT1)\n"
//                + "    )\n"
//                + "  )");
//        dbh.setUserPass(constant.dbConUser, "pin123");

        if (mode == 2) {
          PropertyConfigurator.configure(getClass().getClassLoader().getResource("../../config/log4j.properties"));
          pcm = PortalConnectionManager.getInstance().getConnection();
        }

        dbh.setDBConnInfo(constant.dbConApps);
        dbh.setUserPass(constant.dbConUser, constant.dbConPass);
        dbh.openConnection();

        String updateSql = "";
        String getOldSql = "";
        String updateSql2 = "";

        if (mode == 1 || mode == 3) {

          getOldSql = "select obj_id0, delivery_descr, delivery_prefer, mobile_number \n"
                  + " from tm_inv_profile_t ttip\n"
                  + "  , account_t ta\n"
                  + "  , profile_t tpro\n"
                  + " where\n"
                  + "  ta.account_no = ? \n"
                  + "  and ta.poid_Id0 = tpro.account_obj_id0\n"
                  + "  and tpro.poid_Id0 = ttip.obj_id0";

          if (mode == 1) {
            updateSql = "update tm_inv_profile_t set "
                    + (chkPMedia.isSelected() ? "delivery_prefer = 0," : "")
                    + " delivery_descr = ? "
                    + " where obj_id0 = ? ";

            updateSql2 = "update tm_inv_profile_t set "
                    + (chkPMedia.isSelected() ? "delivery_prefer = 0," : "")
                    + " delivery_descr = ? , mobile_number = ? "
                    + " where obj_id0 = ? ";
          } else if (mode == 3) {
            updateSql = "update tm_inv_profile_t set "
                    + (chkRemoveEmail.isSelected() ? "delivery_descr = ''," : "")
                    + " delivery_prefer = ? "
                    + " where obj_id0 = ? ";
          }

          modetext = "Patch Bill Stream";

        } else if (mode == 2) {

          getOldSql = "select\n"
                  + "    ta.poid_Id0, tbi.poid_id0, tbi.pay_type, tbi.actg_cycle_dom \n"
                  + " from\n"
                  + "    account_T ta\n"
                  + "    , billinfo_t tbi\n"
                  + " where\n"
                  + "    ta.poid_Id0 = tbi.account_obj_Id0\n"
                  + "    and ta.account_no = ? ";
          updateSql = "select actg_cycle_dom, future_bill_t from billinfo_t where poid_id0 = ? ";

          modetext = "Patch BP";
          irisNo = txtIrisNo.getText().trim();
//          int sel = cbNewBP.getSelectedIndex();
//          if(sel == -1){
//            throw new NullPointerException("BP not selected");
//          }
          patchvalue = (String) cbNewBP.getModel().getSelectedItem();
          if (irisNo.isEmpty()) {
            throw new NullPointerException("Iris No is empty");
          }

        }

        psGet = dbh.createPS(getOldSql);
        psUpdate = dbh.createPS(updateSql);
        if (mode == 1) {
          psUpdate2 = dbh.createPS(updateSql2);
        }

      } catch (SQLException e) {
        Utilities.popup(e.getMessage());
        throw e;
      } catch (EBufException eb) {
        Utilities.popup(eb.getMessage());
        throw eb;
      }

      disableBtn();

    }

    private String patchEmail(String ba, boolean overwrite) {
      String output;
      String acc = ba;
      String mobileno = "";
      PreparedStatement pstouse = psUpdate;
      if (mode == 1) {
        if (!ba.contains("|")) {

          output = "invalid input: " + ba + constant.LINE_SEPARATOR;

          return output;
        }
        String[] input = ba.split("\\|");
        acc = input[0];
        newValue = input[1];

        if (input.length == 3) {
          mobileno = input[2];
          pstouse = psUpdate2;

        }

      } else if (mode == 3) {
        newValue = "1";
      }

      pbar.progress(acc, counter++);

      try {
        psGet.setString(1, acc);

        ResultSet rs = psGet.executeQuery();

        if (rs.next()) {
          String poid = dbHandler.dbGetString(rs, 1);
          String oldval = dbHandler.dbGetString(rs, 2);
          String currmedia = dbHandler.dbGetString(rs, 3);
          String oldmobile = dbHandler.dbGetString(rs, 4);

          if (mode == 1) {
            if (overwrite || oldval.trim().isEmpty()) {
              pstouse.setString(1, newValue);

              if (mobileno.isEmpty()) {
                pstouse.setString(2, poid);
                int upcount = pstouse.executeUpdate();

                output = acc + " : " + oldval + " -> " + newValue + " (" + upcount + " record updated)" + constant.LINE_SEPARATOR;
              } else {
                pstouse.setString(2, mobileno);
                pstouse.setString(3, poid);
                int upcount = pstouse.executeUpdate();

                output = acc + " : " + oldval + "+" + oldmobile + " -> " + newValue + "+" + mobileno + " (" + upcount + " record updated)" + constant.LINE_SEPARATOR;
              }

            } else {
              output = acc + " : maintain old value -> " + oldval + constant.LINE_SEPARATOR;
            }
          } else if (mode == 3) {
            pstouse.setString(1, newValue);
            pstouse.setString(2, poid);
            int upcount = pstouse.executeUpdate();

            output = acc + "|" + currmedia + "|" + oldval + "|" + poid
                    + "|" + upcount + " record updated)" + constant.LINE_SEPARATOR;
          } else {
            output = "";
          }

        } else {
          output = acc + " not found" + constant.LINE_SEPARATOR;
        }

      } catch (SQLException e) {
        output = acc + " error: " + e.toString() + constant.LINE_SEPARATOR;
        Utilities.log(me, "error update " + acc, constant.ERROR);
        Utilities.logStack(me, e);
      }

      return output;

    }

    private String patchBP(String ba) {
      String output = "";

      if (ba.trim().isEmpty()) {
        return "";
      }

      pbar.progress(ba, counter++);

      try {
        psGet.setString(1, ba);

        ResultSet rs = psGet.executeQuery();
        if (rs.next()) {
          String accpoid = dbHandler.dbGetString(rs, 1);
          String tbipoid = dbHandler.dbGetString(rs, 2);
          String paytype = dbHandler.dbGetString(rs, 3);
          String oldbp = dbHandler.dbGetString(rs, 4);

          // build the flist
          String flist_str
                  = "0 PIN_FLD_ACCOUNT_OBJ               POID [0] 0.0.0.1 /account " + accpoid + " 0\n"
                  + "0 PIN_FLD_POID                      POID [0] 0.0.0.1 /account " + accpoid + " 0\n"
                  + "0 PIN_FLD_BILLINFO                 ARRAY [0] allocated 5, used 5\n"
                  + "1     PIN_FLD_ACTG_FUTURE_DOM        INT [0] " + patchvalue + "\n"
                  + "1     PIN_FLD_POID                  POID [0] 0.0.0.1 /billinfo " + tbipoid + " 0\n"
                  + "1     PIN_FLD_PAY_TYPE              ENUM [0] " + paytype + "\n"
                  + "0 PIN_FLD_PROGRAM_NAME               STR [0] \"opstool_" + irisNo + "\"";

          FList inf = FList.createFromString(flist_str);

          FList out = pcm.opcode(PortalOp.CUST_UPDATE_CUSTOMER, inf);

          psUpdate.setString(1, tbipoid);
          rs = psUpdate.executeQuery();
          if (rs.next()) {
            String newbp = dbHandler.dbGetString(rs, 1);
            String futuredate = Utilities.tsToDate(rs.getLong(2), "dd/MM/yyyy");

            output = "BA#" + ba + " changed from " + oldbp + " to " + newbp + ". Future bill date: " + futuredate;

          } else {
            output = "BA#" + ba + " testnap completed without error. But no search result for billinfo " + tbipoid;
          }

        } else {
          output = "BA#" + ba + " not found";
        }

      } catch (EBufException | SQLException e) {
        output = "Error BA#" + ba + " : " + e.getMessage();
        Utilities.logStack(me, e);
      }

      return output + constant.LINE_SEPARATOR;
    }

    private void patch() {
      StringBuilder sb = new StringBuilder();
      String output = "";
      boolean overwrite = chkOverwriteEmail.isSelected();
      pbar = ProgressHandleFactory.createHandle(modetext);
      pbar.start(accno.size());
      counter = 0;

      if (mode == 1) {
        output = "Bulk email patch";
      } else if (mode == 2) {
        output = "Change BP\n"
                + "Iris no: " + irisNo + "\n"
                + "Target BP: " + patchvalue + "\n\n";
      } else if (mode == 3) {
        output = "Bulk patch bill media to paper";
      }

      sb.append(output + constant.LINE_SEPARATOR);
      txtOutput.setText(sb.toString());

      for (String ba : accno) {

        if (!runstate) {
          sb.append("Stopped\n");
          txtOutput.setText(sb.toString());
          break;
        }

        if (mode == 1 || mode == 3) {
          sb.append(patchEmail(ba, overwrite));
        } else if (mode == 2) {
          sb.append(patchBP(ba));
        }

        txtOutput.setText(sb.toString());
      }

      sb.append("Finished\n");
      txtOutput.setText(sb.toString());

      pbar.finish();
    }

  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jLabel2 = new javax.swing.JLabel();
    jPanel1 = new javax.swing.JPanel();
    btnPEmail = new javax.swing.JButton();
    chkPMedia = new javax.swing.JCheckBox();
    chkOverwriteEmail = new javax.swing.JCheckBox();
    jSplitPane1 = new javax.swing.JSplitPane();
    jPanel2 = new javax.swing.JPanel();
    jLabel1 = new javax.swing.JLabel();
    jScrollPane1 = new javax.swing.JScrollPane();
    txtAccNo = new javax.swing.JTextArea();
    jPanel3 = new javax.swing.JPanel();
    lblReady = new javax.swing.JLabel();
    jScrollPane2 = new javax.swing.JScrollPane();
    txtOutput = new javax.swing.JTextArea();
    btnStop = new javax.swing.JButton();
    jPanel4 = new javax.swing.JPanel();
    btnChangeBP = new javax.swing.JButton();
    jLabel3 = new javax.swing.JLabel();
    txtIrisNo = new javax.swing.JTextField();
    jLabel4 = new javax.swing.JLabel();
    cbNewBP = new javax.swing.JComboBox<>();
    jPanel5 = new javax.swing.JPanel();
    btnToPaper = new javax.swing.JButton();
    chkRemoveEmail = new javax.swing.JCheckBox();

    org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.jLabel2.text")); // NOI18N

    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.jPanel1.border.title"))); // NOI18N
    jPanel1.setToolTipText(org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.jPanel1.toolTipText")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(btnPEmail, org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.btnPEmail.text")); // NOI18N
    btnPEmail.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnPEmailActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(chkPMedia, org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.chkPMedia.text")); // NOI18N
    chkPMedia.setToolTipText(org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.chkPMedia.toolTipText")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(chkOverwriteEmail, org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.chkOverwriteEmail.text")); // NOI18N
    chkOverwriteEmail.setToolTipText(org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.chkOverwriteEmail.toolTipText")); // NOI18N

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(btnPEmail)
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(chkOverwriteEmail)
          .addComponent(chkPMedia))
        .addContainerGap())
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addComponent(btnPEmail)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(chkPMedia)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addComponent(chkOverwriteEmail))
    );

    org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.jLabel1.text")); // NOI18N

    txtAccNo.setColumns(20);
    txtAccNo.setRows(5);
    jScrollPane1.setViewportView(txtAccNo);

    javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
    jPanel2.setLayout(jPanel2Layout);
    jPanel2Layout.setHorizontalGroup(
      jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel2Layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
          .addGroup(jPanel2Layout.createSequentialGroup()
            .addComponent(jLabel1)
            .addGap(0, 0, Short.MAX_VALUE)))
        .addContainerGap())
    );
    jPanel2Layout.setVerticalGroup(
      jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel2Layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jLabel1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
        .addContainerGap())
    );

    jSplitPane1.setLeftComponent(jPanel2);

    org.openide.awt.Mnemonics.setLocalizedText(lblReady, org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.lblReady.text")); // NOI18N

    txtOutput.setColumns(20);
    txtOutput.setRows(5);
    jScrollPane2.setViewportView(txtOutput);

    javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
    jPanel3.setLayout(jPanel3Layout);
    jPanel3Layout.setHorizontalGroup(
      jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel3Layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE)
          .addGroup(jPanel3Layout.createSequentialGroup()
            .addComponent(lblReady)
            .addGap(0, 0, Short.MAX_VALUE)))
        .addContainerGap())
    );
    jPanel3Layout.setVerticalGroup(
      jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel3Layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(lblReady)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
        .addContainerGap())
    );

    jSplitPane1.setRightComponent(jPanel3);

    org.openide.awt.Mnemonics.setLocalizedText(btnStop, org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.btnStop.text")); // NOI18N
    btnStop.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnStopActionPerformed(evt);
      }
    });

    jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.jPanel4.border.title"))); // NOI18N
    jPanel4.setToolTipText(org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.jPanel4.toolTipText")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(btnChangeBP, org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.btnChangeBP.text")); // NOI18N
    btnChangeBP.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnChangeBPActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.jLabel3.text")); // NOI18N

    txtIrisNo.setText(org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.txtIrisNo.text")); // NOI18N
    txtIrisNo.setToolTipText(org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.txtIrisNo.toolTipText")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.jLabel4.text")); // NOI18N

    cbNewBP.setMaximumRowCount(5);
    cbNewBP.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "4", "7", "10", "13", "16", "19", "22", "25", "28" }));

    javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
    jPanel4.setLayout(jPanel4Layout);
    jPanel4Layout.setHorizontalGroup(
      jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel4Layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
            .addGap(0, 0, Short.MAX_VALUE)
            .addComponent(btnChangeBP))
          .addGroup(jPanel4Layout.createSequentialGroup()
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jLabel3)
              .addComponent(jLabel4))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(txtIrisNo, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(cbNewBP, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        .addContainerGap())
    );
    jPanel4Layout.setVerticalGroup(
      jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel4Layout.createSequentialGroup()
        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel3)
          .addComponent(txtIrisNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel4)
          .addComponent(cbNewBP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btnChangeBP))
    );

    jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.jPanel5.border.title"))); // NOI18N
    jPanel5.setToolTipText(org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.jPanel5.toolTipText")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(btnToPaper, org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.btnToPaper.text")); // NOI18N
    btnToPaper.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnToPaperActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(chkRemoveEmail, org.openide.util.NbBundle.getMessage(bpTopComponent.class, "bpTopComponent.chkRemoveEmail.text")); // NOI18N

    javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
    jPanel5.setLayout(jPanel5Layout);
    jPanel5Layout.setHorizontalGroup(
      jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel5Layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(btnToPaper)
          .addComponent(chkRemoveEmail))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    jPanel5Layout.setVerticalGroup(
      jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel5Layout.createSequentialGroup()
        .addComponent(btnToPaper)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(chkRemoveEmail))
    );

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jSplitPane1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
          .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btnStop)
          .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jSplitPane1)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(btnStop)))
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void btnPEmailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPEmailActionPerformed
    // TODO add your handling code here:

    mode = 1;
    runstate = true;

    bgWorker bw = new bgWorker();
    bw.addPropertyChangeListener(this);
    bw.execute();

  }//GEN-LAST:event_btnPEmailActionPerformed

  private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
    // TODO add your handling code here:
    runstate = false;
    btnStop.setEnabled(false);
  }//GEN-LAST:event_btnStopActionPerformed

  private void btnChangeBPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnChangeBPActionPerformed
    // TODO add your handling code here:

    mode = 2;
    runstate = true;

    bgWorker bw = new bgWorker();
    bw.addPropertyChangeListener(this);
    bw.execute();
  }//GEN-LAST:event_btnChangeBPActionPerformed

  private void btnToPaperActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnToPaperActionPerformed
    // TODO add your handling code here:
    mode = 3;
    runstate = true;

    bgWorker bw = new bgWorker();
    bw.addPropertyChangeListener(this);
    bw.execute();
  }//GEN-LAST:event_btnToPaperActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton btnChangeBP;
  private javax.swing.JButton btnPEmail;
  private javax.swing.JButton btnStop;
  private javax.swing.JButton btnToPaper;
  private javax.swing.JComboBox<String> cbNewBP;
  private javax.swing.JCheckBox chkOverwriteEmail;
  private javax.swing.JCheckBox chkPMedia;
  private javax.swing.JCheckBox chkRemoveEmail;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanel4;
  private javax.swing.JPanel jPanel5;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JSplitPane jSplitPane1;
  private javax.swing.JLabel lblReady;
  private javax.swing.JTextArea txtAccNo;
  private javax.swing.JTextField txtIrisNo;
  private javax.swing.JTextArea txtOutput;
  // End of variables declaration//GEN-END:variables

  private String me = "Bulk Patcher";
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

      if (data.toLowerCase().startsWith("acc")) {
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
    btnPEmail.setEnabled(false);
    txtAccNo.setEditable(false);

    chkOverwriteEmail.setEnabled(false);
    chkPMedia.setEnabled(false);
  }

  private void enableBtn() {
    btnStop.setEnabled(false);
    btnPEmail.setEnabled(true);
    txtAccNo.setEditable(true);

    chkOverwriteEmail.setEnabled(true);
    chkPMedia.setEnabled(true);
  }

  private void customInit() {

    caret = (DefaultCaret) txtOutput.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

  }
}
