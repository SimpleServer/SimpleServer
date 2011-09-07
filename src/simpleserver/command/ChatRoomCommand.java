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
import simpleserver.Coordinate.Dimension;
import simpleserver.config.xml.Config;
import simpleserver.config.xml.Group;
import simpleserver.message.AreaChat;
import simpleserver.message.DimensionChat;
import simpleserver.message.GlobalChat;
import simpleserver.message.GroupChat;
import simpleserver.message.LocalChat;
import simpleserver.message.PrivateChat;

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

    String mode = args[0].toLowerCase();
    if (mode.equals("global")) {
      player.setChat(new GlobalChat(player));
    } else if (mode.equals("dimension")) {

      Dimension dim = player.getDimension();

      if (args.length > 1 && Dimension.get(args[1]) != Dimension.LIMBO) {
        dim = Dimension.get(args[1]);
      }
      player.setChat(new DimensionChat(player, dim));
    } else if (mode.equals("group")) {
      Group group = player.getGroup();

      Config config = player.getServer().config;

      if (args.length > 1) {
        try {
          int groupId = Integer.parseInt(args[1]);
          if (config.groups.contains(groupId)) {
            group = config.groups.get(groupId);
          } else {
            throw new NumberFormatException("Group doesn't exist");
          }
        } catch (NumberFormatException e) {
          player.addTMessage(Color.RED, "Invalid group ID");
        }
      }

      player.setChat(new GroupChat(player, group));
    } else if (mode.equals("area")) {

      AreaChat room = new AreaChat(player);
      if (room.noArea()) {
        player.addTMessage(Color.RED, "You are in no area at the moment");
      } else {
        player.setChat(room);
      }
    } else if (mode.equals("local")) {

      player.setChat(new LocalChat(player));
    } else if (mode.equals("private")) {

      Player reciever = player.getServer().findPlayer(args[1]);
      if (reciever == null) {
        player.addTMessage(Color.RED, "Player not online (%s)", args[1]);
        return;
      } else {
        player.setChat(new PrivateChat(player, reciever));
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
