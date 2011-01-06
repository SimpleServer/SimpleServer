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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
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
    System.out.println("[SimpleServer] Backing up server...");
    server.runCommand("say", server.l.get("BACKING_UP"));
    server.runCommand("save-off", null);

    File copy;
    try {
      copy = makeTemporaryCopy();
      server.runCommand("save-on", null);

      zipBackup(copy);
    }
    finally {
      deleteRecursively(TEMP_DIRECTORY);
    }
    purgeOldBackups();
    server.runCommand("say", server.l.get("BACKUP_COMPLETE"));
  }

  private boolean needsBackup() {
    long backupPeriod = MILLISECONDS_PER_MINUTE
        * server.options.getInt("autoBackupMins");
    return server.options.getBoolean("autoBackup")
        && backupPeriod < lastBackupAge() && server.numPlayers() > 0
        || forceBackup;
  }

  private long lastBackupAge() {
    BACKUP_DIRECTORY.mkdir();

    long newest = 0;
    for (File file : BACKUP_DIRECTORY.listFiles()) {
      long modified = file.lastModified();
      if (modified > newest) {
        newest = modified;
      }
    }
    return System.currentTimeMillis() - newest;
  }

  private void purgeOldBackups() {
    BACKUP_DIRECTORY.mkdir();
    long maxAge = MILLISECONDS_PER_HOUR
        * server.options.getInt("keepBackupHours");
    for (File file : BACKUP_DIRECTORY.listFiles()) {
      if (System.currentTimeMillis() - file.lastModified() > maxAge) {
        deleteRecursively(file);
      }
    }
  }

  private File makeTemporaryCopy() throws IOException {
    TEMP_DIRECTORY.mkdir();
    File backup = new File(TEMP_DIRECTORY, String.format(NAME_FORMAT,
                                                         new Date()));
    copyRecursively(new File(server.options.get("levelName")), backup);
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
      }
      finally {
        out.close();
      }
    }
    finally {
      fout.close();
    }
    System.out.println("[SimpleServer] Backup saved: " + backup.getPath());
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
    }
    else {
      ZipEntry entry = new ZipEntry(name);
      out.putNextEntry(entry);

      FileInputStream in = new FileInputStream(source);
      try {
        int length;
        while ((length = in.read(copyBuffer)) != -1) {
          out.write(copyBuffer, 0, length);
          Thread.yield();
        }
      }
      finally {
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
    }
    else {
      InputStream in = new FileInputStream(source);
      OutputStream out = new FileOutputStream(target);

      try {
        int length;
        while ((length = in.read(copyBuffer)) > 0) {
          out.write(copyBuffer, 0, length);
        }
      }
      finally {
        try {
          in.close();
        }
        finally {
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
        }
        else {
          file.delete();
        }
      }
    }
    path.delete();
  }

  private final class Archiver extends Thread {
    @Override
    public void run() {
      while (run) {
        if (needsBackup()) {
          try {
            server.saveLock.acquire();
          }
          catch (InterruptedException e) {
            continue;
          }
          forceBackup = false;

          server.runCommand("say", server.l.get("SAVING_MAP"));
          server.setSaving(true);
          server.runCommand("save-all", null);
          while (server.isSaving()) {
            try {
              Thread.sleep(100);
            }
            catch (InterruptedException e) {
            }
          }

          try {
            backup();
          }
          catch (IOException e) {
            server.errorLog(e, "Server Backup Failure");
            e.printStackTrace();
            System.out.println("[WARNING] Automated Server Backup Failure!");
          }
          server.saveLock.release();
        }

        try {
          Thread.sleep(60000);
        }
        catch (InterruptedException e) {
        }
      }
    }
  }
}
