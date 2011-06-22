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

import static simpleserver.lang.Translations.t;
import simpleserver.Color;
import simpleserver.Player;

public class LocalToggle extends AbstractCommand implements PlayerCommand {
  public LocalToggle() {
    super("localtoggle [on|off]", "Toggle local chat mode");
  }

  public void execute(Player player, String message) {
    String[] arguments = extractArguments(message);
    if (arguments.length >= 1) {
      if (arguments[0].equals("on")) {
        player.setLocalChat(true);
      } else if (arguments[0].equals("off")) {
        player.setLocalChat(false);
      } else {
        player.addTMessage(Color.RED, "Only modes %s and %s allowed!", "\"on\"", "\"off\"");
        return;
      }
    } else {
      player.setLocalChat(!player.localChat());
    }
    String mode = (player.localChat()) ? t("enabled") : t("disabled");
    player.addTMessage(Color.GRAY, "Local chat %s", mode);
  }
}
