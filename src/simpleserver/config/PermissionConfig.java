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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import simpleserver.Coordinate;
import simpleserver.Coordinate.Dimension;
import simpleserver.Group;
import simpleserver.Player;
import simpleserver.Server;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

class DTDEntityResolver implements EntityResolver {
  PermissionConfig c;

  public DTDEntityResolver(PermissionConfig c) {
    this.c = c;
  }

  public InputSource resolveEntity(String publicId, String systemId) {
    return new InputSource(c.getDTD());
  }
}

class DTDErrorHandler implements ErrorHandler {
  private final String prefix = "[WARNING][permissions.xml] ";
  private PermissionConfig parent = null;

  public DTDErrorHandler(PermissionConfig p) {
    parent = p;
  }

  private void printExceptionInfo(SAXParseException e) {
    System.out.println(prefix + "L. " + e.getLineNumber() + ": " + e.getMessage());
    parent.loadsuccess = false;
  }

  public void warning(SAXParseException e) {
    printExceptionInfo(e);
  }

  public void error(SAXParseException e) {
    printExceptionInfo(e);
  }

  public void fatalError(SAXParseException e) {
    printExceptionInfo(e);
  }
}

@SuppressWarnings("unchecked")
public class PermissionConfig extends AbstractConfig {
  private Server server = null;

  private boolean isDefault = false; // true if this is loaded from defaults
  private PermissionConfig permDefaults = null; // contains defaults

  public boolean loadsuccess = true;

  private XMLConfiguration config;

  public PermissionConfig(Server server) {
    super("permissions.xml");
    this.server = server;
    isDefault = false;

    permDefaults = new PermissionConfig(server, true);
    // load defaults in the background
  }

  public PermissionConfig(Server server, boolean isDefault) {
    super("permissions.xml");
    this.server = server;
    this.isDefault = isDefault;

    loadDefaults();
  }

  public InputStream getDTD() {
    return super.getClass().getResourceAsStream(resourceLocation + "/permissions.dtd");
  }

  // prepare a new XMLConfiguration Object for loading
  private XMLConfiguration initConf(boolean validate) {
    XMLConfiguration conf = new XMLConfiguration();

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = null;
    try {
      dbf.setValidating(validate);
      db = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
    }

    if (db != null) {
      db.setEntityResolver(new DTDEntityResolver(this));
      db.setErrorHandler(new DTDErrorHandler(this));

      conf.setDocumentBuilder(db);
    }

    conf.setExpressionEngine(new XPathExpressionEngine());
    conf.setDelimiterParsingDisabled(true);

    return conf;
  }

  // replacement for GroupList.groupExists
  public boolean groupExists(int id) {
    List ids = config.getList("/groups/group/@id");
    return ids.contains(String.valueOf(id));
  }

  // replacement for GroupList.getGroup
  public Group getGroup(int id) {
    if (!groupExists(id)) {
      return null;
    }

    String pathpart = "/groups/group[@id='" + id + "']/@";
    String name = config.getString(pathpart + "name", "");
    String showtitle = config.getString(pathpart + "showTitle", "");
    String isadmin = config.getString(pathpart + "ignoreChestlocks", "");
    String color = config.getString(pathpart + "color", "");
    String forwardsCommands = config.getString(pathpart + "forwardUnknownCommands", "");
    String warmup = config.getString(pathpart + "warmup", "");
    String cooldown = config.getString(pathpart + "cooldown", "");

    /* set defaults for missing attributes */
    if (name.equals("")) {
      name = "NamelessGroup";
    }
    if (color.equals("")) {
      color = "f";
    }
    if (showtitle.equals("")) {
      showtitle = "false";
    }
    if (isadmin.equals("")) {
      isadmin = "false";
    }
    if (forwardsCommands.equals("")) {
      forwardsCommands = "true";
    }
    if (warmup.equals("")) {
      warmup = "0";
    }
    if (cooldown.equals("")) {
      cooldown = "0";
    }

    return new Group(name, Boolean.valueOf(showtitle), Boolean.valueOf(isadmin), color, Boolean.valueOf(forwardsCommands),
                     Integer.valueOf(warmup), Integer.valueOf(cooldown));
  }

  private int getIPGroup(String ipAddress) {
    String group = "";

    group = config.getString("/members/ip[@address='" + ipAddress + "']/@group", "");

    if (group.equals("")) {
      return server.options.getInt("defaultGroup");
    }

    return Integer.valueOf(group);
  }

  private String xpath_lcase(String attr) {
    return "translate(" + attr + ",'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')";
  }

  public int getNameGroup(String name) {
    name = name.toLowerCase();
    String group = "";

    group = config.getString("/members/player[" + xpath_lcase("@name") + "='" + escape(name) + "']/@group", "");

    if (group.equals("")) {
      return server.options.getInt("defaultGroup");
    }

    return Integer.valueOf(group);
  }

  // replacement for (IP)MemberList.getGroup and does the former work of
  // Player.updateGroup
  public int getPlayerGroup(Player player) {
    return getPlayerGroup(player.getName(), player.getIPAddress());
  }

  private int getPlayerGroup(String name, String ip) {
    int grp = 0;

    int nameGroup = getNameGroup(name);
    int ipGroup = getIPGroup(ip);

    if (ipGroup > nameGroup) {
      grp = ipGroup;
    } else {
      grp = nameGroup;
    }

    return grp;
  }

  // replacement for MemberList.setGroup
  public void setPlayerGroup(Player player, int group) {
    setPlayerGroup(player.getName(), group);
  }

  public void setPlayerGroup(String name, int group) {
    name = name.toLowerCase();
    String val = config.getString("/members/player[" + xpath_lcase("@name") + "='" + escape(name) + "']/@group", "");
    if (val.equals("")) {
      config.addProperty("/members player@name", name);
      config.addProperty("/members/player[@name='" + escape(name) + "'][1] @group", "");
    }

    config.setProperty("/members/player[@name='" + escape(name) + "']/@group", String.valueOf(group));
    server.updateGroup(name);
    save();
  }

  public void setIPGroup(String ip, int group) {
    String val = config.getString("/members/ip[" + xpath_lcase("@address") + "='" + ip + "']/@group", "");
    if (val.equals("")) {
      config.addProperty("/members ip@address", ip);
      config.addProperty("/members/ip[@address='" + ip + "'][1] @group", "");
    }

    config.setProperty("/members/ip[@address='" + ip + "']/@group", String.valueOf(group));
    server.updateGroups();
    save();
  }

  // replaces Group.isMember with nickname permissions
  public boolean includesPlayer(String allowstr, Player player) {
    if (allowstr == null || allowstr.equals("")) {
      return false;
    }

    String[] str = allowstr.split(";");
    boolean isgrpmember = false;
    if (!str[0].equals("") && !str[0].equals("-")) {
      isgrpmember = parseGroups(str[0]).contains(player.getGroupId());
    }

    if (str.length < 2) {
      return isgrpmember;
    }

    String[] nicks = str[1].split(",");
    for (int i = 0; i < nicks.length; i++) {
      nicks[i] = nicks[i].toLowerCase();
    }

    return isgrpmember || Arrays.asList(nicks).contains(player.getName().toLowerCase());
  }

  // moved from class Group, parses ranges and listings of groups
  private ImmutableSet<Integer> parseGroups(String idString) {
    String[] segments = idString.split(",");
    ImmutableSortedSet.Builder<Integer> groups = ImmutableSortedSet.naturalOrder();
    for (String segment : segments) {
      if (segment.matches("^\\s*$")) {
        continue;
      }

      try {
        groups.add(Integer.valueOf(segment));
        continue;
      } catch (NumberFormatException e) {
      }

      if (segment.indexOf('+') == segment.length() - 1) {
        int num = 0;
        try {
          num = Integer.valueOf(segment.split("\\+")[0]);
        } catch (NumberFormatException e) {
          System.out.println("Unable to parse group: " + segment);
          continue;
        }
        List ids = config.getList("/groups/group/@id");
        for (int j = 0; j < ids.size(); j++) {
          int n = Integer.valueOf(ids.get(j).toString());
          if (n >= num) {
            groups.add(n);
          }
        }
        continue;
      }

      String[] range = segment.split("-");
      if (range.length != 2) {
        System.out.println("Unable to parse group: " + segment);
        continue;
      }

      int low;
      int high;
      try {
        low = Integer.valueOf(range[0]);
        high = Integer.valueOf(range[1]);
      } catch (NumberFormatException e) {
        System.out.println("Unable to parse group: " + segment);
        continue;
      }

      if (low > high) {
        System.out.println("Unable to parse group: " + segment);
        continue;
      }

      for (int k = low; k <= high; ++k) {
        groups.add(k);
      }
    }

    return groups.build();
  }

  public boolean forwardsUnknownCommands(Player player) {
    return player.getGroup().getForwardsCommands();
  }

  public boolean canOpenChests(Player player, Coordinate coordinate) {
    boolean allowed = true;
    String pathpart = "/permissions/blocks/";

    String permstr = config.getString(pathpart + "chests/@allow");
    if (permstr != null && !permstr.equals("")) {
      allowed = includesPlayer(permstr, player);
    }

    String xpath = getAreanodeForCoordinate(coordinate);
    String[] areas = getAllAreasFromAreaXPath(xpath);

    for (String area : areas) {
      String areapath = "//area[@name='" + area + "']";

      permstr = config.getString(areapath + pathpart + "chests/@allow");
      if (permstr != null && !permstr.equals("")) {
        allowed = includesPlayer(permstr, player);
      }
    }

    return allowed;
  }

  // replacement for BlockList.playerAllowed
  // return: [place, destroy, use, take]
  public boolean[] getPlayerBlockPermissions(Player player, Coordinate blockCoord, int bID) {
    boolean[] perms = new boolean[4];

    if (blockCoord == null) {
      blockCoord = player.position();
    }

    ArrayList<String> attrs = new ArrayList<String>();
    attrs.add("allowPlace");
    attrs.add("allowDestroy");
    attrs.add("allowUse");
    attrs.add("allowTake");

    String pathpart = "/permissions/blocks/block[@id='" + String.valueOf(bID) + "']/@";
    String xpath = getAreanodeForCoordinate(blockCoord);
    String[] areas = getAllAreasFromAreaXPath(xpath);

    // get global permissions for all blocks
    for (int i = 0; i < attrs.size(); i++) {
      String permstr = config.getString("/permissions/blocks/@" + attrs.get(i), "");
      if (permstr.equals("")) {
        perms[i] = true;
      } else {
        perms[i] = includesPlayer(permstr, player);
      }
    }

    // get areawide permissions for all blocks
    for (String area : areas) {
      String areapath = "//area[@name='" + area + "']";

      for (int n = 0; n < attrs.size(); n++) {
        String permstr = config.getString(areapath + "/permissions/blocks/@" + attrs.get(n), "");
        if (!permstr.equals("")) {
          perms[n] = includesPlayer(permstr, player);
        }
      }
    }

    // single block tag overrides the areawide allowPlace
    String place_allow = config.getString(pathpart + "allow", "");
    if (!place_allow.equals("")) {
      perms[0] = includesPlayer(place_allow, player);
    }
    String place_disallow = config.getString(pathpart + "disallow", "");
    if (!place_disallow.equals("")) {
      perms[0] = !includesPlayer(place_disallow, player);
    }

    // single block tag overrides the areawide allowPlace
    for (String area : areas) {
      String areapath = "//area[@name='" + area + "']";

      place_allow = config.getString(areapath + pathpart + "allow", "");
      if (!place_allow.equals("")) {
        perms[0] = includesPlayer(place_allow, player);
      }
      place_disallow = config.getString(areapath + pathpart + "disallow", "");
      if (!place_disallow.equals("")) {
        perms[0] = !includesPlayer(place_disallow, player);
      }
    }

    return perms;
  }

  public List getAllCommands() {
    return config.getList("/permissions/commands/command/@name");
  }

  // replacement for CommandList.playerAllowed
  public boolean playerCommandAllowed(String cmd, Player player) {
    boolean allowed = false;
    String pathpart = "/permissions/commands/command[@name='" + escape(cmd) + "']/@";
    String globalpermission = config.getString(pathpart + "allow", "");
    if (!globalpermission.equals("")) {
      allowed = includesPlayer(globalpermission, player);
      String globalrestriction = config.getString(pathpart + "disallow", "");
      if (!globalrestriction.equals("")) {
        allowed = !includesPlayer(globalrestriction, player);
      }
    }

    // get all parent areas and check them incrementally
    String xpath = getAreanodeForCoordinate(player.position());
    String[] areas = getAllAreasFromAreaXPath(xpath);

    for (String area : areas) {
      String path = "//area[@name='" + escape(area) + "']";
      String areaallow = config.getString(path + pathpart + "allow", "");
      String areadisallow = config.getString(path + pathpart + "disallow", "");

      if (!areaallow.equals("")) {
        allowed = includesPlayer(areaallow, player);
      }
      if (!areadisallow.equals("")) {
        allowed = !includesPlayer(areadisallow, player);
      }
    }

    return allowed;
  }

  // replacement for CommandList.getAliases
  public String[] getCommandAliases(String name) {
    String aliasstr = config.getString("/permissions/commands/command[@name='" + escape(name) + "']/@aliases", "");

    if (aliasstr.equals("")) {
      return new String[] {};
    }

    return aliasstr.split(",");
  }

  // replacement for CommandList.lookupCommand
  public String lookupCommand(String name) {
    List cmds = config.getList("/permissions/commands/command/@name");

    if (cmds.contains(name)) {
      return name;
    }

    for (int i = 0; i < cmds.size(); i++) { // alias?
      String cmd = cmds.get(i).toString();
      String[] aliases = getCommandAliases(cmd);

      if (Arrays.asList(aliases).contains(name)) {
        return cmd;
      }
    }

    // not found :(
    return null;
  }

  // add new commands from the defaults to the config
  private void checkNewCommands() {
    List cmds = config.getList("/permissions/commands/command/@name");
    List defcmds = permDefaults.config.getList("/permissions/commands/command/@name");

    for (Object cmdname : defcmds) {
      cmdname = cmdname.toString();
      if (!cmds.contains(cmdname)) {
        config.addProperty("/permissions/commands command@name", cmdname);

        String path = "/permissions/commands/command[@name='" + cmdname + "']";
        String[] attrs = { "@aliases", "@allow", "@hidden", "@forward" };
        for (String attr : attrs) {
          String val = permDefaults.config.getString(path + "/" + attr, "");
          if (!val.equals("")) {
            config.addProperty(path + "[1] " + attr, val);
          }
        }
      }
    }
  }

  // replacement for CommandList.setGroup
  public void setCommandGroup(String cmd, int grp) {
    String val = config.getString("/permissions/commands/command[@name='" + escape(cmd) + "']/@allow", "");
    if (val.equals("")) {
      config.addProperty("/permissions/commands/ command@name", cmd);
      config.addProperty("/permissions/commands/command[@name='" + escape(cmd) + "'][1] @allow", String.valueOf(grp));
    }

    config.setProperty("/permissions/commands/command[@name='" + escape(cmd) + "']/@allow", String.valueOf(grp));
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

  public String getCurrentArea(Player player) {
    return getAreanameForCoordinate(player.position());
  }

  public String getAreanameForCoordinate(Coordinate coord) {
    String node = getAreanodeForCoordinate(coord);
    if (node.equals("")) {
      return "";
    }

    return config.getString(node + "/@name");
  }

  // Returns full XPath to the area node -> contains also names of parent areas
  private String getAreanodeForCoordinate(Coordinate coord) {
    String nodepath = "";

    boolean found = false;
    while (!found) {
      found = true;

      List areas = config.getList(nodepath + "/areas/area/@name");
      List starts = config.getList(nodepath + "/areas/area/@start");
      List ends = config.getList(nodepath + "/areas/area/@end");

      if (areas.size() == 0 || areas.size() != starts.size() || areas.size() != ends.size()) {
        break;
      }

      for (int i = 0; i < areas.size(); i++) {
        if (areaContainsCoordinate(parseCoords(starts.get(i).toString()),
                                   parseCoords(ends.get(i).toString()), coord)) {
          nodepath += "/areas/area[@name='" + escape(areas.get(i).toString()) + "']";
          found = false;
          break;
        }
      }
    }

    return nodepath;
  }

  // input like: /areas/area[@name="outer"]/areas/area[@name="Inner"]
  // output like: ["outer","inner"]
  private String[] getAllAreasFromAreaXPath(String path) {
    if (path.equals("")) {
      return new String[] {};
    }

    ArrayList<String> areas = new ArrayList<String>();

    for (int i = 0; i < path.length(); i++) {
      int start = path.indexOf("'", i);
      if (start == -1) {
        break;
      }
      int end = path.indexOf("'", start + 1);

      areas.add(path.substring(start + 1, end));
      i = end;
    }

    return areas.toArray(new String[areas.size()]);
  }

  private boolean areaContainsCoordinate(Coordinate start, Coordinate end, Coordinate coord) {
    if (start.dimension() != end.dimension()) {
      System.out.println("[SimpleServer] Warning: The dimension values do not match for area!");
      return false;
    }

    if (start.dimension() != coord.dimension() || end.dimension() != coord.dimension()) {
      return false;
    }

    Coordinate max = new Coordinate(Math.max(start.x(), end.x()),
                                    (byte) Math.max(start.y(), end.y()),
                                    Math.max(start.z(), end.z()));
    Coordinate min = new Coordinate(Math.min(start.x(), end.x()),
                                    (byte) Math.min(start.y(), end.y()),
                                    Math.min(start.z(), end.z()));

    if (max.y() == 0) {
      max = max.setY((byte) 127);
    }

    return coord.x() >= min.x() &&
           coord.z() >= min.z() &&
           coord.y() >= min.y() &&
           coord.x() <= max.x() &&
           coord.z() <= max.z() &&
           coord.y() <= max.y();
  }

  private Coordinate parseCoords(String c) {
    if (c == null || c.length() == 0) {
      return null;
    }

    String[] dim = c.split(";");
    String[] coords = dim[0].split(",");
    if (coords.length < 2) {
      return null;
    }

    int i = 0;
    Integer x = Integer.valueOf(coords[i++]);
    byte y = 0;
    if (coords.length == 3) {
      y = (byte) Integer.valueOf(coords[i++]).intValue();
    }
    Integer z = Integer.valueOf(coords[i]);

    Dimension dimension = Dimension.EARTH;
    if (dim.length == 2) {
      dimension = Dimension.get(dim[1]);
      if (dimension == Dimension.LIMBO) {
        dimension = Dimension.EARTH;
        System.out.println("[SimpleServer] Warning: The dimension value was invalid for an area!");
      }
    }

    return new Coordinate(x, y, z, dimension);
  }

  public void createPlayerArea(Player player) {
    String name = player.getName().toLowerCase();

    if (playerHasArea(player)) {
      return;
    }

    config.addProperty("/areas area@owner", name);
    String path = "/areas/area[@owner='" + escape(name) + "']";

    config.addProperty(path + "[1] @name", name + "'s private area");
    config.addProperty(path + "[1] @start", player.areastart.x() + "," + player.areastart.z() + ";" + player.areastart.dimension());
    config.addProperty(path + "[1] @end", player.areaend.x() + "," + player.areaend.z() + ";" + player.areaend.dimension());

    config.addProperty(path + "[1] permissions/blocks@allowPlace", ";" + name);
    config.addProperty(path + "[1]/permissions/blocks @allowDestroy", ";" + name);
    config.addProperty(path + "[1]/permissions/blocks @allowUse", ";" + name);
    config.addProperty(path + "[1]/permissions/blocks @allowTake", ";" + name);
    config.addProperty(path + "[1]/permissions/blocks chests@allow", ";" + name);

    save();
  }

  public void removePlayerArea(Player player) {
    String name = player.getName().toLowerCase();
    if (!playerHasArea(player)) {
      return;
    }

    String path = "/areas/area[@owner='" + escape(name) + "']";
    config.clearTree(path);

    if (config.getList("/areas/area/@name").size() == 0) {
      config.addProperty("/ areas", " "); // add areas tag back
    }

    save();
  }

  public void renamePlayerArea(Player player, String label) {
    String name = player.getName().toLowerCase();
    if (!playerHasArea(player)) {
      return;
    }

    String path = "/areas/area[@owner='" + escape(name) + "']";
    config.setProperty(path + "/ @name", label);

    save();
  }

  public boolean hasAreaWithName(String label) {
    return !config.getString("/areas/area[@name='" + escape(label) + "']/@name", "").equals("");
  }

  public boolean playerHasArea(Player player) {
    String name = player.getName().toLowerCase();
    return !config.getString("/areas/area[@owner='" + escape(name) + "']/@owner", "").equals("");
  }

  public boolean commandShouldBeExecuted(String cmd) {
    String val = config.getString("/permissions/commands/command[@name='" + escape(cmd) + "']/@forward", "");
    if (val.equals("only")) {
      return false;
    }
    return true;
  }

  public boolean commandShouldPassThroughToMod(String cmd) {
    String val = config.getString("/permissions/commands/command[@name='" + escape(cmd) + "']/@forward", "");
    if (val.equals("true") || val.equals("only")) {
      return true;
    }
    return false;
  }

  public boolean commandIsHidden(String cmd) {
    String val = config.getString("/permissions/commands/command[@name='" + escape(cmd) + "']/@hidden", "");
    if (val.equals("true")) {
      return true;
    }
    return false;
  }

  @Override
  protected void loadHeader() {
    // No header for XML config required
  }

  @Override
  public void load() {
    if (isDefault) {
      return; // should not be called in default config!
    }

    XMLConfiguration confbuff = null;

    try {
      // load stored config
      loadsuccess = true;
      InputStream stream = new FileInputStream(getFile());
      confbuff = initConf(true);
      confbuff.load(stream);

      if (!loadsuccess) {
        loadsuccess = true;
        System.out.println("Trying to load permissions.xml ignoring DTD warnings...");
        stream = new FileInputStream(getFile());
        confbuff = initConf(false);
        confbuff.load(stream);
      }

      config = confbuff; // No problems loading -> set loaded config as real
      // config
      checkNewCommands(); // append new commands found in default to current
      // config
    }

    catch (FileNotFoundException e) {
      System.out.println("Trying to convert old configuration files...");
      if (new ConfToXml().convertFiles()) {
        server.kits.load();
        load();

      } else {
        System.out.println(getFilename() + " is missing.  Loading defaults.");
        loadDefaults();
        save();
      }
    }

    catch (ConfigurationException e) {
      System.out.println("[SimpleServer] " + e);
      System.out.println("[SimpleServer] Failed to load " + getFilename());

      if (config != null) {
        System.out.println("[SimpleServer] Warning:   permission.xml NOT reloaded!");
        System.out.println("               Saving now will overwrite your changes!");
      }

      loadsuccess = false;
    }
  }

  public void loadDefaults() {
    try {
      config = initConf(true);
      InputStream stream = getResourceStream();
      config.load(stream);
      loadsuccess = true;
    } catch (ConfigurationException ex) {
      System.out.println("[SimpleServer] " + ex);
      System.out.println("[SimpleServer] Failed to load defaults for " + getFilename());
      loadsuccess = false;
    }
  }

  @Override
  public void save() {
    try {
      OutputStream stream = new FileOutputStream(getFile());
      config.save(stream);
    }

    catch (Exception e) {
      System.out.println("[SimpleServer] " + e);
      System.out.println("[SimpleServer] Failed to save " + getFilename());
    }
  }

  private static String escape(String attribute) {
    if (attribute == null) {
      return "";
    }
    return attribute.replaceAll("'", "&apos;");
  }
}
