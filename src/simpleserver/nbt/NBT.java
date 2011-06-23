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

public enum NBT {
  END(NBTEnd.class),
  BYTE(NBTByte.class),
  SHORT(NBTShort.class),
  INT(NBTInt.class),
  LONG(NBTLong.class),
  FLOAT(NBTFloat.class),
  DOUBLE(NBTDouble.class),
  ARRAY(NBTArray.class),
  STRING(NBTString.class),
  LIST(NBTList.class),
  COMPOUND(NBTCompound.class);

  private Class<? extends NBTag> c;

  NBT(Class<? extends NBTag> c) {
    this.c = c;
  }

  public byte id() {
    return (byte) ordinal();
  }

  protected static NBTag loadTag(DataInputStream in, boolean named) throws Exception {
    return loadTag(in, named, in.readByte());
  }

  protected static NBTag loadTag(DataInputStream in, boolean named, byte type) throws Exception {
    if (type < 0 || type > 10) {
      throw new Exception("Unknown NBT type");
    }
    try {
      return values()[type].c.getDeclaredConstructor(DataInputStream.class, Boolean.class).newInstance(in, named);
    } catch (Exception e) {
      throw new Exception("Something went horribly wrong: " + e.getMessage());
    }
  }

}
