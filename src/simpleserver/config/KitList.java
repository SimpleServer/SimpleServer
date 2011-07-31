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

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.Server;
import simpleserver.bot.BotController.ConnectException;

import com.google.common.collect.ImmutableList;

public class KitList extends PropertiesConfig {
  private final Server server;

  private static final class Kit {
    public final String groups;
    public final ImmutableList<Entry> items;

    private static final class Entry {
      private final int item;
      private final short damage;
      private final int amount;

      public Entry(int item, short damage, int amount) {
        this.item = item;
        this.damage = damage;
        this.amount = amount;
      }

      public Entry(int item, int amount) {
        this(item, (short) 0, amount);
      }
    }

    public Kit(String groups, ImmutableList<Entry> items) {
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
    if ((kit != null) && (server.permissions.includesPlayer(kit.groups, player))) {
      for (Kit.Entry entry : kit.items) {
        try {
          player.give(entry.item, entry.damage, entry.amount);
        } catch (ConnectException e) {
          player.addTMessage(Color.RED, "Giving %s failed!", entry.item);
        }
      }
      return true;
    }
    return false;
  }

  public void listKits(Player player) {
    StringBuilder kitList = new StringBuilder();
    for (String name : kits.keySet()) {
      Kit kit = kits.get(name);
      if (server.permissions.includesPlayer(kit.groups, player)) {
        kitList.append(name);
        kitList.append(", ");
      }
    }
    if (kitList.length() == 0) {
      player.addTMessage(Color.RED, "You can't use any kits");
    } else {
      player.addTCaptionedMessage("Allowed kits", kitList.substring(0, kitList.length() - 2));
    }
  }

  @Override
  public void load() {
    super.load();

    kits.clear();
    for (Entry<Object, Object> entry : properties.entrySet()) {
      String[] options = entry.getValue().toString().split("\\|");
      if (options.length < 2) {
        System.out.println("Skipping bad kit list entry " + entry.getValue());
        continue;
      }

      boolean legacy = false;

      ImmutableList.Builder<Kit.Entry> items = ImmutableList.builder();
      for (int c = 1; c < options.length; ++c) {

        if (options[c].contains(":")) {
          // legacy
          Kit.Entry item = loadLegacyEntry(options[c]);
          if (item != null) {
            items.add(item);
            legacy = true;
          }
          continue;
        }

        String[] item = options[c].split("\\*");
        String[] data = item[0].split("\\.");
        if (item.length < 1 || item.length > 2 || data.length > 2 || data.length < 1) {
          System.out.println("Skipping bad kit item " + options[c]);
          continue;
        }

        Integer block;
        Short damage = 0;
        Integer amount = 1;

        try {
          if (data.length == 2) {
            damage = Short.parseShort(data[1]);
          }
          block = Integer.parseInt(data[0]);
          if (item.length == 2) {
            amount = Integer.parseInt(item[1]);
          }
        } catch (NumberFormatException e) {
          System.out.println("Skipping bad kit item " + options[c]);
          continue;
        }

        items.add(new Kit.Entry(block, damage, amount));
      }

      Kit kit = new Kit(options[0], items.build());
      kits.put(entry.getKey().toString().toLowerCase(), kit);

      if (legacy) {
        StringBuilder convertedEntry = new StringBuilder(kit.groups);
        for (Kit.Entry item : kit.items) {
          convertedEntry.append("|" + item.item);
          if (item.damage != 0) {
            convertedEntry.append("." + item.damage);
          }
          if (item.amount != 1) {
            convertedEntry.append("*" + item.amount);
          }
        }
        properties.setProperty(entry.getKey().toString(), convertedEntry.toString());
        System.out.println("Converting Kit " + entry.getKey().toString() + " to new format.");
      }
    }
  }

  private Kit.Entry loadLegacyEntry(String line) {
    String[] item = line.split(":");
    if (item.length != 2) {
      System.out.println("Skipping bad kit item " + line);
      return null;
    }

    Integer block;
    Integer amount;
    try {
      block = Integer.parseInt(item[0]);
      amount = Integer.parseInt(item[1]);
    } catch (NumberFormatException e) {
      System.out.println("Skipping bad kit item " + line);
      return null;
    }

    return new Kit.Entry(block, amount);
  }
}
