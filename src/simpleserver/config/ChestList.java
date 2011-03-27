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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import simpleserver.Coordinate;
import simpleserver.Player;

public class ChestList extends AsciiConfig {
  private final ConcurrentMap<Coordinate, Chest> locations;

  public ChestList() {
    super("chest-list.txt");

    locations = new ConcurrentHashMap<Coordinate, Chest>();
  }

  public boolean giveLock(String player, int x, byte y, int z,
                        boolean isGroupLock, String name) {
    Coordinate coordinate = new Coordinate(x, y, z);
    if (locations.containsKey(coordinate)) {
      locations.remove(coordinate);
    }

    Chest chest = new Chest(player, coordinate, isGroupLock, name);
    locations.put(coordinate, chest);

    save();
    return true;
  }

  public synchronized boolean giveLock(Player player, int x, byte y, int z,
                                       boolean isGroupLock, String name) {
    return giveLock(player.getName(), x, y, z, isGroupLock, name);
  }
  
  public void addOpenChest(int x, byte y, int z) {
    giveLock("-", x, y, z, false, "");
  }

  public boolean isChest(int x, byte y, int z) {
    return locations.containsKey(new Coordinate(x, y, z));
  }
  
  public boolean canOpen(Player player, Coordinate coords) {
    return coords == null || canOpen(player, coords.x, coords.y, coords.z);
  }

  private Chest adjacentChest(Coordinate coords) {
    return adjacentChest(coords.x, coords.y, coords.z);
  }
  
  public Chest adjacentChest(int x, byte y, int z) {
    Chest chest = chestAt(new Coordinate(x + 1, y, z));
    if(chest == null)
      chest = chestAt(new Coordinate(x - 1, y, z));
    if(chest == null)
      chest = chestAt(new Coordinate(x, y, z + 1));
    if(chest == null)
      chest = chestAt(new Coordinate(x, y, z - 1));
    return chest;
  }
  
  private Chest chestAt(Coordinate coord) {
    if(locations.containsKey(coord)) {
      return locations.get(coord);
    }
    return null;
  }

  public boolean canOpen(Player player, int x, byte y, int z) {
    Coordinate coordinate = new Coordinate(x, y, z);
    Chest chest = chestAt(coordinate);
    return (chest == null) || (chest.ownedBy(player) || chest.isOpen());
  }

  public boolean isLocked(int x, byte y, int z) {
    Chest chest = locations.get(new Coordinate(x, y, z));
    return chest != null && !chest.isOpen();
  }

  public boolean isLocked(Coordinate coords) {
    return !(coords == null) && isLocked(coords.x, coords.y, coords.z);
  }

  public synchronized void releaseLock(int x, byte y, int z) {
    Chest chest = chestAt(new Coordinate(x,y,z));
    if(chest != null) {
      chest.unlock();
      save();
    }
  }
  
  public Map<String, Integer> chestList(Player player) {
    Map<String, Integer> list = new HashMap<String, Integer>();
    for(Chest chest : locations.values()) {
      if(chest.ownedBy(player)) {
        if(list.containsKey(chest.name)) {
          list.put(chest.name, list.get(chest.name)+1);
        } else {
          list.put(chest.name, 1);
        }
      }
    }
    return list;
  }
  
  public String chestName(Coordinate coords) {
    Chest chest = chestAt(coords);
    return (chest == null) ? "Open Chest" : chest.name();
  }
  
  public List<Chest> getChestsByName(String name) {
    List<Chest> chests = new ArrayList<Chest>();
    for(Chest chest : locations.values()) {
      if(chest.name().equals(name)) {
        chests.add(chest);
      }
    }
    return chests;
  }

  public void unlock(Coordinate coords) {
    Chest chest = chestAt(coords);
    if(chest != null) {
       chest.unlock();
       chest = this.adjacentChest(coords);
       if(chest != null) {
         chest.unlock();
       }
       save();
    }
  }
  

  @Override
  public void load() {
    locations.clear();

    super.load();
  }

  @Override
  protected void loadLine(String line) {
    line = line.trim();
    if (line.length() == 0) {
      return;
    }

    String[] tokens = line.split(",");
    if (tokens.length >= 5) {
      int x;
      byte y;
      int z;
      String name;
      try {
        x = Integer.parseInt(tokens[2]);
        y = Byte.parseByte(tokens[3]);
        z = Integer.parseInt(tokens[4]);
      }
      catch (NumberFormatException e) {
        System.out.println("Skipping malformed chest metadata: " + line);
        return;
      }
      
      if(tokens.length >= 6) {
         name = tokens[5];
      } else {
        name = (tokens[0].equals("-")) ? "" : "Locked Chest";
      }
      
      giveLock(tokens[0], x, y, z, Boolean.parseBoolean(tokens[1]), name);
    }
  }

  @Override
  protected String saveString() {
    StringBuilder output = new StringBuilder();
    for (Chest chest : locations.values().toArray(new Chest[0])) {
      output.append(chest.owner);
      output.append(",");
      output.append(chest.isGroup);
      output.append(",");
      output.append(chest.coordinate.x);
      output.append(",");
      output.append(chest.coordinate.y);
      output.append(",");
      output.append(chest.coordinate.z);
      output.append(",");
      output.append(chest.name());
      output.append("\n");
    }
    return output.toString();
  }


  public static final class Chest {
    private String owner;
    private final Coordinate coordinate;
    private final boolean isGroup;
    private String name;

    private Chest(String player, Coordinate coordinate, boolean isGroup, String name) {
      this.owner = player;
      this.coordinate = coordinate;
      this.isGroup = isGroup;
      this.rename(name);
    }

    public String name() {
      return name;
    }

    public void unlock() {
      owner = "-";
      
    }

    public boolean isOpen() {
      return owner.equals("-");
    }

    public String owner() {
      return owner;
    }

    public void lock(Player player) {
      this.owner = player.getName();
    }

    public boolean ownedBy(Player player) {
      return owner.toLowerCase().equals(player.getName().toLowerCase());
    }

    public void rename(String name) {
      this.name = name;
    }
  }


}
