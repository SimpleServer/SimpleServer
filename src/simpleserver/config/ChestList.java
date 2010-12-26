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

public class ChestList extends AsciiConfig {
  private final ConcurrentMap<String, Chest> names;
  private final ConcurrentMap<Coordinate, Chest> locations;

  public ChestList() {
    super("chest-list.txt");

    names = new ConcurrentHashMap<String, Chest>();
    locations = new ConcurrentHashMap<Coordinate, Chest>();
  }

  public synchronized boolean giveLock(String name, int x, byte y, int z,
                                       boolean isGroupLock) {
    Coordinate coordinate = new Coordinate(x, y, z);
    if (names.containsKey(names) || locations.containsKey(coordinate)) {
      return false;
    }

    name = name.toLowerCase();
    Chest chest = new Chest(name, coordinate, isGroupLock);
    names.put(name, chest);
    locations.put(coordinate, chest);

    save();
    return true;
  }

  public boolean hasLock(String name) {
    return names.get(name.toLowerCase()) != null;
  }

  public boolean hasLock(int x, byte y, int z) {
    return locations.containsKey(new Coordinate(x, y, z));
  }

  public boolean hasAdjacentLock(int x, byte y, int z) {
    return locations.containsKey(new Coordinate(x + 1, y, z))
        || locations.containsKey(new Coordinate(x - 1, y, z))
        || locations.containsKey(new Coordinate(x, y, z + 1))
        || locations.containsKey(new Coordinate(x, y, z - 1));
  }

  public boolean ownsLock(String name, int x, byte y, int z) {
    Coordinate coordinate = new Coordinate(x, y, z);
    Chest chest = names.get(name.toLowerCase());
    return (chest != null) && (chest.coordinate.equals(coordinate));
  }

  public synchronized void releaseLock(String name) {
    Chest chest = names.remove(name.toLowerCase());
    if (chest != null) {
      locations.remove(chest.coordinate);
    }

    save();
  }

  public synchronized void releaseLock(int x, byte y, int z) {
    Chest chest = locations.remove(new Coordinate(x, y, z));
    if (chest != null) {
      names.remove(chest.name);
    }

    save();
  }

  @Override
  public void load() {
    names.clear();
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
    for (Chest chest : names.values().toArray(new Chest[0])) {
      output.append(chest.name);
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

  private static final class Coordinate {
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

  private static final class Chest {
    private final String name;
    private final Coordinate coordinate;
    private final boolean isGroup;

    private Chest(String name, Coordinate coordinate, boolean isGroup) {
      this.name = name;
      this.coordinate = coordinate;
      this.isGroup = isGroup;
    }
  }
}
