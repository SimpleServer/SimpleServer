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
import simpleserver.config.PermissionConfig;

public class MyAreaCommand extends AbstractCommand implements PlayerCommand {
  public MyAreaCommand() {
    super("myarea [start|end|save|unsave]", "Manage your personal area");
  }

  private boolean areaSizeOk(Player player) {
      return (Math.abs(player.areastart.x - player.areaend.x) <= 50)
          && (Math.abs(player.areastart.y - player.areaend.y) <= 50);
  }

  public void execute(Player player, String message) {
    PermissionConfig perm = player.getServer().permissions;
    String arguments[] = extractArguments(message);

    if (arguments.length == 0) {
        player.addMessage("\u00a7cError! Command requires argument!");
        return;
    }

    if (arguments[0].equals("start")) {
      player.areastart = perm.coordinateFromPlayer(player);
      player.areastart.y = 0; //no height limit
      player.addMessage("\u00a77Start coordinate set.");
    } else if (arguments[0].equals("end")) {
      player.areaend = perm.coordinateFromPlayer(player);
      player.areaend.y = 0; //no height limit
      player.addMessage("\u00a77End coordinate set.");
    } else if (arguments[0].equals("save")) {
      if (perm.playerHasArea(player)) {
        player.addMessage("\u00a7cNew area can not be saved before you unsave your old one!");
        return;
      }
      if (!perm.getCurrentArea(player).equals("")) {
        player.addMessage("\u00a7cYou can not create your area within an existing area!");
        return;
      }
      if (player.areastart == null || player.areaend == null) {
        player.addMessage("\u00a7cDefine start and end coordinates for your area first!");
        return;
      }
      if (!areaSizeOk(player)) {
        player.addMessage("\u00a7cYour area is allowed to have a maximum size of 50x50!");
        return;
      }

      perm.createPlayerArea(player);
      player.addMessage("\u00a77Your area has been saved!");
    } else if (arguments[0].equals("unsave")) {
      if (!perm.playerHasArea(player)) {
        player.addMessage("\u00a7cYou currently have no personal area which can be unsaved!");
        return;
      }

      perm.removePlayerArea(player);
      player.addMessage("\u00a77Your area has been unsaved!");
    }

  }
}
