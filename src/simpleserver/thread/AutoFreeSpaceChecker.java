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

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FileUtils;

import simpleserver.Server;

public class AutoFreeSpaceChecker {
  private static final int period = 5 * 60 * 1000;

  private final Timer timer;
  private final Server server;

  public AutoFreeSpaceChecker(Server server) {
    check(false);

    timer = new Timer();
    timer.schedule(new Checker(), 0, period);

    this.server = server;
  }

  public void check(boolean beforeBackup) {
    try {
      long neededSizeKb;
      if (beforeBackup) {
        neededSizeKb = Math.round(FileUtils.sizeOfDirectory(new File(server.options.get("levelName"))) / 1024) * 2;
      } else {
        neededSizeKb = 50 * 1024;
      }

      long freeSpaceKb = FileSystemUtils.freeSpaceKb();
      if (freeSpaceKb < neededSizeKb) {
        System.out.println("[SimpleServer] Warning: You have only " +
                           Math.round(freeSpaceKb / 1024) +
                           " MB free space in this drive!");
        System.out.println("[SimpleServer] Trying to delete old backups...");

        int filesDeleted = 0;
        while (FileSystemUtils.freeSpaceKb() < neededSizeKb) {
          File firstCreatedFile = AutoBackup.oldestBackup();

          if (firstCreatedFile != null) {
            System.out.println("[SimpleServer] Deleting: " + firstCreatedFile.getPath());
            firstCreatedFile.delete();
            filesDeleted++;
          } else {
            System.out.println("[SimpleServer] No backups found...");
            return;
          }
        }

        System.out.println("[SimpleServer] Deleted " + filesDeleted + " backup archives.");
      }
    } catch (IOException e) {
      System.out.println("[SimpleServer] " + e);
      System.out.println("[SimpleServer] Free Space Checker Failed!");
    }
  }

  public void cleanup() {
    timer.cancel();
  }

  private final class Checker extends TimerTask {
    private Checker() {
      super();
    }

    @Override
    public void run() {
      check(false);
    }
  }
}
