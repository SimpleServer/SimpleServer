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
package simpleserver.telnet;

import static simpleserver.util.Util.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import simpleserver.Server;

public class TelnetServer {
  private Server server;
  private ServerSocket socket;

  private List<TelnetTCP> connections;
  private Listener listener;

  private boolean run = true;

  public TelnetServer(Server server) {
    this.server = server;

    connections = new LinkedList<TelnetTCP>();
    listener = new Listener();
    listener.start();
    listener.setName("TelnetServer");
  }

  public void stop() {
    run = false;
    if (socket != null) {
      try {
        socket.close();
      } catch (IOException e) {
      }
    }
    for (TelnetTCP connection : connections) {
      connection.close();
    }
  }

  private class Listener extends Thread {
    @Override
    public void run() {
      String ip = server.options.get("ipAddress");
      String portstr = server.options.get("telnetPort");
      int port = 25678;
      try {
        port = Integer.valueOf(portstr);
      } catch (NumberFormatException e) {
      }

      InetAddress address;
      if (ip.equals("0.0.0.0")) {
        address = null;
      } else {
        try {
          address = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
          e.printStackTrace();
          System.out.println("Invalid listening address " + ip);
          return;
        }
      }

      try {
        socket = new ServerSocket(port, 0, address);
      } catch (IOException e) {
        System.out.println("Could not listen on port " + port
            + "!\nIs it already in use? Telnet is not available!");
        return;
      }

      print("Telnet listening on "
          + socket.getInetAddress().getHostAddress() + ":"
          + socket.getLocalPort());

      try {
        while (run) {
          Socket client;
          try {
            client = socket.accept();
          } catch (IOException e) {
            if (run) {
              e.printStackTrace();
              System.out.println("Telnet server failed!");
            }
            break;
          }
          connections.add(new TelnetTCP(client, server));
        }
      } finally {
        try {
          socket.close();
        } catch (IOException e) {
        }
      }
    }
  }
}
