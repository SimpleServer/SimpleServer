/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package simpleserver.command;

import simpleserver.Player;

public class SetGroupCommand extends PlayerCommand {
  public SetGroupCommand() {
    super("setgroup");
  }

  @Override
  protected void executeWithTarget(Player player, String message, String target)
      throws InterruptedException {
    if (player.getGroup() <= player.server.members.checkName(target)) {
      player.addMessage("\302\247cYou cannot set the group of this user!");
      return;
    }

    String[] arguments = extractArguments(message);
    if (arguments.length > 1) {
      int group;
      try {
        group = Integer.parseInt(arguments[1]);
      }
      catch (NumberFormatException e) {
        player.addMessage("\302\247cGroup must be a number!");
        return;
      }

      if (group >= player.getGroup()) {
        player.addMessage("\302\247cYou cannot promote a user to a higher group!");
      }
      else {
        player.server.members.setGroup(target, group);
        player.addMessage("Player " + target + "'s group was set to " + group
            + "!");
        player.server.adminLog.addMessage("User " + player.getName()
            + " set player's group:\t " + target + "\t(" + group + ")");
      }
    }
  }
}
