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
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FileSystemUtils;

public class AutoFreeSpaceChecker {
  private static final File BACKUP_DIRECTORY = new File("backups");
  private static final int period = 5 * 60 * 1000;

  private final Timer timer;

  public AutoFreeSpaceChecker() {
    check();

    timer = new Timer();
    timer.schedule(new Checker(), 0, period);
  }

  public void check() {
    try {
      while (FileSystemUtils.freeSpaceKb() < 50 * 1024) {
        File[] files = BACKUP_DIRECTORY.listFiles(new FileFilter() {
          public boolean accept(File file) {
            return file.isFile() && file.getPath().contains(".zip");
          }
        });

        long firstCreatedTime = Long.MAX_VALUE;
        File firstCreatedFile = null;
        for (File file : files) {
          DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
          GregorianCalendar cal = new GregorianCalendar();
          Date fileTime;
          try {
            fileTime = format.parse(file.getName().split(".zip")[0]);
          } catch (ParseException e) {
            continue;
          }
          cal.setTime(fileTime);

          if (cal.getTimeInMillis() < firstCreatedTime) {
            firstCreatedFile = file;
            firstCreatedTime = cal.getTimeInMillis();
          }
        }

        if (firstCreatedFile != null) {
          firstCreatedFile.delete();
        } else {
          break;
        }
      }
    } catch (IOException e) {
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
      check();
    }
  }
}
