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
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import simpleserver.Coordinate;
import simpleserver.Player;

@SuppressWarnings("deprecation")
public class Config extends PermissionContainer {
  public PropertyStorage properties;
  public PlayerStorage players;
  public IpStorage ips;
  public GroupStorage groups;
  public DimensionStorage dimensions;
  public EventStorage events;

  public Config() {
    super("config");
  }

  @Override
  void addStorages() {
    addStorage("property", properties = new PropertyStorage());
    addStorage("player", players = new PlayerStorage());
    addStorage("ip", ips = new IpStorage());
    addStorage("group", groups = new GroupStorage());
    super.addStorages();
    addStorage("dimension", dimensions = new DimensionStorage());
    addStorage("event", events = new EventStorage());
  }

  void save(ContentHandler handler, XMLSerializer serializer) throws SAXException, IOException {
    handler.startElement("", "", tag, new AttributesImpl());
    saveChilds(handler, !properties.getBoolean("xmlInlineAttributes"), properties.getBoolean("xmlPCDATA"));
    handler.endElement("", "", tag);
  }

  public Group getGroup(Player player) throws SAXException {
    Integer playerGroup = players.get(player);
    Integer ipGroup = ips.get(player);
    int groupid;
    if (playerGroup == null && ipGroup == null) {
      groupid = properties.getInt("defaultGroup");
    } else if (playerGroup == null || (ipGroup != null && playerGroup < ipGroup)) {
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

  public AreaStoragePair playerArea(Player player) {
    String name = player.getName().toLowerCase();
    for (DimensionConfig dim : dimensions) {
      Stack<AreaStorage> stack = new Stack<AreaStorage>();
      stack.add(dim.topAreas);
      while (!stack.isEmpty()) {
        AreaStorage storage = stack.pop();
        for (Area area : storage) {
          if (name.equals(area.owner)) {
            return new AreaStoragePair(storage, area);
          }
          stack.add(area.areas);
        }
      }
    }
    return null;
  }

  public static class AreaStoragePair {
    public AreaStorage storage;
    public Area area;

    public AreaStoragePair(AreaStorage storage, Area area) {
      this.storage = storage;
      this.area = area;
    }
  }

  public List<PermissionContainer> containers(Coordinate coordinate) {
    List<PermissionContainer> containers = new LinkedList<PermissionContainer>();

    containers.add(this);

    DimensionConfig dim = dimensions.get(coordinate.dimension());
    if (dim != null) {
      containers.add(dim);
      containers.addAll(dim.areas.get(coordinate));
    }

    return containers;
  }

  public Permission getCommandPermission(String name, String args, Coordinate coordinate) {
    Permission perm = null;
    for (PermissionContainer container : containers(coordinate)) {
      if (container.commands.contains(name)) {
        perm = container.commands.get(name).allow(args);
      }
    }
    return perm == null ? new Permission() : perm;
  }

  public BlockPermission blockPermission(Player player, Coordinate coordinate) {
    return blockPermission(player, coordinate, 0);
  }

  public BlockPermission blockPermission(Player player, Coordinate coordinate, int id) {
    BlockPermission perm = new BlockPermission();

    for (PermissionContainer area : containers(coordinate)) {
      perm.add(area.allblocks.blocks);
      if (id > 0) {
        perm.add(area.blocks.get(id));
      }
      perm.add(area.chests.chests);
    }

    perm.finish(player);
    return perm;
  }

  public static class BlockPermission {
    public boolean place;
    public boolean destroy;
    public boolean use;
    public boolean give;
    public boolean chest;

    private Permission placePerm;
    private Permission destroyPerm;
    private Permission usePerm;
    private Permission givePerm;
    private Permission chestPerm;

    void add(AllBlocks allblocks) {
      if (allblocks != null) {
        if (allblocks.place != null) {
          placePerm = allblocks.place;
        }
        if (allblocks.destroy != null) {
          destroyPerm = allblocks.destroy;
        }
        if (allblocks.use != null) {
          usePerm = allblocks.use;
        }
        if (allblocks.give != null) {
          givePerm = allblocks.give;
        }
      }
    }

    void add(Chests chests) {
      if (chests != null) {
        chestPerm = chests.allow;
      }
    }

    void add(Block block) {
      if (block != null) {
        if (block.place != null) {
          placePerm = block.place;
        }
        if (block.give != null) {
          givePerm = block.give;
        }
      }
    }

    void finish(Player player) {
      place = placePerm == null ? true : placePerm.contains(player);
      destroy = destroyPerm == null ? true : destroyPerm.contains(player);
      give = givePerm == null ? true : givePerm.contains(player);
      use = usePerm == null ? true : usePerm.contains(player);
      chest = chestPerm == null ? true : chestPerm.contains(player);
    }
  }
}
