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

  NBTag(DataInputStream in, boolean named) throws Exception {
    if (named) {
      name = new NBTString(in, false);
    }
    this.named = named;
    loadValue(in);
  }

  NBTag() {
  }

  protected abstract byte id();

  abstract Object get();

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
    if (named) {
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

  protected static NBTag loadTag(DataInputStream in, boolean named) throws Exception {
    return loadTag(in, named, in.readByte());
  }

  protected static NBTag loadTag(DataInputStream in, boolean named, byte type) throws Exception {
    switch (type) {
      case 0:
        return new NBTEnd(in);
      case 1:
        return new NBTByte(in, named);
      case 2:
        return new NBTShort(in, named);
      case 3:
        return new NBTInt(in, named);
      case 4:
        return new NBTLong(in, named);
      case 5:
        return new NBTFloat(in, named);
      case 6:
        return new NBTDouble(in, named);
      case 7:
        return new NBTArray(in, named);
      case 8:
        return new NBTString(in, named);
      case 9:
        return new NBTList(in, named);
      case 10:
        return new NBTCompound(in, named);
      default:
        throw new Exception("Unknown NBT type");
    }
  }
}
