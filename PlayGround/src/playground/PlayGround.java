/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package playground;

import com.jcraft.jsch.JSch;
import com.portal.pcm.PortalContext;
import javax.swing.JOptionPane;

/**
 *
 * @author amer
 */
public class PlayGround {

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    // TODO code application logic here

  //    db();
  //ldap();
  //    ssh();
    cmplay();

  }

  private static void db() {
    maieskiuel aa = new maieskiuel();

    aa.readDB();

  }

  private static void ldap() {
    try {

      IshieldLdap ldp = new IshieldLdap("s53788", "Awesom01");

      System.out.println("Check if user exist");
      if (ldp.isUserExist()) {
        System.out.println("Check if user in group");
        if (ldp.userInGroup()) {
          System.out.println("authenticate password");
          if (ldp.authenticate()) {
            JOptionPane.showMessageDialog(null, "AUthorized", "Error authenticating user", JOptionPane.ERROR_MESSAGE);

          }
        } else {
          JOptionPane.showMessageDialog(null, "Not authorized", "Error authenticating user", JOptionPane.ERROR_MESSAGE);
        }
      } else {
        JOptionPane.showMessageDialog(null, "Not exist", "Error authenticating user", JOptionPane.ERROR_MESSAGE);
      }

    } catch (Exception e) {
    }
  }

  private static void ssh() {
    SSHManager sm = new SSHManager("S53788", "Awesom01", "10.41.24.82", "");

    String err = sm.connect();

    if (err != null) {
      System.err.println(err);
      return;
    }

//    exec(sm, "/home/S53788/check_all.sh");
    String aa = sm.sendCommand("/home/S53788/check_all.sh");

    System.out.println(aa);

    sm.close();

  }

  private static void exec(SSHManager sm, String cmd) {
    System.out.println(sm.sendCommand(cmd));
  }

  private static void cmplay() {
    connectTo("pcp://root.0.0.0.1:password@10.41.22.238:11960/service/admin_client 1");
    
    connectTo("pcp://root.0.0.0.1:password@10.41.22.239:11960/service/admin_client 1");
    
//    connectTo("pcp://root.0.0.0.1:password@10.41.22.34:11959/service/admin_client 1");

  }

  
  private static void connectTo(String cmptr) {
    try {
      PortalConnectionManager.changeCmPtr(cmptr);
      PortalContext pc = PortalConnectionManager.getInstance().getConnection();
      System.out.println("Connected to " + pc.getHost() + ":" + pc.getPort());
      pc.close(true);
      
      // try to switch the 
    } catch (Exception e) {
      e.printStackTrace();
    }
    
  }

}
