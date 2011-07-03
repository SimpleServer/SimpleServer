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

import simpleserver.Coordinate.Dimension;
import simpleserver.Player;
import simpleserver.Position;
import simpleserver.nbt.NBTArray;
import simpleserver.nbt.NBTByte;
import simpleserver.nbt.NBTCompound;
import simpleserver.nbt.NBTDouble;
import simpleserver.nbt.NBTFloat;
import simpleserver.nbt.NBTString;

public class PlayerData {
  private NBTCompound node;
  public Stats stats = new Stats(this);

  private final static String PLAYERS = "players";

  void load(NBTCompound data) {
    if (data.containsKey(PLAYERS)) {
      try {
        node = data.getCompound(PLAYERS);
        return;
      } catch (Exception e) {
        System.out.println("[WARNING] Player list is corrupt. Replacing it with empty list...");
      }
    }
    node = new NBTCompound(PLAYERS);
    data.put(node);
    stats.loadOldConfig();
  }

  NBTCompound get(String name) {
    name = name.toLowerCase();
    if (node.containsKey(name)) {
      return node.getCompound(name);
    } else {
      NBTCompound player = new NBTCompound(name);
      node.put(player);
      return player;
    }
  }

  public String getRealName(String playerName) {
    NBTCompound playerData = get(playerName.toLowerCase());
    String field = PlayerField.FULL_NAME.toString();
    if (playerData.containsKey(field)) {
      return playerData.getString(field).get();
    } else {
      NBTString tag = new NBTString(field, playerName);
      playerData.put(tag);
      return tag.get();
    }
  }

  public String getRenameName(String playerName) {
    NBTCompound playerData = get(playerName.toLowerCase());
    String field = PlayerField.RENAME_NAME.toString();
    if (playerData.containsKey(field)) {
      return playerData.getString(field).get();
    } else {
      NBTString tag = new NBTString(field, playerName);
      playerData.put(tag);
      return tag.get();
    }
  }

  public byte[] getPwHash(String playerName) {
    NBTCompound playerData = get(playerName.toLowerCase());
    String field = PlayerField.PW_HASH.toString();
    if (playerData.containsKey(field)) {
      byte[] a = playerData.getArray(field).get();
      return a;
    }
    return null;
  }

  public Position getHome(String playerName) {
    NBTCompound playerData = get(playerName.toLowerCase());
    String field = PlayerField.HOME.toString();
    if (playerData.containsKey(field)) {
      NBTCompound home = playerData.getCompound(field);
      double x = home.getDouble("x").get();
      double y = home.getDouble("y").get();
      double z = home.getDouble("z").get();
      Dimension dim = Dimension.get(home.getByte("Dimension").get());
      float yaw = home.getFloat("yaw").get();
      float pitch = home.getFloat("pitch").get();
      return new Position(x, y, z, dim, yaw, pitch);
    }
    return null;
  }

  public void setRealName(String realName) {
    NBTCompound playerData = get(realName.toLowerCase());
    String field = PlayerField.FULL_NAME.toString();
    if (playerData.containsKey(field)) {
      playerData.getString(field).set(realName);
    } else {
      NBTString tag = new NBTString(field, realName);
      playerData.put(tag);
    }
  }

  public void setRenameName(Player player, String renameName) {
    NBTCompound playerData = get(player.getName(true).toLowerCase());
    String field = PlayerField.RENAME_NAME.toString();
    if (playerData.containsKey(field)) {
      playerData.getString(field).set(renameName);
    } else {
      NBTString tag = new NBTString(field, player.getName());
      playerData.put(tag);
    }
  }

  public void setPw(String playerName, byte[] pwHash) {
    NBTCompound playerData = get(playerName.toLowerCase());
    String field = PlayerField.PW_HASH.toString();
    if (playerData.containsKey(field)) {
      playerData.getArray(field).set(pwHash);
    } else {
      NBTArray tag = new NBTArray(field, pwHash);
      playerData.put(tag);
    }
  }

  public void setHome(String playerName, Position home) {
    NBTCompound playerData = get(playerName.toLowerCase());
    String field = PlayerField.HOME.toString();

    if (playerData.containsKey(field)) {
      playerData.remove(field);
    }
    NBTCompound tag = new NBTCompound(field);
    tag.put(new NBTDouble("x", home.x));
    tag.put(new NBTDouble("y", home.y));
    tag.put(new NBTDouble("z", home.z));
    tag.put(new NBTByte("Dimension", home.dimension.index()));
    tag.put(new NBTFloat("yaw", home.yaw));
    tag.put(new NBTFloat("pitch", home.pitch));
    // TODO what is this used for?
    // tag.put(new NBTArray(CAPS, capitals(name)));
    playerData.put(tag);
  }

  public void removeHome(String playerName) {
    NBTCompound playerData = get(playerName.toLowerCase());
    String field = PlayerField.HOME.toString();

    if (playerData.containsKey(field)) {
      playerData.remove(field);
    }
  }

  public enum PlayerField {
    FULL_NAME, // String
    RENAME_NAME, // String
    PW_HASH, // byte[]
    HOME; // Position
  }

}
