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
import simpleserver.Server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class KitList extends PropertiesConfig {
  private final Server server;

  private static final class Kit {
    public final ImmutableSet<Integer> groups;
    public final ImmutableList<Entry> items;

    private static final class Entry {
      private final int item;
      private final int amount;

      public Entry(int item, int amount) {
        this.item = item;
        this.amount = amount;
      }
    }

    public Kit(ImmutableSet<Integer> groups, ImmutableList<Entry> items) {
      this.groups = groups;
      this.items = items;
    }
  }

  private ConcurrentMap<String, Kit> kits;

  public KitList(Server parent) {
    super("kit-list.txt");

    server = parent;
    kits = new ConcurrentHashMap<String, Kit>();
  }

  public boolean giveKit(Player player, String kitName) {
    Kit kit = kits.get(kitName.toLowerCase());
    if ((kit != null) && (Group.isMember(kit.groups, player))) {
      for (Kit.Entry entry : kit.items) {
        String baseCommand = player.getName() + " " + entry.item;
        for (int c = 0; c < entry.amount / 64; ++c) {
          server.runCommand("give", baseCommand + " " + 64);
        }
        server.runCommand("give", baseCommand + " " + entry.amount % 64);
      }
      return true;
    }
    return false;
  }

  public void listKits(Player player) {
    StringBuilder kitList = new StringBuilder();
    kitList.append("\u00a77Allowed kits: \u00a7f");
    for (String name : kits.keySet()) {
      Kit kit = kits.get(name);
      if (Group.isMember(kit.groups, player)) {
        kitList.append(name);
        kitList.append(", ");
      }
    }

    player.addMessage(kitList.substring(0, kitList.length() - 2));
  }

  @Override
  public void load() {
    super.load();

    kits.clear();
    for (Entry<Object, Object> entry : properties.entrySet()) {
      String[] options = entry.getValue().toString().split(",");
      if (options.length < 2) {
        System.out.println("Skipping bad kit list entry " + entry.getValue());
        continue;
      }

      ImmutableList.Builder<Kit.Entry> items = ImmutableList.builder();
      for (int c = 1; c < options.length; ++c) {
        String[] item = options[c].split(":");
        if (item.length != 2) {
          System.out.println("Skipping bad kit item " + options[c]);
          continue;
        }

        Integer block;
        Integer amount;
        try {
          block = Integer.parseInt(item[0]);
          amount = Integer.parseInt(item[1]);
        }
        catch (NumberFormatException e) {
          System.out.println("Skipping bad kit item " + options[c]);
          continue;
        }

        items.add(new Kit.Entry(block, amount));
      }

      Kit kit = new Kit(Group.parseGroups(options[0], ";"), items.build());
      kits.put(entry.getKey().toString().toLowerCase(), kit);
    }
  }
}
