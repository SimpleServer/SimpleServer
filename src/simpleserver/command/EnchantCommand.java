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
package simpleserver.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.lang.WordUtils;

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.bot.BotController.ConnectException;
import simpleserver.bot.Giver;
import simpleserver.config.GiveAliasList;
import simpleserver.config.GiveAliasList.Item;
import simpleserver.config.GiveAliasList.Suggestion;
import simpleserver.nbt.Inventory.Enchantment;
import simpleserver.nbt.Inventory.Slot;

public class EnchantCommand extends AbstractCommand implements PlayerCommand {
  public EnchantCommand() {
    super("enchant [ITEM | add ID:LEVEL | give | remove ID]", "Spawns enchanted items");
  }

  private static final HashMap<Integer, String> ENCHANTMENTS = new HashMap<Integer, String>();
  private static final HashMap<Integer, Integer[]> APPLIABLE = new HashMap<Integer, Integer[]>();
  private static final HashMap<Integer, String> ITEMS = new HashMap<Integer, String>();
  private static final String[] LEVELS = new String[] { "0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };

  static {
    ENCHANTMENTS.put(0, "Protection");
    ENCHANTMENTS.put(1, "Fire Protection");
    ENCHANTMENTS.put(2, "Feather Falling");
    ENCHANTMENTS.put(3, "Blast Protection");
    ENCHANTMENTS.put(4, "Proectile Protection");
    ENCHANTMENTS.put(5, "Respiration");
    ENCHANTMENTS.put(6, "Aqua Affinity");
    ENCHANTMENTS.put(16, "Sharpness");
    ENCHANTMENTS.put(17, "Smite");
    ENCHANTMENTS.put(18, "Bane of Arthropods");
    ENCHANTMENTS.put(19, "Knockback");
    ENCHANTMENTS.put(20, "Fire Aspect");
    ENCHANTMENTS.put(21, "Looting");
    ENCHANTMENTS.put(32, "Efficiency");
    ENCHANTMENTS.put(33, "Silk Touch");
    ENCHANTMENTS.put(34, "Unbreaking");
    ENCHANTMENTS.put(35, "Fortune");
    ENCHANTMENTS.put(48, "Power");
    ENCHANTMENTS.put(49, "Punch");
    ENCHANTMENTS.put(50, "Flame");
    ENCHANTMENTS.put(51, "Infinity");

    ITEMS.put(256, "Iron Shovel");
    ITEMS.put(257, "Iron Pickaxe");
    ITEMS.put(258, "Iron Axe");
    ITEMS.put(267, "Iron Sword");
    ITEMS.put(268, "Wooden Sword");
    ITEMS.put(269, "Wooden Shovel");
    ITEMS.put(270, "Wooden Pickaxe");
    ITEMS.put(271, "Wooden Axe");
    ITEMS.put(272, "Stone Sword");
    ITEMS.put(273, "Stone Shovel");
    ITEMS.put(274, "Stone Pickaxe");
    ITEMS.put(275, "Stone Axe");
    ITEMS.put(276, "Diamond Sword");
    ITEMS.put(277, "Diamond Shovel");
    ITEMS.put(278, "Diamond Pickaxe");
    ITEMS.put(279, "Diamond Axe");
    ITEMS.put(283, "Gold Sword");
    ITEMS.put(284, "Gold Shovel");
    ITEMS.put(285, "Gold Pickaxe");
    ITEMS.put(286, "Gold Axe");
    ITEMS.put(298, "Leather Cap");
    ITEMS.put(299, "Leather Tunic");
    ITEMS.put(300, "Leather Pants");
    ITEMS.put(301, "Leather Boots");
    ITEMS.put(302, "Chain Helmet");
    ITEMS.put(303, "Chain Chestplate");
    ITEMS.put(304, "Chain Leggings");
    ITEMS.put(305, "Chain Boots");
    ITEMS.put(306, "Iron Helmet");
    ITEMS.put(307, "Iron Chestplate");
    ITEMS.put(308, "Iron Leggings");
    ITEMS.put(309, "Iron Boots");
    ITEMS.put(310, "Diamond Helmet");
    ITEMS.put(311, "Diamond Chestplate");
    ITEMS.put(312, "Diamond Leggings");
    ITEMS.put(313, "Diamond Boots");
    ITEMS.put(314, "Gold Helmet");
    ITEMS.put(315, "Gold Chestplate");
    ITEMS.put(316, "Gold Leggings");
    ITEMS.put(317, "Gold Boots");
    ITEMS.put(261, "Bow");

    for (int i = 0; i <= 5; i++) {
      if (i == 2) {
        APPLIABLE.put(i, new Integer[] { 301, 305, 309, 313, 317 });
      } else {
        APPLIABLE.put(i, new Integer[] { 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317 });
      }
    }
    for (int i = 5; i <= 6; i++) {
      APPLIABLE.put(i, new Integer[] { 298, 302, 306, 310, 314 });
    }
    for (int i = 16; i <= 21; i++) {
      APPLIABLE.put(i, new Integer[] { 267, 268, 272, 276, 283, 314 });
    }
    for (int i = 32; i <= 35; i++) {
      APPLIABLE.put(i, new Integer[] { 256, 257, 258, 269, 270, 271, 273, 274, 275, 277, 278, 279, 284, 285, 286 });
    }
    for (int i = 48; i <= 51; i++) {
      APPLIABLE.put(i, new Integer[] { 261 });
    }
  }

  private HashMap<Player, Slot> sessions = new HashMap<Player, Slot>();

  private static Collection<Integer> availableEnchantments(Slot item) {
    ArrayList<Integer> available = new ArrayList<Integer>();
    for (int ench : ENCHANTMENTS.keySet()) {
      if (item.enchantedWith(ench)) {
        continue;
      }
      for (int id : APPLIABLE.get(ench)) {
        if (id == item.id) {
          available.add(ench);
          break;
        }
      }
    }
    return available;
  }

  private void chatItem(Player player, Slot item) {
    player.addTMessage(Color.GRAY, "Current item:");
    player.addMessage(Color.CYAN, "%s (%s)", ITEMS.get(Integer.valueOf(item.id)), item.id);
    if (item.enchantments().size() == 0) {
      player.addMessage(Color.DARK_GRAY, "None");
    } else {
      for (Enchantment ench : item.enchantments()) {
        player.addMessage(Color.DARK_GRAY, "%s %s", ENCHANTMENTS.get(Integer.valueOf(ench.id)), LEVELS[ench.level]);
      }
    }
    player.addMessage(" ");
    player.addTMessage(Color.GRAY, "Available enchantments:");
    if (availableEnchantments(item).size() == 0) {
      player.addMessage(Color.DARK_GRAY, "None");
    } else {
      for (Integer ench : availableEnchantments(item)) {
        player.addMessage(Color.DARK_GRAY, "%s: %s%s", ench, Color.WHITE, ENCHANTMENTS.get(ench));
      }
    }
  }

  private Slot getSessionItem(Player player) {
    return getSessionItem(player, true);
  }

  private Slot getSessionItem(Player player, boolean warning) {
    if (sessions.containsKey(player)) {
      return sessions.get(player);
    } else {
      if (warning) {
        player.addMessage(Color.RED, "You didn't select an item yet");
      }
      return null;
    }
  }

  public void execute(Player player, String message) {
    String[] parts = message.trim().split(" ");
    Slot item;
    if (parts.length == 1) {
      if ((item = getSessionItem(player, false)) == null) {
        player.addTMessage(Color.GRAY, "You have to select an item to enchant first.");
        player.addTMessage(Color.GRAY, "Use: %s%s ITEM", commandPrefix(), name);
      } else {
        chatItem(player, item);
      }
    } else if (parts[1].equals("add")) {
      if ((item = getSessionItem(player)) == null) {
        return;
      }
      boolean changed = false;
      for (int i = 2; i < parts.length; i++) {
        String[] ench = parts[i].split(":");
        try {
          Integer id = Integer.valueOf(ench[0]);
          int level = 10;
          if (ench.length >= 2) {
            level = Integer.valueOf(ench[1]);
            if (level < 1 || level > 10) {
              player.addTMessage(Color.RED, "The enchantment level must be between 1 and 10");
              continue;
            }
          }
          if (!availableEnchantments(item).contains(id)) {
            if (ENCHANTMENTS.containsKey(Integer.valueOf(id))) {
              player.addTMessage(Color.RED, "%s is not available for the current item", ENCHANTMENTS.get(id));
            } else {
              player.addTMessage(Color.RED, "No enchantment with id %s exists.", id);
            }

          } else {
            item.addEnchantment(new Enchantment(id, level));
            changed = true;
          }
        } catch (NumberFormatException e) {
          player.addTMessage(Color.RED, "%s is not a valid enchantment", parts[i]);
        }
      }
      if (changed) {
        chatItem(player, item);
      }
    } else if (parts[1].equals("remove")) {
      if ((item = getSessionItem(player)) == null) {
        return;
      }
      boolean changed = false;
      for (int i = 2; i < parts.length; i++) {
        try {
          Integer id = Integer.valueOf(parts[i]);
          if (!item.enchantedWith(id)) {
            player.addTMessage(Color.RED, "The item was not enchanted with %s", (ENCHANTMENTS.containsKey(id) ? ENCHANTMENTS.get(id) : id));
          } else {
            item.removeEnchantment(id);
            changed = true;
          }
        } catch (NumberFormatException e) {
          player.addTMessage(Color.RED, "%s is not a valid enchantment", parts[i]);
        }
      }
      if (changed) {
        chatItem(player, item);
      }
    } else if (parts[1].equals("spawn") || parts[1].equals("give")) {
      if ((item = getSessionItem(player)) == null) {
        return;
      }
      Giver bot = new Giver(player);
      bot.add(item);
      try {
        player.getServer().bots.connect(bot);
      } catch (ConnectException e) {
        player.addTMessage(Color.RED, "An unknown error occured");
      }
    } else {
      int id = 0;
      if (parts.length == 2) {
        try {
          id = Integer.valueOf(parts[1]);
        } catch (NumberFormatException e) {
          GiveAliasList alias = player.getServer().giveAliasList;
          Item itemAlias = alias.getItemId(parts[1]);
          if (itemAlias == null) {
            Suggestion correctName = alias.findWithLevenshtein(parts[1]);
            if (correctName.distance < 4) {
              id = alias.getItemId(correctName.name).id;
            }
          } else {
            id = itemAlias.id;
          }
        }
      } else {
        String name = WordUtils.capitalize(extractArgument(message));
        if (ITEMS.containsValue(name)) {
          for (Integer itemID : ITEMS.keySet()) {
            if (ITEMS.get(itemID).equals(name)) {
              id = itemID;
              break;
            }
          }
        }
      }
      if (id == 0) {
        player.addTMessage(Color.RED, "Can't find item");
      } else if (!ITEMS.containsKey(id)) {
        player.addTMessage(Color.RED, "This item is not enchantable");
      } else {
        item = new Slot(id);
        chatItem(player, item);
        player.addTMessage(Color.GRAY, "You can now add enchantments with %s%s%s", commandPrefix(), name, " add ID:LEVEL");
        sessions.put(player, item);
      }
    }
  }
}
