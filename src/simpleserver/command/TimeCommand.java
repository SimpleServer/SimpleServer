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

import java.util.Timer;
import java.util.TimerTask;

import simpleserver.Player;
import simpleserver.Server;

public class TimeCommand extends AbstractCommand implements PlayerCommand {
  private static final int DELAY_NIGHT = 7*60*1000;
  private static final int DELAY_DAY = 10*60*1000;
  private static final int DELAY = 5*1000;
  private static final int DAY = 0;
  private static final int NIGHT = 13800;

  private boolean frozen = false;
  private Player player;
  private Server server;
  private Timer timer;

  public TimeCommand() {
    super("time number|day|night [freeze|unfreeze]",
    "Set or freeze time");
  }

  public void execute(Player player, String message) {
    server = player.getServer();
    this.player = player;

    String[] arguments = extractArguments(message);

    if (arguments.length == 0) {
      usage();
    } else if (arguments.length >= 1) {
      String argument = arguments[0];
      int time = 0;
      if(argument.equals("day")) {
        time = DAY;
      } else if (argument.equals("night")) {
        time = NIGHT;
      } else if (argument.equals("set") || argument.equals("add")) {
        player.addMessage("§cThis is not the standard time command.");
        usage();
        return;
      } else if (argument.equals("unfreeze")) {
        unfreeze();
        return;
      } else {
        try {
          time = Integer.parseInt(argument);
        } catch(NumberFormatException e) {
          player.addMessage("\u00a7cInvalid argument!");
          usage();
          return;
        }
        if (time < 0 || time > 23999) {
          player.addMessage("\u00a7cTime must be either a value in the range 0-23999 or day/night!");
          return;
        }
      }
      
      unfreeze();

      if (arguments.length < 2) //Prevents double calling (from freeze)
        setTime(time);

      if (arguments.length >= 2) {
        argument = arguments[1];

        if (argument.equals("freeze")) {
          freeze(time);
        } else {
          player.addMessage("\u00a7cOptional 2nd argument must be freeze!");
        }
      }
    }
  }

  private void usage() {
    player.addMessage("§7Usage: !time [0-23999|night|day] [freeze|unfreeze]");
  }
  
  private void setTime(int time) {
    server.runCommand("time", "set "+time);
  }

  public void unfreeze() {
    if(timer != null) {
      timer.cancel();
      frozen = false;

      player.addMessage("\u00a77Time unfrozen");
    }
  }

  private void freeze(int time) {
    if(!frozen) {
      frozen = true;
      timer = new Timer();
      int delay = DELAY;
      if(time == DAY) {
        delay = DELAY_DAY;
      } else if(time == NIGHT) {
        delay = DELAY_NIGHT;
      }
      timer.schedule(new TimeFreezer(this, time), 0, delay);

      player.addMessage("\u00a77Time frozen");
    }
  }

  private class TimeFreezer extends TimerTask {

    private TimeCommand parent;
    private int freezetime;

    public TimeFreezer(TimeCommand parent, int time) {
      this.parent = parent;
      this.freezetime = time;
    }

    @Override
    public void run() {
      parent.setTime(freezetime);
    }
  }
}
