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
package simpleserver.thread;

import static simpleserver.lang.Translations.t;
import static simpleserver.util.Util.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import simpleserver.Server;

public class AutoBackup {
  private static final long MILLISECONDS_PER_MINUTE = 1000 * 60;
  private static final long MILLISECONDS_PER_HOUR = MILLISECONDS_PER_MINUTE * 60;
  private static final String NAME_FORMAT = "%tF-%1$tH-%1$tM";
  private static final File BACKUP_DIRECTORY = new File("backups");
  private static final File TEMP_DIRECTORY = new File("tmp");

  private final Server server;
  private final byte[] copyBuffer;
  private final Archiver archiver;

  private volatile boolean run = true;
  private volatile boolean forceBackup = false;
  private volatile boolean pauseBackup = false;

  public AutoBackup(Server server) {
    this.server = server;
    purgeOldBackups();

    copyBuffer = new byte[8192];

    archiver = new Archiver();
    archiver.start();
    archiver.setName("AutoBackup");
  }

  public void stop() {
    run = false;
    archiver.interrupt();
  }

  public void forceBackup() {
    forceBackup = true;
    archiver.interrupt();
  }

  private void backup() throws IOException {
    if (server.config.properties.getBoolean("announceBackup")) {
      print("Backing up server...");
    }
    announce(t("Backing up..."));

    File copy;
    try {
      copy = makeTemporaryCopy();
      server.runCommand("save-on", null);

      zipBackup(copy);
    } finally {
      deleteRecursively(TEMP_DIRECTORY);
    }
    purgeOldBackups();
    announce(t("Backup Complete!"));
  }

  public void announce(String message) {
    if (server.config.properties.getBoolean("announceBackup")) {
      server.runCommand("say", message);
    }
  }

  private boolean needsBackup() {
    long backupPeriod = MILLISECONDS_PER_MINUTE
        * server.config.properties.getInt("autoBackupMins");
    return server.config.properties.getBoolean("autoBackup")
        && backupPeriod < lastBackupAge() && !pauseBackup
        || forceBackup;
  }

  private long lastBackupAge() {
    long age = age(newestBackup());
    return (age >= 0) ? age : Long.MAX_VALUE;
  }

  private void purgeOldBackups() {
    long maxAge = MILLISECONDS_PER_HOUR * server.config.properties.getInt("keepBackupHours");
    File file;
    while (age(file = oldestBackup()) > maxAge) {
      deleteRecursively(file);
    }
  }

  private File makeTemporaryCopy() throws IOException {
    TEMP_DIRECTORY.mkdir();

    File backup = new File(TEMP_DIRECTORY, String.format(NAME_FORMAT, new Date()));
    copyRecursively(new File(server.options.get("levelName")), backup);

    File configBackup = new File(backup, "config");
    copyRecursively(new File("simpleserver"), configBackup);
    copyRecursively(new File("simpleserver.properties"),
                    new File(configBackup, "simpleserver.properties"));

    File bukkitSettings = new File("bukkit.yml");
    if (bukkitSettings.exists()) {
      copyRecursively(bukkitSettings, new File(configBackup, "bukkit.yml"));
    }

    File nether = new File(server.options.get("levelName") + "_nether");
    if (nether.exists()) {
      copyRecursively(nether, new File(backup, server.options.get("levelName") + "_nether"));
    }

    /*
    File plugins = new File("plugins");
    if (plugins.exists()) {
      copyRecursively(plugins, new File(backup, "plugins"));
    }
    */

    return backup;
  }

  private void zipBackup(File source) throws IOException {
    File backup = new File(BACKUP_DIRECTORY,
                           String.format(NAME_FORMAT + ".zip", new Date()));
    FileOutputStream fout = new FileOutputStream(backup);
    try {
      ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(fout));
      try {
        zipRecursively(source, out);
      } finally {
        out.close();
      }
    } finally {
      fout.close();
    }
    print("Backup saved: " + backup.getPath());
  }

  private void zipRecursively(File source, ZipOutputStream out)
      throws IOException {
    for (File file : source.listFiles()) {
      zipRecursively(file, out, source.getPath().length() + 1);
    }
  }

  private void zipRecursively(File source, ZipOutputStream out, int prefixLength)
      throws IOException {
    String name = source.getPath().substring(prefixLength);
    if (source.isDirectory()) {
      ZipEntry entry = new ZipEntry(name + "/");
      out.putNextEntry(entry);
      out.closeEntry();

      for (File file : source.listFiles()) {
        zipRecursively(file, out, prefixLength);
      }
    } else {
      ZipEntry entry = new ZipEntry(name);
      out.putNextEntry(entry);

      FileInputStream in = new FileInputStream(source);
      try {
        int length;
        while ((length = in.read(copyBuffer)) != -1) {
          out.write(copyBuffer, 0, length);
          Thread.yield();
        }
      } finally {
        in.close();
      }
      out.closeEntry();
    }
  }

  private void copyRecursively(File source, File target) throws IOException {
    if (source.isDirectory()) {
      target.mkdir();

      for (String child : source.list()) {
        if (!child.contains("tmp_chunk.dat")) {
          copyRecursively(new File(source, child), new File(target, child));
        }
      }
    } else {
      if (source.getName().equals("level.dat_old")) {
        return;
      }
      InputStream in = new FileInputStream(source);
      OutputStream out = new FileOutputStream(target);

      try {
        int length;
        while ((length = in.read(copyBuffer)) > 0) {
          out.write(copyBuffer, 0, length);
        }
      } finally {
        try {
          in.close();
        } finally {
          out.close();
        }
      }
    }
  }

  private void deleteRecursively(File path) {
    if (path.exists() && path.isDirectory()) {
      for (File file : path.listFiles()) {
        if (file.isDirectory()) {
          deleteRecursively(file);
        } else {
          file.delete();
        }
      }
    }
    path.delete();
  }

  public static File newestBackup() {
    return getBackup(false);
  }

  public static File oldestBackup() {
    return getBackup(true);
  }

  private static File getBackup(boolean old) {
    BACKUP_DIRECTORY.mkdir();
    File[] files = BACKUP_DIRECTORY.listFiles(new FileFilter() {
      public boolean accept(File file) {
        return file.isFile() && file.getPath().contains(".zip");
      }
    });
    long firstCreatedTime = old ? Long.MAX_VALUE : 0;
    File firstCreatedFile = null;
    for (File file : files) {
      long date;
      try {
        date = date(file);
      } catch (ParseException e) {
        continue;
      }

      if ((old && date < firstCreatedTime) || (!old && date > firstCreatedTime)) {
        firstCreatedFile = file;
        firstCreatedTime = date;
      }
    }
    return firstCreatedFile;
  }

  private static long date(File file) throws ParseException {
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
    GregorianCalendar cal = new GregorianCalendar();
    Date fileTime;
    fileTime = format.parse(file.getName().split(".zip")[0]);
    cal.setTime(fileTime);
    return cal.getTimeInMillis();
  }

  private static long age(File file) {
    try {
      if (file == null) {
        return -1;
      } else {
        return System.currentTimeMillis() - date(file);
      }
    } catch (ParseException e) {
      return System.currentTimeMillis() - file.lastModified();
    }
  }

  private final class Archiver extends Thread {
    @Override
    public void run() {
      while (run) {
        if (needsBackup()) {
          try {
            server.saveLock.acquire();
          } catch (InterruptedException e) {
            continue;
          }
          forceBackup = false;

          if (server.config.properties.getBoolean("announceSave")) {
            server.runCommand("say", t("Saving Map..."));
          }
          server.setSaving(true);
          server.runCommand("save-all", null);
          while (server.isSaving()) {
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
            }
          }

          server.runCommand("save-off", null);
          server.autoSpaceCheck.check(true);

          try {
            backup(); // does enable saving
          } catch (IOException e) {
            server.errorLog(e, "Server Backup Failure");
            print(e);
            print("Automated Server Backup Failure!");
          }
          server.saveLock.release();

          if (server.numPlayers() == 0) {
            pauseBackup = true;
          }
        }
        if (pauseBackup && server.numPlayers() > 0) {
          pauseBackup = false;
        }

        try {
          Thread.sleep(60000);
        } catch (InterruptedException e) {
        }
      }
    }
  }
}
