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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class GiveAliasList extends PropertiesConfig {
  private static final String[] suffixes = new String[] { "s", "block",
      "blocks", "ore", "ores", "es" };

  private final Map<String, Item> aliases;

  public GiveAliasList() {
    super("give-alias-list.txt", true);

    aliases = new HashMap<String, Item>();
  }

  public Item getItemId(String itemAlias) {
    itemAlias = itemAlias.toLowerCase();
    Item item = aliases.get(itemAlias);

    for (String suffix : suffixes) {
      if (item != null) {
        break;
      }

      if (itemAlias.endsWith(suffix)) {
        int prefixLength = itemAlias.length() - suffix.length();
        item = aliases.get(itemAlias.substring(0, prefixLength));
      }
    }

    return item;
  }

  @Override
  public void load() {
    super.load();

    aliases.clear();
    for (Entry<Object, Object> alias : properties.entrySet()) {
      Integer id;
      Short damage = 0;
      String[] parts = ((String) alias.getValue()).split(":");
      try {
        id = Integer.valueOf(parts[0]);
        if (parts.length > 1) {
          damage = Short.valueOf(parts[1]);
        }
      } catch (NumberFormatException e) {
        System.out.println("Invalid give alias: " + alias.toString());
        continue;
      }

      aliases.put(((String) alias.getKey()).toLowerCase(), new Item(id, damage));
    }
  }

  public class Item {
    public int id;
    public short damage;

    public Item(int id) {
      this(id, (short) 0);
    }

    public Item(int id, short damage) {
      this.id = id;
      this.damage = damage;
    }
  }
}
