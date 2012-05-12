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
package simpleserver.command;

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.Server;

public class BackupCommand extends AbstractCommand implements PlayerCommand,
    ServerCommand {
  public BackupCommand() {
    super("backup [tag]", "Backup the map with optional tag");
  }

  public void execute(final Player player, final String message) {
    execute(new Com() {
      public void sendMsg(String m) {
        player.addTMessage(Color.GRAY, m);
      }

      public Server getServer() {
        return player.getServer();
      }

      public String getMessage() {
        return message;
      }

    });
  }

  public void execute(final Server server, final String message, final CommandFeedback feedback) {
    execute(new Com() {
      public void sendMsg(String m) {
        feedback.send(m);
      }

      public Server getServer() {
        return server;
      }

      public String getMessage() {
        return message;
      }
    });
  }

  /**
   * Interface to get data from or communicate with caller (server or player)
   */
  public interface Com {
    void sendMsg(String m);

    Server getServer();

    String getMessage();
  }

  private void execute(Com com) {
    String[] arguments = extractArguments(com.getMessage());
    if (arguments.length > 1) {
      com.sendMsg("Wrong number of Arguments!");
      return;
    }
    com.sendMsg("Forcing backup!");
    if (arguments.length == 0) { // without tag
      com.getServer().forceBackup();
    } else { // 'arguments.length == 1': with tag
      com.getServer().forceBackup(arguments[0]);
    }
  }
}
