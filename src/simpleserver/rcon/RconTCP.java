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
package simpleserver.rcon;

import java.io.IOException;
import java.net.Socket;

import simpleserver.Server;

public class RconTCP {
  private Socket socket;
  long lastRead;
  private Thread t1;
  private boolean closed = false;
  boolean auth = false;

  public RconTCP(Socket socket, Server server) {
    this.socket = socket;

    System.out.println("[SimpleServer] RCON Connection from " + getIPAddress()
        + "!");
    server.requestTracker.addRequest(getIPAddress());
    if (server.isIPBanned(getIPAddress())) {
      System.out.println("[SimpleServer] IP " + getIPAddress() + " is banned!");
      close();
    }

    lastRead = System.currentTimeMillis();
    t1 = new Thread() {
      @Override
      public void run() {
        if (testTimeout()) {
          close();
          return;
        }
        try {
          Thread.sleep(1000);
        }
        catch (InterruptedException e) {
          return;
        }
      }
    };
    t1.start();

    try {
      new Thread(new RconHandler(socket, this, server)).start();
    }
    catch (IOException e) {
      e.printStackTrace();
      close();
    }
  }

  private String getIPAddress() {
    return socket.getInetAddress().getHostAddress();
  }

  public boolean testTimeout() {
    return System.currentTimeMillis() - lastRead > RconHandler.IDLE_TIME;
  }

  public boolean isClosed() {
    return closed;
  }

  public void close() {
    if (!closed) {
      closed = true;
      auth = false;

      t1.interrupt();
      try {
        socket.close();
      }
      catch (IOException e) {
      }
    }
  }
}
