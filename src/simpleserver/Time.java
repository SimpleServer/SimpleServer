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
package simpleserver;

import java.util.Timer;
import java.util.TimerTask;

public class Time {
  private static final int DELAY_NIGHT = 7 * 60 * 1000;
  private static final int DELAY_DAY = 10 * 60 * 1000;
  private static final int DELAY = 5 * 1000;
  public static final int DAY = 0;
  public static final int NIGHT = 13800;

  private long time;
  private boolean frozen;
  private boolean setOnUpdate;
  private long freezeTime;
  private Timer timer;
  private Server server;

  public Time(Server server) {
    this.server = server;
  }

  public void set(long time) {
    server.runCommand("time", "add " + (24000 - this.time % 24000 + time));
  }

  public void set() {
    if (frozen) {
      setOnUpdate = true;
    }
  }

  public void is(long time) {
    this.time = time;
    if (setOnUpdate) {
      setOnUpdate = false;
      set(freezeTime);
    }
  }

  public long get() {
    return time;
  }

  public void freeze() {
    freeze(get() % 24000);
  }

  public void freeze(long time) {
    if (!frozen) {
      frozen = true;
      timer = new Timer();
      int delay = DELAY;
      if (time == DAY) {
        delay = DELAY_DAY;
      } else if (time == NIGHT) {
        delay = DELAY_NIGHT;
      }
      freezeTime = time;
      timer.schedule(new TimeFreezer(this), 0, delay);
      save();
    }
  }

  public boolean unfreeze() {
    if (frozen && timer != null) {
      timer.cancel();
      setOnUpdate = false;
      frozen = false;
      save();
      return true;
    }
    return false;
  }

  public long parse(String i) throws NumberFormatException {
    if (i.toLowerCase().equals("day")) {
      return DAY;
    } else if (i.toLowerCase().equals("night")) {
      return NIGHT;
    } else {
      return Long.parseLong(i) % 24000;
    }
  }

  private void save() {
    if (!frozen) {
      server.data.unsetFreezeTime();
    } else {
      server.data.setFreezeTime((int) freezeTime);
    }
  }

  private class TimeFreezer extends TimerTask {
    private Time parent;

    public TimeFreezer(Time parent) {
      this.parent = parent;
    }

    @Override
    public void run() {
      parent.set();
    }
  }

}
