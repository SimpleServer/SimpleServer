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

import simpleserver.Player;
import simpleserver.config.LegacyStats;
import simpleserver.config.LegacyStats.Statistic;
import simpleserver.nbt.NBTCompound;
import simpleserver.nbt.NBTInt;

public class Stats {
  private final PlayerData playerData;

  Stats(PlayerData playerData) {
    this.playerData = playerData;
  }

  private final static String STATS = "stats";

  public int get(Player player, StatField field) {
    return getInt(player.getName(), field.toString()).get();
  }

  public void set(Player player, StatField field, int value) {
    getInt(player.getName(), field.toString()).set(value);
  }

  public int add(Player player, StatField field, int amount) {
    NBTInt tag = getInt(player.getName(), field.toString());
    tag.set(tag.get() + amount);
    return tag.get();
  }

  private NBTCompound getStats(String name) {
    NBTCompound player = playerData.get(name);
    if (player.containsKey(STATS)) {
      return player.getCompound(STATS);
    } else {
      NBTCompound tag = new NBTCompound(STATS);
      player.put(tag);
      return tag;
    }
  }

  private NBTInt getInt(String name, String key) {
    NBTCompound player = getStats(name);
    if (player.containsKey(key)) {
      return player.getInt(key);
    } else {
      NBTInt tag = new NBTInt(key, 0);
      player.put(tag);
      return tag;
    }
  }

  public enum StatField {
    PLAY_TIME,
    BLOCKS_DESTROYED,
    BLOCKS_PLACED;
  }

  public void loadOldConfig() {
    LegacyStats old = new LegacyStats();
    old.load();
    for (String name : old.stats.keySet()) {
      Statistic oldStats = old.stats.get(name);
      NBTCompound tag = playerData.get(name);
      NBTCompound stats = new NBTCompound("stats");
      stats.put(new NBTInt(StatField.PLAY_TIME.toString(), oldStats.minutes));
      stats.put(new NBTInt(StatField.BLOCKS_DESTROYED.toString(), oldStats.blocksDestroyed));
      stats.put(new NBTInt(StatField.BLOCKS_PLACED.toString(), oldStats.blocksPlaced));
      tag.put(stats);
    }
    old.save();
  }
}
