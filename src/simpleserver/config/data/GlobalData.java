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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import simpleserver.Resource;
import simpleserver.nbt.GZipNBTFile;
import simpleserver.nbt.NBTInt;
import simpleserver.thread.AutoBackup;

public class GlobalData implements Resource {
  private final static String FILENAME = "simpleserver.dat";
  private final static String FOLDER = "simpleserver";
  private final static String BACKUP_FOLDER = "config";
  private final static String PATH = FOLDER + File.separator + FILENAME;
  private final static String BACKUP_PATH = BACKUP_FOLDER + File.separator + FILENAME;

  GZipNBTFile nbt;
  public Warp warp;
  public PlayerData players;
  public Chests chests;
  public EnchantableItems enchantable;

  public GlobalData() {
    warp = new Warp();
    players = new PlayerData();
    chests = new Chests();
    enchantable = new EnchantableItems();
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

    warp.load(nbt.root());
    players.load(nbt.root());
    chests.load(nbt.root());
    enchantable.load(nbt.root());

    save();

    chests.freeMemory();
  }

  public void save() {
    chests.save();

    try {
      nbt.save(PATH);
    } catch (IOException e) {
      System.out.println("[ERROR] Writing simpleserver.dat failed");
    }
  }

  public int freezeTime() {
    if (!nbt.root().containsKey("freezeTime")) {
      return -1;
    } else {
      return nbt.root().getInt("freezeTime").get();
    }
  }

  public void setFreezeTime(int time) {
    nbt.root().put(new NBTInt("freezeTime", time));
    save();
  }

  public void unsetFreezeTime() {
    nbt.root().remove("freezeTime");
    save();
  }
}
