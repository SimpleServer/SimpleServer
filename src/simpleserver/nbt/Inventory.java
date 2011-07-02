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

import java.util.HashMap;

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

  public byte add(int id, int count, int damage) {
    return add((short) id, (byte) count, (short) damage);
  }

  public void set(byte slot, short id, byte count, short damage) {
    slots.put(slot, new Slot(id, count, damage));
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

  public class Slot {
    public short id;
    public byte count;
    public short damage;

    public Slot(short id, byte count, short damage) {
      this.id = id;
      this.count = count;
      this.damage = damage;
    }

    public NBTCompound compound() {
      NBTCompound compound = new NBTCompound();
      compound.put(new NBTShort("id", id));
      compound.put(new NBTByte("Count", count));
      compound.put(new NBTShort("Damage", damage));
      return compound;
    }
  }
}
