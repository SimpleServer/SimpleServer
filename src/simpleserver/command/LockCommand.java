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

import simpleserver.Color;
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
        player.addTMessage(Color.GRAY, "Chests you place or open will no longer be locked.");
        return;
      }
    }
    if (name != null && name.equals("list")) {
      Map<String, Integer> list = player.getServer().data.chests.chestList(player);
      if (list.size() == 0) {
        player.addTMessage(Color.GRAY, "You don't have any locked chests.");
      } else {
        player.addTMessage(Color.GRAY, "Your locked chests:");
        for (String current : list.keySet()) {
          player.addMessage(Color.GRAY, list.get(current) + " " + current);
        }
      }
    } else {
      player.addTMessage(Color.GRAY, "Create or open a chest, and it will be locked to you.");
      player.setAttemptedAction(Action.Lock);
      player.setChestName(name);
    }
  }

  @Override
  public void usage(Player player) {
    String lock = commandPrefix() + "lock";
    player.addTMessage(Color.GRAY, "Locked chests are chests that can be opened only by you and admins who have recieved rights to do so.");
    player.addTMessage(Color.GRAY, "To lock a chest, use %s", Color.WHITE + lock);
    player.addTMessage(Color.GRAY, "To give the locked chest a name, use %s name", Color.WHITE + lock);
    player.addTMessage(Color.GRAY, "To see all your locked chests, use %s", Color.WHITE + lock + " list");
  }
}
