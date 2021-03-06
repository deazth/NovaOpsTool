/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ops.com;

/**
 *
 * @author TM30152
 */
import com.portal.pcm.EBufException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.portal.pcm.FList;
import com.portal.pcm.PortalContext;
import com.portal.pcm.PortalOp;
import java.io.FileInputStream;
import org.apache.log4j.PropertyConfigurator;

public class PortalConnectionManager {

  private final static Logger LOGGER = Logger.getLogger(PortalConnectionManager.class.getName());
  private static final PortalConnectionManager pcm = new PortalConnectionManager();
  private static final String NEW_LINE = System.getProperty("line.separator");
  private static boolean inited = false;
  private static Properties configProp;
  //private PortalContext ctx;
  private FList output;

  private PortalConnectionManager() {
    init();
  }

  public static PortalConnectionManager getInstance() {
    return pcm;
  }

  private void init() {
    inited = true;
    Utilities.log("PCM", "Init PCM", constant.DEBUG);
    LOGGER.debug("Entering init() method");
    try {
//      PropertyConfigurator.configure(getClass().getClassLoader().getResource("/files/log4j.properties"));
      InputStream in = getClass().getResourceAsStream("/files/Infranet.properties");
      //getClass().getResourceAsStream("/files/Infranet.properties");
      configProp = new Properties();
      configProp.load(in);
    } catch (Exception e) {
      e.printStackTrace();
      LOGGER.error(e.getMessage());
      System.exit(1);
    }
    LOGGER.debug("Leaving init() method");
  }

  //private PortalContext ctx = null;
  public PortalContext getConnection() throws EBufException {
    LOGGER.debug("Entering getConnection() method");
    PortalContext ctx = null;
    try {
      ctx = new PortalContext(configProp);
      ctx.connect();
      Utilities.log("PCM", "Connected to " + ctx.getUserName() + "@" + ctx.getHost() + ":" + ctx.getPort(), constant.DEBUG);
      LOGGER.info("PortalContext: " + ctx);

    } catch (EBufException e) {
      e.printStackTrace();
      LOGGER.error(e.getMessage());
      throw e;
    }
    LOGGER.debug("Leaving getConnection() method");
    return ctx;
  }

  public void closeConnection(PortalContext ctx) {
    //public void closeConnection() {
    LOGGER.debug("Entering closeConnection() method");
    try {
      if (ctx != null) {
        ctx.close(true);
        LOGGER.info("PortalContext: " + ctx + " CLOSED.");
      }
    } catch (Exception e) {
      e.printStackTrace();
      LOGGER.error(e.getMessage());
    }
    LOGGER.debug("Leaving closeConnection() method");
  }

  public boolean runOpcode(PortalContext ctx, FList input, int opId, int opFlag) {
    boolean bFlag = true;
    try {
      LOGGER.debug(input.asString());
      LOGGER.debug("runOpcode: " + PortalOp.opToString(opId));
      output = new FList();
      output = ctx.opcode(opId, opFlag, input);
      if (!output.isEmpty()) {
        LOGGER.debug("Output flist is NOT empty.");
        //LOGGER.info(output.asString());
      } else {
        LOGGER.warn("Output flist is empty.");
        bFlag = false;
      }
    } catch (Exception e) {
      bFlag = false;
      //e.printStackTrace();
      LOGGER.error(e.getMessage() + PortalConnectionManager.NEW_LINE + input);
    }
    return bFlag;
  }

  public FList runOpcodeWithReturn(PortalContext ctx, FList input, int opId, int opFlag) {
    try {
      LOGGER.debug(input.asString());
      LOGGER.debug("runOpcode: " + PortalOp.opToString(opId));
      output = new FList();
      output = ctx.opcode(opId, opFlag, input);
      if (!output.isEmpty()) {
        LOGGER.debug("Output flist is NOT empty.");
        //LOGGER.info(output.asString());
      } else {
        LOGGER.warn("Output flist is empty.");
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage() + PortalConnectionManager.NEW_LINE + input);
    }
    return output;
  }

  public long getDB(PortalContext ctx) {
    //LOGGER.debug("Database getDB(): " + ctx.getCurrentDB());
    return ctx.getCurrentDB();
    //return 1L;
  }
}
