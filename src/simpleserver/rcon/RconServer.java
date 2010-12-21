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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import simpleserver.Server;

public class RconServer {
  private Server server;
  private ServerSocket socket;

  private List<RconTCP> connections;
  private Listener listener;

  private boolean run = true;

  public RconServer(Server server) {
    this.server = server;

    connections = new LinkedList<RconTCP>();
    listener = new Listener();
    listener.start();
    listener.setName("RconServer");
  }

  public void stop() {
    run = false;
    if (socket != null) {
      try {
        socket.close();
      }
      catch (IOException e) {
      }
    }
    for (RconTCP connection : connections) {
      connection.close();
    }
  }

  private class Listener extends Thread {
    @Override
    public void run() {
      int port = server.options.getInt("rconPort");
      try {
        socket = new ServerSocket(port);
      }
      catch (IOException e) {
        System.out.println("Could not listen on port " + port
            + "!\nIs it already in use? RCON is not available!");
        return;
      }
      System.out.println("Opened RCON on port: " + port + "!");

      try {
        while (run) {
          Socket client;
          try {
            client = socket.accept();
          }
          catch (IOException e) {
            if (run) {
              e.printStackTrace();
              System.out.println("RCON server failed!");
            }
            break;
          }
          connections.add(new RconTCP(client, server));
        }
      }
      finally {
        try {
          socket.close();
        }
        catch (IOException e) {
        }
      }
    }
  }
}
