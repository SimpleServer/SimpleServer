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
package simpleserver.command;

import java.io.File;
import java.io.FileFilter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.Server;
import simpleserver.thread.AutoBackup;

public abstract class RollbackCommand extends AbstractCommand {
  private static final File BACKUP_DIRECTORY = new File("backups");
  private static final String BACKUP_FORMAT = BACKUP_DIRECTORY + File.separator + "%s.zip";

  private Player player;
  private Server server;

  public RollbackCommand() {
    super("rollback [help|latest|#|name] [quick]", "Restore the server to a backup file");
  }

  public void execute(String[] arguments, Player executor) {
    player = executor;
    execute(arguments);
  }

  public void execute(String[] arguments, Server executor) {
    server = executor;
    execute(arguments);
  }

  private void execute(String[] arguments) {
    HashMap<String, String> backups = getTenLatest();
    Server target = server;
    if (player != null) {
      target = player.getServer();
    }

    if (arguments.length == 0) {
      if (backups.values().isEmpty()) {
        tInfo("There are no backups!");
      } else {
        tInfo("Latest backups:");
        for (int x = 1; x <= 10; x++) {
          String shortCode = backups.get(Integer.valueOf(x).toString());
          if (shortCode != null) {
            info("#" + x + ": " + shortCode);
          }
        }
      }
      return;
    }

    String argument = arguments[0];
    boolean quick = false;
    if (arguments.length > 1) {
      quick = arguments[1].equals("quick");
    }

    if (argument.equals("help")) {
      String rollback = commandPrefix() + "rollback";
      tInfo("Usage:");
      tInfo("%s: list 10 latest backups", rollback);
      tInfo("%s: use the latest backup", rollback + " latest");
      tInfo("%s: use backup with that number", rollback + " #");
      tInfo("%s: use backup with that filename", rollback + " name");
    } else if (argument.equals("latest")) {
      File latest = AutoBackup.newestBackup();
      if (latest == null) {
        tError("No backups found!");
        return;
      }

      if (player != null) {
        if (quick) {
          target.adminLog("User " + player.getName() + " initiated a quick roll-back to latest backup!");
          System.out.println("[SimpleServer] " + player.getName() + " initiated a quick roll-back to latest backup.");
        } else {
          target.adminLog("User " + player.getName() + " initiated a roll-back to latest backup!");
          System.out.println("[SimpleServer] " + player.getName() + " initiated a roll-back to latest backup.");
        }
      }

      target.rollback.initiate(latest.getPath(), quick);
    } else {
      if (player != null) {
        if (quick) {
          target.adminLog("User " + player.getName() + " initiated a quick roll-back to " + argument + "!");
          System.out.println("[SimpleServer] " + player.getName() + " initiated a quick roll-back to " + argument);
        } else {
          target.adminLog("User " + player.getName() + " initiated a roll-back to " + argument + "!");
          System.out.println("[SimpleServer] " + player.getName() + " initiated a roll-back to latest backup." + argument);
        }
      }

      String shortCode = null;
      try {
        String num = Integer.valueOf(argument).toString();
        shortCode = backups.get(num);
      } catch (Exception e) {
      }

      if (shortCode != null) {
        target.rollback.initiate(String.format(BACKUP_FORMAT, shortCode), quick);
      } else {
        target.rollback.initiate(String.format(BACKUP_FORMAT, argument), quick);
      }
    }

    server = null;
    player = null;
  }

  protected void tError(String message, Object... args) {
    if (player != null) {
      player.addTMessage(Color.RED, message, args);
    } else {
      System.out.println("[SimpleServer] " + String.format(message, args));
    }
  }

  protected void tInfo(String message, Object... args) {
    if (player != null) {
      player.addTMessage(Color.GRAY, message, args);
    } else {
      System.out.println("[SimpleServer] " + String.format(message, args));
    }
  }

  protected void info(String message, Object... args) {
    if (player != null) {
      player.addMessage(Color.GRAY, message, args);
    } else {
      System.out.println("[SimpleServer] " + String.format(message, args));
    }
  }

  protected HashMap<String, String> getTenLatest() {
    final HashMap<String, String> list = new HashMap<String, String>();

    BACKUP_DIRECTORY.mkdir();
    for (int x = 1; x <= 10; x++) {
      File[] files = BACKUP_DIRECTORY.listFiles(new FileFilter() {
        public boolean accept(File file) {
          for (String listedFile : list.values()) {
            if (listedFile.equals(file.getName().split(".zip")[0])) {
              return false;
            }
          }
          return file.isFile() && file.getPath().contains(".zip");
        }
      });

      if (files.length == 0) {
        break;
      }

      long firstCreatedTime = 0;
      File firstCreatedFile = null;
      for (File file : files) {
        long date;
        try {
          date = date(file);
        } catch (ParseException e) {
          continue;
        }

        if (date > firstCreatedTime) {
          firstCreatedFile = file;
          firstCreatedTime = date;
        }
      }

      if (firstCreatedFile != null) {
        list.put(Integer.valueOf(x).toString(), firstCreatedFile.getName().split(".zip")[0]);
      }
    }

    return list;
  }

  private long date(File file) throws ParseException {
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
    GregorianCalendar cal = new GregorianCalendar();
    Date fileTime;
    fileTime = format.parse(file.getName().split(".zip")[0]);
    cal.setTime(fileTime);
    return cal.getTimeInMillis();
  }
}
