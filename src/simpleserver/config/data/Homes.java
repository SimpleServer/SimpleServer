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

import simpleserver.Position;
import simpleserver.nbt.NBTCompound;

public class Homes {
  private final static String HOME = "home";

  private final PlayerData playerData;

  Homes(PlayerData playerData) {
    this.playerData = playerData;
  }

  public Position get(String playerName) {
    NBTCompound player = playerData.get(playerName.toLowerCase());
    if (player.containsKey(HOME)) {
      return new Position(player.getCompound(HOME));
    }
    return null;
  }

  public void remove(String playerName) {
    NBTCompound player = playerData.get(playerName.toLowerCase());
    if (player.containsKey(HOME)) {
      player.remove(HOME);
    }
  }

  public void set(String playerName, Position home) {
    NBTCompound player = playerData.get(playerName.toLowerCase());
    NBTCompound tag = home.tag();
    tag.rename(HOME);
    player.put(tag);
  }
}
