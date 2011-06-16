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

import java.util.Map;

import simpleserver.Player;
import simpleserver.Player.Action;

public class LockCommand extends AbstractCommand implements PlayerCommand {
  public LockCommand() {
    super("lock [name|list]", "Create or list locked chests");
  }

  public void execute(Player player, String message) {
    String name = extractArgument(message);
    if (name == null) {
      if (player.isAttemptLock()) {
        player.setAttemptedAction(null);
        player.addMessage("\u00a77" +
            t.get("Chests you place or open will no longer be locked."));
        return;
      } else {
        name = "Locked Chest";
      }
    }
    if (name.equals("list")) {
      Map<String, Integer> list = player.getServer().chests.chestList(player);
      if (list.size() == 0) {
        player.addMessage("\u00a77" + t.get("You don't have any locked chests."));
      } else {
        player.addMessage("\u00a77" + t.get("Your locked chests:"));
        for (String current : list.keySet()) {
          player.addMessage("\u00a77 " + list.get(current) + " " + current);
        }
      }
    } else {
      player.addMessage("\u00a77" +
          t.get("Create or open a chest, and it will be locked to you."));
      player.setAttemptedAction(Action.Lock);
      player.setChestName(name);
    }
  }
}
