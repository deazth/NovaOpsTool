/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ops.launch;

import java.awt.BorderLayout;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.netbeans.api.autoupdate.InstallSupport;
import org.netbeans.api.autoupdate.InstallSupport.Validator;
import org.netbeans.api.autoupdate.OperationContainer;
import org.netbeans.api.autoupdate.OperationContainer.OperationInfo;
import org.netbeans.api.autoupdate.OperationException;
import org.netbeans.api.autoupdate.OperationSupport.Restarter;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.modules.ModuleInstall;
import org.openide.windows.WindowManager;

public class Installer extends ModuleInstall {

  JLabel lbl = new JLabel("Knocking the door...");
//  JDialog loading = new JDialog(null, "Dialog", Dialog.ModalityType.APPLICATION_MODAL);

  @Override
  public void restored() {
    // TODO

    System.out.println("1");

    JPanel p1 = new JPanel(new BorderLayout());
    p1.add(lbl, BorderLayout.CENTER);
//    loading.setUndecorated(true);
//    loading.getContentPane().add(p1);
//    loading.pack();
//    loading.setLocationRelativeTo(null);
//    loading.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
//    loading.setModal(true);

    System.out.println("2");
    
    
    

    GuiUpdater lw = new GuiUpdater();
    lw.addPropertyChangeListener(null);
    
    WindowManager.getDefault().invokeWhenUIReady(lw);
//    lw.execute();

//    loading.setVisible(true);
    System.out.println("4");

  }

  class GuiUpdater extends SwingWorker<Void, Void> {

    @Override
    protected Void doInBackground() throws Exception {
      try {

        URL u = new URL("http://10.14.28.102:8081/BillAppsPatcher/opstool/lock.html");
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();

        huc.setRequestMethod("GET");  //OR  huc.setRequestMethod ("HEAD"); 
        huc.connect();
        int code = huc.getResponseCode();

        if (code != 200) {
          JOptionPane.showMessageDialog(null, "lock " + code, "dun dun dunnn...", JOptionPane.ERROR_MESSAGE);
          System.exit(1);
        }

        updateHandler();

//        loading.dispose();
      } catch (Exception e) {
        JOptionPane.showMessageDialog(null, e.getMessage(), "dun dun dunnn...", JOptionPane.ERROR_MESSAGE);
        System.exit(1);

      }

      return null;
    }

    private void updateHandler() {
      
      

      lbl.setText("Asking Life for lemons...");
      System.out.println("Checking for updates");
      Collection<UpdateElement> elements4update = new HashSet<UpdateElement>();
      List<UpdateUnit> updateUnits = UpdateManager.getDefault().getUpdateUnits();

      // check for items to be updated
      for (UpdateUnit unit : updateUnits) {
        if (unit.getInstalled() != null) { // means the plugin already installed
          if (!unit.getAvailableUpdates().isEmpty()) { // has updates
            elements4update.add(unit.getAvailableUpdates().get(0)); // add plugin with highest version
          }
        }
      }

      if (elements4update.isEmpty()) {
        System.out.println("No new lemons");
        lbl.setText("No new updates");
        return;
      }

      // list down updateable items
      lbl.setText("Counting beans...");
      OperationContainer<InstallSupport> container = OperationContainer.createForUpdate();
      for (UpdateElement element : elements4update) {
        if (container.canBeAdded(element.getUpdateUnit(), element)) {
          OperationInfo<InstallSupport> operationInfo = container.add(element);
          if (operationInfo == null) {
            continue;
          }
          container.add(operationInfo.getRequiredElements());
        }
      }

      // download it
      try {
        lbl.setText("Downloading trojan horses. yeah..");
        System.out.println("download");
        ProgressHandle downloadHandle = ProgressHandleFactory.createHandle("dummy-download-handle");
        InstallSupport support = container.getSupport();
        Validator dlValidator = support.doDownload(downloadHandle, false, true);

        System.out.println("checking installer");
        lbl.setText("Double checking those horses");
        ProgressHandle validateHandle = ProgressHandleFactory.createHandle("dummy-validate-handle");
        org.netbeans.api.autoupdate.InstallSupport.Installer installer = support.doValidate(dlValidator, validateHandle);

        System.out.println("replacing modules");
        lbl.setText("Replacing old horses");
        ProgressHandle installHandle = ProgressHandleFactory.createHandle("dummy-install-handle");
        Restarter restart = support.doInstall(installer, installHandle);

        if (restart != null) {
          lbl.setText("New trojan horses in place. rebooting..");
          support.doRestart(restart, installHandle);
        }

      } catch (OperationException oe) {
        JOptionPane.showMessageDialog(null, oe.getMessage(), "dun dun dunnn...", JOptionPane.ERROR_MESSAGE);
      }

    }

  }

}
