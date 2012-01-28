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

import java.util.Iterator;

import simpleserver.nbt.NBT;
import simpleserver.nbt.NBTCompound;
import simpleserver.nbt.NBTList;
import simpleserver.nbt.NBTShort;
import simpleserver.stream.StreamTunnel;

public class EnchantableItems {
  private NBTList<NBTShort> node = null;
  private final String ENCHANTABLE = "enchantable";

  public void add(short id) {
    node.add(new NBTShort(id));
  }

  public void clear() {
    node.clear();
  }

  void load(NBTCompound data) {
    if (data.containsKey(ENCHANTABLE)) {
      try {
        node = data.getList(ENCHANTABLE).cast();
        update();
        return;
      } catch (Exception e) {
        System.out.println("[WARNING] Enchantable item list is corrupt. Replacing it with default list...");
      }
    }
    node = new NBTList<NBTShort>(ENCHANTABLE, NBT.SHORT);
    data.put(node);
    loadDefaults();
    update();
  }

  // Update list smoothly in case players are on the server
  public void update() {
    Iterator<Short> it = StreamTunnel.ENCHANTABLE.iterator();
    while (it.hasNext()) {
      if (!node.contains(new NBTShort(it.next()))) {
        it.remove();
      }
    }
    for (NBTShort id : node) {
      if (!StreamTunnel.ENCHANTABLE.contains(id.get())) {
        StreamTunnel.ENCHANTABLE.add(id.get());
      }
    }
  }

  public void loadDefaults() {
    add((short) 0x15a); // Fishing rod
    add((short) 0x167); // Shears
    add((short) 0x105); // Bow
    // Tools
    for (short id = 256; id <= 259; id++) {
      add(id);
    }
    for (short id = 267; id <= 279; id++) {
      add(id);
    }
    for (short id = 283; id <= 286; id++) {
      add(id);
    }
    for (short id = 290; id <= 294; id++) {
      add(id);
    }
    // Armor
    for (short id = 298; id <= 317; id++) {
      add(id);
    }
  }
}
