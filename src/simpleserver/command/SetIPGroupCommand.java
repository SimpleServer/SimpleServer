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

import java.util.regex.Pattern;

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.Server;

public class SetIPGroupCommand extends SetGroupCommand implements ServerCommand {
  public SetIPGroupCommand() {
    super("setipgroup IP|Player",
          "Set the group ID of an IP address");
  }

  @Override
  protected void setGroup(Player player, int group, String target) {
    Server server = player.getServer();
    Player targetPlayer = player.getServer().findPlayer(target);
    if (targetPlayer != null) {
      target = targetPlayer.getIPAddress();
    } else if (!Pattern.matches("^(\\d{1,3}\\.){3}\\d{1,3}$", target)) {
      player.addTMessage(Color.RED, "You must specify a user or a valid IP!");
      return;
    }
    server.permissions.setIPGroup(target, group);

    player.addTMessage(Color.GRAY, "Group of %s was set to %s!", target, group);
    server.adminLog("User " + player.getName() + " set IP's group:\t "
        + target + "\t(" + group + ")");
  }

  @Override
  protected void setGroup(Server server, int group, String target) {
    Player targetPlayer = server.findPlayer(target);
    if (targetPlayer != null) {
      target = targetPlayer.getIPAddress();
    } else if (!Pattern.matches("^(\\d{1,3}\\.){3}\\d{1,3}$", target)) {
      System.out.println("[SimpleServer] You must specify a user or a valid IP!");
      return;
    }
    server.permissions.setIPGroup(target, group);
    System.out.println("[SimpleServer] Group of " + target + " was set to " + group + "!");
  }
}
