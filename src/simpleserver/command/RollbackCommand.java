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

public class RollbackCommand extends AbstractCommand implements PlayerCommand,
    ServerCommand {
  private static final File BACKUP_DIRECTORY = new File("backups");
  private static final String BACKUP_FORMAT = BACKUP_DIRECTORY + File.separator + "%s.zip";

  public RollbackCommand() {
    super("rollback [help|latest|#|name] [quick]", "Restore the server to a backup file");
  }

  public void execute(Player player, String message) {
    Server server = player.getServer();
    String arguments[] = extractArguments(message);
    HashMap<String, String> backups = getTenLatest();

    if (arguments.length == 0) {
      player.addTMessage(Color.GRAY, "Latest backups:");
      for (int x = 1; x <= 10; x++) {
        String shortCode = backups.get(Integer.valueOf(x).toString());
        if (shortCode != null) {
          player.addMessage(Color.GRAY, "#" + x + ": " + shortCode);
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
      player.addTMessage(Color.GRAY, "Usage:");
      player.addTMessage(Color.GRAY, "%s: list 10 latest backups", rollback);
      player.addTMessage(Color.GRAY, "%s: use the latest backup", rollback + " latest");
      player.addTMessage(Color.GRAY, "%s: use backup with that number", rollback + " #");
      player.addTMessage(Color.GRAY, "%s: use backup with that filename", rollback + " name");
    } else if (argument.equals("latest")) {
      File latest = AutoBackup.newestBackup();
      if (latest == null) {
        player.addTMessage(Color.RED, "No backups found.");
        return;
      }

      if (quick) {
        player.getServer().adminLog("User " + player.getName() + " initiated a quick roll-back to latest backup!");
        System.out.println("[SimpleServer] " + player.getName() + " initiated a quick roll-back to latest backup.");
      } else {
        player.getServer().adminLog("User " + player.getName() + " initiated a roll-back to latest backup!");
        System.out.println("[SimpleServer] " + player.getName() + " initiated a roll-back to latest backup.");
      }

      server.rollback.initiate(latest.getPath(), quick);
    } else {
      if (quick) {
        player.getServer().adminLog("User " + player.getName() + " initiated a quick roll-back to " + argument + "!");
        System.out.println("[SimpleServer] " + player.getName() + " initiated a quick roll-back to " + argument);
      } else {
        player.getServer().adminLog("User " + player.getName() + " initiated a roll-back to " + argument + "!");
        System.out.println("[SimpleServer] " + player.getName() + " initiated a roll-back to latest backup." + argument);
      }

      String shortCode = null;
      try {
        String num = Integer.valueOf(argument).toString();
        shortCode = backups.get(num);
      } catch (Exception e) {
      }

      if (shortCode != null) {
        server.rollback.initiate(String.format(BACKUP_FORMAT, shortCode), quick);
      } else {
        server.rollback.initiate(String.format(BACKUP_FORMAT, argument), quick);
      }
    }
  }

  public void execute(Server server, String message) {
    String arguments[] = extractArguments(message);
    HashMap<String, String> backups = getTenLatest();

    if (arguments.length == 0) {
      System.out.println("[SimpleServer] Latest backups:");
      for (int x = 1; x <= 10; x++) {
        String shortCode = backups.get(Integer.valueOf(x).toString());
        if (shortCode != null) {
          System.out.println("[SimpleServer] #" + x + ": " + shortCode);
        }
      }
      return;
    }

    String argument = arguments[0];
    boolean quick = false;
    if (arguments.length > 1) {
      quick = arguments[1].equals("quick");
    }

    if (argument.equals("latest")) {
      File latest = AutoBackup.newestBackup();
      if (latest == null) {
        System.out.println("[SimpleServer] No backups found.");
        return;
      }

      server.rollback.initiate(latest.getPath(), quick);
    } else {
      String shortCode = null;
      try {
        String num = Integer.valueOf(argument).toString();
        shortCode = backups.get(num);
      } catch (Exception e) {
      }

      if (shortCode != null) {
        server.rollback.initiate(String.format(BACKUP_FORMAT, shortCode), quick);
      } else {
        server.rollback.initiate(String.format(BACKUP_FORMAT, argument), quick);
      }
    }
  }

  private HashMap<String, String> getTenLatest() {
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
