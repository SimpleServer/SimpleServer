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

import simpleserver.nbt.NBTCompound;

public class PlayerData {
  private NBTCompound node;
  public Stats stats = new Stats(this);

  void load(NBTCompound data) {
    if (data.containsKey("players")) {
      try {
        node = data.getCompound("players");
        return;
      } catch (Exception e) {
        System.out.println("[WARNING] Player list is corrupt. Replacing it with empty list...");
      }
    }
    node = new NBTCompound("players");
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
}
