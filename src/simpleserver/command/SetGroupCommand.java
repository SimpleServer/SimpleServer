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

import static simpleserver.util.Util.*;

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.Server;

public class SetGroupCommand extends PlayerArgCommand {
  public SetGroupCommand() {
    super("setgroup PLAYER GROUP",
          "Set the group ID of the named player");
  }

  public SetGroupCommand(String name, String commandCode) {
    super(name, commandCode);
  }

  protected boolean allowed(Player player, int group, String target) {
    Server server = player.getServer();
    if (server.config.players.group(target) != null && player.getGroupId() <= server.config.players.group(target)) {
      player.addTMessage(Color.RED, "You cannot set the group of this user!");
      return false;
    }
    if (group >= player.getGroupId()) {
      player.addTMessage(Color.RED, "You cannot promote to your group or higher!");
      return false;
    }
    if (server.authenticator.isGuestName(target)) {
      player.addTMessage(Color.RED, "You cannot promote a guest!");
      return false;
    }
    return true;
  }

  @Override
  protected void executeWithTarget(Player player, String message, String target) {
    String[] arguments = extractArguments(message);
    int group;

    if (arguments.length < 2) {
      player.addTMessage(Color.RED, "You must specify a group!");
      return;
    }
    try {
      group = Integer.parseInt(arguments[1]);
    } catch (NumberFormatException e) {
      player.addTMessage(Color.RED, "Group must be a number!");
      return;
    }

    if (allowed(player, group, target)) {
      setGroup(player, group, target);
      player.getServer().updateGroup(target);
    }
  }

  protected void setGroup(Player player, int group, String target) {
    Server server = player.getServer();
    server.config.players.setGroup(target, group);
    server.saveConfig();

    if (server.options.getBoolean("enableCustAuthExport")) {
      server.custAuthExport.updateGroup(target, group);
    }

    player.addTMessage(Color.GRAY, "Player %s's group was set to %s!",
                       target, new Integer(group).toString());
    server.adminLog("User " + player.getName() + " set player's group:\t "
        + target + "\t(" + group + ")");
  }

  @Override
  protected void executeWithTarget(Server server, String message, String target, CommandFeedback feedback) {
    String[] arguments = extractArguments(message);
    int group;

    if (arguments.length < 2) {
      feedback.send("You must specify a group!");
      return;
    }
    try {
      group = Integer.parseInt(arguments[1]);
    } catch (NumberFormatException e) {
      feedback.send("Group must be a number!");
      return;
    }

    setGroup(server, group, arguments[0]);
    server.updateGroup(arguments[0]);
  }

  protected void setGroup(Server server, int group, String target) {
    server.config.players.setGroup(target, group);
    server.saveConfig();
    print("Player " + target + "'s group was set to " + new Integer(group).toString() + "!");

    if (server.options.getBoolean("enableCustAuthExport")) {
      server.custAuthExport.updateGroup(target, group);
    }
  }
}
