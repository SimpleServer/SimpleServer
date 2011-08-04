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

public abstract class OnlinePlayerArgCommand extends AbstractCommand implements
    PlayerCommand {
  private final boolean playerOptional;

  protected OnlinePlayerArgCommand(String name, String commandCode) {
    this(name, commandCode, false);
  }

  protected OnlinePlayerArgCommand(String name, String commandCode,
                                   boolean playerOptional) {
    super(name, commandCode);

    this.playerOptional = playerOptional;
  }

  public void execute(Player player, String message) {
    String[] arguments = extractArguments(message);

    if (arguments.length > 0) {
      Player target = player.getServer().findPlayer(arguments[0]);
      if (target == null) {
        player.addTMessage(Color.RED, "Player not online (%s)", arguments[0]);
      } else {
        executeWithTarget(player, message, target);
      }
    } else {
      if (playerOptional) {
        executeWithTarget(player, message, null);
      } else {
        noTargetSpecified(player, message);
      }
    }
  }

  protected abstract void executeWithTarget(Player player, String message,
                                            Player target);

  protected void noTargetSpecified(Player player, String message) {
    player.addTMessage(Color.RED, "No player specified.");
  }

  @Override
  public String getHelpText(String prefix) {
    return super.getHelpText(prefix) + " " + t("(case-insensitive, name prefix works for online players)");
  }
}
