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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import simpleserver.Server;

public class AutoBackup {
  private static final long MILLISECONDS_PER_MINUTE = 1000 * 60;
  private static final long MILLISECONDS_PER_HOUR = MILLISECONDS_PER_MINUTE * 60;
  private static final String NAME_FORMAT = "%tF-%1$tH-%1$tM";
  private static final File BACKUP_BASE_DIRECTORY = new File("backups");
  private static final File BACKUP_AUTO_DIRECTORY = new File(BACKUP_BASE_DIRECTORY, "auto");
  private static final File BACKUP_TAGGED_DIRECTORY = new File(BACKUP_BASE_DIRECTORY, "tagged");
  private static final File TEMP_DIRECTORY = new File("tmp");

  //bukkit depending files and directories
  private static final List<File> RESOURCE_DIRS_CONFIG_BUKKIT = new ArrayList(Arrays.asList(
          new File("bukkit.yml")
  ));
  /*
   * Directories and files to backup and restore with the current configuration (bukkit yes/no)
   * Added here are the settings-independant resources
   * Resources are devided into CONFIG and MAP
   */
  private static final List<File> RESOURCES_CONFIG = new ArrayList<File>(Arrays.asList(
          new File("simpleserver"),
          new File("simpleserver.properties")
  ));
  private static final List<File> RESOURCES_MAP = new ArrayList<File>();

  private final Server server;
  private final byte[] copyBuffer;
  private final Archiver archiver;

  private volatile boolean run = true;
  private volatile boolean forceBackup = false;
  private volatile boolean pauseBackup = false;

  private volatile String tag = null; // tag for next backup ('null' means
                                      // date/no tagged backup)

  public AutoBackup(Server server) {
    this.server = server;

    // Create backup directories if not present
    BACKUP_AUTO_DIRECTORY.mkdirs();
    BACKUP_TAGGED_DIRECTORY.mkdirs();

    //initialize resource directories
    RESOURCES_MAP.add(new File(server.options.get("levelName")));
    if (new File("bukkit.yml").exists()) {
      RESOURCES_CONFIG.addAll(RESOURCE_DIRS_CONFIG_BUKKIT);
      RESOURCES_MAP.add(new File(server.options.get("levelName" ) + "_nether"));
      RESOURCES_MAP.add(new File(server.options.get("levelName" ) + "_the_end"));
    }

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
    forceBackup(null);
  }

  public void forceBackup(String tag) {
    this.tag = tag;
    forceBackup = true;
    archiver.interrupt();
  }

  // TODO do not overwrite backups!
  private void backup() throws IOException {
    if (server.config.properties.getBoolean("announceBackup")) {
      println("Backing up server...");
    }
    announce(t("Backing up..."));

    File copy;
    try {
      copy = makeTemporaryCopy();
      server.runCommand("save-on", null);
      zipBackup(copy); // create actual backup file
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
    File backupConfig = new File(backup, "config");
    File backupMap = new File(backup, "map");

    for (File file : RESOURCES_CONFIG) {
      copyRecursively(file, backupConfig);
    }

    for (File file : RESOURCES_MAP) {
      copyRecursively(file, backupMap);
    }

    return backup;
  }

  /**
   * Zip and name the temporary created backup
   *
   * @param source Directory to zip
   * @throws IOException
   */
  private void zipBackup(File source) throws IOException {
    File dir = BACKUP_TAGGED_DIRECTORY;

    if (tag == null) {
      dir = BACKUP_AUTO_DIRECTORY;
      tag = String.format(NAME_FORMAT, new Date());
    }

    File backup = new File(dir, tag + ".zip");
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
    println("Backup saved: " + backup.getPath());
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
    // Search for backups in BACKUP_AUTO_DIRECTORY
    File[] files = getAutoBackups();
    long firstCreatedTime = old ? Long.MAX_VALUE : 0;
    File firstCreatedFile = null;
    for (File file : files) {
      long date;
      try {
        date = dateMillis(file);
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

  private static File[] getAutoBackups() {
    return BACKUP_AUTO_DIRECTORY.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.isFile() && file.getPath().contains(".zip");
      }
    });
  }

  /**
   * Like 'getAutoBackups()', but sorted from newest to oldest.
   * @return
   */
  private static File[] getSortedAutoBackups() {
    //sort files by date (newest to oldest)
    File[] files = getAutoBackups();
    java.util.Arrays.sort(files, new Comparator<File>() {
      @Override
      public int compare(File o1, File o2) {
        try {
          return date(o2).compareTo(date(o1));
        } catch (ParseException ex) { //should not be thrown
          return 0;
        }
      }
    });
    return files;
  }

  public static String listLastAutoBackups(int n) {
    StringBuilder sb = new StringBuilder();
    sb.append("Last ").append(n).append(" auto backups:");
    File[] files = getSortedAutoBackups();
    for (int i = 1; i <= n && i <= files.length; i++) {
      try {
        sb.append("\n").append("@").append(i).append(" ").append(dateFormatted(files[i-1]));
      } catch (ParseException ex) {
        continue;
      }
    }
    return sb.toString();
  }

  private static Date date(File file) throws ParseException {
    return new SimpleDateFormat("yyyy-MM-dd-HH-mm").parse(file.getName().split(".zip")[0]);
  }

  private static String dateFormatted(File file) throws ParseException {
    return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date(file));
  }

  private static long dateMillis(File file) throws ParseException {
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(date(file));
    return cal.getTimeInMillis();
  }

  private static long age(File file) {
    try {
      if (file == null) {
        return -1;
      } else {
        return System.currentTimeMillis() - dateMillis(file);
      }
    } catch (ParseException e) {
      return System.currentTimeMillis() - file.lastModified();
    }
  }

  /**
   * Rollback to n-th last auto backup.
   * @param n
   * @return
   */
  public void rollback(int n) throws Exception {
    File[] backups = getSortedAutoBackups();
    try {
      rollback(backups[n-1]);
    } catch (ArrayIndexOutOfBoundsException ex) {
      throw new Exception("Wrong backup number!");
    }
  }

  /**
   * Rollback to backup with tag 'tag'.
   * @param tag
   * @return
   */
  public void rollback(String tag) throws Exception {
    File backup = new File(BACKUP_TAGGED_DIRECTORY, tag + ".zip");
    if (!backup.isFile()) {
      throw new Exception("Backup does not exist!");
    }
  }

  /**
   * Rollback to server status at backup
   * in archive 'backup'.
   * @param backup Zip archive containing correct server backup
   */
  private void rollback(File backup) {

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
            println(e);
            println("Automated Server Backup Failure!");
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
