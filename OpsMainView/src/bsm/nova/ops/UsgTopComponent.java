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
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.text.DefaultCaret;
import javax.xml.parsers.ParserConfigurationException;
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
import org.xml.sax.SAXException;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//bsm.nova.ops//Usg//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "UsgTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "bsm.nova.ops.UsgTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_UsgAction",
        preferredID = "UsgTopComponent"
)
@Messages({
  "CTL_UsgAction=Usg",
  "CTL_UsgTopComponent=Usg Window",
  "HINT_UsgTopComponent=This is a Usg window"
})
public final class UsgTopComponent extends TopComponent implements PropertyChangeListener {

  @Override
  public void propertyChange(PropertyChangeEvent evt) {

  }

  class patchWorker extends SwingWorker<Void, Void> {

    private ResultSet recordRs;
    private dbHandler dbh;
    private PreparedStatement psupdate;
    private final ProgressHandle pbar = ProgressHandleFactory.createHandle("Fixing EDR");

    @Override
    protected Void doInBackground() {
      jButton2.setEnabled(false);

      pbar.start();

      try {
        init();

        process();

        close();
      } catch (Exception e) {
        Utilities.log(me, "error update " + e.toString(), constant.ERROR);
        Utilities.logStack(me, e);
      }

      pbar.finish();
      jButton2.setEnabled(true);
      return null;
    }

    private void process() throws SQLException {

      String query = "select c.*\n"
              + "from suspended_usage_t a, susp_usage_telco_info_t b,SUSP_USAGE_EDR_BUF c\n"
              + "where a.poid_id0 = b.obj_id0\n"
              + "and a.poid_id0 = c.obj_id0\n"
              + "and a.status in (0,1)\n"
              + "and a.error_code = '16503'\n"
              + "and b.service_type = '11'";

//      String query = "select * from Susp_Usage_Edr_Buf where obj_Id0 = 23382186685";
      if (!isRunningState) {
        return;
      }

      output("Getting list from DB");
      int counter = 0;
      recordRs = dbh.executeSelect(query);
      output("List obtained. Begin processing...");

      while (recordRs.next()) {
        if (!isRunningState) {
          return;
        }
        counter++;
        String poid = dbHandler.dbGetString(recordRs, "obj_id0");
        Blob edr = recordRs.getBlob("edr_buf");
        String fixed = "";

//        output("poid: " + poid);
//        output("buf:");
        try {
          fixed = getFixedEDR(edr);
//          output(fixed);

        } catch (Exception e) {
          output("error getting buf: " + e.toString());
          Utilities.logStack(me, e);
          continue;
        }

        // update back to db
        psupdate.setString(2, poid);
        psupdate.setBytes(1, fixed.getBytes());

        if (psupdate.executeUpdate() != 0) {
          output(counter + ": " + poid + " updated");
        }

      }

    }

    private String getFixedEDR(Blob edr) throws SQLException, ParserConfigurationException, SAXException, IOException {
      String data = new String(edr.getBytes(1, (int) edr.length()));
//      output(data);

      int pos = data.indexOf("id=\"1.25.1\">") + 12;
      String P1 = data.substring(0, pos);
      String rem = data.substring(pos);
      rem = rem.substring(rem.indexOf("<"));

      pos = rem.indexOf("id=\"1.62.1\">") + 12;
      String P2 = rem.substring(0, pos);
      rem = rem.substring(pos);
      rem = rem.substring(rem.indexOf("<"));

      return P1 + "04" + P2 + "IL01" + rem;
    }

//    private String getStringFromDocument(Document doc) {
//      try {
//        DOMSource domSource = new DOMSource(doc);
//        StringWriter writer = new StringWriter();
//        StreamResult result = new StreamResult(writer);
//        TransformerFactory tf = TransformerFactory.newInstance();
//        Transformer transformer = tf.newTransformer();
//        transformer.transform(domSource, result);
//        return writer.toString();
//      } catch (TransformerException ex) {
//        Utilities.logStack(me, ex);
//        return null;
//      }
//    }
    private void init() throws SQLException {
      sb = new StringBuilder();

      DefaultCaret caret = (DefaultCaret) jTextArea1.getCaret();
      caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

      output("Fix EDR started");

      dbh = new dbHandler("apps");

      dbh.setDBConnInfo(constant.dbConApps);
      dbh.setUserPass(constant.dbConUser, constant.dbConPass);
      dbh.openConnection();

      psupdate = dbh.createPS("update Susp_Usage_Edr_Buf set edr_buf = ? where obj_id0 = ? ");
    }

    private void close() {

      output("Fix EDR ended");

      try {
        dbh.closeConnection();
      } catch (Exception e) {
      }
    }

  }

  public UsgTopComponent() {
    initComponents();
    setName(Bundle.CTL_UsgTopComponent());
    setToolTipText(Bundle.HINT_UsgTopComponent());

  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jButton1 = new javax.swing.JButton();
    jButton2 = new javax.swing.JButton();
    jScrollPane1 = new javax.swing.JScrollPane();
    jTextArea1 = new javax.swing.JTextArea();
    jButton3 = new javax.swing.JButton();

    org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(UsgTopComponent.class, "UsgTopComponent.jButton1.text")); // NOI18N
    jButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton1ActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(jButton2, org.openide.util.NbBundle.getMessage(UsgTopComponent.class, "UsgTopComponent.jButton2.text")); // NOI18N
    jButton2.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton2ActionPerformed(evt);
      }
    });

    jTextArea1.setColumns(20);
    jTextArea1.setRows(5);
    jScrollPane1.setViewportView(jTextArea1);

    org.openide.awt.Mnemonics.setLocalizedText(jButton3, org.openide.util.NbBundle.getMessage(UsgTopComponent.class, "UsgTopComponent.jButton3.text")); // NOI18N
    jButton3.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton3ActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
          .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
          .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 293, Short.MAX_VALUE)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jButton1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jButton2)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jButton3)))
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    // TODO add your handling code here:

    Scanner sc;
    dFileWriter df;
    File infolder = null;
    String outfolder = ".";
    int counter = 0;

    JFileChooser inputc = new JFileChooser(".");
    inputc.setDialogTitle("Select input directory");
    inputc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    inputc.setAcceptAllFileFilterUsed(false);

    if (inputc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      infolder = inputc.getSelectedFile();
    } else {
      return;
    }

    inputc = new JFileChooser(".");
    inputc.setDialogTitle("Select output directory");
    inputc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    inputc.setAcceptAllFileFilterUsed(false);

    if (inputc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      outfolder = inputc.getSelectedFile().getAbsolutePath();
    } else {
      return;
    }

    for (File af : infolder.listFiles()) {
      System.out.println(af.getName());

//      if (!af.getName().endsWith("done")) {
//        continue;
//      }
      try {
        String infile = af.getName();
        sc = new Scanner(af);
        df = new dFileWriter(outfolder + "/" + infile);
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }

      try {
        String line = sc.nextLine();
        df.println(line);

        while (sc.hasNextLine()) {

          line = sc.nextLine();

          if (line.startsWith("120")) {
            String[] arr = line.split("\\|");

            if (arr.length != 30) {
              System.out.println(line);
              continue;
            }

            if (arr[26].equals("0")) {
              arr[26] = "";
            }
            arr[26] = arr[26] + "A";

            //arr[26] = "" + counter++;
            String outline = "";

            for (String aa : arr) {
//          System.out.println(i + " - " + aa);  
              outline += aa + "|";
            }

            df.println(outline.substring(0, outline.length() - 1));
          } else {
            df.println(line);
          }

        }

        df.flush("-1");

      } catch (Exception e) {
        e.printStackTrace();
      }
    }


  }//GEN-LAST:event_jButton1ActionPerformed

  private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
    // TODO add your handling code here:

    isRunningState = true;
    patchWorker pw = new patchWorker();
    pw.addPropertyChangeListener(this);
    pw.execute();

  }//GEN-LAST:event_jButton2ActionPerformed

  private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
    // TODO add your handling code here:
    isRunningState = false;
  }//GEN-LAST:event_jButton3ActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton jButton1;
  private javax.swing.JButton jButton2;
  private javax.swing.JButton jButton3;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JTextArea jTextArea1;
  // End of variables declaration//GEN-END:variables

  private String me = "Module Shina";
  private StringBuilder sb;
  private boolean isRunningState = false;

  @Override
  public void componentOpened() {
    // TODO add custom code on component opening
  }

  @Override
  public void componentClosed() {
    // TODO add custom code on component closing
  }

  private void output(String line) {

    sb.append(line);
    sb.append(constant.LINE_SEPARATOR);

    jTextArea1.setText(sb.toString());

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
