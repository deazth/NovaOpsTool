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
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import ops.com.Utilities;
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
        dtd = "-//bsm.nova.dev//setPL//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "setPLTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "bsm.nova.dev.setPLTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_setPLAction",
        preferredID = "setPLTopComponent"
)
@Messages({
  "CTL_setPLAction=Set Price List?",
  "CTL_setPLTopComponent=setPL Window",
  "HINT_setPLTopComponent=This is a setPL window"
})
public final class setPLTopComponent extends TopComponent implements PropertyChangeListener{

  private ArrayList<String> listnama;
  private ArrayList<String> listpoid;
  private ArrayList<String> listproduct;
  private ArrayList<String> listID;

  private String me = "Set Price List";

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    
  }

  class genWorker extends SwingWorker<Void, Void> {

    private int recCounter;
//    private StringBuilder oput;
    final ProgressHandle pbar = ProgressHandleFactory.createHandle("Generate Price List");

    @Override
    protected Void doInBackground() {

      jButton1.setEnabled(false);
      
      
      // get the save file
      JFileChooser fc = new JFileChooser();
      fc.setDialogType(JFileChooser.SAVE_DIALOG);
      fc.setDialogTitle("Save file as");

      if (fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
        jButton1.setEnabled(true);
        return null;
      }

      File savefile = fc.getSelectedFile();
      try {
        dFileWriter dfw = new dFileWriter(savefile);

        recCounter = 0;
//        oput = new StringBuilder();
        pbar.start();
        readInput();

        pbar.switchToDeterminate(listnama.size());

        for (recCounter = 0; recCounter < listnama.size(); recCounter++) {
          pbar.progress(recCounter);
          dfw.println(buildFlist());
//          oput.append(buildFlist());
        }

        dfw.flush("-1");
        Runtime.getRuntime().exec("explorer.exe /select," + savefile.getAbsolutePath());

      } catch (IOException e) {
        Utilities.logStack(me, e);
      }

      pbar.finish();
      jButton1.setEnabled(true);
      return null;
    }

    private String buildFlist() {
      String ret = "r << XXX 3\n"
              + "0 PIN_FLD_POID           POID [0] 0.0.0.1 /product -1 0\n"
              + "0 PIN_FLD_PROGRAM_NAME    STR [0] \"Outpayment Product Creation\"\n"
              + "0 PIN_FLD_PRODUCTS      ARRAY [0] allocated 34, used 34\n"
              + "1     PIN_FLD_POID           POID [0] 0.0.0.1 /product -1 0\n"
              + "1     PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "1     PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "1     PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "1     PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "1     PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "1     PIN_FLD_BASE_PRODUCT_OBJ   POID [0] 0.0.0.0  0 0\n"
              + "1     PIN_FLD_DESCR           STR [0] \"GANTI_NAMA_KAT_SINI\"\n"
              + "1     PIN_FLD_END_T        TSTAMP [0] (0)\n"
              + "1     PIN_FLD_NAME            STR [0] \"GANTI_NAMA_KAT_SINI\"\n"
              + "1     PIN_FLD_OWN_MAX      DECIMAL [0] NULL\n"
              + "1     PIN_FLD_OWN_MIN      DECIMAL [0] NULL\n"
              + "1     PIN_FLD_PARTIAL        ENUM [0] 1\n"
              + "1     PIN_FLD_PERMITTED       STR [0] \"GANTI_POID_TYPE\"\n"
              + "1     PIN_FLD_PRIORITY     DECIMAL [0] 0\n"
              + "1     PIN_FLD_PROVISIONING_TAG    STR [0] \"\"\n"
              + "1     PIN_FLD_PURCHASE_MAX DECIMAL [0] NULL\n"
              + "1     PIN_FLD_PURCHASE_MIN DECIMAL [0] NULL\n"
              + "1     PIN_FLD_START_T      TSTAMP [0] (0)\n"
              + "1     PIN_FLD_TAILORMADE      INT [0] 0\n"
              + "1     PIN_FLD_TAX_SUPPLIER    INT [0] 0\n"
              + "1     PIN_FLD_TYPE           ENUM [0] GANTI_PRODUCT_TYPE\n"
              + "1     PIN_FLD_ZONEMAP_NAME    STR [0] \"\"\n"
              + "1     PIN_FLD_USAGE_MAP     ARRAY [0] allocated 20, used 12\n"
              + "2         PIN_FLD_EVENT_TYPE      STR [0] \"/event/billing/product/fee/purchase\"\n"
              + "2         PIN_FLD_FLAGS           INT [0] 0\n"
              + "2         PIN_FLD_INCR_QUANTITY DECIMAL [0] 0\n"
              + "2         PIN_FLD_INCR_UNIT      ENUM [0] 0\n"
              + "2         PIN_FLD_MIN_QUANTITY DECIMAL [0] 0\n"
              + "2         PIN_FLD_MIN_UNIT       ENUM [0] 0\n"
              + "2         PIN_FLD_RATE_PLAN_NAME    STR [0] \"rate -1 0\n"
              + "2         PIN_FLD_RATE_PLAN_SELECTOR_OBJ   POID [0] 0.0.0.0  0 0\n"
              + "2         PIN_FLD_ROUNDING_MODE   ENUM [0] 0\n"
              + "2         PIN_FLD_RUM_NAME        STR [0] \"Occurrence\"\n"
              + "2         PIN_FLD_TIMEZONE_MODE   ENUM [0] 0\n"
              + "2         PIN_FLD_TOD_MODE       ENUM [0] 0\n"
              + "1     PIN_FLD_RATE_PLANS    ARRAY [0] allocated 20, used 17\n"
              + "2         PIN_FLD_POID           POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "2         PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "2         PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "2         PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "2         PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "2         PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "2         PIN_FLD_BILL_OFFSET     INT [0] 0\n"
              + "2         PIN_FLD_CURRENCY        INT [0] 840\n"
              + "2         PIN_FLD_CYCLE_FEE_FLAGS    INT [0] 0\n"
              + "2         PIN_FLD_EVENT_TYPE      STR [0] \"/event/billing/product/fee/purchase\"\n"
              + "2         PIN_FLD_NAME            STR [0] \"rate -1 0\n"
              + "2         PIN_FLD_OFFSET_UNIT    ENUM [0] 0\n"
              + "2         PIN_FLD_PRODUCT_OBJ    POID [0] 0.0.0.1 /product -1 0\n"
              + "2         PIN_FLD_TAX_CODE        STR [0] \"OS\"\n"
              + "2         PIN_FLD_TAX_WHEN       ENUM [0] 2\n"
              + "2         PIN_FLD_RATE_TIERS    ARRAY [0] allocated 20, used 5\n"
              + "3             PIN_FLD_DATE_RANGE_TYPE   ENUM [0] 0\n"
              + "3             PIN_FLD_NAME            STR [0] \"Tier 1\"\n"
              + "3             PIN_FLD_PRIORITY     DECIMAL [0] 0\n"
              + "3             PIN_FLD_RATE_OBJ       POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_RATE_INDEX      INT [0] 0\n"
              + "2         PIN_FLD_RATES         ARRAY [0] allocated 20, used 15\n"
              + "3             PIN_FLD_POID           POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "3             PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "3             PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "3             PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "3             PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "3             PIN_FLD_DESCR           STR [0] \"GANTI_NAMA_KAT_SINI\"\n"
              + "3             PIN_FLD_PRORATE_FIRST   ENUM [0] 702\n"
              + "3             PIN_FLD_PRORATE_LAST   ENUM [0] 702\n"
              + "3             PIN_FLD_RATE_PLAN_OBJ   POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "3             PIN_FLD_STEP_RESOURCE_ID    INT [0] 0\n"
              + "3             PIN_FLD_STEP_TYPE      ENUM [0] 0\n"
              + "3             PIN_FLD_TAILORMADE_DATA    STR [0] \"\"\n"
              + "3             PIN_FLD_TYPE           ENUM [0] 740\n"
              + "3             PIN_FLD_QUANTITY_TIERS  ARRAY [0] allocated 20, used 3\n"
              + "4                 PIN_FLD_STEP_MAX     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_STEP_MIN     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_BAL_IMPACTS   ARRAY [0] allocated 20, used 13\n"
              + "5                     PIN_FLD_ELEMENT_ID      INT [0] GANTI_ID_KAT_SINI\n"
              + "5                     PIN_FLD_END_T        TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_FIXED_AMOUNT DECIMAL [0] 0\n"
              + "5                     PIN_FLD_FLAGS           INT [0] 2\n"
              + "5                     PIN_FLD_GL_ID           INT [0] 0\n"
              + "5                     PIN_FLD_IMPACT_CATEGORY    STR [0] \"\"\n"
              + "5                     PIN_FLD_SCALED_AMOUNT DECIMAL [0] 1\n"
              + "5                     PIN_FLD_SCALED_UNIT    ENUM [0] 0\n"
              + "5                     PIN_FLD_START_T      TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_RELATIVE_START_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_START_OFFSET    INT [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_OFFSET    INT [0] 0\n"
              + "1     PIN_FLD_RATE_PLANS    ARRAY [1] allocated 20, used 17\n"
              + "2         PIN_FLD_POID           POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "2         PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "2         PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "2         PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "2         PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "2         PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "2         PIN_FLD_BILL_OFFSET     INT [0] 0\n"
              + "2         PIN_FLD_CURRENCY        INT [0] 702\n"
              + "2         PIN_FLD_CYCLE_FEE_FLAGS    INT [0] 0\n"
              + "2         PIN_FLD_EVENT_TYPE      STR [0] \"/event/billing/product/fee/purchase\"\n"
              + "2         PIN_FLD_NAME            STR [0] \"rate -1 0\n"
              + "2         PIN_FLD_OFFSET_UNIT    ENUM [0] 0\n"
              + "2         PIN_FLD_PRODUCT_OBJ    POID [0] 0.0.0.1 /product -1 0\n"
              + "2         PIN_FLD_TAX_CODE        STR [0] \"OS\"\n"
              + "2         PIN_FLD_TAX_WHEN       ENUM [0] 2\n"
              + "2         PIN_FLD_RATE_TIERS    ARRAY [0] allocated 20, used 5\n"
              + "3             PIN_FLD_DATE_RANGE_TYPE   ENUM [0] 0\n"
              + "3             PIN_FLD_NAME            STR [0] \"Tier 1\"\n"
              + "3             PIN_FLD_PRIORITY     DECIMAL [0] 0\n"
              + "3             PIN_FLD_RATE_OBJ       POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_RATE_INDEX      INT [0] 0\n"
              + "2         PIN_FLD_RATES         ARRAY [0] allocated 20, used 15\n"
              + "3             PIN_FLD_POID           POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "3             PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "3             PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "3             PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "3             PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "3             PIN_FLD_DESCR           STR [0] \"GANTI_NAMA_KAT_SINI\"\n"
              + "3             PIN_FLD_PRORATE_FIRST   ENUM [0] 702\n"
              + "3             PIN_FLD_PRORATE_LAST   ENUM [0] 702\n"
              + "3             PIN_FLD_RATE_PLAN_OBJ   POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "3             PIN_FLD_STEP_RESOURCE_ID    INT [0] 0\n"
              + "3             PIN_FLD_STEP_TYPE      ENUM [0] 0\n"
              + "3             PIN_FLD_TAILORMADE_DATA    STR [0] \"\"\n"
              + "3             PIN_FLD_TYPE           ENUM [0] 740\n"
              + "3             PIN_FLD_QUANTITY_TIERS  ARRAY [0] allocated 20, used 3\n"
              + "4                 PIN_FLD_STEP_MAX     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_STEP_MIN     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_BAL_IMPACTS   ARRAY [0] allocated 20, used 13\n"
              + "5                     PIN_FLD_ELEMENT_ID      INT [0] GANTI_ID_KAT_SINI\n"
              + "5                     PIN_FLD_END_T        TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_FIXED_AMOUNT DECIMAL [0] 0\n"
              + "5                     PIN_FLD_FLAGS           INT [0] 2\n"
              + "5                     PIN_FLD_GL_ID           INT [0] 0\n"
              + "5                     PIN_FLD_IMPACT_CATEGORY    STR [0] \"\"\n"
              + "5                     PIN_FLD_SCALED_AMOUNT DECIMAL [0] 1\n"
              + "5                     PIN_FLD_SCALED_UNIT    ENUM [0] 0\n"
              + "5                     PIN_FLD_START_T      TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_RELATIVE_START_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_START_OFFSET    INT [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_OFFSET    INT [0] 0\n"
              + "1     PIN_FLD_RATE_PLANS    ARRAY [2] allocated 20, used 17\n"
              + "2         PIN_FLD_POID           POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "2         PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "2         PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "2         PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "2         PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "2         PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "2         PIN_FLD_BILL_OFFSET     INT [0] 0\n"
              + "2         PIN_FLD_CURRENCY        INT [0] 826\n"
              + "2         PIN_FLD_CYCLE_FEE_FLAGS    INT [0] 0\n"
              + "2         PIN_FLD_EVENT_TYPE      STR [0] \"/event/billing/product/fee/purchase\"\n"
              + "2         PIN_FLD_NAME            STR [0] \"rate -1 0\n"
              + "2         PIN_FLD_OFFSET_UNIT    ENUM [0] 0\n"
              + "2         PIN_FLD_PRODUCT_OBJ    POID [0] 0.0.0.1 /product -1 0\n"
              + "2         PIN_FLD_TAX_CODE        STR [0] \"OS\"\n"
              + "2         PIN_FLD_TAX_WHEN       ENUM [0] 2\n"
              + "2         PIN_FLD_RATE_TIERS    ARRAY [0] allocated 20, used 5\n"
              + "3             PIN_FLD_DATE_RANGE_TYPE   ENUM [0] 0\n"
              + "3             PIN_FLD_NAME            STR [0] \"Tier 1\"\n"
              + "3             PIN_FLD_PRIORITY     DECIMAL [0] 0\n"
              + "3             PIN_FLD_RATE_OBJ       POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_RATE_INDEX      INT [0] 0\n"
              + "2         PIN_FLD_RATES         ARRAY [0] allocated 20, used 15\n"
              + "3             PIN_FLD_POID           POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "3             PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "3             PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "3             PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "3             PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "3             PIN_FLD_DESCR           STR [0] \"GANTI_NAMA_KAT_SINI\"\n"
              + "3             PIN_FLD_PRORATE_FIRST   ENUM [0] 702\n"
              + "3             PIN_FLD_PRORATE_LAST   ENUM [0] 702\n"
              + "3             PIN_FLD_RATE_PLAN_OBJ   POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "3             PIN_FLD_STEP_RESOURCE_ID    INT [0] 0\n"
              + "3             PIN_FLD_STEP_TYPE      ENUM [0] 0\n"
              + "3             PIN_FLD_TAILORMADE_DATA    STR [0] \"\"\n"
              + "3             PIN_FLD_TYPE           ENUM [0] 740\n"
              + "3             PIN_FLD_QUANTITY_TIERS  ARRAY [0] allocated 20, used 3\n"
              + "4                 PIN_FLD_STEP_MAX     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_STEP_MIN     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_BAL_IMPACTS   ARRAY [0] allocated 20, used 13\n"
              + "5                     PIN_FLD_ELEMENT_ID      INT [0] GANTI_ID_KAT_SINI\n"
              + "5                     PIN_FLD_END_T        TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_FIXED_AMOUNT DECIMAL [0] 0\n"
              + "5                     PIN_FLD_FLAGS           INT [0] 2\n"
              + "5                     PIN_FLD_GL_ID           INT [0] 0\n"
              + "5                     PIN_FLD_IMPACT_CATEGORY    STR [0] \"\"\n"
              + "5                     PIN_FLD_SCALED_AMOUNT DECIMAL [0] 1\n"
              + "5                     PIN_FLD_SCALED_UNIT    ENUM [0] 0\n"
              + "5                     PIN_FLD_START_T      TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_RELATIVE_START_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_START_OFFSET    INT [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_OFFSET    INT [0] 0\n"
              + "1     PIN_FLD_RATE_PLANS    ARRAY [3] allocated 20, used 17\n"
              + "2         PIN_FLD_POID           POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "2         PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "2         PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "2         PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "2         PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "2         PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "2         PIN_FLD_BILL_OFFSET     INT [0] 0\n"
              + "2         PIN_FLD_CURRENCY        INT [0] 756\n"
              + "2         PIN_FLD_CYCLE_FEE_FLAGS    INT [0] 0\n"
              + "2         PIN_FLD_EVENT_TYPE      STR [0] \"/event/billing/product/fee/purchase\"\n"
              + "2         PIN_FLD_NAME            STR [0] \"rate -1 0\n"
              + "2         PIN_FLD_OFFSET_UNIT    ENUM [0] 0\n"
              + "2         PIN_FLD_PRODUCT_OBJ    POID [0] 0.0.0.1 /product -1 0\n"
              + "2         PIN_FLD_TAX_CODE        STR [0] \"OS\"\n"
              + "2         PIN_FLD_TAX_WHEN       ENUM [0] 2\n"
              + "2         PIN_FLD_RATE_TIERS    ARRAY [0] allocated 20, used 5\n"
              + "3             PIN_FLD_DATE_RANGE_TYPE   ENUM [0] 0\n"
              + "3             PIN_FLD_NAME            STR [0] \"Tier 1\"\n"
              + "3             PIN_FLD_PRIORITY     DECIMAL [0] 0\n"
              + "3             PIN_FLD_RATE_OBJ       POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_RATE_INDEX      INT [0] 0\n"
              + "2         PIN_FLD_RATES         ARRAY [0] allocated 20, used 15\n"
              + "3             PIN_FLD_POID           POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "3             PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "3             PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "3             PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "3             PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "3             PIN_FLD_DESCR           STR [0] \"GANTI_NAMA_KAT_SINI\"\n"
              + "3             PIN_FLD_PRORATE_FIRST   ENUM [0] 702\n"
              + "3             PIN_FLD_PRORATE_LAST   ENUM [0] 702\n"
              + "3             PIN_FLD_RATE_PLAN_OBJ   POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "3             PIN_FLD_STEP_RESOURCE_ID    INT [0] 0\n"
              + "3             PIN_FLD_STEP_TYPE      ENUM [0] 0\n"
              + "3             PIN_FLD_TAILORMADE_DATA    STR [0] \"\"\n"
              + "3             PIN_FLD_TYPE           ENUM [0] 740\n"
              + "3             PIN_FLD_QUANTITY_TIERS  ARRAY [0] allocated 20, used 3\n"
              + "4                 PIN_FLD_STEP_MAX     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_STEP_MIN     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_BAL_IMPACTS   ARRAY [0] allocated 20, used 13\n"
              + "5                     PIN_FLD_ELEMENT_ID      INT [0] GANTI_ID_KAT_SINI\n"
              + "5                     PIN_FLD_END_T        TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_FIXED_AMOUNT DECIMAL [0] 0\n"
              + "5                     PIN_FLD_FLAGS           INT [0] 2\n"
              + "5                     PIN_FLD_GL_ID           INT [0] 0\n"
              + "5                     PIN_FLD_IMPACT_CATEGORY    STR [0] \"\"\n"
              + "5                     PIN_FLD_SCALED_AMOUNT DECIMAL [0] 1\n"
              + "5                     PIN_FLD_SCALED_UNIT    ENUM [0] 0\n"
              + "5                     PIN_FLD_START_T      TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_RELATIVE_START_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_START_OFFSET    INT [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_OFFSET    INT [0] 0\n"
              + "1     PIN_FLD_RATE_PLANS    ARRAY [4] allocated 20, used 17\n"
              + "2         PIN_FLD_POID           POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "2         PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "2         PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "2         PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "2         PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "2         PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "2         PIN_FLD_BILL_OFFSET     INT [0] 0\n"
              + "2         PIN_FLD_CURRENCY        INT [0] 344\n"
              + "2         PIN_FLD_CYCLE_FEE_FLAGS    INT [0] 0\n"
              + "2         PIN_FLD_EVENT_TYPE      STR [0] \"/event/billing/product/fee/purchase\"\n"
              + "2         PIN_FLD_NAME            STR [0] \"rate -1 0\n"
              + "2         PIN_FLD_OFFSET_UNIT    ENUM [0] 0\n"
              + "2         PIN_FLD_PRODUCT_OBJ    POID [0] 0.0.0.1 /product -1 0\n"
              + "2         PIN_FLD_TAX_CODE        STR [0] \"OS\"\n"
              + "2         PIN_FLD_TAX_WHEN       ENUM [0] 2\n"
              + "2         PIN_FLD_RATE_TIERS    ARRAY [0] allocated 20, used 5\n"
              + "3             PIN_FLD_DATE_RANGE_TYPE   ENUM [0] 0\n"
              + "3             PIN_FLD_NAME            STR [0] \"Tier 1\"\n"
              + "3             PIN_FLD_PRIORITY     DECIMAL [0] 0\n"
              + "3             PIN_FLD_RATE_OBJ       POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_RATE_INDEX      INT [0] 0\n"
              + "2         PIN_FLD_RATES         ARRAY [0] allocated 20, used 15\n"
              + "3             PIN_FLD_POID           POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "3             PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "3             PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "3             PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "3             PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "3             PIN_FLD_DESCR           STR [0] \"GANTI_NAMA_KAT_SINI\"\n"
              + "3             PIN_FLD_PRORATE_FIRST   ENUM [0] 702\n"
              + "3             PIN_FLD_PRORATE_LAST   ENUM [0] 702\n"
              + "3             PIN_FLD_RATE_PLAN_OBJ   POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "3             PIN_FLD_STEP_RESOURCE_ID    INT [0] 0\n"
              + "3             PIN_FLD_STEP_TYPE      ENUM [0] 0\n"
              + "3             PIN_FLD_TAILORMADE_DATA    STR [0] \"\"\n"
              + "3             PIN_FLD_TYPE           ENUM [0] 740\n"
              + "3             PIN_FLD_QUANTITY_TIERS  ARRAY [0] allocated 20, used 3\n"
              + "4                 PIN_FLD_STEP_MAX     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_STEP_MIN     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_BAL_IMPACTS   ARRAY [0] allocated 20, used 13\n"
              + "5                     PIN_FLD_ELEMENT_ID      INT [0] GANTI_ID_KAT_SINI\n"
              + "5                     PIN_FLD_END_T        TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_FIXED_AMOUNT DECIMAL [0] 0\n"
              + "5                     PIN_FLD_FLAGS           INT [0] 2\n"
              + "5                     PIN_FLD_GL_ID           INT [0] 0\n"
              + "5                     PIN_FLD_IMPACT_CATEGORY    STR [0] \"\"\n"
              + "5                     PIN_FLD_SCALED_AMOUNT DECIMAL [0] 1\n"
              + "5                     PIN_FLD_SCALED_UNIT    ENUM [0] 0\n"
              + "5                     PIN_FLD_START_T      TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_RELATIVE_START_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_START_OFFSET    INT [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_OFFSET    INT [0] 0\n"
              + "1     PIN_FLD_RATE_PLANS    ARRAY [5] allocated 20, used 17\n"
              + "2         PIN_FLD_POID           POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "2         PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "2         PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "2         PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "2         PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "2         PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "2         PIN_FLD_BILL_OFFSET     INT [0] 0\n"
              + "2         PIN_FLD_CURRENCY        INT [0] 458\n"
              + "2         PIN_FLD_CYCLE_FEE_FLAGS    INT [0] 0\n"
              + "2         PIN_FLD_EVENT_TYPE      STR [0] \"/event/billing/product/fee/purchase\"\n"
              + "2         PIN_FLD_NAME            STR [0] \"rate -1 0\n"
              + "2         PIN_FLD_OFFSET_UNIT    ENUM [0] 0\n"
              + "2         PIN_FLD_PRODUCT_OBJ    POID [0] 0.0.0.1 /product -1 0\n"
              + "2         PIN_FLD_TAX_CODE        STR [0] \"OS\"\n"
              + "2         PIN_FLD_TAX_WHEN       ENUM [0] 2\n"
              + "2         PIN_FLD_RATE_TIERS    ARRAY [0] allocated 20, used 5\n"
              + "3             PIN_FLD_DATE_RANGE_TYPE   ENUM [0] 0\n"
              + "3             PIN_FLD_NAME            STR [0] \"Tier 1\"\n"
              + "3             PIN_FLD_PRIORITY     DECIMAL [0] 0\n"
              + "3             PIN_FLD_RATE_OBJ       POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_RATE_INDEX      INT [0] 0\n"
              + "2         PIN_FLD_RATES         ARRAY [0] allocated 20, used 15\n"
              + "3             PIN_FLD_POID           POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "3             PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "3             PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "3             PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "3             PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "3             PIN_FLD_DESCR           STR [0] \"GANTI_NAMA_KAT_SINI\"\n"
              + "3             PIN_FLD_PRORATE_FIRST   ENUM [0] 702\n"
              + "3             PIN_FLD_PRORATE_LAST   ENUM [0] 702\n"
              + "3             PIN_FLD_RATE_PLAN_OBJ   POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "3             PIN_FLD_STEP_RESOURCE_ID    INT [0] 0\n"
              + "3             PIN_FLD_STEP_TYPE      ENUM [0] 0\n"
              + "3             PIN_FLD_TAILORMADE_DATA    STR [0] \"\"\n"
              + "3             PIN_FLD_TYPE           ENUM [0] 740\n"
              + "3             PIN_FLD_QUANTITY_TIERS  ARRAY [0] allocated 20, used 3\n"
              + "4                 PIN_FLD_STEP_MAX     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_STEP_MIN     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_BAL_IMPACTS   ARRAY [0] allocated 20, used 13\n"
              + "5                     PIN_FLD_ELEMENT_ID      INT [0] GANTI_ID_KAT_SINI\n"
              + "5                     PIN_FLD_END_T        TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_FIXED_AMOUNT DECIMAL [0] 0\n"
              + "5                     PIN_FLD_FLAGS           INT [0] 2\n"
              + "5                     PIN_FLD_GL_ID           INT [0] 0\n"
              + "5                     PIN_FLD_IMPACT_CATEGORY    STR [0] \"\"\n"
              + "5                     PIN_FLD_SCALED_AMOUNT DECIMAL [0] 1\n"
              + "5                     PIN_FLD_SCALED_UNIT    ENUM [0] 0\n"
              + "5                     PIN_FLD_START_T      TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_RELATIVE_START_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_START_OFFSET    INT [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_OFFSET    INT [0] 0\n"
              + "1     PIN_FLD_RATE_PLANS    ARRAY [6] allocated 20, used 17\n"
              + "2         PIN_FLD_POID           POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "2         PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "2         PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "2         PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "2         PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "2         PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "2         PIN_FLD_BILL_OFFSET     INT [0] 0\n"
              + "2         PIN_FLD_CURRENCY        INT [0] 978\n"
              + "2         PIN_FLD_CYCLE_FEE_FLAGS    INT [0] 0\n"
              + "2         PIN_FLD_EVENT_TYPE      STR [0] \"/event/billing/product/fee/purchase\"\n"
              + "2         PIN_FLD_NAME            STR [0] \"rate -1 0\n"
              + "2         PIN_FLD_OFFSET_UNIT    ENUM [0] 0\n"
              + "2         PIN_FLD_PRODUCT_OBJ    POID [0] 0.0.0.1 /product -1 0\n"
              + "2         PIN_FLD_TAX_CODE        STR [0] \"OS\"\n"
              + "2         PIN_FLD_TAX_WHEN       ENUM [0] 2\n"
              + "2         PIN_FLD_RATE_TIERS    ARRAY [0] allocated 20, used 5\n"
              + "3             PIN_FLD_DATE_RANGE_TYPE   ENUM [0] 0\n"
              + "3             PIN_FLD_NAME            STR [0] \"Tier 1\"\n"
              + "3             PIN_FLD_PRIORITY     DECIMAL [0] 0\n"
              + "3             PIN_FLD_RATE_OBJ       POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_RATE_INDEX      INT [0] 0\n"
              + "2         PIN_FLD_RATES         ARRAY [0] allocated 20, used 15\n"
              + "3             PIN_FLD_POID           POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "3             PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "3             PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "3             PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "3             PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "3             PIN_FLD_DESCR           STR [0] \"GANTI_NAMA_KAT_SINI\"\n"
              + "3             PIN_FLD_PRORATE_FIRST   ENUM [0] 702\n"
              + "3             PIN_FLD_PRORATE_LAST   ENUM [0] 702\n"
              + "3             PIN_FLD_RATE_PLAN_OBJ   POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "3             PIN_FLD_STEP_RESOURCE_ID    INT [0] 0\n"
              + "3             PIN_FLD_STEP_TYPE      ENUM [0] 0\n"
              + "3             PIN_FLD_TAILORMADE_DATA    STR [0] \"\"\n"
              + "3             PIN_FLD_TYPE           ENUM [0] 740\n"
              + "3             PIN_FLD_QUANTITY_TIERS  ARRAY [0] allocated 20, used 3\n"
              + "4                 PIN_FLD_STEP_MAX     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_STEP_MIN     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_BAL_IMPACTS   ARRAY [0] allocated 20, used 13\n"
              + "5                     PIN_FLD_ELEMENT_ID      INT [0] GANTI_ID_KAT_SINI\n"
              + "5                     PIN_FLD_END_T        TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_FIXED_AMOUNT DECIMAL [0] 0\n"
              + "5                     PIN_FLD_FLAGS           INT [0] 2\n"
              + "5                     PIN_FLD_GL_ID           INT [0] 0\n"
              + "5                     PIN_FLD_IMPACT_CATEGORY    STR [0] \"\"\n"
              + "5                     PIN_FLD_SCALED_AMOUNT DECIMAL [0] 1\n"
              + "5                     PIN_FLD_SCALED_UNIT    ENUM [0] 0\n"
              + "5                     PIN_FLD_START_T      TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_RELATIVE_START_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_START_OFFSET    INT [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_OFFSET    INT [0] 0\n"
              + "1     PIN_FLD_RATE_PLANS    ARRAY [7] allocated 20, used 17\n"
              + "2         PIN_FLD_POID           POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "2         PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "2         PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "2         PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "2         PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "2         PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "2         PIN_FLD_BILL_OFFSET     INT [0] 0\n"
              + "2         PIN_FLD_CURRENCY        INT [0] 392\n"
              + "2         PIN_FLD_CYCLE_FEE_FLAGS    INT [0] 0\n"
              + "2         PIN_FLD_EVENT_TYPE      STR [0] \"/event/billing/product/fee/purchase\"\n"
              + "2         PIN_FLD_NAME            STR [0] \"rate -1 0\n"
              + "2         PIN_FLD_OFFSET_UNIT    ENUM [0] 0\n"
              + "2         PIN_FLD_PRODUCT_OBJ    POID [0] 0.0.0.1 /product -1 0\n"
              + "2         PIN_FLD_TAX_CODE        STR [0] \"OS\"\n"
              + "2         PIN_FLD_TAX_WHEN       ENUM [0] 2\n"
              + "2         PIN_FLD_RATE_TIERS    ARRAY [0] allocated 20, used 5\n"
              + "3             PIN_FLD_DATE_RANGE_TYPE   ENUM [0] 0\n"
              + "3             PIN_FLD_NAME            STR [0] \"Tier 1\"\n"
              + "3             PIN_FLD_PRIORITY     DECIMAL [0] 0\n"
              + "3             PIN_FLD_RATE_OBJ       POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_RATE_INDEX      INT [0] 0\n"
              + "2         PIN_FLD_RATES         ARRAY [0] allocated 20, used 15\n"
              + "3             PIN_FLD_POID           POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "3             PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "3             PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "3             PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "3             PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "3             PIN_FLD_DESCR           STR [0] \"GANTI_NAMA_KAT_SINI\"\n"
              + "3             PIN_FLD_PRORATE_FIRST   ENUM [0] 702\n"
              + "3             PIN_FLD_PRORATE_LAST   ENUM [0] 702\n"
              + "3             PIN_FLD_RATE_PLAN_OBJ   POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "3             PIN_FLD_STEP_RESOURCE_ID    INT [0] 0\n"
              + "3             PIN_FLD_STEP_TYPE      ENUM [0] 0\n"
              + "3             PIN_FLD_TAILORMADE_DATA    STR [0] \"\"\n"
              + "3             PIN_FLD_TYPE           ENUM [0] 740\n"
              + "3             PIN_FLD_QUANTITY_TIERS  ARRAY [0] allocated 20, used 3\n"
              + "4                 PIN_FLD_STEP_MAX     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_STEP_MIN     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_BAL_IMPACTS   ARRAY [0] allocated 20, used 13\n"
              + "5                     PIN_FLD_ELEMENT_ID      INT [0] GANTI_ID_KAT_SINI\n"
              + "5                     PIN_FLD_END_T        TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_FIXED_AMOUNT DECIMAL [0] 0\n"
              + "5                     PIN_FLD_FLAGS           INT [0] 2\n"
              + "5                     PIN_FLD_GL_ID           INT [0] 0\n"
              + "5                     PIN_FLD_IMPACT_CATEGORY    STR [0] \"\"\n"
              + "5                     PIN_FLD_SCALED_AMOUNT DECIMAL [0] 1\n"
              + "5                     PIN_FLD_SCALED_UNIT    ENUM [0] 0\n"
              + "5                     PIN_FLD_START_T      TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_RELATIVE_START_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_START_OFFSET    INT [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_OFFSET    INT [0] 0\n"
              + "1     PIN_FLD_RATE_PLANS    ARRAY [8] allocated 20, used 17\n"
              + "2         PIN_FLD_POID           POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "2         PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "2         PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "2         PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "2         PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "2         PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "2         PIN_FLD_BILL_OFFSET     INT [0] 0\n"
              + "2         PIN_FLD_CURRENCY        INT [0] 764\n"
              + "2         PIN_FLD_CYCLE_FEE_FLAGS    INT [0] 0\n"
              + "2         PIN_FLD_EVENT_TYPE      STR [0] \"/event/billing/product/fee/purchase\"\n"
              + "2         PIN_FLD_NAME            STR [0] \"rate -1 0\n"
              + "2         PIN_FLD_OFFSET_UNIT    ENUM [0] 0\n"
              + "2         PIN_FLD_PRODUCT_OBJ    POID [0] 0.0.0.1 /product -1 0\n"
              + "2         PIN_FLD_TAX_CODE        STR [0] \"OS\"\n"
              + "2         PIN_FLD_TAX_WHEN       ENUM [0] 2\n"
              + "2         PIN_FLD_RATE_TIERS    ARRAY [0] allocated 20, used 5\n"
              + "3             PIN_FLD_DATE_RANGE_TYPE   ENUM [0] 0\n"
              + "3             PIN_FLD_NAME            STR [0] \"Tier 1\"\n"
              + "3             PIN_FLD_PRIORITY     DECIMAL [0] 0\n"
              + "3             PIN_FLD_RATE_OBJ       POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_RATE_INDEX      INT [0] 0\n"
              + "2         PIN_FLD_RATES         ARRAY [0] allocated 20, used 15\n"
              + "3             PIN_FLD_POID           POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "3             PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "3             PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "3             PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "3             PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "3             PIN_FLD_DESCR           STR [0] \"GANTI_NAMA_KAT_SINI\"\n"
              + "3             PIN_FLD_PRORATE_FIRST   ENUM [0] 702\n"
              + "3             PIN_FLD_PRORATE_LAST   ENUM [0] 702\n"
              + "3             PIN_FLD_RATE_PLAN_OBJ   POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "3             PIN_FLD_STEP_RESOURCE_ID    INT [0] 0\n"
              + "3             PIN_FLD_STEP_TYPE      ENUM [0] 0\n"
              + "3             PIN_FLD_TAILORMADE_DATA    STR [0] \"\"\n"
              + "3             PIN_FLD_TYPE           ENUM [0] 740\n"
              + "3             PIN_FLD_QUANTITY_TIERS  ARRAY [0] allocated 20, used 3\n"
              + "4                 PIN_FLD_STEP_MAX     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_STEP_MIN     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_BAL_IMPACTS   ARRAY [0] allocated 20, used 13\n"
              + "5                     PIN_FLD_ELEMENT_ID      INT [0] GANTI_ID_KAT_SINI\n"
              + "5                     PIN_FLD_END_T        TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_FIXED_AMOUNT DECIMAL [0] 0\n"
              + "5                     PIN_FLD_FLAGS           INT [0] 2\n"
              + "5                     PIN_FLD_GL_ID           INT [0] 0\n"
              + "5                     PIN_FLD_IMPACT_CATEGORY    STR [0] \"\"\n"
              + "5                     PIN_FLD_SCALED_AMOUNT DECIMAL [0] 1\n"
              + "5                     PIN_FLD_SCALED_UNIT    ENUM [0] 0\n"
              + "5                     PIN_FLD_START_T      TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_RELATIVE_START_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_START_OFFSET    INT [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_OFFSET    INT [0] 0\n"
              + "1     PIN_FLD_RATE_PLANS    ARRAY [9] allocated 20, used 17\n"
              + "2         PIN_FLD_POID           POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "2         PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "2         PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "2         PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "2         PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "2         PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "2         PIN_FLD_BILL_OFFSET     INT [0] 0\n"
              + "2         PIN_FLD_CURRENCY        INT [0] 999\n"
              + "2         PIN_FLD_CYCLE_FEE_FLAGS    INT [0] 0\n"
              + "2         PIN_FLD_EVENT_TYPE      STR [0] \"/event/billing/product/fee/purchase\"\n"
              + "2         PIN_FLD_NAME            STR [0] \"rate -1 0\n"
              + "2         PIN_FLD_OFFSET_UNIT    ENUM [0] 0\n"
              + "2         PIN_FLD_PRODUCT_OBJ    POID [0] 0.0.0.1 /product -1 0\n"
              + "2         PIN_FLD_TAX_CODE        STR [0] \"OS\"\n"
              + "2         PIN_FLD_TAX_WHEN       ENUM [0] 2\n"
              + "2         PIN_FLD_RATE_TIERS    ARRAY [0] allocated 20, used 5\n"
              + "3             PIN_FLD_DATE_RANGE_TYPE   ENUM [0] 0\n"
              + "3             PIN_FLD_NAME            STR [0] \"Tier 1\"\n"
              + "3             PIN_FLD_PRIORITY     DECIMAL [0] 0\n"
              + "3             PIN_FLD_RATE_OBJ       POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_RATE_INDEX      INT [0] 0\n"
              + "2         PIN_FLD_RATES         ARRAY [0] allocated 20, used 15\n"
              + "3             PIN_FLD_POID           POID [0] 0.0.0.1 /rate -1 0\n"
              + "3             PIN_FLD_CREATED_T    TSTAMP [0] (0)\n"
              + "3             PIN_FLD_MOD_T        TSTAMP [0] (0)\n"
              + "3             PIN_FLD_READ_ACCESS     STR [0] \"B\"\n"
              + "3             PIN_FLD_WRITE_ACCESS    STR [0] \"S\"\n"
              + "3             PIN_FLD_ACCOUNT_OBJ    POID [0] 0.0.0.1 /account 1 0\n"
              + "3             PIN_FLD_DESCR           STR [0] \"GANTI_NAMA_KAT_SINI\"\n"
              + "3             PIN_FLD_PRORATE_FIRST   ENUM [0] 702\n"
              + "3             PIN_FLD_PRORATE_LAST   ENUM [0] 702\n"
              + "3             PIN_FLD_RATE_PLAN_OBJ   POID [0] 0.0.0.1 /rate_plan -1 0\n"
              + "3             PIN_FLD_STEP_RESOURCE_ID    INT [0] 0\n"
              + "3             PIN_FLD_STEP_TYPE      ENUM [0] 0\n"
              + "3             PIN_FLD_TAILORMADE_DATA    STR [0] \"\"\n"
              + "3             PIN_FLD_TYPE           ENUM [0] 740\n"
              + "3             PIN_FLD_QUANTITY_TIERS  ARRAY [0] allocated 20, used 3\n"
              + "4                 PIN_FLD_STEP_MAX     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_STEP_MIN     DECIMAL [0] NULL\n"
              + "4                 PIN_FLD_BAL_IMPACTS   ARRAY [0] allocated 20, used 13\n"
              + "5                     PIN_FLD_ELEMENT_ID      INT [0] GANTI_ID_KAT_SINI\n"
              + "5                     PIN_FLD_END_T        TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_FIXED_AMOUNT DECIMAL [0] 0\n"
              + "5                     PIN_FLD_FLAGS           INT [0] 2\n"
              + "5                     PIN_FLD_GL_ID           INT [0] 0\n"
              + "5                     PIN_FLD_IMPACT_CATEGORY    STR [0] \"\"\n"
              + "5                     PIN_FLD_SCALED_AMOUNT DECIMAL [0] 1\n"
              + "5                     PIN_FLD_SCALED_UNIT    ENUM [0] 0\n"
              + "5                     PIN_FLD_START_T      TSTAMP [0] (0)\n"
              + "5                     PIN_FLD_RELATIVE_START_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_START_OFFSET    INT [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_UNIT   ENUM [0] 0\n"
              + "5                     PIN_FLD_RELATIVE_END_OFFSET    INT [0] 0\n"
              + "XXX\n"
              + "\n"
              + "xop PCM_OP_PRICE_SET_PRICE_LIST 0 3\n\n\n";

      ret = ret.replaceAll("GANTI_NAMA_KAT_SINI", listnama.get(recCounter));
      ret = ret.replaceAll("GANTI_POID_TYPE", listpoid.get(recCounter));
      ret = ret.replaceAll("GANTI_PRODUCT_TYPE", listproduct.get(recCounter));
      ret = ret.replaceAll("GANTI_ID_KAT_SINI", listID.get(recCounter));

      return ret;
    }

    private void readInput() {

      listnama = new ArrayList<String>();
      listpoid = new ArrayList<String>();
      listproduct = new ArrayList<String>();
      listID = new ArrayList<String>();

      String input = txtInput.getText();

      Scanner sc = new Scanner(input);

      while (sc.hasNextLine()) {

        String currLine = sc.nextLine();

        if (currLine.trim().isEmpty()) {
          continue;
        }

        String[] splitData = currLine.split("\\|");

        if (splitData.length != 4) {
          Utilities.log(me, "invalid line: " + currLine, constant.ERROR);
          continue;
        }

        listnama.add(splitData[0]);
        listpoid.add(splitData[1]);
        listproduct.add(splitData[2]);
        listID.add(splitData[3]);

      }

      Utilities.log(me, "Done reading input", constant.DEBUG);

    }

  }

  public setPLTopComponent() {
    initComponents();
    setName(Bundle.CTL_setPLTopComponent());
    setToolTipText(Bundle.HINT_setPLTopComponent());

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
    jButton1 = new javax.swing.JButton();
    jLabel1 = new javax.swing.JLabel();

    txtInput.setColumns(20);
    txtInput.setRows(5);
    jScrollPane1.setViewportView(txtInput);

    org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(setPLTopComponent.class, "setPLTopComponent.jButton1.text")); // NOI18N
    jButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton1ActionPerformed(evt);
      }
    });

    org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(setPLTopComponent.class, "setPLTopComponent.jLabel1.text")); // NOI18N

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 297, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jButton1))
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel1)
            .addGap(0, 0, Short.MAX_VALUE)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jLabel1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jButton1)
            .addGap(0, 0, Short.MAX_VALUE)))
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    // TODO add your handling code here:
    genWorker gw = new genWorker();
    gw.addPropertyChangeListener(this);
    gw.execute();


  }//GEN-LAST:event_jButton1ActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton jButton1;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JTextArea txtInput;
  // End of variables declaration//GEN-END:variables
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
