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
package simpleserver.config.xml;

import java.io.IOException;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import simpleserver.Player;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class Config extends StorageContainer {
  public PropertyStorage properties;
  public PlayerStorage players;
  public IpStorage ips;
  public GroupStorage groups;
  public CommandStorage commands;
  public AllBlocksStorage allblocks;
  public BlockStorage blocks;
  public ChestsStorage chests;
  AreaStorage localAreas;
  public GlobalAreaStorage areas;

  Config() {
    super("config");
  }

  @Override
  void finish() {
    areas.buildTree();
  }

  @Override
  void addStorages() {
    areas = GlobalAreaStorage.newInstance();
    addStorage("property", properties = new PropertyStorage());
    addStorage("player", players = new PlayerStorage());
    addStorage("ip", ips = new IpStorage());
    addStorage("group", groups = new GroupStorage());
    addStorage("command", commands = new CommandStorage());
    addStorage("allblocks", allblocks = new AllBlocksStorage());
    addStorage("block", blocks = new BlockStorage());
    addStorage("chests", chests = new ChestsStorage());
    addStorage("area", localAreas = new AreaStorage());
  }

  void save(ContentHandler handler, XMLSerializer serializer) throws SAXException, IOException {
    handler.startElement("", "", tag, new AttributesImpl());
    saveChilds(handler);
    handler.endElement("", "", tag);
  }

  public Group getGroup(Player player) throws SAXException {
    Integer playerGroup = players.get(player);
    Integer ipGroup = ips.get(player);
    int groupid;
    if (playerGroup == null && ipGroup == null) {
      groupid = properties.getInt("defaultGroup");
    } else if (playerGroup == null || playerGroup < ipGroup) {
      groupid = ipGroup;
    } else {
      groupid = playerGroup;
    }
    Group group = groups.get(groupid);
    if (group == null) {
      throw new SAXException("The group with ID " + groupid + " does not exist.");
    }
    return group;
  }
}
