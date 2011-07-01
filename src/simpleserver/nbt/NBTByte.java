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

public class NBTByte extends NBTag {
  private Byte value;

  NBTByte(DataInputStream in, Boolean named) throws Exception {
    super(in, named);
  }

  public NBTByte(byte value) {
    set(value);
  }

  public NBTByte(String name, byte value) {
    super(name);
    set(value);
  }

  @Override
  protected byte id() {
    return 1;
  }

  @Override
  public Byte get() {
    return value;
  }

  public void set(byte value) {
    this.value = value;
  }

  @Override
  protected void loadValue(DataInputStream in) throws IOException {
    value = in.readByte();
  }

  @Override
  protected void saveValue(DataOutputStream out) throws IOException {
    out.writeByte(value);
  }

  @Override
  protected String valueToString(int level) {
    return String.format("0x%02x", get());
  }
}
