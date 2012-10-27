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
  private final String warning = " A rollback may result in a broken server."
          + " Consider restoring the backup manually or use the force option to rollback anyway.";
  private final String error = "Rollback failure: ";
  private final String afterError = "THE ROLLBACK COULD NOT BE PERFORMED COMPLETELY"
          + " SO YOU PROBABLY HAVE A BROKEN SERVER NOW!"
          + " We recommend to rollback to the backup that was automatically made just before the rollback attempt!";

  public RollbackCommand() {
    super("rollback [force] [tag|@n]", "Reset server to a backup named 'tag' or the n-th last non-tagged backup.");
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
  
  private void execute(final BackupCommand.Com com) {
    String[] args = extractArguments(com.getMessage());
    if (args.length != 1 && args.length != 2) {
      com.sendMsg("Wrong number of arguments!");
      return;
    }
    boolean f = false;
    int i = 0; //args pos
    if (args[i].equals("force")) {
      f = true;
      i++;
    }
    final boolean force = f;
    if (args[i].charAt(0) == '@') { //@n: reference to n-th last auto backup
      try {
        com.getServer().rollback(
                new ExecCom() {
          @Override
          public void sendWarningRollbackAborted(String msg) {
            sendMsg(msg + warning);
          }
          @Override
          public void sendErrorRollbackFail(String msg) {
            sendMsg(error + msg + afterError);
          }
          @Override
          public void sendMsg(String msg) {
            com.sendMsg(msg);
          }
          @Override
          public boolean isForce() {
            return force;
          }
        }, Integer.parseInt(args[i].substring(1, args[i].length())));
      } catch (NumberFormatException ex) {
        com.sendMsg("Expected number after '@'!");
      } catch (Exception ex) {
        com.sendMsg(ex.getMessage());
      }
    } else {
      try {
        com.getServer().rollback(
                new ExecCom() {
          @Override
          public void sendWarningRollbackAborted(String msg) {
            sendMsg(msg + warning);
          }
          @Override
          public void sendErrorRollbackFail(String msg) {
            com.sendMsg(error + msg + afterError);
          }
          @Override
          public void sendMsg(String msg) {
            com.sendMsg(msg);
          }
          @Override
          public boolean isForce() {
            return force;
          }
        }, args[i]);
      } catch (Exception ex) {
        com.sendMsg(ex.getMessage());
      }
    }
  }
  
  /**
   * Interface to provide access to important information
   * during the execution in a different thread.
   */
  public interface ExecCom {
    public void sendWarningRollbackAborted(String msg);
    public void sendErrorRollbackFail(String msg);
    public void sendMsg(String msg);
    public boolean isForce();
  }
  
}
