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
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NBT {

  private DataInputStream in;
  private DataOutputStream out;
  private NBTag root;

  public NBT(String filename) {
    load(filename);
  }

  private void load(String filename) {
    try {
      in = new DataInputStream(new GZIPInputStream(new FileInputStream(filename)));
      root = loadTag(false);
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

  public void save(String filename) {
    try {
      out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));
      root.save();
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public String toString() {
    return root.toString();
  }

  public static void main(String[] args) {
    NBT nbt = new NBT("world/players/sadimusi.dat");
    nbt.save("test.nbt");
    NBT check = new NBT("test.nbt");

    if (nbt.toString().equals(check.toString())) {
      System.out.println("Success!\n\n" + nbt.toString());
    } else {
      System.out.println("FAIL!\n\n" + nbt.toString() + "\ndoes not equal\n\n" + check.toString());
    }
  }

  private abstract class NBTag {
    protected String name;
    private boolean named;

    protected abstract byte id();

    private NBTag(boolean named) throws Exception {
      if (named) {
        name = new NBTString(false).value;
      } else {
        name = "";
      }
      this.named = named;
      loadValue();
    }

    public void save() throws IOException {
      save(true);
    }

    public void saveValue() throws IOException {
      if (named) {
        out.writeShort(name.length());
        out.writeBytes(name);
      }
    }

    protected void loadValue() throws Exception {
    };

    protected abstract Object get();

    @Override
    public String toString() {
      return name + " : " + get() + " (" + getClass().getSimpleName() + ")";
    }

    public void save(boolean tagId) throws IOException {
      if (tagId) {
        out.writeByte(id());
      }
      saveValue();
    }
  }

  private class NBTEnd extends NBTag {
    @Override
    protected byte id() {
      return 0;
    }

    public NBTEnd(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected Object get() {
      return "END";
    }

    @Override
    public void saveValue() throws IOException {
    }
  }

  private class NBTByte extends NBTag {
    @Override
    protected byte id() {
      return 1;
    }

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

    @Override
    public void saveValue() throws IOException {
      super.saveValue();
      out.writeByte(value);
    }
  }

  private class NBTShort extends NBTag {
    @Override
    protected byte id() {
      return 2;
    }

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

    @Override
    public void saveValue() throws IOException {
      super.saveValue();
      out.writeShort(value);
    }
  }

  private class NBTInt extends NBTag {
    @Override
    protected byte id() {
      return 3;
    }

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

    @Override
    public void saveValue() throws IOException {
      super.saveValue();
      out.writeInt(value);
    }
  }

  private class NBTLong extends NBTag {
    @Override
    protected byte id() {
      return 4;
    }

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

    @Override
    public void saveValue() throws IOException {
      super.saveValue();
      out.writeLong(value);
    }
  }

  private class NBTFloat extends NBTag {
    @Override
    protected byte id() {
      return 5;
    }

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

    @Override
    public void saveValue() throws IOException {
      super.saveValue();
      out.writeFloat(value);
    }
  }

  private class NBTDouble extends NBTag {
    @Override
    protected byte id() {
      return 6;
    }

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

    @Override
    public void saveValue() throws IOException {
      super.saveValue();
      out.writeDouble(value);
    }
  }

  private class NBTArray extends NBTag {
    @Override
    protected byte id() {
      return 7;
    }

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

    @Override
    public void saveValue() throws IOException {
      super.saveValue();
      out.writeInt(length);
      out.write(value);
    }
  }

  private class NBTString extends NBTag {
    @Override
    protected byte id() {
      return 8;
    }

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

    @Override
    public void saveValue() throws IOException {
      super.saveValue();
      out.writeShort(length);
      out.writeBytes(value);
    }
  }

  private class NBTList extends NBTag {
    @Override
    protected byte id() {
      return 9;
    }

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

    @Override
    public void saveValue() throws IOException {
      super.saveValue();
      out.writeByte(tagId);
      out.writeInt(length);
      for (NBTag tag : value) {
        tag.save(false);
      }
    }
  }

  private class NBTCompound extends NBTag {
    @Override
    protected byte id() {
      return 10;
    }

    List<NBTag> value;

    public NBTCompound(boolean named) throws Exception {
      super(named);
    }

    @Override
    protected void loadValue() throws Exception {
      value = new LinkedList<NBTag>();
      while (true) {
        NBTag tag = loadTag(true);
        value.add(tag);
        if (tag instanceof NBTEnd) {
          break;
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

    @Override
    public void saveValue() throws IOException {
      super.saveValue();
      out.writeShort(0);
      for (NBTag tag : value) {
        tag.save();
      }
    }
  }
}
