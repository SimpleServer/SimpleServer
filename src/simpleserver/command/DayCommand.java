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

public class DayCommand extends AbstractCommand implements PlayerCommand {
  private static final int DELAY = 10*60*1000;
  
  private boolean frozen = false;
  private Server server;
  private Timer timer;

  public DayCommand() {
    super("day [freeze|unfreeze]",
          "Set or freeze time");
  }

  public void execute(Player player, String message) {
    server = player.getServer();
    
    String[] arguments = extractArguments(message);
    if (arguments.length > 0) {
      String argument = arguments[0];
      if(argument.equals("freeze")) {
        freeze();
        player.addMessage("\u00a77Time frozen");
      } else if(argument.equals("unfreeze")) {
        unfreeze();
        player.addMessage("\u00a77Time unfrozen");
      } else {
        player.addMessage("\u00a7cOnly arguments freeze and unfreeze are allowed!");
      }
    }
    else {
      player.addMessage("\u00a77Time set to sunrise");
      day();
    }
  }
  
  private void day() {
    server.runCommand("time", "set 0");
  }

  public void unfreeze() {
    if(timer != null) {
      timer.cancel();
      frozen = false;
    }
  }
  
  private void freeze() {
    if(!frozen) {
      frozen = true;
      timer = new Timer();
      timer.schedule(new DayFreezer(this), 0, DELAY);
    }
  }
  
  private class DayFreezer extends TimerTask {
 
    private DayCommand parent;

    public DayFreezer(DayCommand parent) {
      this.parent = parent;
    }
    
    @Override
    public void run() {
      parent.day();
    }
  }
}
