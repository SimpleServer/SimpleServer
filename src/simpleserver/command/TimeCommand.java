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

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.Server;

public class TimeCommand extends AbstractCommand implements PlayerCommand {
  private static final int DELAY_NIGHT = 7 * 60 * 1000;
  private static final int DELAY_DAY = 10 * 60 * 1000;
  private static final int DELAY = 5 * 1000;
  private static final int DAY = 0;
  private static final int NIGHT = 13800;

  private boolean frozen = false;
  private Player player;
  private Server server;
  private Timer timer;

  public TimeCommand() {
    super("time number|day|night|unfreeze [freeze]",
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
      if (argument.equals("day")) {
        time = DAY;
      } else if (argument.equals("night")) {
        time = NIGHT;
      } else if (argument.equals("set") || argument.equals("add")) {
        player.addTMessage(Color.RED, "This is not the standard time command.");
        usage();
        return;
      } else if (argument.equals("unfreeze")) {
        unfreeze();
        return;
      } else {
        try {
          time = Integer.parseInt(argument);
        } catch (NumberFormatException e) {
          player.addTMessage(Color.RED, "Invalid argument!");
          usage();
          return;
        }
        if (time < 0 || time > 23999) {
          player.addTMessage(Color.RED, "Time must be either a value in the range 0-23999 or %s!",
                             "day/night");
          return;
        }
      }

      unfreeze();

      if (arguments.length < 2) {
        setTime(time);
      }

      if (arguments.length >= 2) {
        argument = arguments[1];

        if (argument.equals("freeze")) {
          freeze(time);
        } else {
          player.addTMessage(Color.RED, "Optional 2nd argument must be %s!",
                             "freeze");
        }
      }
    }
  }

  private void usage() {
    player.addTMessage(Color.GRAY, "Usage: %s",
                       commandPrefix() + "time 0-23999|day|night|unfreeze [freeze]");
  }

  private void setTime(int time) {
    long servertime = server.time();
    server.runCommand("time", "add " + (24000 - servertime % 24000 + time));
  }

  public void unfreeze() {
    if (frozen && timer != null) {
      timer.cancel();
      frozen = false;

      player.addTMessage(Color.GRAY, "Time unfrozen");
    }
  }

  private void freeze(int time) {
    if (!frozen) {
      frozen = true;
      timer = new Timer();
      int delay = DELAY;
      if (time == DAY) {
        delay = DELAY_DAY;
      } else if (time == NIGHT) {
        delay = DELAY_NIGHT;
      }
      timer.schedule(new TimeFreezer(this, time), 0, delay);

      player.addTMessage(Color.GRAY, "Time frozen");
    }
  }

  private class TimeFreezer extends TimerTask {

    private TimeCommand parent;
    private int freezetime;

    public TimeFreezer(TimeCommand parent, int time) {
      this.parent = parent;
      freezetime = time;
    }

    @Override
    public void run() {
      parent.setTime(freezetime);
    }
  }
}
