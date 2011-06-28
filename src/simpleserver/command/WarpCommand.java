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

import java.util.ArrayList;

import simpleserver.Color;
import simpleserver.Player;

public class WarpCommand extends AbstractCommand implements PlayerCommand {
  public WarpCommand() {
    super("warp [list|remove|add] [name]", "Use and manage waypoints");
  }

  public void execute(Player player, String message) {
    String arguments[] = extractArguments(message);
    if (arguments.length == 0) {
      String warp = commandPrefix() + "warp";
      player.addTMessage(Color.GRAY, "Usage:");
      player.addTMessage(Color.GRAY, "%s name: teleport to waypoint", warp);
      player.addTMessage(Color.GRAY, "%s: list waypoints", warp + " list");
      player.addTMessage(Color.GRAY, "%s name: add waypoint", warp + " add");
      player.addTMessage(Color.GRAY, "%s name: remove waypoint", warp + " remove");
      return;
    }
    String command = arguments[0];
    if (command.equals("list")) {
      player.addTCaptionedMessage("Waypoints", "%s", join(new ArrayList<String>(player.getServer().data.warp.names())));
    } else if (command.equals("add")) {
      if (arguments.length == 1) {
        player.addTMessage(Color.RED, "You have to provide the name of a waypoint");
        return;
      } else if (player.getServer().data.warp.contains(arguments[1])) {
        player.addTMessage(Color.RED, "There already exists a waypoint named %s", arguments[1]);
        return;
      }
      player.getServer().data.warp.set(arguments[1], player.position);
      player.addTMessage(Color.GRAY, "Waypoint added");
    } else if (command.equals("remove")) {
      if (arguments.length == 1) {
        player.addTMessage(Color.RED, "You have to provide the name of a waypoint");
        return;
      }
      player.getServer().data.warp.remove(arguments[1]);
      player.addTMessage(Color.GRAY, "Waypoint removed");
    } else {
      if (!player.getServer().data.warp.contains(command)) {
        player.addTMessage(Color.RED, "No such waypoint exists.");
        return;
      }
      try {
        player.teleport(player.getServer().data.warp.get(command));
      } catch (Exception e) {
        player.addTMessage(Color.RED, "Teleporting failed.");
      }
    }
  }
}
