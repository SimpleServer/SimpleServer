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
package simpleserver.bot;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class NBT {

  private DataInputStream in;

  public NBT(String filename) {
    load(filename);
  }

  private void load(String filename) {
    try {
      in = new DataInputStream(new GZIPInputStream(new FileInputStream(filename)));
      NBTag root = loadTag(false);
      System.out.println(root);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private NBTag loadTag(boolean named) throws Exception {
    return loadTag(named, in.readByte());
  }

  private NBTag loadTag(boolean named, byte type) throws Exception {
    switch (type) {
      case 0:
        return new NBTEnd(false);
      case 1:
        return new NBTByte(named);
      case 2:
        return new NBTShort(named);
      case 3:
        return new NBTInt(named);
      case 4:
        return new NBTLong(named);
      case 5:
        return new NBTFloat(named);
      case 6:
        return new NBTDouble(named);
      case 7:
        return new NBTArray(named);
      case 8:
        return new NBTString(named);
      case 9:
        return new NBTList(named);
      case 10:
        in.readShort();
        return new NBTCompound(named);
      default:
        throw new Exception("Unknown NBT type");
    }
  }

  public static void main(String[] args) {
    new NBT("world/players/sadimusi.dat");
  }

  private abstract class NBTag {
    protected String name;

    private NBTag(boolean named) throws Exception {
      if (named) {
        name = new NBTString(false).value;
      } else {
        name = "";
      }
      loadValue();
    }

    protected void loadValue() throws Exception {
    };

    protected abstract Object get();

    @Override
    public String toString() {
      return name + " : " + get() + " (" + getClass().getSimpleName() + ")";
    }
  }

  private class NBTEnd extends NBTag {
    public NBTEnd(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected Object get() {
      return "END";
    }
  }

  private class NBTByte extends NBTag {
    protected Byte value;

    public NBTByte(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected void loadValue() throws IOException {
      value = in.readByte();
    }

    @Override
    protected Object get() {
      return value;
    }
  }

  private class NBTShort extends NBTag {
    protected Short value;

    public NBTShort(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected void loadValue() throws IOException {
      value = in.readShort();
    }

    @Override
    protected Object get() {
      return value;
    }
  }

  private class NBTInt extends NBTag {
    protected Integer value;

    public NBTInt(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected void loadValue() throws IOException {
      value = in.readInt();
    }

    @Override
    protected Object get() {
      return value;
    }
  }

  private class NBTLong extends NBTag {
    protected Long value;

    public NBTLong(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected void loadValue() throws IOException {
      value = in.readLong();
    }

    @Override
    protected Object get() {
      return value;
    }
  }

  private class NBTFloat extends NBTag {
    protected Float value;

    public NBTFloat(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected void loadValue() throws IOException {
      value = in.readFloat();
    }

    @Override
    protected Object get() {
      return value;
    }
  }

  private class NBTDouble extends NBTag {
    protected Double value;

    public NBTDouble(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected void loadValue() throws IOException {
      value = in.readDouble();
    }

    @Override
    protected Object get() {
      return value;
    }
  }

  private class NBTArray extends NBTag {
    protected byte[] value;
    int length;

    public NBTArray(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected void loadValue() throws IOException {
      length = in.readInt();
      value = new byte[length];
      for (int i = 0; i < length; i++) {
        value[i] = in.readByte();
      }
    }

    @Override
    protected Object get() {
      return value;
    }
  }

  private class NBTString extends NBTag {
    protected String value;
    short length;

    public NBTString(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected void loadValue() throws IOException {
      length = in.readShort();
      byte[] bytes = new byte[length];
      for (int i = 0; i < length; i++) {
        bytes[i] = in.readByte();
      }
      value = new String(bytes);
    }

    @Override
    protected Object get() {
      return value;
    }
  }

  private class NBTList extends NBTag {
    byte tagId;
    int length;
    NBTag value[];

    public NBTList(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected void loadValue() throws Exception {
      tagId = in.readByte();
      length = in.readInt();
      value = new NBTag[length];
      for (int i = 0; i < length; i++) {
        value[i] = loadTag(false, tagId);
      }
    }

    @Override
    protected Object get() {
      return value;
    }

    @Override
    public String toString() {
      StringBuilder string = new StringBuilder();
      string.append(name + "(" + length + ") : [\n");
      for (NBTag tag : value) {
        string.append(tag.toString() + "\n");
      }
      string.append("]");
      return string.toString();
    }

  }

  private class NBTCompound extends NBTag {
    List<NBTag> value;

    public NBTCompound(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected void loadValue() throws Exception {
      value = new LinkedList<NBTag>();
      while (true) {
        NBTag tag = loadTag(true);
        if (tag instanceof NBTEnd) {
          break;
        } else {
          value.add(tag);
        }
      }
    }

    @Override
    public String toString() {
      StringBuilder string = new StringBuilder();
      string.append(name + "{\n");
      for (NBTag tag : value) {
        string.append(tag.toString() + "\n");
      }
      string.append("}");
      return string.toString();
    }

    @Override
    protected Object get() {
      return value;
    }
  }
}
