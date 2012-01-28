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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NBTList<T extends NBTag> extends NBTag implements Iterable<T> {
  private ArrayList<T> value;
  private byte tagId;

  NBTList(DataInputStream in, Boolean named) throws Exception {
    super(in, named);
  }

  public NBTList(byte tagId) {
    this.tagId = tagId;
    value = new ArrayList<T>();
  }

  public NBTList(NBT tag) {
    this(tag.id());
  }

  public NBTList(String name, byte tagId) {
    super(name);
    this.tagId = tagId;
    value = new ArrayList<T>();
  }

  public NBTList(String name, NBT tag) {
    this(name, tag.id());
  }

  @Override
  protected byte id() {
    return 9;
  }

  @Override
  List<T> get() {
    return value;
  }

  @Override
  void set(String value) {
    throw new NumberFormatException("List tags can't be set directly");
  }

  public int size() {
    return value.size();
  }

  public void clear() {
    value.clear();
  }

  public NBT listType() {
    return NBT.values()[tagId];
  }

  @SuppressWarnings({ "unchecked", "hiding" })
  public <T extends NBTag> T cast() {
    return (T) this;
  }

  public boolean contains(T obj) {
    return value.contains(obj);
  }

  public Iterator<T> iterator() {
    return value.iterator();
  }

  public T get(int index) {
    return value.get(index);
  }

  public void add(T tag) {
    if (tagId == tag.id()) {
      value.add(tag);
    }
  }

  public boolean remove(T obj) {
    return value.remove(obj);
  }

  public NBTByte getByte(int index) {
    return (NBTByte) value.get(index);
  }

  public NBTShort getShort(int index) {
    return (NBTShort) value.get(index);
  }

  public NBTInt getInt(int index) {
    return (NBTInt) value.get(index);
  }

  public NBTLong getLong(int name) {
    return (NBTLong) value.get(name);
  }

  public NBTFloat getFloat(int index) {
    return (NBTFloat) value.get(index);
  }

  public NBTDouble getDouble(int index) {
    return (NBTDouble) value.get(index);
  }

  public NBTArray getArray(int index) {
    return (NBTArray) value.get(index);
  }

  public NBTString getString(int index) {
    return (NBTString) value.get(index);
  }

  @SuppressWarnings("unchecked")
  public NBTList<NBTag> getList(int index) {
    return (NBTList<NBTag>) value.get(index);
  }

  public NBTCompound getCompound(int index) {
    return (NBTCompound) value.get(index);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void loadValue(DataInputStream in) throws Exception {
    tagId = in.readByte();
    int length = in.readInt();
    value = new ArrayList<T>(length);
    for (int i = 0; i < length; i++) {
      value.add((T) NBT.loadTag(in, false, tagId));
    }
  }

  @Override
  protected void saveValue(DataOutputStream out) throws IOException {
    out.writeByte(tagId);
    out.writeInt(size());
    for (NBTag tag : value) {
      tag.save(out, false);
    }
  }

  @Override
  protected String valueToString(int level) {
    StringBuilder string = new StringBuilder();
    string.append("[\n");
    for (NBTag tag : value) {
      string.append(tag.toString(level + 1) + "\n");
    }
    string.append(indent(level));
    string.append("]");
    return string.toString();
  }
}
