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
import simpleserver.Coordinate;
import simpleserver.Player;

public class WarpToCoordCommand extends AbstractCommand implements PlayerCommand {
  public WarpToCoordCommand() {
    super("warpto [player] x y z", "Teleport (a player) to the given coordinates");
  }

  public void execute(Player player, String message) {
    String args[] = extractArguments(message);
    Coordinate c;

    if (args.length < 3) {
      player.addTMessage(Color.GRAY, "Usage: warpto [player] x y z");
      return;
    }

    Player target = player;
    if (args.length > 3) {
      String trg = args[0];
      target = player.getServer().findPlayer(trg);
      if (target == null) {
        player.addTMessage(Color.RED, "Player not online (%s)", trg);
        return;
      }
    }

    int x = 0;
    int y = 0;
    int z = 0;
    try {
      x = Integer.valueOf(args[args.length - 3]);
      y = Integer.valueOf(args[args.length - 2]);
      z = Integer.valueOf(args[args.length - 1]);
    } catch (Exception e) {
      player.addTMessage(Color.RED, "Invalid coordinate!");
      return;
    }

    c = new Coordinate(x, y, z);
    target.teleportSelf(c);
    player.getServer().adminLog("Admin " + player.getName() + " teleported:\t "
        + target.getName() + "\tto\t" + c.toString());
  }
}
