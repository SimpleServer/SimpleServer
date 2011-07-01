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
package simpleserver.config.data;

import static simpleserver.lang.Translations.t;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import simpleserver.Coordinate;
import simpleserver.Player;
import simpleserver.config.LegacyChestList;
import simpleserver.nbt.NBT;
import simpleserver.nbt.NBTCompound;
import simpleserver.nbt.NBTList;
import simpleserver.nbt.NBTString;

public class Chests {
  private final static String CHESTS = "chests";

  private NBTCompound root;
  private final ConcurrentMap<Coordinate, Chest> locations;

  public Chests() {
    locations = new ConcurrentHashMap<Coordinate, Chest>();
  }

  void load(NBTCompound data) {
    root = data;
    locations.clear();
    NBTList<NBTCompound> node;
    if (data.containsKey(CHESTS)) {
      try {
        node = data.getList(CHESTS).cast();
        loadChests(node);
        return;
      } catch (Exception e) {
        System.out.println("[WARNING] Chest list is corrupt. Replacing it with empty list...");
      } finally {
        freeMemory();
      }
    }
    loadOldConfig();
  }

  private void loadChests(NBTList<NBTCompound> node) {
    for (int i = 0; i < node.size(); i++) {
      NBTCompound tag = node.get(i);
      Coordinate coord;
      try {
        coord = new Coordinate(tag.getCompound("coordinate"));
      } catch (Exception e) {
        System.out.println("Skipping corrupt chest");
        continue;
      }
      Chest chest;
      if (!tag.containsKey("owner")) {
        chest = new Chest(coord);
      } else {
        String owner = tag.getString("owner").get();
        if (!tag.containsKey("name")) {
          chest = new Chest(owner, coord);
        } else {
          chest = new Chest(owner, coord, tag.getString("name").get());
        }
      }
      locations.put(coord, chest);
    }
  }

  private void loadOldConfig() {
    LegacyChestList old = new LegacyChestList();
    old.load();
    for (Coordinate coord : old.locations.keySet()) {
      simpleserver.config.LegacyChestList.Chest chest = old.locations.get(coord);
      if (chest.isOpen()) {
        locations.put(coord, new Chest(coord));
      } else if (!chest.name().equals("Locked chest")) {
        locations.put(coord, new Chest(chest.owner(), coord));
      } else {
        locations.put(coord, new Chest(chest.owner(), coord, chest.name()));
      }
    }
    old.save();
  }

  void save() {
    NBTList<NBTCompound> node = new NBTList<NBTCompound>(CHESTS, NBT.COMPOUND);
    for (Chest chest : locations.values()) {
      NBTCompound tag = new NBTCompound();
      tag.put(chest.coordinate.tag());
      if (!chest.isOpen()) {
        tag.put(new NBTString("owner", chest.owner.toLowerCase()));
        if (chest.name != null) {
          tag.put(new NBTString("name", chest.name));
        }
      }
      node.add(tag);
    }
    root.put(node);
  }

  void freeMemory() {
    if (root.containsKey(CHESTS)) {
      root.remove(CHESTS);
    }
  }

  public Chest get(Coordinate coordinate) {
    if (isChest(coordinate)) {
      return locations.get(coordinate);
    } else {
      return null;
    }
  }

  public boolean isLocked(Coordinate coordinate) {
    return isChest(coordinate) && !get(coordinate).isOpen();
  }

  public void releaseLock(Coordinate coordinate) {
    locations.remove(coordinate);
  }

  public boolean isChest(Coordinate coordinate) {
    return locations.containsKey(coordinate);
  }

  public Chest adjacentChest(Coordinate coordinate) {
    Chest chest = get(coordinate.add(1, 0, 0));
    if (chest == null) {
      chest = get(coordinate.add(-1, 0, 0));
    }
    if (chest == null) {
      chest = get(coordinate.add(0, 0, 1));
    }
    if (chest == null) {
      chest = get(coordinate.add(0, 0, -1));
    }
    return chest;
  }

  public boolean canOpen(Player player, Coordinate coordinate) {
    return !isLocked(coordinate) || get(coordinate).ownedBy(player);
  }

  public void unlock(Coordinate coordinate) {
    if (isChest(coordinate)) {
      get(coordinate).unlock();
      Chest adjacent = adjacentChest(coordinate);
      if (adjacent != null) {
        adjacent.unlock();
      }
    }
  }

  public String chestName(Coordinate coordinate) {
    if (isLocked(coordinate)) {
      if (get(coordinate).name != null) {
        return get(coordinate).name;
      } else {
        return t("Locked Chest");
      }
    } else {
      return t("Open Chest");
    }
  }

  public void giveLock(String owner, Coordinate coordinate, String name) {
    if (isChest(coordinate)) {
      Chest chest = get(coordinate);
      chest.name = name;
      chest.owner = owner;
    } else {
      locations.put(coordinate, new Chest(owner, coordinate, name));
    }
  }

  public void giveLock(Player player, Coordinate coordinate, String name) {
    giveLock(player.getName(), coordinate, name);
  }

  public void addOpenChest(Coordinate coordinate) {
    locations.put(coordinate, new Chest(coordinate));
  }

  public Map<String, Integer> chestList(Player player) {
    Map<String, Integer> list = new HashMap<String, Integer>();
    for (Chest chest : locations.values()) {
      if (chest.ownedBy(player)) {
        if (list.containsKey(chest.name)) {
          list.put(chest.name, list.get(chest.name) + 1);
        } else {
          list.put(chest.name, 1);
        }
      }
    }
    if (list.containsKey(null)) {
      list.put(t("Locked Chest"), list.get(null));
      list.remove(null);
    }
    return list;
  }

  public List<Chest> getChestsByName(String name) {
    List<Chest> chests = new ArrayList<Chest>();
    for (Chest chest : locations.values()) {
      if (chest.name != null && chest.name.equals(name)) {
        chests.add(chest);
      }
    }
    return chests;
  }

  public static final class Chest {
    public String owner;
    public final Coordinate coordinate;
    public String name;

    private Chest(String player, Coordinate coordinate, String name) {
      owner = player;
      this.coordinate = coordinate;
      this.name = name;
    }

    private Chest(String player, Coordinate coordinate) {
      owner = player;
      this.coordinate = coordinate;
    }

    private Chest(Coordinate coordinate) {
      this.coordinate = coordinate;
    }

    public boolean isOpen() {
      return owner == null;
    }

    public void lock(Player player) {
      owner = player.getName().toLowerCase();
    }

    public void unlock() {
      owner = null;
      name = null;
    }

    public boolean ownedBy(Player player) {
      return owner != null && owner.equals(player.getName().toLowerCase());
    }
  }
}
