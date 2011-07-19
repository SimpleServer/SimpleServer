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
import simpleserver.config.data.Warp;

public class WarpCommand extends AbstractCommand implements PlayerCommand {
  public WarpCommand() {
    super("warp [list|remove|add] [name]", "Use and manage waypoints");
  }

  public void execute(Player player, String message) {
    String arguments[] = extractArguments(message);
    Warp warp = player.getServer().data.warp;
    if (arguments.length == 0) {
      String warpCommand = commandPrefix() + "warp";
      player.addTMessage(Color.GRAY, "Usage:");
      player.addTMessage(Color.GRAY, "%s name: teleport to waypoint", warpCommand);
      player.addTMessage(Color.GRAY, "%s: list waypoints", warpCommand + " list");
      player.addTMessage(Color.GRAY, "%s name: add waypoint", warpCommand + " add");
      player.addTMessage(Color.GRAY, "%s name: remove waypoint", warpCommand + " remove");
      return;
    }
    String command = arguments[0];
    if (command.equals("list")) {
      player.addTCaptionedMessage("Waypoints", "%s", join(player.getServer().data.warp.names()));
    } else if (command.equals("add")) {
      if (arguments.length == 1) {
        player.addTMessage(Color.RED, "You have to provide the name of a waypoint");
        return;
      } else if (warp.contains(arguments[1])) {
        player.addTMessage(Color.RED, "There already exists a waypoint named %s", arguments[1]);
        return;
      }
      warp.set(arguments[1], player.position);
      player.getServer().data.save();
      player.addTMessage(Color.GRAY, "Waypoint added");
    } else if (command.equals("remove")) {
      if (arguments.length == 1) {
        player.addTMessage(Color.RED, "You have to provide the name of a waypoint");
        return;
      }
      String waypoint = warp.getName(arguments[1]);
      if (waypoint == null) {
        player.addTMessage(Color.RED, "No such waypoint exists.");
        return;
      }
      warp.remove(waypoint);
      player.getServer().data.save();
      player.addTMessage(Color.GRAY, "Waypoint removed");
    } else {
      String waypoint = warp.getName(command);
      if (waypoint == null) {
        player.addTMessage(Color.RED, "No such waypoint exists.");
        return;
      }
      player.teleportWithWarmup(warp.get(waypoint));
    }
  }
}
