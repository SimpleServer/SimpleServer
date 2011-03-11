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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import simpleserver.Player;

public class ChestList extends AsciiConfig {
  private final ConcurrentMap<Coordinate, Chest> locations;

  public ChestList() {
    super("chest-list.txt");

    locations = new ConcurrentHashMap<Coordinate, Chest>();
  }

  private boolean giveLock(String player, int x, byte y, int z,
                        boolean isGroupLock) {
    Coordinate coordinate = new Coordinate(x, y, z);
    if (locations.containsKey(coordinate)) {
      return false;
    }

    Chest chest = new Chest(player, coordinate, isGroupLock);
    locations.put(coordinate, chest);

    save();
    return true;
  }

  public synchronized boolean giveLock(Player player, int x, byte y, int z,
                                       boolean isGroupLock) {
    return giveLock(player.getName(), x, y, z, isGroupLock);
  }
  
  public void addOpenChest(int x, byte y, int z) {
    giveLock("-", x, y, z, false);
  }

  public boolean hasLock(String name) {
    return false;
  }

  public boolean hasLock(int x, byte y, int z) {
    if(locations.containsKey(new Coordinate(x, y, z))) {
      return !locations.get(new Coordinate(x, y, z)).isOpen();
    }
    return false;
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

  public boolean ownsLock(Player player, int x, byte y, int z) {
    Coordinate coordinate = new Coordinate(x, y, z);
    Chest chest = locations.get(coordinate);
    return (chest != null) && (chest.owner == player.getName() || chest.isOpen());
  }

  public synchronized void releaseLock(Player player) {
    for(Chest chest : locations.values()) {
      if(chest.owner.equals(player.getName()))
        chest.unlock();
    }

    save();
  }

  public synchronized void releaseLock(int x, byte y, int z) {
    locations.remove(new Coordinate(x, y, z));

    save();
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
    if (tokens.length > 4) {
      int x;
      byte y;
      int z;
      try {
        x = Integer.parseInt(tokens[2]);
        y = Byte.parseByte(tokens[3]);
        z = Integer.parseInt(tokens[4]);
      }
      catch (NumberFormatException e) {
        System.out.println("Skipping malformed chest metadata: " + line);
        return;
      }
      
      giveLock(tokens[0], x, y, z, Boolean.parseBoolean(tokens[1]));
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
      output.append("\n");
    }
    return output.toString();
  }

  public static final class Coordinate {
    private final int x;
    private final byte y;
    private final int z;
    private final int hashCode;

    private Coordinate(int x, byte y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;

      int code = 17;
      code = 37 * code + x;
      code = 37 * code + y;
      code = 37 * code + z;
      hashCode = code;
    }

    public boolean equals(Coordinate coordinate) {
      return (coordinate.x == x) && (coordinate.y == y) && (coordinate.z == z);
    }

    @Override
    public boolean equals(Object object) {
      return (object instanceof Coordinate) && equals((Coordinate) object);
    }

    @Override
    public int hashCode() {
      return hashCode;
    }
  }

  public static final class Chest {
    private String owner;
    private final Coordinate coordinate;
    private final boolean isGroup;

    private Chest(String player, Coordinate coordinate, boolean isGroup) {
      this.owner = player;
      this.coordinate = coordinate;
      this.isGroup = isGroup;
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
  }

}
