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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import simpleserver.Server;
import simpleserver.Player.LocalAddressFactory;

public class Bot {
  private static final LocalAddressFactory addressFactory = new LocalAddressFactory();

  private static final int VERSION = 13;

  protected String name;
  protected Server server;
  private boolean connected;
  private boolean expectDisconnect;
  private boolean ready;

  private Socket socket;
  private DataInputStream in;
  protected DataOutputStream out;

  private Timer timer;
  ReentrantLock writeLock;

  public Bot(Server server, String name) {
    this.name = name;
    this.server = server;

  }

  public void connect() throws UnknownHostException, IOException {

    try {
      InetAddress localAddress = InetAddress.getByName(addressFactory.getNextAddress());
      socket = new Socket(InetAddress.getByName(null), server.options.getInt("internalPort"), localAddress, 0);
    } catch (Exception e) {
      socket = new Socket(InetAddress.getByName(null), server.options.getInt("internalPort"));
    }
    in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

    writeLock = new ReentrantLock();

    connected = true;
    new Tunneler().start();
    timer = new Timer();
    timer.schedule(new KeepAlive(), 0, 30 * 1000);

    handshake();
  }

  protected void prepare() {

  }

  private void handshake() throws IOException {
    out.writeByte(2);
    write(name);
    out.flush();
  }

  public void logout() throws IOException {
    connected = false;
    expectDisconnect = true;
    out.writeByte(0xff);
    write("quitting");
    out.flush();
  }

  protected void login() throws IOException {
    out.writeByte(1);
    out.writeInt(VERSION);
    write(name);
    out.writeLong(0);
    out.writeByte(0);
  }

  protected void ready() throws IOException {
    ready = true;
  }

  protected void handlePacket(byte packetId) throws IOException {
    /*if (packetId != 0x21 && packetId != 0x1f) {
      System.out.println(Integer.toHexString(packetId));
    }*/
    switch (packetId) {
      case 0x2:
        readUTF16();
        login();
        break;
      case 0x1:
        in.readInt();
        readUTF16();
        in.readLong();
        in.readByte();
        break;
      case 0x0d: // Player Position & Look
        out.writeByte(packetId);
        out.writeDouble(in.readDouble());
        double stance = in.readDouble();
        out.writeDouble(in.readDouble());
        out.writeDouble(stance);
        out.writeDouble(in.readDouble());
        out.writeFloat(in.readFloat());
        out.writeFloat(in.readFloat());
        out.writeBoolean(in.readBoolean());
        if (!ready) {
          ready();
        }
        break;
      case (byte) 0xff: // Disconnect/Kick
        String reason = readUTF16();
        error(reason);
        break;

      case 0x00: // Keep Alive
        break;
      case 0x03: // Chat Message
        readUTF16();
        break;
      case 0x04: // Time Update
        in.readLong();
        break;
      case 0x05: // Player Inventory
        in.readInt();
        in.readShort();
        in.readShort();
        in.readShort();
        break;
      case 0x06: // Spawn Position
        readNBytes(12);
        break;
      case 0x07: // Use Entity?
        in.readInt();
        in.readInt();
        in.readBoolean();
        in.readBoolean();
        break;
      case 0x08: // Update Health
        readNBytes(2);
        break;
      case 0x09: // Respawn
        in.readByte();
        break;
      case 0x0a: // Player
        in.readByte();
        break;
      case 0x0b: // Player Position
        readNBytes(33);
        break;
      case 0x0c: // Player Look
        readNBytes(9);
        break;
      case 0x0e: // Player Digging
        in.readByte();
        in.readInt();
        in.readByte();
        in.readInt();
        in.readByte();
        break;
      case 0x0f: // Player Block Placement
        in.readInt();
        in.readByte();
        in.readInt();
        in.readByte();
        final short dropItem = in.readShort();
        if (dropItem != -1) {
          in.readByte();
          in.readShort();
        }
        break;
      case 0x10: // Holding Change
        readNBytes(2);
        break;
      case 0x11: // Use Bed
        readNBytes(14);
        break;
      case 0x12: // Animation
        readNBytes(5);
        break;
      case 0x13: // ???
        in.readInt();
        in.readByte();
        break;
      case 0x14: // Named Entity Spawn
        in.readInt();
        readUTF16();
        readNBytes(16);
        break;
      case 0x15: // Pickup spawn
        readNBytes(24);
        break;
      case 0x16: // Collect Item
        readNBytes(8);
        break;
      case 0x17: // Add Object/Vehicle
        in.readInt();
        in.readByte();
        in.readInt();
        in.readInt();
        in.readInt();
        int flag = in.readInt();
        if (flag > 0) {
          in.readShort();
          in.readShort();
          in.readShort();
        }
        break;
      case 0x18: // Mob Spawn
        in.readInt();
        in.readByte();
        in.readInt();
        in.readInt();
        in.readInt();
        in.readByte();
        in.readByte();
        readUnknownBlob();
        break;
      case 0x19: // Painting
        in.readInt();
        readUTF16();
        in.readInt();
        in.readInt();
        in.readInt();
        in.readInt();
        break;
      case 0x1b: // ???
        readNBytes(18);
        break;
      case 0x1c: // Entity Velocity?
        readNBytes(10);
        break;
      case 0x1d: // Destroy Entity
        readNBytes(4);
        break;
      case 0x1e: // Entity
        readNBytes(4);
        break;
      case 0x1f: // Entity Relative Move
        readNBytes(7);
        break;
      case 0x20: // Entity Look
        readNBytes(6);
        break;
      case 0x21: // Entity Look and Relative Move
        readNBytes(9);
        break;
      case 0x22: // Entity Teleport
        readNBytes(18);
        break;
      case 0x26: // Entity status?
        readNBytes(5);
        break;
      case 0x27: // Attach Entity?
        readNBytes(8);
        break;
      case 0x28: // Entity Metadata
        in.readInt();
        readUnknownBlob();
        break;
      case 0x32: // Pre-Chunk
        readNBytes(9);
        break;
      case 0x33: // Map Chunk
        readNBytes(13);
        int chunkSize = in.readInt();
        readNBytes(chunkSize);
        break;
      case 0x34: // Multi Block Change
        readNBytes(8);
        short arraySize = in.readShort();
        readNBytes(arraySize * 4);
        break;
      case 0x35: // Block Change
        in.readInt();
        in.readByte();
        in.readInt();
        in.readByte();
        in.readByte();
        break;
      case 0x36: // ???
        readNBytes(12);
        break;
      case 0x3c: // Explosion
        readNBytes(28);
        int recordCount = in.readInt();
        readNBytes(recordCount * 3);
        break;
      case 0x3d: // Unknown
        in.readInt();
        in.readInt();
        in.readByte();
        in.readInt();
        in.readInt();
        break;
      case 0x46: // Invalid Bed
        readNBytes(1);
        break;
      case 0x47: // Thunder
        readNBytes(17);
        break;
      case 0x64: // Open window
        in.readByte();
        in.readByte();
        in.readUTF();
        in.readByte();
        break;
      case 0x65:
        in.readByte();
        break;
      case 0x66: // Inventory Item Move
        in.readByte();
        in.readShort();
        in.readByte();
        in.readShort();
        in.readBoolean();
        short moveItem = in.readShort();
        if (moveItem != -1) {
          in.readByte();
          in.readShort();
        }
        break;
      case 0x67: // Inventory Item Update
        in.readByte();
        in.readShort();
        short setItem = in.readShort();
        if (setItem != -1) {
          in.readByte();
          in.readShort();
        }
        break;
      case 0x68: // Inventory
        in.readByte();
        short count = in.readShort();
        for (int c = 0; c < count; ++c) {
          short item = in.readShort();
          if (item != -1) {
            in.readByte();
            in.readShort();
          }
        }
        break;
      case 0x69:
        in.readByte();
        in.readShort();
        in.readShort();
        break;
      case 0x6a:
        in.readByte();
        in.readShort();
        in.readByte();
        break;
      case (byte) 0x82: // Update Sign
        in.readInt();
        in.readShort();
        in.readInt();
        readUTF16();
        readUTF16();
        readUTF16();
        readUTF16();
        break;
      case (byte) 0x83: // Map data
        in.readShort();
        in.readShort();
        byte length = in.readByte();
        readNBytes(0xff & length);
        break;
      case (byte) 0xc8: // Statistic
        readNBytes(5);
        break;
      case (byte) 0xe6: // ModLoaderMP by SDK
        in.readInt(); // mod
        in.readInt(); // packet id
        readNBytes(in.readInt() * 4); // ints
        readNBytes(in.readInt() * 4); // floats
        int sizeString = in.readInt(); // strings
        for (int i = 0; i < sizeString; i++) {
          readNBytes(in.readInt());
        }
        break;
      default:
        error("Unable to handle packet 0x" + Integer.toHexString(packetId));
    }
  }

  private void readUnknownBlob() throws IOException {
    byte unknown = in.readByte();

    while (unknown != 0x7f) {
      int type = (unknown & 0xE0) >> 5;

      switch (type) {
        case 0:
          in.readByte();
          break;
        case 1:
          in.readShort();
          break;
        case 2:
          in.readInt();
          break;
        case 3:
          in.readFloat();
          break;
        case 4:
          readUTF16();
          break;
        case 5:
          in.readShort();
          in.readByte();
          in.readShort();
          break;
        case 6:
          in.readInt();
          in.readInt();
          in.readInt();
      }
      unknown = in.readByte();
    }
  }

  protected String write(String s) throws IOException {
    byte[] bytes = s.getBytes("UTF-16");
    if (s.length() == 0) {
      out.write((byte) 0x00);
      out.write((byte) 0x00);
      return s;
    }
    bytes[0] = (byte) ((s.length() >> 8) & 0xFF);
    bytes[1] = (byte) ((s.length() & 0xFF));
    for (byte b : bytes) {
      out.write(b);
    }
    return s;
  }

  protected String readUTF16() throws IOException {
    short length = in.readShort();
    byte[] bytes = new byte[length * 2 + 2];
    for (short i = 0; i < length * 2; i++) {
      bytes[i + 2] = in.readByte();
    }
    bytes[0] = (byte) 0xfffffffe;
    bytes[1] = (byte) 0xffffffff;
    return new String(bytes, "UTF-16");
  }

  private void readNBytes(int bytes) throws IOException {
    for (int c = 0; c < bytes; ++c) {
      in.readByte();
    }
  }

  protected void die() {
    timer.cancel();
    connected = false;
    try {
      socket.close();
    } catch (IOException e) {
    }
  }

  protected void error(String reason) {
    die();
    if (!expectDisconnect) {
      System.out.print("[SimpleServer] Bot " + name + " died (" + reason + ")");
    }
  }

  private final class Tunneler extends Thread {
    @Override
    public void run() {
      while (connected) {
        writeLock.lock();
        try {
          handlePacket(in.readByte());
          out.flush();
        } catch (IOException e) {
          error("Soket closed");
        } finally {
          writeLock.unlock();
        }
      }
    }
  }

  private final class KeepAlive extends TimerTask {
    @Override
    public void run() {
      if (connected) {
        writeLock.lock();
        try {
          out.writeByte(0x0);
        } catch (IOException e) {
          error("KeepAlive failed");
        } finally {
          writeLock.unlock();
        }
      }
    }
  }
}
