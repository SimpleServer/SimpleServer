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

import simpleserver.Player;
import simpleserver.Server;

public class GivePlayerCommand extends GiveCommand implements ServerCommand {
  public GivePlayerCommand() {
    super("giveplayer PLAYER ITEM [AMOUNT]", "Spawn items for another player", 1);
  }

  public void execute(Server server, String message) {
    String[] arguments = extractArguments(message);

    Player target = getTarget(null, server, arguments);
    if (target == null) {
      return;
    }

    if (arguments.length <= 1) {
      System.out.println("No item or amount specified!");
      return;
    }

    String item = arguments[1];
    Integer id = server.giveAliasList.getItemId(item);
    if (id != null) {
      item = id.toString();
    }

    String amount = null;
    if (arguments.length > 2) {
      amount = arguments[2];
    }

    target.give(item, amount);
  }

  @Override
  protected Player getTarget(Player player, Server server, String[] arguments) {
    Player target = null;
    if (arguments.length > 0) {
      target = server.findPlayer(arguments[0]);
      if (target == null) {
        if (player != null) {
          player.addMessage("\u00a7cPlayer not online (" + arguments[0] + ")");
        } else {
          System.out.println("Player not online (" + arguments[0] + ")");
        }
      }
    } else {
      if (player != null) {
        player.addMessage("\u00a7cNo player or item specified!");
      } else {
        System.out.println("No player or item specified!");
      }
    }

    return target;
  }
}
