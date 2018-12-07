/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ops.com;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author S53788
 */
public class dFileWriter {

//  private FileOutputStream fos;
  private final FileWriter fw;
  private final BufferedWriter bw;
//  private PrintStream ps;
  private final String inFileName;
  private final String FILE_SEPARATOR;
  private final String LINE_SEPARATOR;

  public dFileWriter(String filename) throws IOException {
    this.FILE_SEPARATOR = System.getProperty("file.separator");
//    this.LINE_SEPARATOR = System.getProperty("line.separator");
    this.LINE_SEPARATOR = "\n";
//    fos = new FileOutputStream(filename, true);
//    ps = new PrintStream(fos);

    fw = new FileWriter(filename, true);
    bw = new BufferedWriter(fw);

    Utilities.log("file writer", "Writer init: " + filename, 0);

    inFileName = filename;
  }
  
  public dFileWriter(File infile) throws IOException{
    this.FILE_SEPARATOR = System.getProperty("file.separator");
//    this.LINE_SEPARATOR = System.getProperty("line.separator");
    this.LINE_SEPARATOR = "\n";
    
    fw = new FileWriter(infile, true);
    bw = new BufferedWriter(fw);
    
    inFileName = infile.getName();

    Utilities.log("file writer", "Writer init: " + inFileName, 0);

  }

  public dFileWriter(String filename, boolean append) throws IOException {
    this.FILE_SEPARATOR = System.getProperty("file.separator");
//    this.LINE_SEPARATOR = System.getProperty("line.separator");
    this.LINE_SEPARATOR = "\n";
//    fos = new FileOutputStream(filename, true);
//    ps = new PrintStream(fos);

    fw = new FileWriter(filename, append);
    bw = new BufferedWriter(fw);

    Utilities.log("file writer", "Writer init: " + filename, 0);

    inFileName = filename;
  }

  public void println(String text) throws IOException {
    //ps.println(text);
    bw.write(text);
    bw.newLine();
  }

  public FileWriter getPrintStream() {
    return fw;
  }

  public void print(String text) throws IOException {
//    ps.print(text);
    bw.write(text);
  }

  public int flush(String destination) throws IOException {
//    ps.close();
//    fos.close();

    bw.close();
    fw.close();

    if (destination.equals("-1")) {
      return 0;
    }

    File in = new File(inFileName);
    if (in.length() == 0) {
      return 0;
    }
    String fname = in.getName();

    if (!destination.endsWith(FILE_SEPARATOR)) {
      destination += FILE_SEPARATOR;
    }

    if (!in.renameTo(new File(destination + fname))) {
      System.err.println("Fail to move file : " + inFileName);
      Utilities.log("file writer", "Fail to move file : " + inFileName, 1);
      return 1;
    }
    return 0;
  }

  public static String padd_those_toleft(String text, int start, int end, String need_space) {
    int space = end - start + 1;

    if (text.length() > space) {
      return text.substring(0, space);
    }

    int remaning_space = space - text.length();

    String output = text;

    for (int i = 0; i < remaning_space; i++) {
      output += need_space;
    }

    return output;

  }

  public static String padd_those_toright(String text, int start, int end, String need_space) {
    int space = end - start + 1;

    if (text.length() > space) {
      return text.substring(0, space);
    }

    int remaning_space = space - text.length();

    String output = text;

    for (int i = 0; i < remaning_space; i++) {
      output = need_space + output;

    }

    return output;

  }
}
