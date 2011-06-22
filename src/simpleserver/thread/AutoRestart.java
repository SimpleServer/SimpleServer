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
import simpleserver.Server;

public class AutoRestart {
  private static final long MILLISECONDS_PER_MINUTE = 1000 * 60;

  private final Server server;
  private final Restarter restarter;

  private long lastRestart;
  private volatile boolean run = true;

  public AutoRestart(Server server) {
    this.server = server;

    lastRestart = System.currentTimeMillis();

    restarter = new Restarter();
    restarter.start();
    restarter.setName("AutoRestart");
  }

  public void stop() {
    run = false;
    restarter.interrupt();
  }

  public void announce(String message) {
    if (server.options.getBoolean("announceRestart")) {
      server.runCommand("say", message);
    }
  }

  private boolean needsRestart() {
    long maxAge = System.currentTimeMillis() - MILLISECONDS_PER_MINUTE
        * server.options.getInt("autoRestartMins");
    return server.options.getBoolean("autoRestart") && maxAge > lastRestart;
  }

  private final class Restarter extends Thread {
    @Override
    public void run() {
      while (run) {
        if (needsRestart()) {
          announce(t("Server is restarting in 60 seconds!"));
          try {
            Thread.sleep(30000);
            announce(t("Server is restarting in 30 seconds!"));
            Thread.sleep(27000);
            announce(t("Server is restarting in 3 seconds!"));
            Thread.sleep(3000);
          } catch (InterruptedException e) {
            continue;
          }

          if (!server.isStopping()) {
            server.restart();
          }
        }

        try {
          Thread.sleep(60000);
        } catch (InterruptedException e) {
        }
      }
    }
  }
}
