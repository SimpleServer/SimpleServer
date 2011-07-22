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
import simpleserver.Coordinate.Dimension;
import simpleserver.Group;
import simpleserver.Player;
import simpleserver.config.PermissionConfig;
import simpleserver.message.AreaMessage;
import simpleserver.message.DimensionMessage;
import simpleserver.message.GlobalMessage;
import simpleserver.message.GroupMessage;
import simpleserver.message.LocalMessage;
import simpleserver.message.PrivateMessage;

public class ChatRoomCommand extends AbstractCommand implements PlayerCommand {

  public ChatRoomCommand() {
    super("chat MODE [ARGUMENTS]", "set default chatRoom");
  }

  public void execute(Player player, String message) {
    String[] args = extractArguments(message);

    if (args.length == 0) {
      player.addTMessage(Color.GRAY, "current chatRoom is: %s", player.getChatRoom());
      return;
    }

    String mode = args[0];
    if (mode.equals("global")) {
      player.setMessagePrototype(new GlobalMessage(player));
    } else if (mode.equals("dimension")) {

      Dimension dim = player.getDimension();

      if (args.length > 1 && Dimension.get(args[1]) != Dimension.LIMBO) {
        dim = Dimension.get(args[1]);
      }
      player.setMessagePrototype(new DimensionMessage(player, dim));
    } else if (mode.equals("group")) {
      Group group = player.getGroup();

      PermissionConfig perm = player.getServer().permissions;

      if (args.length > 1) {
        try {
          int groupId = Integer.parseInt(args[1]);
          if (perm.getGroup(groupId) != null) {
            group = perm.getGroup(groupId);
          }
        } catch (NumberFormatException e) {
        }
      }

      player.setMessagePrototype(new GroupMessage(player, group));
    } else if (mode.equals("area")) {

      player.setMessagePrototype(new AreaMessage(player));
    } else if (mode.equals("local")) {

      player.setMessagePrototype(new LocalMessage(player));
    } else if (mode.equals("private")) {

      Player reciever = player.getServer().findPlayer(args[1]);
      if (reciever == null) {
        player.addTMessage(Color.RED, "Player not online (%s)", args[1]);
        return;
      } else {
        player.setMessagePrototype(new PrivateMessage(player, reciever));
      }
    } else {
      player.addTMessage(Color.RED, "specified chatMode does not exist.");
      usage(player);
      return;
    }

    player.addTMessage(Color.GRAY, "chatRoom changed to: %s", player.getChatRoom());

  }

  private void usage(Player player) {
    String chatCommand = commandPrefix() + "chat";
    player.addTMessage(Color.GRAY, "Usage:");
    player.addTMessage(Color.GRAY, "%s: change to global chatMode", chatCommand + " global");
    player.addTMessage(Color.GRAY, "%s [DIMENSION]: change to dimensionChat", chatCommand + " dimension");
    player.addTMessage(Color.GRAY, "%s [GROUP]: change to groupChat", chatCommand + " group");
    player.addTMessage(Color.GRAY, "%s: change to areaChat", chatCommand + " area");
    player.addTMessage(Color.GRAY, "%s: change to localChat", chatCommand + " local");
    player.addTMessage(Color.GRAY, "%s PLAYER: change to privateChat with PLAYER", chatCommand + " private");
  }

}
