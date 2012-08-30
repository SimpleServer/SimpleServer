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
import simpleserver.thread.AutoBackup;

public class BackupCommand extends AbstractCommand implements PlayerCommand,
    ServerCommand {
  public BackupCommand() {
    super("backup [<tag>|list] [<list size>]", "Backup the map with optional tag");
  }

  @Override
  public void execute(final Player player, final String message) {
    execute(new Com() {
      @Override
      public void sendMsg(String m) {
        player.addTMessage(Color.GRAY, m);
      }

      @Override
      public Server getServer() {
        return player.getServer();
      }

      @Override
      public String getMessage() {
        return message;
      }

    });
  }

  @Override
  public void execute(final Server server, final String message, final CommandFeedback feedback) {
    execute(new Com() {
      @Override
      public void sendMsg(String m) {
        feedback.send(m);
      }

      @Override
      public Server getServer() {
        return server;
      }

      @Override
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
    String[] args = extractArguments(com.getMessage());
    if (args.length > 2) {
      com.sendMsg("Wrong number of Arguments!");
      return;
    }
    if (args.length == 0) { // without tag
      com.sendMsg("Forcing backup!");
      com.getServer().forceBackup();
    } else if (args[0].equals("list")) { //list last backups
      try {
        com.sendMsg(AutoBackup.listLastAutoBackups(Integer.parseInt(args[1])));
      } catch (NumberFormatException ex) { //syntax error
        com.sendMsg("Expected number as third argument!");
      } catch (ArrayIndexOutOfBoundsException ex) { //standard list
        com.sendMsg(AutoBackup.listLastAutoBackups(5));
      }
    } else { // args[1] is tag
      com.sendMsg("Forcing backup!");
      com.getServer().forceBackup(args[0]);
    }
  }
}
