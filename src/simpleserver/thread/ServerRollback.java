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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import simpleserver.Server;

public class ServerRollback {
  private static final File TMP_DIRECTORY = new File("tmp");

  private final Server server;
  private String filename;
  private boolean quick;
  private Rollback thread;
  private boolean serverDown = false;

  public ServerRollback(Server server) {
    thread = new Rollback();
    this.server = server;
  }

  public boolean getWaiting() {
    return thread.getWaiting();
  }

  public void initiate(String filename, boolean quick) {
    this.filename = filename;
    this.quick = quick;
    thread = new Rollback();
    thread.start();
  }

  public void replace() {
    if (thread.getWaiting()) {
      serverDown = true;

      while (!thread.getFinished()) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
      }
    }
  }

  private class Rollback extends Thread {
    private boolean waiting = false;
    private boolean finished = true;

    Rollback() {
      super();
      setName("Rollback");
    }

    @Override
    public void run() {
      finished = false;

      File backup = new File(filename);
      if (!backup.exists()) {
        System.out.println("[SimpleServer] There is no such backup!");
        return;
      }

      if (!quick) {
        server.forceBackup();

        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

        while (TMP_DIRECTORY.exists()) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
          }
        }
      }

      System.out.println("[SimpleServer] [Rollback] Extracting the backup archive...");

      String tempName = "rollback_" + backup.getName().split(".zip")[0];
      File tempDirectory = new File(TMP_DIRECTORY, tempName);

      ZipFile zipFile = null;
      InputStream inputStream = null;

      try {
        zipFile = new ZipFile(backup);
        Enumeration<? extends ZipEntry> oEnum = zipFile.entries();
        while (oEnum.hasMoreElements()) {
          ZipEntry zipEntry = oEnum.nextElement();
          File file = new File(tempDirectory, zipEntry.getName());

          if (zipEntry.isDirectory()) {
            file.mkdirs();
          } else {
            inputStream = zipFile.getInputStream(zipEntry);
            write(inputStream, file);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
        tempDirectory.delete();
        System.out.println("[SimpleServer] [Rollback] Roll-back did not finish propertly.");
      } finally {
        try {
          if (zipFile != null) {
            zipFile.close();
          }
          if (inputStream != null) {
            inputStream.close();
          }
        } catch (IOException e) {
        }
      }

      System.out.println("[SimpleServer] [Rollback] Backup archive extracted succesfully.");

      waiting = true;
      server.restart();
      while (!serverDown) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
      }
      waiting = false;

      boolean success = false;

      File backup_config = new File(tempDirectory, "config");
      // replace configurations folder
      File config = new File("simpleserver");
      deleteRecursively(config);
      if (backup_config.renameTo(config)) {
        // replace simpleserver.properties
        File server_properties = new File("simpleserver.properties");
        server_properties.delete();
        if (new File(config, "simpleserver.properties").renameTo(server_properties)) {
          // replace bukkit.yml
          File old_bukkit = new File(config, "bukkit.yml");
          if (old_bukkit.exists()) {
            File bukkit_yml = new File("bukkit.yml");
            bukkit_yml.delete();
            success = old_bukkit.renameTo(bukkit_yml);
          } else {
            success = true;
          }
        }
      }
      if (success) {
        System.out.println("[SimpleServer] [Rollback] Configuration files were moved successfully.");
      } else {
        System.out.println("[SimpleServer] [Rollback] Failed to move configuration files.");
      }

      File backup_plugins = new File(tempDirectory, "plugins");
      if (backup_plugins.exists()) {
        // replace plugins folder
        File plugins = new File("plugins");
        deleteRecursively(plugins);
        if (backup_plugins.renameTo(plugins)) {
          System.out.println("[SimpleServer] [Rollback] Plugins folder was moved successfully.");
        } else {
          System.out.println("[SimpleServer] [Rollback] Failed to move the plugins folder.");
        }
      }

      String netherName = server.options.get("levelName") + "_nether";
      File backup_nether = new File(tempDirectory, netherName);
      if (backup_nether.exists()) {
        // replace "world_nether" folder
        File world_nether = new File(netherName);
        deleteRecursively(world_nether);
        if (backup_nether.renameTo(world_nether)) {
          System.out.println("[SimpleServer] [Rollback] The \"world_nether\" folder was moved successfully.");
        } else {
          System.out.println("[SimpleServer] [Rollback] Failed to move the \"world_nether\" folder.");
        }
      }

      // replace world folder
      File world = new File(server.options.get("levelName"));
      deleteRecursively(world);
      if (tempDirectory.renameTo(world)) {
        System.out.println("[SimpleServer] [Rollback] World folder was moved successfully.");
      } else {
        System.out.println("[SimpleServer] [Rollback] Failed to move world folder.");
      }

      // clean-up
      deleteRecursively(TMP_DIRECTORY);

      finished = true;
    }

    public boolean getFinished() {
      return finished;
    }

    public boolean getWaiting() {
      return waiting;
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

  private void write(InputStream inputStream, File fileToWrite) throws IOException {
    BufferedInputStream buffInputStream = new BufferedInputStream(inputStream);
    FileOutputStream fos = new FileOutputStream(fileToWrite);
    BufferedOutputStream bos = new BufferedOutputStream(fos);

    int byteData;
    while ((byteData = buffInputStream.read()) != -1) {
      bos.write((byte) byteData);
    }

    bos.close();
    fos.close();
    buffInputStream.close();
  }
}
