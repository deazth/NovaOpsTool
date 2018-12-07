/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bsm.nova.dev;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import ops.com.Utilities;
import ops.com.dbHandler;
import ops.com.constant;
import ops.com.dFileWriter;
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
        dtd = "-//bsm.nova.dev//mcs17//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "mcs17TopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "bsm.nova.dev.mcs17TopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_mcs17Action",
        preferredID = "mcs17TopComponent"
)
@Messages({
  "CTL_mcs17Action=mcs17",
  "CTL_mcs17TopComponent=mcs17 Window",
  "HINT_mcs17TopComponent=This is a mcs17 window"
})
public final class mcs17TopComponent extends TopComponent implements PropertyChangeListener {

  public mcs17TopComponent() {
    initComponents();
    runningState = false;
    setName(Bundle.CTL_mcs17TopComponent());
    setToolTipText(Bundle.HINT_mcs17TopComponent());

  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {

  }

  class bgWorker extends SwingWorker<Void, Void> {

    ArrayList<MCSlave> theSlaves;
    int threadcount = 15;
    File outputFile;
    long startdate;
    long enddate;

    public bgWorker(long sd, long ed) {
      startdate = sd;
      enddate = ed;
    }

    @Override
    protected Void doInBackground() throws Exception {
      runningState = true;
//      jButton1.setEnabled(false);
      sbuild = new StringBuilder();
      Utilities.log(me, "MCS module started", 0);
      init();

//      JFileChooser fc = new JFileChooser();
//      fc.setDialogType(JFileChooser.SAVE_DIALOG);
//      fc.setDialogTitle("Save file as");
//
//      if (fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
////        jButton1.setEnabled(true);
//        jButton1.setText("Start!");
//        return null;
//      }
//
//      outputFile = fc.getSelectedFile();
//      ofilewrite = new dFileWriter(outputFile);
//      ofilewrite.println("Account No,Service ID,Customer Name,BP,Currency,Invoice No,Doc Date,Process Date,Segment Code,Cost Center,Adj ID,Reason ID,Adj Desc,Amt No Tax");
//
//      Utilities.log(me, "Going to save to " + outputFile.getAbsolutePath(), 0);
      distribute();
      beginSlavery();
//      dumpToFile();

//      try {
//        ofilewrite.flush("-1");
//      } catch (Exception e) {
//        Utilities.popup("Error flushing output file: " + e.toString());
//        Utilities.logStack(me, e);
//      }
//      jButton1.setEnabled(true);
      jButton1.setText("Start!");
      return null;
    }

    private void init() {

      sb = new StringBuilder();

      theSlaves = new ArrayList<MCSlave>();

      for (int i = 0; i < threadcount; i++) {
        theSlaves.add(new MCSlave(i, startdate, enddate));
      }

      if (jCheckBox1.isSelected()) {
        Utilities.popup("Load from input file");
        loadFromINput();
      }

      Utilities.log(me, "6 slaves kidnapped", 0);
    }

    private void loadFromINput() {
      JFileChooser fc = new JFileChooser();
      fc.setDialogType(JFileChooser.OPEN_DIALOG);
      fc.setDialogTitle("Select input file");

      if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
        jButton1.setEnabled(true);
        runningState = false;
        return;
      }

      File inputfile = fc.getSelectedFile();
      StringBuilder sb = new StringBuilder();

      try {
        Scanner sc = new Scanner(inputfile);

        ProgressHandle br = ProgressHandleFactory.createHandle("Load data from file");
        br.start();

        while (sc.hasNextLine()) {
          sb.append(sc.nextLine());
          sb.append(constant.LINE_SEPARATOR);
        }

        sc.close();

        txtInput.setText(sb.toString());

        br.finish();
      } catch (Exception e) {
        Utilities.popup("Error loading input file: " + e.getMessage());
        Utilities.logStack(me, e);
      }

    }

    private void distribute() {
      if (runningState == false) {
        return;
      }

      Utilities.log(me, "distributing workload to slaves ", 0);
      ProgressHandle loadp = ProgressHandleFactory.createHandle("Load data");
      loadp.start();
      int counter = 0;
      Scanner sc = new Scanner(txtInput.getText());

      while (sc.hasNextLine()) {
        String line = sc.nextLine().trim();
        if (line.isEmpty()) {
          continue;
        }
        theSlaves.get(counter++ % threadcount).add(line);
      }

      loadp.finish();

      Utilities.log(me, "workload distribution completed ", 0);

    }

    private void beginSlavery() {
      if (runningState == false) {
        return;
      }

      Utilities.log(me, "Commencing slavery ", 0);
      ArrayList<Thread> tlist = new ArrayList<Thread>();

      for (int i = 0; i < threadcount; i++) {
        tlist.add(new Thread(theSlaves.get(i)));
      }

      for (Thread aa : tlist) {
        aa.start();
      }

      for (Thread aa : tlist) {
        try {
          aa.join();
        } catch (Exception e) {
        }
      }

      Utilities.log(me, "Enslavement completed ", 0);

    }

    private void dumpToFile() {
      if (runningState == false) {
        return;
      }

      Utilities.log(me, "Dumping result to " + outputFile.getAbsolutePath(), 0);
      try {
        dFileWriter dfw = new dFileWriter(outputFile);
        dfw.println("Account No,Service ID,Customer Name,BP,Currency,Invoice No,Doc Date,Process Date,Segment Code,Cost Center,Adj ID,Reason ID,Adj Desc,Amt No Tax");

        for (int i = 0; i < threadcount; i++) {
//          theSlaves.get(i).print(dfw);
        }

        dfw.flush("-1");

        Runtime.getRuntime().exec("explorer.exe /select," + outputFile.getAbsolutePath());

      } catch (Exception e) {
        Utilities.logStack(me, e);
      }

      Utilities.log(me, "File created", 0);

    }

  }

  class MCSlave implements Runnable {

    dbHandler dbh;
    int tid;
    String getQuery;
    ProgressHandle pbar;

    ArrayList<String> poidlist;
//    ArrayList<String> output;

    public MCSlave(int tcount, long startdate, long enddate) {
      tid = tcount;
      poidlist = new ArrayList<String>();
      getQuery = "insert into adj_mfrs2017\n"
              + "select distinct ta.account_no\n"
              + "       ,ts.LOGIN as service_id\n"
              + "       , tani.last_name as customer_name,tb.ACTG_CYCLE_DOM as BP\n"
              + "       ,decode(ta.currency,'458','MYR','840','USD','978','EUR','702','SGD','344','HKD')currency\n"
              + "       ,to_char(unix_ora_ts_conv(bill_t.end_t),'ddMMYYYY') as doc_date\n"
              + "       ,to_char(unix_ora_ts_conv(ti.created_t),'ddMMYYYY') as processdate\n"
              + "       ,ttcp.tm_segment_code as segment_code,ttcp.tm_cost_center as cost_center\n"
              + "       ,Substr(te.descr,1,9) as adjustment_id,tebm.reason_id as adjustment_code\n"
              + "       ,Substr(te.descr,11,60) as adjustment_description,tebi.gl_id\n"
              + "       ,tcga.gl_offset_acct as gl_account,tcjp.TM_PROD_CODE as product_code,\n"
              + "       case when ta.currency ='458' then tebi.amount\n"
              + "       else \n"
              + "            round(tebi.amount * fad.amount,2)\n"
              + "       end adjustment_amount_MYR,\n"
              + "       case when ta.currency <> '458' then tebi.amount\n"
              + "       end adjustment_amount_FC\n"
              + "from \n"
              + "  pin.account_t@APP_BRM_SVR ta inner join pin.billinfo_t@APP_BRM_SVR tb\n"
              + "    on ta.poid_Id0 = tb.account_obj_Id0\n"
              + "  inner join pin.item_t@APP_BRM_SVR ti\n"
              + "    on ta.poid_id0 = ti.account_obj_Id0\n"
              + "  left join pin.service_t@APP_BRM_SVR ts\n"
              + "    on ti.service_obj_Id0 = ts.poid_Id0\n"
              + "  inner join pin.profile_t@APP_BRM_SVR tpro\n"
              + "    on ta.poid_Id0 = tpro.account_obj_id0\n"
              + "    and tpro.poid_type = '/profile/tm_account'\n"
              + "  inner join pin.tm_cust_profile_t@APP_BRM_SVR ttcp\n"
              + "    on tpro.poid_id0 = ttcp.obj_Id0\n"
              + "  inner join pin.event_t@APP_BRM_SVR te \n"
              + "    on ti.poid_Id0 = te.item_obj_Id0\n"
              + "    and te.poid_type LIKE '/event/billing/adjustment/%'\n"
              + "    AND te.descr not like '%5 Sen Government Rounding%'\n"
              + "  inner join pin.account_nameinfo_t@APP_BRM_SVR tani\n"
              + "    on ta.poid_Id0 = tani.obj_id0\n"
              + "  inner join pin.event_bal_impacts_t@APP_BRM_SVR tebi\n"
              + "    on te.poid_Id0 = tebi.obj_id0\n"
              + "    and tebi.gl_id not in ('40000002','40000003','40000000','40000001')\n"
              + "--    and tebi.gl_id <>'40000002' or tebi.gl_id <> '40000003'\n"
              + "  inner join pin.event_billing_misc_t@APP_BRM_SVR tebm\n"
              + "    on te.poid_Id0 = tebm.obj_id0\n"
              + "  inner join bill_t@APP_BRM_SVR\n"
              + "    on ta.poid_Id0 = bill_T.account_obj_Id0\n"
              + "    and ti.CREATED_T >= bill_t.start_t\n"
              + "    and ti.created_t < bill_t.end_t\n"
              + "  inner join pin.adjcode_mfrs2@APP_BRM_SVR\n"
              + "    on tebm.reason_id = adjcode_mfrs2.string_id\n"
              + "  left join brm_custom.forex_adjustment_detail@APP_BRM_SVR fad\n"
              + "    on fad.item_no = ti.item_no\n"
              + "    and fad.POID_ID0 = ti.poid_id0\n"
              + "  inner join pin.tm_config_jnl_prod_t@APP_BRM_SVR tcjp\n"
              + "    on tcjp.gl_id = tebi.gl_id\n"
              + "    and tcjp.tm_relationship_code = ttcp.TM_RELATIONSHIP_CODE\n"
              + "  inner join pin.config_glid_accts_t@APP_BRM_SVR tcga\n"
              + "    on tcga.rec_id2 = tebi.GL_ID\n"
              + "    and tcga.rec_id2 = tcjp.gl_id\n"
              + "where\n"
              + "    ti.created_t >= " + startdate + " and ti.created_t < " + enddate + "\n"
              + "    and ta.poid_id0 = ? ";
    }

    public void add(String accpoid) {
      poidlist.add(accpoid);
    }

    @Override
    public void run() {
//      output = new ArrayList<String>();

      Utilities.log(me, "Thread#" + tid + ": count = " + poidlist.size(), 0);
      int counter = 0;
      PreparedStatement ps;
      String d = ",";

      try {
        dbh = new dbHandler("staging");
        dbh.setDBConnInfo(constant.dbStgCon);
        dbh.setUserPass(constant.dbStgUser, constant.dbStgPass);
        dbh.openConnection();

        ps = dbh.createPS(getQuery);

      } catch (Exception e) {
        Utilities.log(me, "Thread#" + tid + ": unable to start DB connection", 1);
        Utilities.logStack(me, e);
        return;
      }

      pbar = ProgressHandleFactory.createHandle("TID#" + tid);
      pbar.start(poidlist.size());

      for (String accpoid : poidlist) {
        if (!runningState) {
          break;
        }

        pbar.progress(accpoid, counter);
        counter++;

        if (counter % 1000 == 0) {
          Utilities.log(me, "Thread#" + tid + ": processed = " + counter, 0);
        }

        try {
          ps.setString(1, accpoid);
          int reccount = ps.executeUpdate();

//          ResultSet rs = ps.executeQuery();
//
//          while (rs.next()) {
//            String out = dbHandler.dbGetString(rs, "account_no") + d
//                    + dbHandler.dbGetString(rs, "service_id") + d + "\""
//                    + dbHandler.dbGetString(rs, "customer_name") + "\"" + d
//                    + dbHandler.dbGetString(rs, "BP") + d
//                    + dbHandler.dbGetString(rs, "currency") + d
//                    + dbHandler.dbGetString(rs, "invoice_no") + d
//                    + dbHandler.dbGetString(rs, "doc_date") + d
//                    + dbHandler.dbGetString(rs, "processdate") + d
//                    + dbHandler.dbGetString(rs, "tm_segment_code") + d
//                    + dbHandler.dbGetString(rs, "tm_cost_center") + d
//                    + dbHandler.dbGetString(rs, "adj_id") + d
//                    + dbHandler.dbGetString(rs, "reason_id") + d
//                    + dbHandler.dbGetString(rs, "adj_desc") + d
//                    + dbHandler.dbGetString(rs, "amtnotax") + d;
//
////            output.add(out);
          outToFile("TID#" + tid + "@" + counter + " " + accpoid + " => " + reccount + " row(s) inserted");

//          }
        } catch (Exception e) {
          Utilities.log(me, "Thread#" + tid + ": " + accpoid + " -> " + e.getMessage(), 1);
        }

      }

      try {
        dbh.closeConnection();
      } catch (Exception e) {
      }

      pbar.finish();

      Utilities.log(me, "Thread#" + tid + ": done fetching data", 0);
    }

//    public void print(dFileWriter dfw) throws IOException {
//      Utilities.log(me, "Thread#" + tid + ": begin printing", 0);
//      for (String l : output) {
//        dfw.println(l);
//      }
//      Utilities.log(me, "Thread#" + tid + ": done printing", 0);
//    }
  }

  private synchronized void outToFile(String line) {
//    ofilewrite.println(line);
    String currts = Utilities.tsToDateNow("dd/MM/yyyy HH:mm:ss -> ");
    String out = currts + line + constant.LINE_SEPARATOR;
    sb.append(out);
    txtOutput.setText(sb.toString());

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
    lblOutput = new javax.swing.JLabel();
    jPanel1 = new javax.swing.JPanel();
    jCheckBox1 = new javax.swing.JCheckBox();
    jLabel2 = new javax.swing.JLabel();
    jdStartdate = new org.jdesktop.swingx.JXDatePicker();
    jLabel3 = new javax.swing.JLabel();
    jdEnddate = new org.jdesktop.swingx.JXDatePicker();
    jButton1 = new javax.swing.JButton();

    txtInput.setColumns(20);
    txtInput.setRows(5);
    jScrollPane1.setViewportView(txtInput);

    org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(mcs17TopComponent.class, "mcs17TopComponent.jLabel1.text")); // NOI18N

    txtOutput.setColumns(20);
    txtOutput.setRows(5);
    jScrollPane2.setViewportView(txtOutput);

    org.openide.awt.Mnemonics.setLocalizedText(lblOutput, org.openide.util.NbBundle.getMessage(mcs17TopComponent.class, "mcs17TopComponent.lblOutput.text")); // NOI18N

    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(mcs17TopComponent.class, "mcs17TopComponent.jPanel1.border.title"))); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jCheckBox1, org.openide.util.NbBundle.getMessage(mcs17TopComponent.class, "mcs17TopComponent.jCheckBox1.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(mcs17TopComponent.class, "mcs17TopComponent.jLabel2.text")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(mcs17TopComponent.class, "mcs17TopComponent.jLabel3.text")); // NOI18N

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jCheckBox1)
          .addComponent(jLabel2)
          .addComponent(jdStartdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel3)
          .addComponent(jdEnddate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jCheckBox1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabel2)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jdStartdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabel3)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jdEnddate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addContainerGap(239, Short.MAX_VALUE))
    );

    org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(mcs17TopComponent.class, "mcs17TopComponent.jButton1.text")); // NOI18N
    jButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton1ActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel1))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(lblOutput)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jButton1)
            .addContainerGap())
          .addGroup(layout.createSequentialGroup()
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 385, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel1)
          .addComponent(lblOutput)
          .addComponent(jButton1))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)
          .addComponent(jScrollPane2)
          .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    // TODO add your handling code here:

    long startdate, enddate;
    
    try {
      startdate = jdStartdate.getDate().getTime() / 1000;
      enddate = jdEnddate.getDate().getTime() / 1000;
      
      if(enddate <= startdate){
        Utilities.popup("End date is before or same as start date");
        return;
      }
      
      if(enddate == 0 || startdate == 0){
        Utilities.popup("Invalid start or end date");
        return;
      }
      
    } catch (Exception e) {
      Utilities.popup("Please select proper date range: " + e.toString());
      return;
    }
    
    if(enddate - startdate > 2678400){
      Utilities.popup("Date range more than 31 days. Welcome to hell...");
    }

    if (runningState) {
      runningState = false;
      jButton1.setText("Start!");
    } else {
      runningState = true;

      jButton1.setText("Stop");

      bgWorker bw = new bgWorker(startdate, enddate);
      bw.addPropertyChangeListener(this);
      bw.execute();
    }


  }//GEN-LAST:event_jButton1ActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton jButton1;
  private javax.swing.JCheckBox jCheckBox1;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private org.jdesktop.swingx.JXDatePicker jdEnddate;
  private org.jdesktop.swingx.JXDatePicker jdStartdate;
  private javax.swing.JLabel lblOutput;
  private javax.swing.JTextArea txtInput;
  private javax.swing.JTextArea txtOutput;
  // End of variables declaration//GEN-END:variables

  private ArrayList<String> inputData;
  private boolean runningState = false;
  private String me = "MCS17";
  dFileWriter ofilewrite;
  StringBuilder sb;

  private StringBuilder sbuild;

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
