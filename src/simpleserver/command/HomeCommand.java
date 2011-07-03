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
import simpleserver.Position;

public class HomeCommand extends AbstractCommand implements PlayerCommand {
  public HomeCommand() {
    super("home [set|delete]", "Teleport to and manage your home");
  }

  public void execute(Player player, String message) {
    String playerName = player.getName();
    String arguments[] = extractArguments(message);
    if (arguments.length == 0) {
      Position home = player.getServer().data.players.homes.get(playerName);
      if (home == null) {
        player.addTMessage(Color.RED, "You don't have a home to teleport to!");
        return;
      }
      try {
        player.teleport(home);
      } catch (Exception e) {
        player.addTMessage(Color.RED, "Teleporting failed.");
      }
      return;
    }
    String command = arguments[0];
    if (command.equals("set")) {
      if (player.getServer().data.players.homes.get(playerName) != null) {
        player.addTMessage(Color.RED, "You must delete your old home before saving a new one!");
        return;
      }

      player.getServer().data.players.homes.set(playerName, player.position);
      player.getServer().data.save();
      player.addTMessage(Color.GRAY, "Your home has been added.");
    } else if (command.equals("delete")) {
      if (player.getServer().data.players.homes.get(playerName) == null) {
        player.addTMessage(Color.GRAY, "You don't have a home to delete!");
        return;
      }

      player.getServer().data.players.homes.remove(playerName);
      player.addTMessage(Color.GRAY, "Your home has been deleted.");
    } else {
      String home = commandPrefix() + "home";
      player.addTMessage(Color.GRAY, "Usage:");
      player.addTMessage(Color.GRAY, "%s: teleport home", home);
      player.addTMessage(Color.GRAY, "%s: set your home at your location", home + " set");
      player.addTMessage(Color.GRAY, "%s: delete your home", home + " delete");
    }
  }
}
