/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package simpleserver.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AdminLog {
  private BlockingQueue<String> queue;
  private FileOutputStream stream;
  private Writer writer;

  private boolean run = true;

  public AdminLog() {
    queue = new LinkedBlockingQueue<String>();
    try {
      stream = new FileOutputStream(getLogFile(), true);
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
      System.out.println("Unable to open admin log for writing!");
      return;
    }

    writer = new Writer();
    writer.start();
  }

  public void addMessage(String message) {
    Calendar date = Calendar.getInstance();
    queue.add("[SimpleServer]\t" + date.get(Calendar.HOUR_OF_DAY) + ":"
        + date.get(Calendar.MINUTE) + "\t" + message + "\n");
  }

  public void stop() {
    run = false;
    writer.interrupt();
  }

  private File getLogFile() {
    File logDir = new File("logs");
    logDir.mkdir();

    Calendar date = Calendar.getInstance();
    return new File(logDir, "adminlog_" + date.get(Calendar.YEAR) + "-"
        + (date.get(Calendar.MONTH) + 1) + "-" + date.get(Calendar.DATE) + "-"
        + date.get(Calendar.HOUR_OF_DAY) + "_" + date.get(Calendar.MINUTE)
        + ".txt");
  }

  private final class Writer extends Thread {
    public void run() {
      try {
        while (run) {
          String line;
          try {
            line = queue.take();
          }
          catch (InterruptedException e1) {
            continue;
          }

          try {
            stream.write(line.getBytes());
            stream.flush();
          }
          catch (IOException e) {
            e.printStackTrace();
            System.out.println("Writing to admin log failed!");
            break;
          }
        }
      }
      finally {
        try {
          stream.close();
        }
        catch (IOException e) {
        }
      }
    }
  }
}
