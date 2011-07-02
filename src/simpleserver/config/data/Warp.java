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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import simpleserver.Position;
import simpleserver.Coordinate.Dimension;
import simpleserver.nbt.NBTArray;
import simpleserver.nbt.NBTByte;
import simpleserver.nbt.NBTCompound;
import simpleserver.nbt.NBTDouble;
import simpleserver.nbt.NBTFloat;

public class Warp {
  private NBTCompound node;
  private final String WARP = "warp";
  private final String CAPS = "capitals";

  public Set<String> keys() {
    return node.names();
  }

  public List<String> names() {
    Set<String> keys = keys();
    List<String> names = new ArrayList<String>(keys.size());
    for (String key : keys) {
      names.add(capitalize(key));
    }
    return names;
  }

  public boolean contains(String name) {
    return node.containsKey(name.toLowerCase());
  }

  public String getName(String prefix) {
    prefix = prefix.toLowerCase();
    if (contains(prefix)) {
      return capitalize(prefix);
    }
    for (String name : keys()) {
      if (name.startsWith(prefix)) {
        return capitalize(name);
      }
    }
    return null;
  }

  private String capitalize(String name) {
    return capitalize(name, node.getCompound(name).getArray(CAPS).get());
  }

  public Position get(String name) {
    name = name.toLowerCase();
    if (!node.containsKey(name)) {
      return null;
    }
    NBTCompound p = node.getCompound(name);
    double x = p.getDouble("x").get();
    double y = p.getDouble("y").get();
    double z = p.getDouble("z").get();
    Dimension dim = Dimension.get(p.getByte("Dimension").get());
    float yaw = p.getFloat("yaw").get();
    float pitch = p.getFloat("pitch").get();
    return new Position(x, y, z, dim, yaw, pitch);
  }

  public void set(String name, Position pos) {
    NBTCompound p = new NBTCompound(name.toLowerCase());
    p.put(new NBTDouble("x", pos.x));
    p.put(new NBTDouble("y", pos.y));
    p.put(new NBTDouble("z", pos.z));
    p.put(new NBTByte("Dimension", pos.dimension.index()));
    p.put(new NBTFloat("yaw", pos.yaw));
    p.put(new NBTFloat("pitch", pos.pitch));
    p.put(new NBTArray(CAPS, capitals(name)));
    node.put(p);
  }

  public void remove(String name) {
    name = name.toLowerCase();
    node.remove(name);
  }

  void load(NBTCompound data) {
    if (data.containsKey(WARP)) {
      try {
        node = data.getCompound(WARP);
        capitalizeWaypoints();
        return;
      } catch (Exception e) {
        System.out.println("[WARNING] Warp list is corrupt. Replacing it with empty list...");
      }
    }
    node = new NBTCompound(WARP);
    data.put(node);
  }

  private void capitalizeWaypoints() {
    List<String> keys = new ArrayList<String>(keys());
    for (String name : keys) {
      if (!node.getCompound(name).containsKey(CAPS)) {
        node.getCompound(name).put(new NBTArray(CAPS, capitals(name)));
        node.rename(name, name.toLowerCase());
      }
    }
  }

  public static byte[] capitals(String string) {
    int length = string.length() / 8;
    if (string.length() % 8 != 0) {
      length++;
    }
    byte[] caps = new byte[length];
    for (int i = 0; i < length; i++) {
      byte bools = 0;
      for (int j = 0; j < 8; j++) {
        int pos = i * 8 + j;
        if (pos >= string.length()) {
          break;
        }
        if (string.charAt(pos) >= 0x41 && string.charAt(pos) <= 0x5a) {
          bools = (byte) (bools | (0x1 << j));
        }
      }
      caps[i] = bools;
    }
    return caps;
  }

  public static String capitalize(String string, byte[] capitals) {
    StringBuilder builder = new StringBuilder(string);
    for (int i = 0; i < string.length(); i++) {
      if ((capitals[i / 8] & (1 << i % 8)) != 0) {
        builder.setCharAt(i, (char) (string.charAt(i) - 32));
      }
    }
    return builder.toString();
  }
}
