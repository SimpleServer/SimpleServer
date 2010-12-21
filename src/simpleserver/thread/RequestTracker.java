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

import java.util.HashMap;
import java.util.Map;

import simpleserver.Server;

public class RequestTracker {
  private static final int MAX_REQUESTS = 30;
  private static final int CLEAR_SECONDS = 60;

  private final Server server;
  private final Map<String, Integer> requests;
  private final Tracker tracker;

  private volatile boolean run = true;

  public RequestTracker(Server server) {
    this.server = server;
    requests = new HashMap<String, Integer>();

    tracker = new Tracker();
    tracker.start();
    tracker.setName("RequestTracker");
  }

  public void stop() {
    run = false;
    tracker.interrupt();
  }

  public synchronized void addRequest(String ipAddress) {
    Integer count = requests.get(ipAddress);
    if (count == null) {
      count = 0;
    }

    requests.put(ipAddress, ++count);
    if (count > MAX_REQUESTS) {
      server.adminLog("RequestTracker banned " + ipAddress
          + ":\t Too many requests!");
      server.banKickIP(ipAddress, "Banned: Too many requests!");
    }
  }

  private final class Tracker extends Thread {
    @Override
    public void run() {
      while (run) {
        synchronized (RequestTracker.this) {
          requests.clear();
        }

        try {
          Thread.sleep(CLEAR_SECONDS * 1000);
        }
        catch (InterruptedException e) {
        }
      }
    }
  }
}
