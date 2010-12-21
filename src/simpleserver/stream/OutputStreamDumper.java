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
package simpleserver.stream;

import java.io.BufferedWriter;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class OutputStreamDumper extends FilterOutputStream implements
    DataOutput, StreamDumper {
  private final DataOutputStream out;
  private final BufferedWriter dump;

  private boolean inPacket = false;

  public OutputStreamDumper(DataOutputStream out, OutputStream dump) {
    super(out);

    this.out = out;
    this.dump = new BufferedWriter(new OutputStreamWriter(dump));
  }

  public void cleanup() {
    try {
      dump.close();
    }
    catch (IOException e) {
    }
  }

  public void packetFinished() throws IOException {
    inPacket = false;
    dump.flush();
  }

  @Override
  public void close() throws IOException {
    try {
      out.close();
    }
    finally {
      dump.close();
    }
  }

  @Override
  public void flush() throws IOException {
    out.flush();
    dump.flush();
  }

  @Override
  public void write(int b) throws IOException {
    writeByte(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    out.write(b, off, len);

    for (int c = off; c < off + len; ++c) {
      dump.write(String.format("%02x ", b[c]));
    }

    if (len > 0) {
      dump.write("\n");
    }
  }

  public void writeBoolean(boolean v) throws IOException {
    out.writeBoolean(v);
    dump.write(Boolean.toString(v) + "\n");
  }

  public void writeByte(int v) throws IOException {
    out.writeByte(v);
    if (!inPacket) {
      inPacket = true;
      dump.write(String.format("\nPacket ID: 0x%02x\n", (byte) v));
    }
    else {
      dump.write(v + "b\n");
    }
  }

  public void writeShort(int v) throws IOException {
    out.writeShort(v);
    dump.write((short) v + "s\n");
  }

  public void writeChar(int v) throws IOException {
    out.writeChar(v);
    dump.write((char) v + "\n");
  }

  public void writeInt(int v) throws IOException {
    out.writeInt(v);
    dump.write(v + "i\n");
  }

  public void writeLong(long v) throws IOException {
    out.writeLong(v);
    dump.write(v + "l\n");
  }

  public void writeFloat(float v) throws IOException {
    out.writeFloat(v);
    dump.write(v + "f\n");
  }

  public void writeDouble(double v) throws IOException {
    out.writeDouble(v);
    dump.write(v + "d\n");
  }

  public void writeBytes(String s) throws IOException {
    out.writeBytes(s);
    dump.write(s + "(byte string?)\n");
  }

  public void writeChars(String s) throws IOException {
    out.writeChars(s);
    dump.write(s + "(chars?)\n");
  }

  public void writeUTF(String s) throws IOException {
    out.writeUTF(s);
    dump.write(s + "\n");
  }
}
