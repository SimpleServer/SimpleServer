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
import java.util.Set;

import simpleserver.Player;
import simpleserver.nbt.NBTArray;
import simpleserver.nbt.NBTCompound;
import simpleserver.nbt.NBTString;

public class PlayerData implements Iterable<String> {
  private NBTCompound node;
  public Stats stats = new Stats(this);
  public Homes homes = new Homes(this);

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

  public Set<String> names() {
    return node.names();
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

  public Iterator<String> iterator() {
    return node.names().iterator();
  }

  public int count() {
    return node.size();
  }

  public enum PlayerField {
    FULL_NAME, // String
    RENAME_NAME, // String
    PW_HASH; // byte[]
  }
}
