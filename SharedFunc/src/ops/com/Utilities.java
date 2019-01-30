/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ops.com;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;
import org.openide.windows.*;

/**
 *
 * @author S53788
 */
public class Utilities {

  public static long dateToTS(String date_to_conv, String format)  {

    try {
      DateFormat formatter;
      Date date;
      formatter = new SimpleDateFormat(format);
      date = (Date) formatter.parse(date_to_conv);

      return (date.getTime() / 1000);
    } catch (ParseException e) {
      e.printStackTrace();
      log("Log","bad input. expected format: " + format + ". received input: " + date_to_conv, 1);
    }

    return 0;
  }

  public static String tsToDate(long timestamp, String date_format) {
    Date d = new Date(timestamp * 1000);
    SimpleDateFormat sdf = new SimpleDateFormat(date_format);

    return sdf.format(d);
  }

  public static String tsToDateNow(String date_format) {
    Date d = new Date(System.currentTimeMillis());
    SimpleDateFormat sdf = new SimpleDateFormat(date_format);

    return sdf.format(d);
  }

  /**
   * Print the line to an output window
   *
   * @param line
   * @param mode 1 = error, others normal
   */
  public static void log(String caller, String line, int mode) {
    
//    if(mode < 1){
//      return;
//    }
    
    InputOutput io = IOProvider.getDefault().getIO(caller, false);
    if (mode == 1) {
      io.getErr().println(line);
      io.getErr().close();
//      io.select();
    } else {
      io.getOut().println(line);
      io.getOut().close();
    }

    

  }
  
  public static void logStack(String caller, Exception e){
    InputOutput io = IOProvider.getDefault().getIO(caller, false);
    e.printStackTrace(io.getErr());
    io.select();
  }
  
  public static void popup(String msg){
    JOptionPane.showMessageDialog(null, msg, "Alert", JOptionPane.INFORMATION_MESSAGE);
  }

}
