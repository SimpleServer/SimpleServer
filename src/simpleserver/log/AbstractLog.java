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
package simpleserver.log;

import static simpleserver.util.Util.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AbstractLog {
  private String name;
  protected final boolean enabled;
  private BlockingQueue<String> queue;
  private FileOutputStream stream;
  private Logger logger;

  private boolean logEmpty = true;
  private File filename;

  private volatile boolean run = true;

  protected AbstractLog(String name, boolean enabled) {
    this.name = name;
    this.enabled = enabled;
    if (enabled) {
      queue = new LinkedBlockingQueue<String>();
      try {
        filename = getLogFile();
        stream = new FileOutputStream(filename, true);
      } catch (FileNotFoundException e) {
        print(e);
        print("Unable to open " + name
            + " log for writing!");
        return;
      }

      logger = new Logger();
      logger.start();
      logger.setName("Logger-" + name);
    }
  }

  protected AbstractLog(String name) {
    this(name, true);
  }

  protected void addMessage(String message) {
    if (enabled) {
      logEmpty = false;
      queue.add(String.format("%tF %1$tT\t%2$s\n", new Date(), message));
    }
  }

  public void stop() {
    run = false;
    if (enabled) {
      logger.interrupt();
    }
  }

  private File getLogFile() {
    File logDir = new File("logs");
    logDir.mkdir();

    String fileName = String.format("%s_%tY%2$tm%2$td-%2$tH%2$tM%2$tS.txt",
                                    name, new Date());
    return new File(logDir, fileName);
  }

  private final class Logger extends Thread {
    @Override
    public void run() {
      try {
        while (run) {
          String line;
          try {
            line = queue.take();
          } catch (InterruptedException e1) {
            continue;
          }

          try {
            stream.write(line.getBytes());
            stream.flush();
          } catch (IOException e) {
            print(e);
            print("Writing to " + name
                + " log failed!");
            break;
          }
        }
      } finally {
        try {
          stream.close();
          if (logEmpty) {
            filename.delete();
          }
        } catch (IOException e) {
        }
      }
    }
  }
}
