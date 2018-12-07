/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bsm.nova.ops;

import com.portal.pcm.EBufException;
import com.portal.pcm.FList;
import com.portal.pcm.Poid;
import com.portal.pcm.SparseArray;
import com.portal.pcm.fields.FldDescr;
import com.portal.pcm.fields.FldEndT;
import com.portal.pcm.fields.FldPoid;
import com.portal.pcm.fields.FldProgramName;
import com.portal.pcm.fields.FldServiceObj;
import com.portal.pcm.fields.FldStatus;
import com.portal.pcm.fields.FldStatusFlags;
import com.portal.pcm.fields.FldStatuses;
import java.util.Date;

/**
 *
 * @author S53788
 */
public class RtnObj {

  private long bapoid;
  private long svcpoid;
  private long date;
  private String svctype;

  public RtnObj(long ba, long svc, String stype, long d_t) {
    bapoid = ba;
    svcpoid = svc;
    date = d_t;
    svctype = stype;
  }

  /*
   0 PIN_FLD_POID                 POID [0] 0.0.0.1 /account 20718226007
   0 PIN_FLD_SERVICE_OBJ POID          [0] 0.0.0.1 /service/tm_iptv 20750241852
   0 PIN_FLD_END_T                TSTAMP [0]  (1511539200)
   0 PIN_FLD_PROGRAM_NAME          STR [0] "Testnap"
   0 PIN_FLD_DESCR                 STR [0] "Recovery_RTN"
   0 PIN_FLD_STATUSES            ARRAY [0] allocated 20, used 2
   1     PIN_FLD_STATUS_FLAGS      INT [0] 4
   1     PIN_FLD_STATUS           ENUM [0] 10100
  
  
   */
  public FList getInputFlist() throws EBufException {
    String user = System.getProperty("user.name");

    String inputFlist = "0 PIN_FLD_POID       POID [0] 0.0.0.1 /account " + bapoid + " 1\n"
            + "0 PIN_FLD_SERVICE_OBJ          POID [0] 0.0.0.1 " + svctype + " " + svcpoid + " 1\n"
            + "0 PIN_FLD_PROGRAM_NAME          STR [0] \"OpsTool BulkRTN\"\n"
            + "0 PIN_FLD_END_T              TSTAMP [0] (" + date + ")\n"
            + "0 PIN_FLD_DESCR                 STR [0] \"Recovery_RTN_" + user + "\"\n"
            + "0 PIN_FLD_STATUSES            ARRAY [0] allocated 20, used 2\n"
            + "1     PIN_FLD_STATUS_FLAGS      INT [0] 4\n"
            + "1     PIN_FLD_STATUS           ENUM [0] 10100";

    return FList.createFromString(inputFlist);
  }

}
