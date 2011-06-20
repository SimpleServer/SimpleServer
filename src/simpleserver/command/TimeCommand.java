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

import simpleserver.Server;
import simpleserver.Time;
import simpleserver.lang.Translations;

public abstract class TimeCommand extends AbstractCommand {
  public TimeCommand() {
    super("time [set] [number|day|night|unfreeze] [freeze]", "Set or freeze time");
  }

  private void usage() {
    info(Translations.getInstance().get("Usage:") + " " + commandPrefix() +
         "time 0-23999|day|night|unfreeze [freeze]");
  }

  public void execute(Server server, String message) {
    Time time = server.time;
    String[] arguments = extractArguments(message);

    if (arguments.length == 0) {
      long servertime = time.get();
      long realtime = (servertime + 6000) % 24000;
      tCaptionedInfo("Current time", "%d:%d (%d)", realtime / 1000,
                     (realtime % 1000) * 6 / 100, servertime % 24000);
    } else if (arguments.length >= 1) {
      int arg = 0;
      if (arguments[arg].equals("set")) {
        arg++;
      }
      String argument = arguments[arg];
      long value;

      if (argument.equals("add")) {
        tError("This is not the standard time command.");
        usage();
        return;
      } else if (argument.equals("unfreeze")) {
        time.unfreeze();
        tInfo("Time unfrozen");
        return;
      } else if (argument.equals("freeze")) {
        time.freeze();
        tInfo("Time frozen");
        return;
      } else {
        try {
          value = time.parse(argument);
        } catch (NumberFormatException e) {
          tError("Invalid argument!");
          usage();
          return;
        }
      }

      arg++;
      if (time.unfreeze()) {
        tInfo("Time unfrozen");
      }

      if (arguments.length <= arg) {
        time.set(value);
        tInfo("Time set");
      } else {
        argument = arguments[arg];

        if (argument.equals("freeze")) {
          time.freeze(value);
          tInfo("Time frozen");
        } else {
          tError("Optional 2nd argument must be freeze!");
        }
      }
    }
  }

  protected abstract void error(String message);

  protected abstract void tError(String message);

  protected abstract void info(String message);

  protected abstract void tInfo(String message);

  protected abstract void captionedInfo(String caption, String message, Object... args);

  protected abstract void tCaptionedInfo(String caption, String message, Object... args);
}
