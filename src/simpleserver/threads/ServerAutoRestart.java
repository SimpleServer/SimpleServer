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
package simpleserver.threads;

import simpleserver.Server;

public class ServerAutoRestart {
  private static final long MILLISECONDS_PER_MINUTE = 1000 * 60;

  private final Server server;
  private final Restarter restarter;

  private boolean run = true;
  private long lastRestart;

  public ServerAutoRestart(Server server) {
    this.server = server;

    lastRestart = System.currentTimeMillis();

    restarter = new Restarter();
    restarter.start();
  }

  public void stop() {
    run = false;
    restarter.interrupt();
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
          server.runCommand("say", server.l.get("SERVER_RESTART_60"));
          try {
            Thread.sleep(30000);
            server.runCommand("say", server.l.get("SERVER_RESTART_30"));
            Thread.sleep(27000);
            server.runCommand("say", server.l.get("SERVER_RESTART_3"));
            Thread.sleep(3000);
          }
          catch (InterruptedException e) {
            continue;
          }

          if (!server.isStopping()) {
            server.restart();
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
