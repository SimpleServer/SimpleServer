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

import static simpleserver.lang.Translations.t;
import static simpleserver.util.Util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import simpleserver.Server;
import simpleserver.command.CommandFeedback;
import simpleserver.command.InvalidCommand;
import simpleserver.command.ServerCommand;

public class MessageHandler {
  private final Server server;
  private final static Pattern DISCONNECT = Pattern.compile(".*\\[INFO\\] (.*) lost connection:.*");
  private final static Pattern CONNECT = Pattern.compile(".*\\[INFO\\] (.*) \\[.*\\] logged in with entity id \\d+ at .*");

  private boolean loaded = false;
  private int ignoreLines = 0;

  private CommandFeedback feedback = new CommandFeedback() {
    public void send(String message, Object... args) {
      print(String.format(message, args));
    }
  };

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
        print(exception);
      }

      String baseError = "[SimpleServer] Minecraft process stopped unexpectedly!";
      if (server.config.properties.getBoolean("exitOnFailure")) {
        System.out.println(baseError);
        server.stop();
      } else {
        System.out.println(baseError + " Automatically restarting...");
        server.restart();
      }
    }
  }

  public void handleQuit() {
    handleError(null);
  }

  public void handleOutput(String line) {
    if (!server.config.properties.getBoolean("debug") && line.contains("\tat")) {
      return;
    }

    Integer[] ports = server.getRobotPorts();
    if (ports != null) {
      for (Integer port : ports) {
        if (port != null) {
          if (line.contains(port.toString())) {
            server.removeRobotPort(port);
            return;
          }
        }
      }
    }

    if (ignoreLine()) {
      return;
    }
    if (line.contains("[INFO] Done (")) {
      synchronized (this) {
        loaded = true;
        notifyAll();
      }
    } else if (line.contains("[INFO] CONSOLE: Save complete.") || line.contains("[INFO] Save complete.")) {
      server.setSaving(false);
      if (server.options.getBoolean("announceBackup")) {
        server.runCommand("say", t("Save Complete!"));
      }
    } else if (line.contains("[SEVERE] Unexpected exception")) {
      handleError(new Exception(line));
    } else if (line.matches("^>+$")) {
      return;
    } else if (line.contains("SERVER IS RUNNING IN OFFLINE/INSECURE MODE") && server.config.properties.getBoolean("onlineMode")) {
      ignoreNextLines(3);
      return;
    } else {
      Matcher connect = CONNECT.matcher(line);
      if (connect.find()) {
        if (server.bots.ninja(connect.group(1))) {
          return;
        }
      } else {
        Matcher disconnect = DISCONNECT.matcher(line);
        if (disconnect.find()) {
          if (server.bots.ninja(disconnect.group(1))) {
            return;
          }
        }
      }
    }

    server.addOutputLine(line);
    System.out.println(line);
  }

  public boolean parseCommand(String line) {
    ServerCommand command = server.getCommandParser().getServerCommand(line.split(" ")[0]);
    if ((command != null) && !(command instanceof InvalidCommand)) {
      command.execute(server, line, feedback);
      return !command.shouldPassThroughToConsole(server);
    }
    return false;
  }

  private void ignoreNextLines(int count) {
    ignoreLines = count;
  }

  private boolean ignoreLine() {
    if (ignoreLines > 0) {
      ignoreLines--;
      return true;
    }
    return false;
  }
}
