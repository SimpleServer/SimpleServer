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
import simpleserver.nbt.NBTArray;
import simpleserver.nbt.NBTCompound;

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
    return new Position(node.getCompound(name));
  }

  public void set(String name, Position pos) {
    NBTCompound tag = pos.tag();
    tag.rename(name.toLowerCase());
    tag.put(new NBTArray(CAPS, capitals(name)));
    node.put(tag);
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
