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

import static simpleserver.lang.Translations.t;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import simpleserver.Coordinate;
import simpleserver.Player;
import simpleserver.Coordinate.Dimension;

public class LegacyChestList extends AsciiConfig {
  public final ConcurrentMap<Coordinate, Chest> locations;

  public LegacyChestList() {
    super("chest-list.txt");

    locations = new ConcurrentHashMap<Coordinate, Chest>();
  }

  @Override
  public void load() {
    locations.clear();

    super.load();
  }

  @Override
  public void save() {
    File stats = new File("simpleserver" + File.separator + "chest-list.txt");
    File oldconf = new File("simpleserver" + File.separator + "oldconf");
    oldconf.mkdir();
    stats.renameTo(new File(oldconf.getPath() + File.separator + "chest-list.txt"));
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
      Dimension dimension = Dimension.EARTH;
      String name;
      try {
        x = Integer.parseInt(tokens[2]);
        y = Byte.parseByte(tokens[3]);
        z = Integer.parseInt(tokens[4]);
      } catch (NumberFormatException e) {
        System.out.println("Skipping malformed chest metadata: " + line);
        return;
      }

      if (tokens.length == 6) {
        name = tokens[5];
      } else if (tokens.length >= 7) {
        dimension = Dimension.get(tokens[5]);
        name = tokens[6];
      } else {
        name = (tokens[0].equals("-")) ? "-" : t("Locked Chest");
      }

      Coordinate coordinate = new Coordinate(x, y, z, dimension);
      locations.put(coordinate, new Chest(tokens[0], coordinate, Boolean.parseBoolean(tokens[1]), name));
    }
  }

  @Override
  protected void missingFile() {
  }

  @Override
  protected String saveString() {
    // TODO Auto-generated method stub
    return null;
  }

  public static final class Chest {
    private String owner;
    private String name;

    private Chest(String player, Coordinate coordinate, boolean isGroup, String name) {
      owner = player;
      rename(name);
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
      owner = player.getName();
    }

    public boolean ownedBy(Player player) {
      return owner.toLowerCase().equals(player.getName().toLowerCase());
    }

    public void rename(String name) {
      this.name = name;
    }
  }
}
