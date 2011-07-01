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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import simpleserver.Player;
import simpleserver.Position;
import simpleserver.Resource;
import simpleserver.Coordinate.Dimension;
import simpleserver.config.LegacyStats.Statistic;
import simpleserver.nbt.GZipNBTFile;
import simpleserver.nbt.NBTByte;
import simpleserver.nbt.NBTCompound;
import simpleserver.nbt.NBTDouble;
import simpleserver.nbt.NBTFloat;
import simpleserver.nbt.NBTInt;
import simpleserver.thread.AutoBackup;

public class GlobalData implements Resource {
  private final static String FILENAME = "simpleserver.dat";
  private final static String FOLDER = "simpleserver";
  private final static String BACKUP_FOLDER = "config";
  private final static String PATH = FOLDER + File.separator + FILENAME;
  private final static String BACKUP_PATH = BACKUP_FOLDER + File.separator + FILENAME;

  private GZipNBTFile nbt;
  public Warp warp;
  public PlayerData players;

  public GlobalData() {
    warp = new Warp();
    players = new PlayerData();
  }

  public void load() {
    try {
      nbt = new GZipNBTFile(PATH);
    } catch (FileNotFoundException e) {
      System.out.println("simpleserver.dat is missing. Generating empty NBT file.");
      nbt = new GZipNBTFile();
    } catch (Exception e) {
      System.out.println("[WARNING] simpleserver.dat is corrupt. Loading from latest backup...");
      ZipFile file = null;
      try {
        File backup = AutoBackup.newestBackup();
        file = new ZipFile(backup);
        Enumeration<? extends ZipEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          String name = entry.getName();
          if (name.equals(BACKUP_PATH)) {
            nbt = new GZipNBTFile(file.getInputStream(entry));
            break;
          }
        }
        if (nbt == null) {
          throw new Exception();
        }
      } catch (Exception e1) {
        System.out.println("[WARNING] Fetching from backup failed. Generating empty NBT file.");
        nbt = new GZipNBTFile();
      } finally {
        if (file != null) {
          try {
            file.close();
          } catch (IOException e1) {
          }
        }
      }
    }

    warp.load();
    players.load();
  }

  public void save() {
    try {
      nbt.save(PATH);
    } catch (IOException e) {
      System.out.println("[ERROR] Writing simpleserver.dat failed");
    }
  }

  public class Warp {
    private NBTCompound node;

    public Set<String> names() {
      return node.names();
    }

    public boolean contains(String name) {
      return node.containsKey(name);
    }

    public Position get(String name) {
      if (!node.containsKey(name)) {
        return null;
      }
      NBTCompound p = node.getCompound(name);
      double x = p.getDouble("x").get();
      double y = p.getDouble("y").get();
      double z = p.getDouble("z").get();
      Dimension dim = Dimension.get(p.getByte("Dimension").get());
      float yaw = p.getFloat("yaw").get();
      float pitch = p.getFloat("pitch").get();
      return new Position(x, y, z, dim, yaw, pitch);
    }

    public void set(String name, Position pos) {
      NBTCompound p = new NBTCompound(name);
      p.put(new NBTDouble("x", pos.x));
      p.put(new NBTDouble("y", pos.y));
      p.put(new NBTDouble("z", pos.z));
      p.put(new NBTByte("Dimension", pos.dimension.index()));
      p.put(new NBTFloat("yaw", pos.yaw));
      p.put(new NBTFloat("pitch", pos.pitch));
      node.put(p);
    }

    public void remove(String name) {
      node.remove(name);
    }

    private void load() {
      if (nbt.root().containsKey("warp")) {
        try {
          node = nbt.root().getCompound("warp");
          return;
        } catch (Exception e) {
          System.out.println("[WARNING] Warp list is corrupt. Replacing it with empty list...");
        }
      }
      node = new NBTCompound("warp");
      nbt.root().put(node);
    }
  }

  public class PlayerData {
    private NBTCompound node;
    public Stats stats = new Stats();

    private void load() {
      if (nbt.root().containsKey("players")) {
        try {
          node = nbt.root().getCompound("players");
          return;
        } catch (Exception e) {
          System.out.println("[WARNING] Player list is corrupt. Replacing it with empty list...");
        }
      }
      node = new NBTCompound("players");
      nbt.root().put(node);
      loadOldStats();
    }

    private void loadOldStats() {
      LegacyStats old = new LegacyStats();
      old.load();
      for (String name : old.stats.keySet()) {
        Statistic oldStats = old.stats.get(name);
        NBTCompound tag = new NBTCompound(name);
        NBTCompound stats = new NBTCompound("stats");
        stats.put(new NBTInt(StatField.PLAY_TIME.toString(), oldStats.minutes));
        stats.put(new NBTInt(StatField.BLOCKS_DESTROYED.toString(), oldStats.blocksDestroyed));
        stats.put(new NBTInt(StatField.BLOCKS_PLACED.toString(), oldStats.blocksPlaced));
        tag.put(stats);
        node.put(tag);
      }
      old.save();
    }

    private NBTCompound get(String name) {
      name = name.toLowerCase();
      if (node.containsKey(name)) {
        return node.getCompound(name);
      } else {
        NBTCompound player = new NBTCompound(name);
        node.put(player);
        return player;
      }
    }

    public class Stats {
      private final static String STATS = "stats";

      public int get(Player player, StatField field) {
        return getInt(player.getName(), field.toString()).get();
      }

      public void set(Player player, StatField field, int value) {
        getInt(player.getName(), field.toString()).set(value);
      }

      public int add(Player player, StatField field, int amount) {
        NBTInt tag = getInt(player.getName(), field.toString());
        tag.set(tag.get() + amount);
        return tag.get();
      }

      private NBTCompound getStats(String name) {
        NBTCompound player = PlayerData.this.get(name);
        if (player.containsKey(STATS)) {
          return player.getCompound(STATS);
        } else {
          NBTCompound tag = new NBTCompound(STATS);
          player.put(tag);
          return tag;
        }
      }

      private NBTInt getInt(String name, String key) {
        NBTCompound player = getStats(name);
        if (player.containsKey(key)) {
          return player.getInt(key);
        } else {
          NBTInt tag = new NBTInt(key, 0);
          player.put(tag);
          return tag;
        }
      }
    }
  }

  public enum StatField {
    PLAY_TIME,
    BLOCKS_DESTROYED,
    BLOCKS_PLACED;
  }
}
