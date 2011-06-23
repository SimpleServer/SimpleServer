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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class NBTCompound extends NBTag {
  private HashMap<String, NBTag> value;

  NBTCompound(DataInputStream in, Boolean named) throws Exception {
    super(in, named);
  }

  public NBTCompound(String name) {
    super(name);
    value = new HashMap<String, NBTag>();
  }

  public NBTCompound() {
    value = new HashMap<String, NBTag>();
  }

  @Override
  protected byte id() {
    return 10;
  }

  @Override
  HashMap<String, NBTag> get() {
    return value;
  }

  public boolean containsKey(String name) {
    return value.containsKey(name);
  }

  public NBTag get(String name) {
    return value.get(name);
  }

  public void put(NBTag tag) {
    value.put(tag.name.get(), tag);
  }

  public void remove(String name) {
    value.remove(name);
  }

  public NBTByte getByte(String name) {
    return (NBTByte) value.get(name);
  }

  public NBTShort getShort(String name) {
    return (NBTShort) value.get(name);
  }

  public NBTInt getInt(String name) {
    return (NBTInt) value.get(name);
  }

  public NBTLong getLong(String name) {
    return (NBTLong) value.get(name);
  }

  public NBTFloat getFloat(String name) {
    return (NBTFloat) value.get(name);
  }

  public NBTDouble getDouble(String name) {
    return (NBTDouble) value.get(name);
  }

  public NBTArray getArray(String name) {
    return (NBTArray) value.get(name);
  }

  public NBTString getString(String name) {
    return (NBTString) value.get(name);
  }

  @SuppressWarnings("unchecked")
  public NBTList<NBTag> getList(String name) {
    return (NBTList<NBTag>) value.get(name);
  }

  public NBTCompound getCompound(String name) {
    return (NBTCompound) value.get(name);
  }

  @Override
  protected void loadValue(DataInputStream in) throws Exception {
    value = new HashMap<String, NBTag>();
    while (true) {
      NBTag tag = NBT.loadTag(in, true);
      if (tag instanceof NBTEnd) {
        break;
      }
      value.put(tag.name.get(), tag);
    }
  }

  @Override
  protected void saveValue(DataOutputStream out) throws IOException {
    for (NBTag tag : value.values()) {
      tag.save(out);
    }
    new NBTEnd().save(out);
  }

  @Override
  protected String valueToString(int level) {
    StringBuilder string = new StringBuilder();
    string.append("{\n");
    for (NBTag tag : value.values()) {
      string.append(tag.toString(level + 1) + "\n");
    }
    string.append(indent(level));
    string.append("}");
    return string.toString();
  }
}
