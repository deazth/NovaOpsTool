/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ops.com;

/**
 * Contain all constants used within this program
 * 
 * @author S53788
 */
public class constant {

  
  // return values
  public static final int RET_MISSING_FILE = 101;
  // return values : exception
  public static final int RET_PARSE_EXCEPTION = 201;
  public static final int RET_SQL_EXCEPTION = 202;
  // file move option
  public static final int MOVE_OVERWRITE = 10001;
  public static final int MOVE_NEW_NAME = 10002;
  public static final int MOVE_NO_OVERWRITE = 10003;

  // run mode
  public static final int RUN_DEBUG = 0;
  public static final int RUN_ALL = 1;
  public static final int RUN_BILL = 2;
  public static final int RUN_PYMT = 3;
  public static final int RUN_ALMOST_DUE_3 = 4;
  public static final int RUN_ALMOST_DUE_1 = 5;
  public static final int RUN_PASS_DUE_1 = 6;

  // misc
  public static final String FILE_SEPARATOR = System.getProperty("file.separator");
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  // log type
  public static final int MESSAGE   = 0;
  public static final int ERROR   = 1;
  public static final int WARNING = 2;
  public static final int DEBUG   = 3;
  
  
  // connections
  public static final String dbConApps = "jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST=10.41.23.141)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.41.23.141)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=HBRMPRD)))";
  public static final String dbConUser = "pin";
  public static final String dbConPass = "Npin_123";
  
  public static final String dbCloneApps = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.41.23.127)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=HBRMPRD)))";

  public static final String dbConEAI = "jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST=10.41.23.148)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.41.23.148)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=HEAIPRD)))";
  public static final String dbEAIUser = "NOVA_READ";
  public static final String dbEAIPass = "NOVA_READ123";
  
  public static final String dbConSBL = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.41.23.127)(PORT=1521)))(CONNECT_DATA=(FAILOVER_MODE=(TYPE=session)(METHOD=basic)(RETRIES=20)(DELAY=5))(SERVER=dedicated)(SERVICE_NAME=HSBLPRD)))";
  public static final String dbSBLUser = "operate_read";
  public static final String dbSBLPass = "operate_read";
  
  public static final String dbStgCon = "jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST=10.41.23.172)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.41.23.173)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=HSTGPRD)))";
  public static final String dbStgUser = "tmpin_batch";
  public static final String dbStgPass = "Ntp_123";
}
