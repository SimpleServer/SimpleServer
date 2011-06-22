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

public class NBTList extends NBTag {
  private NBTag[] value;
  private byte tagId;
  private int length;

  NBTList(DataInputStream in, boolean named) throws Exception {
    super(in, named);
  }

  @Override
  protected byte id() {
    return 9;
  }

  @Override
  NBTag[] get() {
    return value;
  }

  NBTag get(int index) {
    return value[index];
  }

  @Override
  protected void loadValue(DataInputStream in) throws Exception {
    tagId = in.readByte();
    length = in.readInt();
    value = new NBTag[length];
    for (int i = 0; i < length; i++) {
      value[i] = loadTag(in, false, tagId);
    }
  }

  @Override
  protected void saveValue(DataOutputStream out) throws IOException {
    out.writeByte(tagId);
    out.writeInt(length);
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
