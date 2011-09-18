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
import simpleserver.bot.BotController.ConnectException;
import simpleserver.config.GiveAliasList.Item;
import simpleserver.config.GiveAliasList.Suggestion;

public abstract class GiveCommand extends AbstractCommand {
  protected Player executor;

  public GiveCommand(String name, String usage) {
    super(name, usage);
  }

  public void execute(String[] arguments, Player target, String logName, Player source) {
    if (target == null) {
      return;
    }

    if (arguments.length != 0) {
      int id;
      short damage = 0;

      String[] request = arguments[0].split(":");
      if (request[0].matches("\\d+")) {
        id = Integer.valueOf(request[0]);
      } else {
        Item item = target.getServer().giveAliasList.getItemId(request[0]);
        if (item == null) {
          Suggestion suggestion = target.getServer().giveAliasList.findWithLevenshtein(request[0]);
          if (suggestion.distance > 4 || suggestion.distance * 2 > request[0].length()) {
            tError("Can't find item %s", request[0]);
            return;
          }
          item = target.getServer().giveAliasList.getItemId(suggestion.name);
          tError("Can't find item %s, giving %s instead", request[0], suggestion.name);
        }
        id = item.id;
        damage = item.damage;
      }

      if (source != null && !allowed(source, id)) {
        tError("You are not allowed to give this block.");
        return;
      }

      if (request.length > 1) {
        try {
          damage = Short.valueOf(request[1]);
        } catch (Exception e) {
          tError("Invalid damage value: %s", request[1]);
          return;
        }
      }

      int amount = 1;
      if (arguments.length > 1) {
        try {
          amount = Integer.valueOf(arguments[1]);
        } catch (Exception e) {
          tError("Invalid amount: %s", arguments[1]);
          return;
        }
      }

      if (amount > 1000) {
        tError("You can't give more than 1000 items at once.");
        return;
      }

      target.getServer().adminLog("Give:\t" + logName + "\t"
                                  + target.getName() + "\t" + id + ":" + damage + "\t("
                                  + amount + ")");

      try {
        target.give(id, damage, amount);
      } catch (ConnectException e) {
        tError("Giving failed!");
      }
    } else {
      tError("No item or amount specified!");
    }
  }

  private boolean allowed(Player player, int id) {
    return player.getServer().config.blockPermission(player, player.position(), id).give;
  }

  protected void tError(String message, Object... args) {
    executor.addTMessage(Color.RED, message, args);
  }

  protected Player getTarget(String name, Server server) {
    Player target = null;
    target = server.findPlayer(name);
    if (target == null) {
      tError("Player not online (%s)", name);
    }
    return target;
  }
}
