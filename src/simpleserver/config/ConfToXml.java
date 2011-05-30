/*
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package simpleserver.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;

class ConfToXml {

  private boolean allFilesOk = false;
  private String confpath = "simpleserver";
  private String backupdir = confpath + "/oldconf";

  private void failmsg(String message) {
    allFilesOk = false;
    System.out.println(message);
  }

  private boolean exists(String path) {
    return (new File(path)).exists();
  }

  private boolean checkOldFiles() {
    allFilesOk = true;

    if (!exists(confpath)) {
      failmsg("ERROR: Could not find config directory!\n"
          + "You have to run the script from the server directory!\n"
          + "(The directory where SimpleServer.jar is located)");
    }

    String[] files = { "group-list.txt", "member-list.txt", "ip-member-list.txt",
        "command-list.txt", "block-list.txt", "kit-list.txt" };

    for (String file : files) {
      if (!exists(confpath + "/" + file)) {
        failmsg("ERROR: Could not find " + file);
      }
    }

    return allFilesOk;
  }

  private ArrayList<String> readLines(String filename) throws Exception {
    ArrayList<String> lines = new ArrayList<String>();

    BufferedReader in = new BufferedReader(new FileReader(filename));

    String line = "";
    while ((line = in.readLine()) != null) {
      if (line.charAt(0) != '#') {
        lines.add(line);
      }
    }

    in.close();

    Collections.sort(lines);

    return lines;
  }

  private boolean writeFile(String filename, String data) {
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter(filename));
      out.write(data);
      out.close();
    } catch (Exception e) {
      System.err.println("ERROR: " + e.getMessage());
      return false;
    }
    return true;
  }

  private boolean backupFile(String filename, String newname) {
    File f1 = new File(filename);
    File f2 = new File(newname);
    boolean success = f1.renameTo(f2);
    return success;
  }

  public static String joinStrings(String[] strs, String delim) {
    String ret = "";

    if (strs == null || strs.length == 0) {
      return "";
    }

    if (strs.length == 1) {
      return strs[0];
    }

    for (int i = 0; i < (strs.length - 1); i++) {
      ret += strs[i] + delim;
    }
    ret += strs[strs.length - 1];

    return ret;
  }

  // replace seperators in kit-list.txt from , to | and from ; to ,
  private boolean updateKitlist() {
    ArrayList<String> kitlines = null;
    try {
      kitlines = readLines(confpath + "/kit-list.txt");
    } catch (Exception e) {
      System.out.println("ERROR: Could not read kit-list.txt!");
      return false;
    }

    // Skip conversion if it is already done
    for (String line : kitlines) {
      if (line.indexOf("|") != -1) {
        return true;
      }
    }

    if (!backupFile(confpath + "/kit-list.txt", backupdir + "/kit-list.txt")) {
      System.out.println("ERROR: Could not backup old kit-list.txt");
      return false;
    }

    String newkits = "";
    for (String line : kitlines) {
      line = joinStrings(line.split(","), "|");
      line = joinStrings(line.split(";"), ",");

      newkits += line + "\n";
    }

    if (!writeFile(confpath + "/kit-list.txt", newkits)) {
      System.out.println("ERROR: Could not write new kit-list.txt");
      return false;
    }

    System.out.println("Your kit-list.txt has been updated!");
    return true;
  }

  // Converts single 0 or -1 to -1+/0+ (meaning groups >= x, as intended)
  private String convertIdList(String idlist) {
    String ids[] = idlist.split(",");
    for (int i = 0; i < ids.length; i++) {
      if (ids[i].equals("0") || ids[i].equals("-1")) {
        ids[i] += "+";
      }
    }

    return joinStrings(ids, ",");
  }

  // Generate permissions.xml from other old config files
  private boolean generatePermissionsXml() {
    ArrayList<String> groups = null;
    ArrayList<String> members = null;
    ArrayList<String> ipmembers = null;
    ArrayList<String> commands = null;
    ArrayList<String> blocks = null;

    // load old config file lines
    try {
      groups = readLines(confpath + "/group-list.txt");
      members = readLines(confpath + "/member-list.txt");
      ipmembers = readLines(confpath + "/ip-member-list.txt");
      commands = readLines(confpath + "/command-list.txt");
      blocks = readLines(confpath + "/block-list.txt");
    } catch (Exception e) {
      System.out.println("ERROR: Could not load one or more of your config files!");
      System.err.println("Exception message: " + e.getMessage());

      return false;
    }

    // parse old configs and generate xml entries

    String groupxml = "";
    String memberxml = "";
    String commandxml = "";
    String blockxml = "";

    for (String group : groups) {
      String id = group.split("=")[0];
      String[] rest = group.split("=")[1].split(",");

      groupxml += "    <group id=\"" + id + "\" name=\"" + rest[0]
          + "\" color=\"" + rest[3] + "\" showTitle=\"" + rest[1]
          + "\" ignoreChestlocks=\"" + rest[2] + "\" />\n";
    }

    for (String player : members) {
      String[] tmp = player.split("=");
      memberxml += "    <player name=\"" + tmp[0] + "\" group=\"" + tmp[1] + "\" />\n";
    }
    for (String ip : ipmembers) {
      String[] tmp = ip.split("=");
      memberxml += "    <ip address=\"" + tmp[0] + "\" group=\"" + tmp[1] + "\" />\n";
    }

    for (String command : commands) {
      String cmd = command.split("=")[0];
      String ids = "";
      String aliases = "";

      if (command.indexOf(";") != -1) {
        aliases = command.split("=")[1].split(";")[0];
        ids = convertIdList(command.split("=")[1].split(";")[1]);
      } else {
        ids = convertIdList(command.split("=")[1]);
      }

      commandxml += "      <command name=\"" + cmd + "\" ";
      if (!aliases.equals("")) {
        commandxml += "aliases=\"" + aliases + "\" ";
      }
      commandxml += "allow=\"" + ids + "\" />\n";
    }

    // hardcoded addition of !area and !myarea (for user convenience)
    commandxml += "      <command name=\"area\" allow=\"0+\" />\n";
    commandxml += "      <command name=\"myarea\" allow=\"2+\" />\n";

    for (String block : blocks) {
      String[] tmp = block.split("=");
      blockxml += "      <block id=\"" + tmp[0]
          + "\" allow=\"" + convertIdList(tmp[1]) + "\" />\n";
    }

    // bring everything together
    String xmldata = "";
    xmldata += "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n";
    xmldata += "<!DOCTYPE config SYSTEM \"permissions.dtd\">\n";
    xmldata += "<config>\n";
    xmldata += "<!-- AUTOMATICALLY GENERATED BY conf2xml.jar -->\n";
    xmldata += "<!-- See the default configuration to get information about the different elements -->\n\n";
    xmldata += "  <groups>\n";
    xmldata += groupxml;
    xmldata += "  </groups>\n\n";
    xmldata += "  <members>\n";
    xmldata += memberxml;
    xmldata += "  </members>\n\n";
    xmldata += "  <permissions>\n    <commands>\n";
    xmldata += commandxml;
    xmldata += "    </commands>\n\n    <blocks allowPlace=\"0+\" allowDestroy=\"0+\" allowUse=\"0+\">\n";
    xmldata += blockxml;
    xmldata += "    </blocks>\n  </permissions>\n\n  <areas>\n  </areas>\n</config>\n";

    // write file

    if (exists(confpath + "/permissions.xml")) {
      System.out.println("permissions.xml already exists! Renamed to permissions.xml.bak!");
      backupFile(confpath + "/permissions.xml", confpath + "/permissions.xml.bak");
    }

    if (!writeFile(confpath + "/permissions.xml", xmldata)) {
      System.out.println("ERROR: Could not write permissions.xml");
      return false;
    }

    System.out.println("[INFO] Your permissions.xml has been generated!");
    return true;
  }

  public boolean convertFiles() {
    if (!checkOldFiles()) {
      return false;
    }

    (new File(backupdir)).mkdir(); // create dir where the backups go

    if (!updateKitlist()) {
      return false;
    }

    if (!generatePermissionsXml()) {
      return false;
    }

    backupFile(confpath + "/group-list.txt", backupdir + "/group-list.txt");
    backupFile(confpath + "/member-list.txt", backupdir + "/member-list.txt");
    backupFile(confpath + "/ip-member-list.txt", backupdir + "/ip-member-list.txt");
    backupFile(confpath + "/command-list.txt", backupdir + "/command-list.txt");
    backupFile(confpath + "/block-list.txt", backupdir + "/block-list.txt");

    System.out.println("\tYour old files have been saved in " + backupdir);
    System.out.println("\tYou can delete them if you want.");

    return true;
  }

  public static void main(String[] args) {
    new ConfToXml().convertFiles();
  }

}
