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
package simpleserver.config;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import simpleserver.Group;
import simpleserver.Player;

import com.google.common.collect.ImmutableSet;

public class BlockList extends PropertiesConfig {
  private final ConcurrentMap<Integer, ImmutableSet<Integer>> blocks;

  public BlockList() {
    super("block-list.txt", true);

    blocks = new ConcurrentHashMap<Integer, ImmutableSet<Integer>>();
  }

  public boolean contains(int blockID) {
    return blocks.containsKey(blockID);
  }

  public boolean playerAllowed(Player player, int blockID) {
    ImmutableSet<Integer> groups = blocks.get(blockID);
    if (groups != null) {
      return Group.isMember(groups, player);
    }
    return true;
  }

  @Override
  public void load() {
    super.load();

    blocks.clear();
    for (Entry<Object, Object> entry : properties.entrySet()) {
      Integer block;
      try {
        block = Integer.parseInt(entry.getKey().toString());
      }
      catch (NumberFormatException e) {
        System.out.println("Skipping bad block list entry " + entry.getKey());
        continue;
      }

      blocks.put(block, Group.parseGroups(entry.getValue().toString()));
    }
  }
}
