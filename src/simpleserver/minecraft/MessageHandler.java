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
package simpleserver.minecraft;

import simpleserver.Server;
import simpleserver.command.InvalidCommand;
import simpleserver.command.ServerCommand;

public class MessageHandler {
  private final Server server;

  private boolean loaded = false;

  public MessageHandler(Server server) {
    this.server = server;
  }

  public synchronized void waitUntilLoaded() throws InterruptedException {
    if (!loaded) {
      wait();
    }
  }

  public void handleError(Exception exception) {
    if (!server.isRestarting() && !server.isStopping()) {
      if (exception != null) {
        exception.printStackTrace();
      }

      String baseError = "[SimpleServer] Minecraft process stopped unexpectedly!";
      if (server.options.getBoolean("exitOnFailure")) {
        System.out.println(baseError);
        server.stop();
      }
      else {
        System.out.println(baseError + " Automatically restarting...");
        server.restart();
      }
    }
  }

  public void handleQuit() {
    handleError(null);
  }

  public void handleOutput(String line) {
    if (!server.options.getBoolean("debug") && line.contains("\tat")) {
      return;
    }

    Integer[] ports = server.getRobotPorts();
    if (ports != null) {
      for (int i = 0; i < ports.length; i++) {
        if (ports[i] != null) {
          if (line.contains(ports[i].toString())) {
            server.removeRobotPort(ports[i]);
            return;
          }
        }
      }
    }

    if (line.contains("[INFO] Done! ")) {
      synchronized (this) {
        loaded = true;
        notifyAll();
      }
    }
    else if (line.contains("[INFO] CONSOLE: Save complete.")) {
      server.setSaving(false);
      server.runCommand("say", server.l.get("SAVE_COMPLETE"));
    }
    else if (line.contains("[SEVERE] Unexpected exception")) {
      handleError(new Exception(line));
    }

    server.addOutputLine(line);
    System.out.println(line);
  }

  public boolean parseCommand(String line) {
    ServerCommand command = server.getCommandParser().getServerCommand(line);
    if ((command != null) && (command.getClass() != InvalidCommand.class)) {
      command.execute(server, line);
      return !command.shouldPassThroughToConsole();
    }
    return false;
  }
}
