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

import java.io.IOException;
import java.io.InputStream;

import simpleserver.Server;

public class AutoRun {
  private static final long MILLISECONDS_PER_MINUTE = 1000 * 60;

  private final Server server;
  private final String command;
  private final Runner runner;

  private long lastRun;
  private volatile boolean run = true;

  public AutoRun(Server server, String command) {
    this.server = server;
    this.command = command;

    lastRun = System.currentTimeMillis();

    runner = new Runner();
    runner.start();
    runner.setName("AutoRun");
  }

  public void stop() {
    run = false;
    runner.interrupt();
  }

  private boolean needsRun() {
    long maxAge = System.currentTimeMillis() - MILLISECONDS_PER_MINUTE
        * server.options.getInt("c10tMins");
    return server.options.contains("c10tArgs") && maxAge > lastRun
        && server.numPlayers() > 0;
  }

  private static final class OutputConsumer extends Thread {
    private final InputStream in;

    private OutputConsumer(InputStream in) {
      this.in = in;
    }

    @Override
    public void run() {
      byte[] buf = new byte[256];
      try {
        while (in.read(buf) >= 0) {
        }
      }
      catch (IOException e) {
      }
    }
  }

  private final class Runner extends Thread {
    @Override
    public void run() {
      while (run) {
        if (needsRun()) {
          try {
            server.saveLock.acquire();
          }
          catch (InterruptedException e) {
            continue;
          }
          server.runCommand("say", "Mapping Server!");
          server.runCommand("save-off", null);

          lastRun = System.currentTimeMillis();
          try {
            Process process;
            try {
              process = Runtime.getRuntime().exec(command);
            }
            catch (IOException e) {
              server.runCommand("say", "Mapping Failed!");
              e.printStackTrace();
              System.out.println("[SimpleServer] Cron Failed! Bad Command!");
              server.errorLog(e, "AutoRun Failure");
              continue;
            }

            new OutputConsumer(process.getInputStream()).start();
            new OutputConsumer(process.getErrorStream()).start();

            int exitCode;
            while (true) {
              try {
                exitCode = process.waitFor();
                break;
              }
              catch (InterruptedException e) {
                if (!run) {
                  process.destroy();
                }
              }
            }

            if (exitCode < 0) {
              System.out.println("[SimpleServer] c10t Failed! Exited with code "
                  + exitCode + "!");
              server.runCommand("say", "Mapping Failed!");
            }
            else {
              server.runCommand("say", "Mapping Complete!");
            }
          }
          finally {
            server.runCommand("save-on", null);
            server.saveLock.release();
          }
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
