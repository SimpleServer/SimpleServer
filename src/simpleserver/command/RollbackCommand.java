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

public class RollbackCommand extends AbstractCommand implements PlayerCommand, ServerCommand {

  public RollbackCommand() {
    super("rollback [tag|@n]", "Reset server to a backup named 'tag' or the n-th last non-tagged backup.");
  }

  @Override
  public void execute(final Player player, final String message) {
    execute(new BackupCommand.Com() {

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
    execute(new BackupCommand.Com() {
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
  
  private void execute(BackupCommand.Com com) {
    String[] args = extractArguments(com.getMessage());
    if (args.length != 1) {
      com.sendMsg("Wrong number of arguments!");
      return;
    }
    if (args[0].charAt(0) == '@') { //@n: reference to n-th last auto backup
      try {
        com.getServer().rollback(Integer.parseInt(args[0].substring(1, args[0].length())));
      } catch (NumberFormatException ex) {
        com.sendMsg("Expected number after '@'!");
      } catch (Exception ex) {
        com.sendMsg(ex.getMessage());
      }
    } else {
      try {
        com.getServer().rollback(args[0]);
      } catch (Exception ex) {
        com.sendMsg(ex.getMessage());
      }
    }
  }
  
}
