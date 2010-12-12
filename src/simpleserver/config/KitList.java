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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import simpleserver.Group;
import simpleserver.Player;
import simpleserver.Server;

public class KitList extends PropertiesConfig {
  private Server server;

  private static final class Kit {
    public int[] groups;
    public LinkedList<Entry> items = new LinkedList<Entry>();

    private static final class Entry {
      int item;
      int amount;

      public Entry(int item, int amount) {
        this.item = item;
        this.amount = amount;
      }
    }

    public Kit(int[] groups) {
      this.groups = groups;
    }

    public void addItem(int item, int amount) {
      items.add(new Entry(item, amount));
    }
  }

  private Map<String, Kit> kits;

  public KitList(Server parent) {
    super("kit-list.txt");

    this.server = parent;
    this.kits = new HashMap<String, Kit>();
  }

  public void giveKit(Player player, String kitName)
      throws InterruptedException {
    Kit kit = kits.get(kitName);
    if ((kit != null) && (Group.contains(kit.groups, player))) {
      for (Kit.Entry entry : kit.items) {
        String baseCommand = "give " + player.getName() + " " + entry.item;
        for (int c = 0; c < entry.amount / 64; ++c) {
          server.runCommand(baseCommand + " " + 64);
        }
        server.runCommand(baseCommand + " " + entry.amount % 64);
      }
    }
  }

  public void listKits(Player player) {
    StringBuilder kitList = new StringBuilder();
    kitList.append("Allowed kits: ");
    for (String name : kits.keySet()) {
      Kit kit = kits.get(name);
      if (Group.contains(kit.groups, player)) {
        kitList.append(name);
        kitList.append(", ");
      }
    }

    player.addMessage(kitList.substring(0, kitList.length() - 2));
  }

  public void load() {
    super.load();

    kits.clear();
    for (Entry<Object, Object> entry : entrySet()) {
      String[] options = entry.getValue().toString().split(",");
      if (options.length < 2) {
        System.out.println("Skipping bad kit list entry " + entry.getValue());
        continue;
      }

      int[] groups = Group.parseGroups(options[0], ";");
      Kit kit = new Kit(groups);

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

        kit.addItem(block, amount);
      }

      kits.put(entry.getKey().toString(), kit);
    }
  }
}
