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
package simpleserver.config.data;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import simpleserver.Player;
import simpleserver.Position;
import simpleserver.nbt.NBT;
import simpleserver.nbt.NBTByte;
import simpleserver.nbt.NBTCompound;
import simpleserver.nbt.NBTList;
import simpleserver.nbt.NBTString;

public class Homes {
  private final static String HOME = "home";

  private final PlayerData playerData;

  Homes(PlayerData playerData) {
    this.playerData = playerData;
  }

  public HomePoint get(String playerName) {
    NBTCompound player = playerData.get(playerName);
    if (player.containsKey(HOME)) {
      return new HomePoint(player.getCompound(HOME));
    }
    return null;
  }

  public List<String> getHomesPlayerInvitedTo(String playerName) {
    List<String> invitedHomes = new LinkedList<String>();
    for (String name : playerData.names()) {
      NBTCompound player = playerData.get(name);
      if (player.containsKey(HOME)) {
        HomePoint home = new HomePoint(player.getCompound(HOME));
        if (home.invites.contains(new NBTString(playerName))) {
          invitedHomes.add(name);
        }
      }
    }
    return invitedHomes;
  }

  public void getVisitableHomes(String playerName, List<String> invitedHomes, List<String> publicHomes) {
    for (String name : playerData.names()) {
      NBTCompound player = playerData.get(name);
      if (player.containsKey(HOME)) {
        HomePoint home = new HomePoint(player.getCompound(HOME));
        if (home.isPublic) {
          publicHomes.add(name);
        } else if (home.invites.contains(new NBTString(playerName)) || name.equals(playerName.toLowerCase())) {
          invitedHomes.add(name);
        }
      }
    }
  }

  public void remove(String playerName) {
    NBTCompound player = playerData.get(playerName);
    if (player.containsKey(HOME)) {
      player.remove(HOME);
    }
  }

  public void set(String playerName, HomePoint homePoint) {
    NBTCompound player = playerData.get(playerName);
    player.put(homePoint.tag());
  }

  public HomePoint makeHomePoint(Position position) {
    return new HomePoint(position);
  }

  public class HomePoint {
    private final static String PUBLIC = "public";
    private final static String INVITES = "invites";

    public Position position;
    public boolean isPublic;
    public NBTList<NBTString> invites;

    public HomePoint(Position position) {
      this.position = position;
      isPublic = false;
      invites = new NBTList<NBTString>(INVITES, NBT.STRING);
    }

    public HomePoint(Position position, boolean isPublic, NBTList<NBTString> invites) {
      this.position = position;
      this.isPublic = isPublic;
      this.invites = invites;
    }

    public HomePoint(NBTCompound tag) {
      position = new Position(tag);
      isPublic = tag.getByte(PUBLIC).get().equals((byte) 1);
      invites = tag.getList(INVITES).cast();
    }

    public String getInvitedPlayer(String prefix) {
      if (getPlayersInvited().contains(prefix)) {
        return prefix;
      }
      for (String playerName : getPlayersInvited()) {
        if (playerName.startsWith(prefix)) {
          return playerName;
        }
      }
      return prefix;
    }

    public List<String> getPlayersInvited() {
      List<String> playersInvited = new LinkedList<String>();
      Iterator<NBTString> iterator = invites.iterator();
      while (iterator.hasNext()) {
        playersInvited.add(iterator.next().get());
      }
      return playersInvited;
    }

    public boolean getPlayerInvited(Player player) {
      return invites.contains(new NBTString(player.getName()));
    }

    public NBTCompound tag() {
      NBTCompound tag = position.tag();
      tag.rename(HOME);
      NBTByte publicValue = new NBTByte(PUBLIC, isPublic ? (byte) 1 : (byte) 0);
      tag.put(publicValue);
      tag.put(invites);
      return tag;
    }
  }
}
