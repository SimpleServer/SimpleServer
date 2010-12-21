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

import simpleserver.Server;

public class AutoSave {
  private static final long MILLISECONDS_PER_MINUTE = 1000 * 60;

  private final Server server;
  private final Saver saver;

  private long lastSave;
  private volatile boolean run = true;
  private volatile boolean forceSave = false;

  public AutoSave(Server server) {
    this.server = server;

    lastSave = System.currentTimeMillis();

    saver = new Saver();
    saver.start();
    saver.setName("AutoSave");
  }

  public void stop() {
    run = false;
    saver.interrupt();
  }

  public void forceSave() {
    forceSave = true;
    saver.interrupt();
  }

  private boolean needsSave() {
    long maxAge = System.currentTimeMillis() - MILLISECONDS_PER_MINUTE
        * server.options.getInt("autoSaveMins");
    return server.options.getBoolean("autoSave") && maxAge > lastSave
        && server.numPlayers() > 0 || forceSave;
  }

  private final class Saver extends Thread {
    @Override
    public void run() {
      while (run) {
        if (needsSave()) {
          try {
            server.saveLock.acquire();
          }
          catch (InterruptedException e) {
            continue;
          }
          forceSave = false;

          server.runCommand("say", server.l.get("SAVING_MAP"));
          server.setSaving(true);
          server.runCommand("save-all", null);
          while (server.isSaving()) {
            try {
              Thread.sleep(100);
            }
            catch (InterruptedException e) {
            }
          }

          server.saveLock.release();
          lastSave = System.currentTimeMillis();
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
