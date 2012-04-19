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

public class GameModeCommand extends AbstractCommand implements PlayerCommand {

  public GameModeCommand() {
    super("gamemode [PLAYER] MODE", "set gameMode for a specific player.");
  }

  public void execute(Player player, String message) {
    String[] args = extractArguments(message);
    Player target;
    Integer gameMode;

    try {
      if (args.length == 1) {
        target = player;
        gameMode = Integer.parseInt(args[0]);
      } else if (args.length == 2) {
        target = player.getServer().findPlayer(args[0]);
        if (target == null) {
          player.addTMessage(Color.RED, "Player not online (%s)", args[1]);
          return;
        }
        gameMode = Integer.parseInt(args[1]);
      } else {
        player.addTMessage(Color.RED, "Invalid number of arguments!");
        return;
      }
    } catch (NumberFormatException e) {
      player.addTMessage(Color.RED, "Invalid gameMode %s!", args[0]);
      return;
    }

    if (gameMode != 0 && gameMode != 1) {
      player.addTMessage(Color.RED, "Invalid gameMode %d!", gameMode);
      return;
    }
    player.getServer().runCommand("gamemode", gameMode + " " + target.getName());

  }
}
