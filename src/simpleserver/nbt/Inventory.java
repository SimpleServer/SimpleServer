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
package simpleserver.nbt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Inventory {
  private HashMap<Byte, Slot> slots;;

  public Inventory() {
    slots = new HashMap<Byte, Slot>();
  }

  public byte add(short id, byte count) {
    return add(id, count, (short) 0);
  }

  public byte add(int id, int count) {
    return add((short) id, (byte) count);
  }

  public byte add(short id, byte count, short damage) {
    byte slot = bestSlot();
    set(slot, id, count, damage);
    return slot;
  }

  public byte add(Slot item) {
    byte slot = bestSlot();
    set(slot, item);
    return slot;
  }

  public void set(byte slot, Slot item) {
    slots.put(slot, item);
  }

  public byte add(int id, int count, int damage) {
    return add((short) id, (byte) count, (short) damage);
  }

  public void set(byte slot, short id, byte count, short damage) {
    set(slot, new Slot(id, count, damage));
  }

  public Slot get(byte slot) {
    return slots.get(slot);
  }

  public NBTList<NBTCompound> nbt() {
    NBTList<NBTCompound> list = new NBTList<NBTCompound>("Inventory", NBT.COMPOUND);
    for (byte slot : slots.keySet()) {
      NBTCompound tag = slots.get(slot).compound();
      tag.put(new NBTByte("Slot", slot));
      list.add(tag);
    }
    return list;
  }

  private byte bestSlot() {
    for (byte i = 0; i <= 44; i++) {
      if (free(i)) {
        return i;
      }
    }
    return 0;
  }

  private boolean free(byte slot) {
    return !slots.containsKey(slot);
  }

  public static byte networkSlot(byte slot) {
    if (slot <= 8) {
      return (byte) (slot + 36);
    }
    return slot;
  }

  public static class Slot {
    public short id;
    public byte count;
    public short damage;
    private List<Enchantment> enchantments = new ArrayList<Enchantment>(0);

    public Slot(int id) {
      this(id, 1, 0);
    }

    public Slot(int id, int count) {
      this(id, count, 0);
    }

    public Slot(int id, int count, int damage) {
      this.id = (short) id;
      this.count = (byte) count;
      this.damage = (short) damage;
    }

    public void addEnchantment(Enchantment enchantment) {
      enchantments.add(enchantment);
    }

    public List<Enchantment> enchantments() {
      return enchantments;
    }

    public boolean enchantedWith(int id) {
      for (Enchantment enchantment : enchantments) {
        if (enchantment.id == id) {
          return true;
        }
      }
      return false;
    }

    public void removeEnchantment(Integer id2) {
      for (Enchantment enchantment : enchantments) {
        if (enchantment.id == id) {
          enchantments.remove(enchantment);
          return;
        }
      }
    }

    public NBTCompound compound() {
      NBTCompound compound = new NBTCompound();
      compound.put(new NBTShort("id", id));
      compound.put(new NBTByte("Count", count));
      compound.put(new NBTShort("Damage", damage));
      if (enchantments.size() > 0) {
        NBTCompound tag = new NBTCompound("tag");
        NBTList<NBTCompound> ench = new NBTList<NBTCompound>("ench", NBT.COMPOUND);
        for (Enchantment enchantment : enchantments) {
          ench.add(enchantment.compound());
        }
        tag.put(ench);
        compound.put(tag);
      }
      return compound;
    }

    public void write(DataOutputStream out) throws IOException {
      out.writeShort(id);
      out.writeByte(count);
      out.writeShort(damage);
      out.writeShort(-1);
    }

  }

  public static class Enchantment {
    public short id;
    public short level;

    public Enchantment(int i, int j) {
      id = (short) i;
      level = (short) j;
    }

    public NBTCompound compound() {
      NBTCompound compound = new NBTCompound();
      compound.put(new NBTShort("id", id));
      compound.put(new NBTShort("lvl", level));
      return compound;
    }
  }
}
