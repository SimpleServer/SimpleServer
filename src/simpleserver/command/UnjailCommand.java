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

public class UnjailCommand extends OnlinePlayerArgCommand {
  public UnjailCommand() {
    super("unjail PLAYER", "Return the jailed player back to his origin group");
  }

  protected boolean allowed(Player player, Player target) {
    if (target.getGroupId() >= player.getGroupId()) {
      player.addTMessage(Color.RED, "You cannot unjail players that are in your group or higher!");
      return false;
    }
    return true;
  }

  @Override
  protected void executeWithTarget(Player player, String message, Player target) {
    if (allowed(player, target)) {
      if (target.getIsJailed()) {
        target.unjail();
        player.addTMessage(Color.GRAY, "Player %s is now un-jailed.", target.getName());
        player.getServer().adminLog("Admin " + player.getName() + " unjailed player:\t " +
            target.getName());
      } else {
        player.addTMessage(Color.GRAY, "Player %s wasn't jailed before.", target.getName());
      }
    }
  }

  @Override
  protected void noTargetSpecified(Player player, String message) {
    player.addTMessage(Color.RED, "No player specified.");
  }
}
