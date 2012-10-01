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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.IOException;
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

import org.apache.commons.io.FileUtils;

import simpleserver.Server;
import simpleserver.util.IO;

public class AutoBackup {
  private static final String VERSION = "1"; //version of the backup system for compatibility
  private static final long MILLISECONDS_PER_MINUTE = 1000 * 60;
  private static final long MILLISECONDS_PER_HOUR = MILLISECONDS_PER_MINUTE * 60;
  private static final String NAME_FORMAT = "%tF-%1$tH-%1$tM";
  private static final File BACKUP_BASE_DIRECTORY = new File("backups");
  private static final File BACKUP_AUTO_DIRECTORY = new File(BACKUP_BASE_DIRECTORY, "auto");
  private static final File BACKUP_TAGGED_DIRECTORY = new File(BACKUP_BASE_DIRECTORY, "tagged");
  private static final File TEMP_DIRECTORY = new File("tmp");
  private static final String BACKUP_CONFIG_FOLDER = "config";
  private static final String BACKUP_MAP_FOLDER = "map";

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
  
  //Filter to exclude unimportant files
  private static final FileFilter filter = new FileFilter() {
    @Override
    public boolean accept(File pathname) {
      return (!pathname.getName().equals("level.dat_old"));
    }
  };

  private final Server server;
  private final byte[] copyBuffer;
  private final Archiver archiver;

  private volatile boolean run = true;
  private volatile boolean forceBackup = false;
  private volatile boolean pauseBackup = false;
  //private volatile boolean rollback = false;

  private volatile String tag = null; // tag for next backup ('null' means
                                      // date/no tagged backup)
  private volatile File rollback = null; //backup to roll back to (initiate rollback by setting != null)

  public AutoBackup(Server server) {
    this.server = server;

    // Create backup directories if not present
    BACKUP_AUTO_DIRECTORY.mkdirs();
    BACKUP_TAGGED_DIRECTORY.mkdirs();

    //initialize resource directories
    RESOURCES_MAP.add(server.getWorldDirectory());
    if (server.isBukkitServer()) {
      RESOURCES_CONFIG.addAll(RESOURCE_DIRS_CONFIG_BUKKIT);
    }

    purgeOldBackups();

    copyBuffer = new byte[8192];

    archiver = new Archiver();
    archiver.start();
    archiver.setName("AutoBackup");
  }

  /**
   * Stop the system / thread.
   * Note that it does not stop immediately if a rollback is being peformed.
   */
  public void stop() {
    run = false;
    if (rollback == null) {
      archiver.interrupt();
    }
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
      this.tag = null; //reset tag switch
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

  /**
   * Prepare the backup archive.
   * @return
   * @throws IOException 
   */
  private File makeTemporaryCopy() throws IOException {
    Date date = new Date();
    File backup = new File(TEMP_DIRECTORY, String.format(NAME_FORMAT, date));
    File backupConfig = new File(backup, BACKUP_CONFIG_FOLDER);
    File backupMap = new File(backup, BACKUP_MAP_FOLDER);

    for (File file : RESOURCES_CONFIG) {
      copy(file, new File(backupConfig, file.getName()));
    }

    for (File file : RESOURCES_MAP) {
      copy(file, new File (backupMap, file.getName()));
    }
    
    //Create backup info file
    PrintWriter out = new PrintWriter(new File(backup, "backup.info"));
    out.println("Backup system version: " + VERSION);
    out.print("Backup date: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date));
    out.close();

    return backup;
  }

  /**
   * Zip and name the temporary created backup.
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
    IO.zip(source, backup);
    
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

  /**
   * Copy file or directory 'source' to 'target'-directoy,
   * which is created if non-existent.
   * Files not accepted by 'filter' are ignored.
   * @param source
   * @param target
   * @throws IOException 
   */
  private void copy(File source, File target) throws IOException {
    if (source.isDirectory()) {
      FileUtils.copyDirectory(source, target, filter);
    } else {
      if (filter.accept(source)) {
        FileUtils.copyFile(source, target);
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

  /**
   * Get newest / oldest backup (auto backup).
   * @param old
   * @return 
   */
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
      rollback = backups[n-1];
      archiver.interrupt();
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
    rollback = new File(BACKUP_TAGGED_DIRECTORY, tag + ".zip");
    if (!rollback.isFile()) {
      rollback = null;
      throw new Exception("Backup does not exist!");
    }
    archiver.interrupt();
  }

  /**
   * Rollback to server status at backup 'rollback'.
   */
  private void rollback() throws IOException {
    IO.unzip(rollback, TEMP_DIRECTORY);
    //collect files to restore, delete present files
    List<File> backup = new ArrayList<File>();
    File backupConfig = new File(TEMP_DIRECTORY, BACKUP_CONFIG_FOLDER);
    for (File file : RESOURCES_CONFIG) {
      deleteRecursively(file);
      backup.add(new File(backupConfig, file.getName()));
    }
    File backupMap = new File(TEMP_DIRECTORY, BACKUP_MAP_FOLDER);
    for (File file : RESOURCES_MAP) {
      deleteRecursively(file);
      backup.add(new File(backupMap, file.getName()));
    }
    File dest = new File(".");
    for (File file : backup) {
      if (file.isDirectory()) {
        FileUtils.copyDirectoryToDirectory(file, dest);
      } else {
        FileUtils.copyFileToDirectory(file, dest);
      }
    }
    deleteRecursively(TEMP_DIRECTORY);
  }

  private final class Archiver extends Thread {
    @Override
    public void run() {
      while (run) {
        if (needsBackup()) {
          doBackup();
        } else if (rollback != null) {
          tag = null;
          doBackup();
          doRollback();
          rollback = null;
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
    
    private void doBackup() {
      try {
            server.saveLock.acquire();
          } catch (InterruptedException e) {
            return;
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
    
    private void doRollback() {
      server.manualRestart();
      try {
        rollback();
      } catch (IOException ex) {
        println("Rollback failure: " + ex);
      }
      server.continueRestart();
    }
  }
}
