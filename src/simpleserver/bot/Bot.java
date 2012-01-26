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

import static simpleserver.stream.StreamTunnel.ENCHANTABLE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.locks.ReentrantLock;

import simpleserver.Coordinate.Dimension;
import simpleserver.Position;
import simpleserver.Server;

public class Bot {
  private static final int VERSION = 24;

  protected String name;
  protected Server server;
  private boolean connected;
  private boolean expectDisconnect;
  protected boolean ready;
  protected boolean dead;

  private Socket socket;
  protected DataInputStream in;
  protected DataOutputStream out;

  ReentrantLock writeLock;
  protected Position position;
  protected BotController controller;
  protected boolean gotFirstPacket = false;
  private byte lastPacket;
  private short health;

  public Bot(Server server, String name) {
    this.name = name;
    this.server = server;
    position = new Position();
  }

  void connect() throws UnknownHostException, IOException {

    try {
      InetAddress localAddress = InetAddress.getByName(Server.addressFactory.getNextAddress());
      socket = new Socket(InetAddress.getByName(null), server.options.getInt("internalPort"), localAddress, 0);
    } catch (Exception e) {
      socket = new Socket(InetAddress.getByName(null), server.options.getInt("internalPort"));
    }
    in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

    writeLock = new ReentrantLock();

    connected = true;
    new Tunneler().start();

    handshake();
  }

  boolean ninja() {
    return false;
  }

  protected void positionUpdate() throws IOException {
  }

  private void keepAlive(int keepAliveId) throws IOException {
    writeLock.lock();
    out.writeByte(0x0);
    out.writeInt(keepAliveId);
    writeLock.unlock();
  }

  private void handshake() throws IOException {
    writeLock.lock();
    out.writeByte(2);
    write(name);
    out.flush();
    writeLock.unlock();
  }

  public void logout() throws IOException {
    die();
    expectDisconnect = true;
    out.writeByte(0xff);
    write("quitting");
    out.flush();
  }

  protected void login() throws IOException {
    writeLock.lock();
    out.writeByte(1);
    out.writeInt(VERSION);
    write(name);
    out.writeLong(0);
    write("DEFAULT");
    out.writeInt(0);
    out.writeByte(0);
    out.writeByte(0);
    out.writeByte(0);
    out.writeByte(0);
    writeLock.unlock();
  }

  private void respawn() throws IOException {
    writeLock.lock();
    out.writeByte(9);
    out.writeByte(position.dimension.index());
    out.writeByte(0);
    out.writeShort(128);
    out.writeLong(0);
    write("DEFAULT");
    writeLock.unlock();
  }

  protected void ready() throws IOException {
    ready = true;
  }

  protected void walk(double d) {
    double heading = position.yaw * Math.PI / 180;
    position.x -= Math.sin(heading) * d;
    position.z += Math.cos(heading) * d;
  }

  protected void ascend(double d) {
    position.y += d;
    position.stance += d;

    if (position.stance - position.y > 1.6 || position.stance - position.y < 0.15) {
      position.stance = position.y + 0.5;
    }
  }

  protected void sendPosition() throws IOException {
    writeLock.lock();
    position.send(out);
    writeLock.unlock();
  }

  protected boolean trashdat() {
    return true;
  }

  protected void handlePacket(byte packetId) throws IOException {
    // System.out.println("Packet: 0x" + Integer.toHexString(packetId));
    switch (packetId) {
      case 0x2: // Handshake
        readUTF16();
        login();
        break;
      case 0x1: // Login Request
        in.readInt();
        readUTF16();
        in.readLong();
        readUTF16();
        in.readInt();
        position.dimension = Dimension.get(in.readByte());
        in.readByte();
        in.readByte();
        in.readByte();
        break;
      case 0x0d: // Player Position & Look
        double x = in.readDouble();
        double stance = in.readDouble();
        double y = in.readDouble();
        double z = in.readDouble();
        float yaw = in.readFloat();
        float pitch = in.readFloat();
        boolean onGround = in.readBoolean();
        position.updatePosition(x, y, z, stance);
        position.updateLook(yaw, pitch);
        position.updateGround(onGround);
        if (!ready) {
          sendPosition();
          ready();
        } else if (dead) {
          sendPosition();
          dead = false;
        }
        positionUpdate();
        break;
      case 0x0b: // Player Position
        double x2 = in.readDouble();
        double stance2 = in.readDouble();
        double y2 = in.readDouble();
        double z2 = in.readDouble();
        boolean onGround2 = in.readBoolean();
        position.updatePosition(x2, y2, z2, stance2);
        position.updateGround(onGround2);
        positionUpdate();
        break;
      case (byte) 0xff: // Disconnect/Kick
        String reason = readUTF16();
        error(reason);
        break;

      case 0x00: // Keep Alive
        keepAlive(in.readInt());
        break;
      case 0x03: // Chat Message
        readUTF16();
        break;
      case 0x04: // Time Update
        in.readLong();
        break;
      case 0x05: // Entity Equipment
        in.readInt();
        in.readShort();
        in.readShort();
        in.readShort();
        break;
      case 0x06: // Spawn Position
        readNBytes(12);
        break;
      case 0x07: // Use Entity
        in.readInt();
        in.readInt();
        in.readBoolean();
        in.readBoolean();
        break;
      case 0x08: // Update Health
        health = in.readShort();
        in.readShort();
        in.readFloat();
        if (health <= 0) {
          dead = true;
          respawn();
        }
        break;
      case 0x09: // Respawn
        position.dimension = Dimension.get(in.readByte());
        in.readByte();
        in.readByte();
        in.readShort();
        in.readLong();
        readUTF16();
        break;
      case 0x0a: // Player
        in.readByte();
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
        readItem();
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
      case 0x13: // Entity Action
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
        in.readByte();
        readUnknownBlob();
        break;
      case 0x19: // Entity: Painting
        in.readInt();
        readUTF16();
        in.readInt();
        in.readInt();
        in.readInt();
        in.readInt();
        break;
      case 0x1a: // Experience Orb
        in.readInt();
        in.readInt();
        in.readInt();
        in.readInt();
        in.readShort();
        break;
      case 0x1c: // Entity Velocity
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
      case 0x23: // ???, added in 12w03a
        in.readInt();
        in.readByte();
        break;
      case 0x26: // Entity Status
        readNBytes(5);
        break;
      case 0x27: // Attach Entity
        readNBytes(8);
        break;
      case 0x28: // Entity Metadata
        in.readInt();
        readUnknownBlob();
        break;
      case 0x29: // Entity Effect
        in.readInt();
        in.readByte();
        in.readByte();
        in.readShort();
        break;
      case 0x2a: // Remove Entity Effect
        in.readInt();
        in.readByte();
        break;
      case 0x2b: // Experience
        in.readFloat();
        in.readShort();
        in.readShort();
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
      case 0x36: // Block Action
        readNBytes(12);
        break;
      case 0x3c: // Explosion
        readNBytes(28);
        int recordCount = in.readInt();
        readNBytes(recordCount * 3);
        break;
      case 0x3d: // Sound/Particle Effect
        in.readInt();
        in.readInt();
        in.readByte();
        in.readInt();
        in.readInt();
        break;
      case 0x46: // New/Invalid State
        readNBytes(2);
        break;
      case 0x47: // Thunderbolt
        readNBytes(17);
        break;
      case 0x64: // Open Window
        in.readByte();
        in.readByte();
        readUTF16();
        in.readByte();
        break;
      case 0x65: // Close Window
        in.readByte();
        break;
      case 0x66: // Window Click
        in.readByte();
        in.readShort();
        in.readByte();
        in.readShort();
        in.readBoolean();
        readItem();
        break;
      case 0x67: // Set Slot
        in.readByte();
        in.readShort();
        readItem();
        break;
      case 0x68: // Window Items
        in.readByte();
        short count = in.readShort();
        for (int c = 0; c < count; ++c) {
          readItem();
        }
        break;
      case 0x69: // Update Window Property
        in.readByte();
        in.readShort();
        in.readShort();
        break;
      case 0x6a: // Transaction
        in.readByte();
        in.readShort();
        in.readByte();
        break;
      case 0x6b: // Creative Inventory Action
        in.readShort();
        readItem();
        break;
      case (byte) 0x6c: // Enchant Item
        readNBytes(2);
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
      case (byte) 0x83: // Item Data
        in.readShort();
        in.readShort();
        byte length = in.readByte();
        readNBytes(0xff & length);
        break;
      case (byte) 0xc8: // Increment Statistic
        readNBytes(5);
        break;
      case (byte) 0xc9: // Player List Item
        readUTF16();
        in.readBoolean();
        in.readShort();
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
      case (byte) 0xfa: // Plugin Message
        readUTF16();
        short arrayLength = in.readShort();
        readNBytes(0xff & arrayLength);
        break;
      case (byte) 0xfe: // Server List Ping
        break;
      default:
        error("Unable to handle packet 0x" + Integer.toHexString(packetId)
            + " after 0x" + Integer.toHexString(lastPacket));
    }
    lastPacket = packetId;
  }

  private void readItem() throws IOException {
    short id;
    if ((id = in.readShort()) > 0) {
      in.readByte();
      in.readShort();
      if (ENCHANTABLE.contains(id)) {
        short length;
        if ((length = in.readShort()) > 0) {
          readNBytes(length);
        }
      }
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
    connected = false;
    if (controller != null) {
      controller.remove(this);
    }
    if (trashdat()) {
      File dat = new File(server.options.get("levelName") + File.separator + "players" + File.separator + name + ".dat");
      if (controller != null) {
        controller.trash(dat);
      } else {
        dat.delete();
      }
    }
  }

  protected void error(String reason) {
    die();
    if (!expectDisconnect) {
      System.out.print("[SimpleServer] Bot " + name + " died (" + reason + ")");
    }
  }

  public void setController(BotController controller) {
    this.controller = controller;
  }

  private final class Tunneler extends Thread {
    @Override
    public void run() {
      while (connected) {
        try {
          handlePacket(in.readByte());
          out.flush();
          if (!gotFirstPacket) {
            gotFirstPacket = true;
          }
        } catch (IOException e) {
          if (!gotFirstPacket) {
            try {
              connect();
            } catch (Exception e2) {
              error("Soket closed on reconnect");
            }
            break;
          } else {
            error("Soket closed");
          }
        }
      }
    }
  }
}
