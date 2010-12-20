/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package simpleserver.config;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import simpleserver.Group;
import simpleserver.Player;

import com.google.common.collect.ImmutableSet;

public class ItemWatchList extends PropertiesConfig {
  private static final class Options {
    private final int threshold;
    private final ImmutableSet<Integer> groups;

    public Options(int threshold, ImmutableSet<Integer> groups) {
      this.threshold = threshold;
      this.groups = groups;
    }
  }

  private final ConcurrentMap<Integer, Options> items;

  public ItemWatchList() {
    super("item-watch-list.txt");

    items = new ConcurrentHashMap<Integer, Options>();
  }

  public boolean contains(int blockID) {
    return items.containsKey(blockID);
  }

  public boolean playerAllowed(Player player, int blockID, int amount) {
    Options options = items.get(blockID);
    if (options != null) {
      return amount >= options.threshold
          && Group.isMember(options.groups, player);
    }

    return true;
  }

  @Override
  public void load() {
    super.load();

    items.clear();
    for (Entry<Object, Object> entry : entrySet()) {
      Integer block;
      try {
        block = Integer.parseInt(entry.getKey().toString());
      }
      catch (NumberFormatException e) {
        System.out.println("Skipping bad item watch list entry "
            + entry.getKey());
        continue;
      }

      String[] options = entry.getValue().toString().split(":");
      if (options.length != 2) {
        System.out.println("Skipping bad item watch list entry "
            + entry.getValue());
        continue;
      }

      Integer threshold;
      try {
        threshold = Integer.parseInt(options[0].trim());
      }
      catch (NumberFormatException e) {
        System.out.println("Skipping bad item watch list entry " + options[0]);
        continue;
      }

      items.put(block, new Options(threshold, Group.parseGroups(options[1])));
    }
  }
}
