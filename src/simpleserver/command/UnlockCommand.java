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

import java.util.List;

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.Player.Action;
import simpleserver.config.data.Chests.Chest;

public class UnlockCommand extends AbstractCommand implements PlayerCommand {
  public UnlockCommand() {
    super("unlock [name]");
  }

  public void execute(Player player, String message) {
    String name = extractArgument(message);
    if (name == null) {
      player.setAttemptedAction(Action.Unlock);
      player.addTMessage(Color.GRAY, "The next chest you open will get unlocked.");
    } else {
      List<Chest> chests = player.getServer().data.chests.getChestsByName(name);
      for (Chest chest : chests) {
        chest.unlock();
      }
      player.getServer().data.save();
      if (chests.size() > 1) {
        player.addTMessage(Color.GRAY, "%d chests have been unlocked!", chests.size());
      } else {
        player.addTMessage(Color.GRAY, "The chest has been unlocked!");
      }
    }
  }

  @Override
  public void usage(Player player) {
    String unlock = parser.commandPrefix() + "unlock";
    player.addTMessage(Color.GRAY, "To unlock the locked chest, use %s and then open the chest", Color.WHITE + unlock + Color.GRAY);
    player.addTMessage(Color.GRAY, "To unlock a named chest, just use %s name", Color.WHITE + unlock + Color.GRAY);
    player.addTMessage(Color.GRAY, "Admins with rights can also unlock a locked chest with the first method");
  }
}
