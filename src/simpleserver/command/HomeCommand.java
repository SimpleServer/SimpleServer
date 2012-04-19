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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.config.data.Homes;
import simpleserver.config.data.Homes.HomePoint;
import simpleserver.nbt.NBTString;

public class HomeCommand extends AbstractCommand implements PlayerCommand {
  public HomeCommand() {
    super("home [help|set|delete|public|private|ilist|list|invite|uninvite|inspect] [name]", "Teleport to and manage your home");
  }

  public void execute(Player player, String message) {
    Homes homes = player.getServer().data.players.homes;
    String playerName = player.getName();
    String arguments[] = extractArguments(message);
    if (arguments.length == 0) {
      teleportHome(player);
      return;
    }
    String command = arguments[0];
    if (command.equals("help")) {
      usage(player);
    } else if (command.equals("set")) {
      if (homes.get(playerName) != null) {
        player.addTMessage(Color.RED, "You must delete your old home before saving a new one!");
        return;
      }

      homes.set(playerName, homes.makeHomePoint(player.position));
      player.getServer().data.save();
      player.addTMessage(Color.GRAY, "Your home has been saved.");
    } else if (command.equals("delete")) {
      if (homes.get(playerName) == null) {
        player.addTMessage(Color.GRAY, "You don't have a home to delete!");
        return;
      }

      homes.remove(playerName);
      player.getServer().data.save();
      player.addTMessage(Color.GRAY, "Your home has been deleted.");
    } else if (command.equals("public")) {
      HomePoint home = homes.get(playerName);
      if (home == null) {
        player.addTMessage(Color.RED, "You don't have a home to manage!");
        return;
      }
      if (home.isPublic == true) {
        player.addTMessage(Color.GRAY, "Your home is already public!");
        return;
      }
      home.isPublic = true;
      homes.set(playerName, home);
      player.getServer().data.save();
      player.addTMessage(Color.GRAY, "Your home is now public!");
    } else if (command.equals("private")) {
      HomePoint home = homes.get(playerName);
      if (home == null) {
        player.addTMessage(Color.RED, "You don't have a home to manage!");
        return;
      }
      if (home.isPublic == false) {
        player.addTMessage(Color.GRAY, "Your home is already private!");
        return;
      }
      home.isPublic = false;
      homes.set(playerName, home);
      player.getServer().data.save();
      player.addTMessage(Color.GRAY, "Your home is now private!");
    } else if (command.equals("ilist")) {
      List<String> inviteList = new LinkedList<String>();
      List<String> publicList = new LinkedList<String>();
      homes.getVisitableHomes(playerName, inviteList, publicList);
      if (inviteList.isEmpty() && publicList.isEmpty()) {
        player.addTMessage(Color.GRAY, "You cannot visit any home.");
        return;
      }

      player.addTMessage(Color.GRAY, "Public homes: %s", join(publicList).trim());
      player.addTMessage(Color.GRAY, "Homes you are invited to: %s", join(inviteList).trim());

    } else if (command.equals("list")) {
      HomePoint home = homes.get(playerName);
      if (home == null) {
        player.addTMessage(Color.RED, "You don't have a home to manage!");
        return;
      }
      if (home.isPublic) {
        player.addTMessage(Color.GRAY, "Your home is public; everyone can visit it.");
        return;
      }
      Set<String> list = home.getPlayersInvited();
      if (list.isEmpty()) {
        player.addTMessage(Color.GRAY, "You haven't invited anyone.");
        return;
      }
      player.addTMessage(Color.GRAY, "You have invited %s.", join(list).trim());
    } else if (command.equals("invite")) {
      HomePoint home = homes.get(playerName);
      if (home == null) {
        player.addTMessage(Color.RED, "You don't have a home to manage!");
        return;
      }
      if (arguments.length == 1) {
        player.addTMessage(Color.RED, "Invalid argument!");
        usage(player);
        return;
      }
      String iPlayer = arguments[1];
      Player onlineTarget = player.getServer().findPlayer(iPlayer);
      if (onlineTarget != null) {
        iPlayer = onlineTarget.getName();
      }
      if (!home.invites.contains(new NBTString(iPlayer))) {
        home.invites.add(new NBTString(iPlayer));
        player.addTMessage(Color.GRAY, "You just invited %s.", iPlayer);
        if (onlineTarget != null) {
          onlineTarget.addTMessage(Color.GRAY, "You were just invited to visit %s's home.", player.getName());
        }
      } else {
        player.addTMessage(Color.GRAY, "Player has already been invited.");
      }
    } else if (command.equals("uninvite")) {
      HomePoint home = homes.get(playerName);
      if (home == null) {
        player.addTMessage(Color.RED, "You don't have a home to manage!");
        return;
      }
      if (arguments.length == 1) {
        player.addTMessage(Color.RED, "Invalid argument!");
        usage(player);
        return;
      }
      String uiPlayer = home.getInvitedPlayer(arguments[1]);
      if (home.invites.contains(new NBTString(uiPlayer))) {
        home.invites.remove(new NBTString(uiPlayer));
        player.addTMessage(Color.GRAY, "You just uninvited %s.", uiPlayer);
      } else {
        player.addTMessage(Color.GRAY, "Player wasn't invited.");
      }
    } else if (command.equals("inspect")) {
      if (arguments.length < 2) {
        player.addTMessage(Color.GRAY, "No player name given!");
        return;
      }
      String vPlayer = arguments[1];
      HomePoint home = homes.get(vPlayer);
      if (home == null) {
        player.addTMessage(Color.GRAY, "Player doesn't have a home set!");
        return;
      }
      player.teleportWithWarmup(home.position);
    } else {
      if (command.toLowerCase().equals(playerName.toLowerCase())) {
        teleportHome(player);
        return;
      }
      String target = command;
      String onlinePlayer = player.getServer().findName(target);
      if (onlinePlayer != null) {
        target = onlinePlayer;
      } else {
        Set<String> list = homes.getHomesPlayerInvitedTo(playerName);
        for (String p : list) {
          if (p.startsWith(target)) {
            target = p;
            break;
          }
        }
      }
      HomePoint home = homes.get(target);
      if (home == null) {
        usage(player);
        return;
      }
      if ((home.isPublic && player.getServer().findPlayer(target) != null) ||
          home.invites.contains(new NBTString(playerName)) ||
          target.toLowerCase().equals(playerName.toLowerCase())) {
        player.teleportWithWarmup(home.position);
      } else {
        player.addTMessage(Color.RED, "You are not allowed to visit %s's home.", command);
      }
    }
  }

  private void teleportHome(Player player) {
    HomePoint home = player.getServer().data.players.homes.get(player.getName());
    if (home == null) {
      player.addTMessage(Color.RED, "You don't have a home to teleport to!");
      return;
    }
    player.teleportWithWarmup(home.position);
  }

  private void usage(Player player) {
    String playerName = player.getName();
    String home = commandPrefix() + "home";
    HomePoint playerHome = player.getServer().data.players.homes.get(playerName);

    player.addTMessage(Color.GRAY, "Usage:");
    if (playerHome != null) {
      player.addTMessage(Color.GRAY, "%s:%s teleport home", home, Color.WHITE);
    }
    player.addTMessage(Color.GRAY, "%s name:%s visit someone's home", home, Color.WHITE);
    player.addTMessage(Color.GRAY, "%s:%s set your home at your location", home + " set", Color.WHITE);
    if (playerHome != null) {
      player.addTMessage(Color.GRAY, "%s:%s delete your home", home + " delete", Color.WHITE);
      if (!playerHome.isPublic) {
        player.addTMessage(Color.GRAY, "%s:%s make everyone able to visit your home", home + " public", Color.WHITE);
      } else {
        player.addTMessage(Color.GRAY, "%s:%s only allow invited players to visit", home + " private", Color.WHITE);
      }
    }
    player.addTMessage(Color.GRAY, "%s:%s see homes you can visit", home + " ilist", Color.WHITE);
    if (playerHome != null) {
      player.addTMessage(Color.GRAY, "%s:%s see players who can visit your home", home + " list", Color.WHITE);
      if (!playerHome.isPublic) {
        player.addTMessage(Color.GRAY, "%s name:%s allow player to visit your home", home + " invite", Color.WHITE);
        player.addTMessage(Color.GRAY, "%s name:%s disallow player to visit your home", home + " uninvite", Color.WHITE);
      }
    }
  }
}
