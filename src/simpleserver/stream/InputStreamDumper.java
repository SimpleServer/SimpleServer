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
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class InputStreamDumper extends FilterInputStream implements DataInput,
    StreamDumper {
  private final DataInputStream in;
  private final BufferedWriter dump;

  private boolean inPacket = false;

  public InputStreamDumper(DataInputStream in, OutputStream dump) {
    super(in);

    this.in = in;
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
      in.close();
    }
    finally {
      dump.close();
    }
  }

  public void flush() throws IOException {
    dump.flush();
  }

  public void readFully(byte b[]) throws IOException {
    in.readFully(b, 0, b.length);
  }

  public void readFully(byte b[], int off, int len) throws IOException {
    in.readFully(b, off, len);

    for (int c = off; c < off + len; ++c) {
      dump.write(String.format("%02x ", b[c]));
    }

    if (len > 0) {
      dump.write("\n");
    }
  }

  public boolean readBoolean() throws IOException {
    boolean newBoolean = in.readBoolean();
    dump.write(Boolean.toString(newBoolean) + "\n");
    return newBoolean;
  }

  public byte readByte() throws IOException {
    byte newByte = in.readByte();
    if (!inPacket) {
      inPacket = true;
      dump.write(String.format("\nPacket ID: 0x%02x\n", newByte));
    }
    else {
      dump.write(newByte + "b\n");
    }
    return newByte;
  }

  public int readUnsignedByte() throws IOException {
    int newUnsignedByte = in.readUnsignedByte();
    dump.write(newUnsignedByte + "ub\n");
    return newUnsignedByte;
  }

  public short readShort() throws IOException {
    short newShort = in.readShort();
    dump.write(newShort + "s\n");
    return newShort;
  }

  public int readUnsignedShort() throws IOException {
    int newUnsignedShort = in.readUnsignedShort();
    dump.write(newUnsignedShort + "us\n");
    return newUnsignedShort;
  }

  public char readChar() throws IOException {
    char newChar = in.readChar();
    dump.write(newChar + "\n");
    return newChar;
  }

  public int readInt() throws IOException {
    int newInt = in.readInt();
    dump.write(newInt + "i\n");
    return newInt;
  }

  public long readLong() throws IOException {
    long newLong = in.readLong();
    dump.write(newLong + "l\n");
    return newLong;
  }

  public float readFloat() throws IOException {
    float newFloat = in.readFloat();
    dump.write(newFloat + "f\n");
    return newFloat;
  }

  public double readDouble() throws IOException {
    double newDouble = in.readDouble();
    dump.write(newDouble + "d\n");
    return newDouble;
  }

  public String readUTF() throws IOException {
    String newString = in.readUTF();
    dump.write(newString + "\n");
    return newString;
  }

  public int skipBytes(int n) throws IOException {
    return in.skipBytes(n);
  }

  @Deprecated
  public String readLine() throws IOException {
    return in.readLine();
  }
}
