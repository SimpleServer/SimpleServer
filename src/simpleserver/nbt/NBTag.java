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

public abstract class NBTag {
  protected NBTString name;
  protected boolean named;

  protected NBTag(DataInputStream in, boolean named) throws Exception {
    if (named) {
      name = new NBTString(in, false);
    }
    this.named = named;
    loadValue(in);
  }

  protected NBTag() {
    named = false;
  }

  protected NBTag(String name) {
    this.name = new NBTString(name);
    named = true;
  }

  protected abstract byte id();

  public NBT type() {
    return NBT.values()[id()];
  }

  abstract Object get();

  abstract void set(String value);

  public NBTString name() {
    if (named && name.get().length() > 0) {
      return name;
    }
    return null;
  }

  public void rename(String name) {
    if (!named) {
      named = true;
      this.name = new NBTString(name);
    } else {
      this.name.set(name);
    }
  }

  void save(DataOutputStream out) throws IOException {
    save(out, true);
  }

  protected void save(DataOutputStream out, boolean tagId) throws IOException {
    if (tagId) {
      out.writeByte(id());
    }
    if (named) {
      name.save(out, false);
    }
    saveValue(out);
  }

  protected void saveValue(DataOutputStream out) throws IOException {
  }

  protected void loadValue(DataInputStream in) throws Exception {
  }

  protected String toString(int level) {
    StringBuilder builder = indent(level);
    if (named && name.get().length() > 0) {
      builder.append(name.get());
      builder.append(": ");
    }
    builder.append(valueToString(level));
    builder.append(" (");
    builder.append(getClass().getSimpleName());
    builder.append(")");
    return builder.toString();
  }

  @Override
  public String toString() {
    return toString(0);
  }

  protected String valueToString(int level) {
    return get().toString();
  }

  protected static StringBuilder indent(int level) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < level; i++) {
      builder.append("  ");
    }
    return builder;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NBTag)) {
      return false;
    }
    NBTag tag = (NBTag) o;
    return named == tag.named && tag.get().equals(get());
  }

  static NBTCompound load(DataInputStream in) throws Exception {
    NBTag root = NBT.loadTag(in, true);
    if (!(root instanceof NBTCompound)) {
      throw new Exception("NBT stream has the wrong format");
    }
    return (NBTCompound) root;
  }
}
