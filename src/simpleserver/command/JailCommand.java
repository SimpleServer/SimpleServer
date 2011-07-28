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
import simpleserver.config.data.Warp;

public class JailCommand extends OnlinePlayerArgCommand {
  public JailCommand() {
    super("jail PLAYER [MINUTES] [REASON]", "Send the player to jail and demote him to a lower group");
  }

  protected boolean allowed(Player player, Player target) {
    if (target.getGroupId() >= player.getGroupId()) {
      player.addTMessage(Color.RED, "You cannot jail players that are in your group or higher!");
      return false;
    }
    return true;
  }

  @Override
  protected void executeWithTarget(Player player, String message, Player target) {
    if (allowed(player, target)) {
      Warp warp = player.getServer().data.warp;
      String waypoint = warp.getName("jail");
      if (waypoint == null) {
        player.addTMessage(Color.GRAY, "The jail location is not set!");
        return;
      }

      String[] arguments = extractArguments(message);

      int mins = 0;
      String reason = null;
      if (arguments.length > 1) {
        try {
          mins = Integer.valueOf(arguments[1]);
        } catch (NumberFormatException e) {
          reason = extractArgument(message, 1);
        }

        if (reason == null) {
          reason = extractArgument(message, 2);
        }
      }
      if (reason == null) {
        reason = t("Jailed by an admin.");
      }

      if (target.jail(mins, reason)) {
        if (mins == 0) {
          player.addTMessage(Color.GRAY, "Player %s is jailed forever!", target.getName());
        } else if (mins == 1) {
          player.addTMessage(Color.GRAY, "Player %s is jailed for 1 minute!", target.getName());
        } else {
          player.addTMessage(Color.GRAY, "Player %s is jailed for %s minutes!", target.getName(), mins);
        }
        player.getServer().adminLog("Admin " + player.getName() + " jailed player:\t " +
              target.getName() + "\t(" + reason + ")");
      } else {
        player.addTMessage(Color.GRAY, "Could not send player %s to jail!");
      }
    }
  }

  @Override
  protected void noTargetSpecified(Player player, String message) {
    player.addTMessage(Color.RED, "No player specified.");
  }
}
