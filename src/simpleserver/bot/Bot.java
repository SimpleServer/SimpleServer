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

import static simpleserver.util.Util.print;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.locks.ReentrantLock;

import simpleserver.Coordinate.Dimension;
import simpleserver.Main;
import simpleserver.Position;
import simpleserver.Server;
import simpleserver.stream.Encryption.ServerEncryption;

public class Bot {
  protected String name;
  protected Server server;
  private boolean connected;
  private boolean expectDisconnect;
  protected boolean ready;
  protected boolean dead;
  protected int playerEntityId;

  private Socket socket;
  protected DataInputStream in;
  protected DataOutputStream out;

  ReentrantLock writeLock;
  protected Position position;
  protected BotController controller;
  protected boolean gotFirstPacket = false;
  private byte lastPacket;
  private float health;

  private ServerEncryption encryption = new ServerEncryption();

  protected ByteBuffer incoming = null;
  protected ByteBuffer outgoing = null;
  protected int state = 0;

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
    in = new DataInputStream(socket.getInputStream());
    out = new DataOutputStream(socket.getOutputStream());

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
    byte[] keepAlive = ByteBuffer.allocate(4).putInt(keepAliveId).array();
    sendPacketIndependently((byte) 0x00, keepAlive);
    writeLock.unlock();
  }

  private void handshake() throws IOException {
    writeLock.lock();

    byte[] v = encodeVarInt(Main.protocolVersion);
    byte[] h = setUTF8("localhost");
    short  p = (short) (server.options.getInt("internalPort") & 0xFFFF);
    byte[] s = encodeVarInt(2);

    ByteBuffer handshake = ByteBuffer.allocate(v.length + h.length + 2 + s.length);
    handshake.put(v).put(h).putShort(p).put(s);
    sendPacketIndependently((byte) 0x00, handshake.array());
    this.state = 2;
    writeLock.unlock();
    login();
  }

  public void logout() throws IOException {
    die();
    //@todo send valid JSON quit message.
    expectDisconnect = true;
//    out.writeByte(0xff);
//    write("quitting");
//    out.flush();
  }

  protected void login() throws IOException {
    writeLock.lock();
    byte[] name = setUTF8(this.name);

    ByteBuffer login = ByteBuffer.allocate(name.length);
    login.put(name);
    sendPacketIndependently((byte) 0x00, login.array());
    writeLock.unlock();
  }

  private void respawn() throws IOException {
    writeLock.lock();
    //out.writeByte(0xcd);
    //out.writeByte(1);
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

  protected void handlePacket() throws IOException {
    int length = decodeVarInt();

    // read length into byte[], copy into ByteBuffer
    byte[] buf = new byte[length];
    in.readFully(buf, 0, length);
    incoming = ByteBuffer.wrap(buf);
    outgoing = ByteBuffer.allocate(length * 2);

    Byte packetId  = (byte) decodeVarInt();
    handlePacket(packetId);
  }

  protected void handlePacket(byte packetId) throws IOException {
    // System.out.println("Packet: 0x" + Integer.toHexString(packetId));
    switch (this.state) {
      case 0: // handshake

        break;

      case 1: // status

        break;

      case 2: // login
         switch(packetId) {
           case 0x00: // Disconnect
             readUTF8();
             break;

           case 0x02: // Login-Success
             String uuid = readUTF8();
             name = readUTF8();
             this.state = 3;
             break;
         }
        break;

      case 3: // play
        switch (packetId) {
          case 0x00: // Keep Alive
            keepAlive(incoming.getInt());
            break;

          case 0x01: // Login Request
            int eid = in.readInt();
            if (playerEntityId == 0) {
              playerEntityId = eid;
            }

            readUnsignedByte();
            position.dimension = Dimension.get(incoming.get());
            readUnsignedByte();
            readUnsignedByte();
            readUTF8();
            break;

          case 0x02: // Chat-Message
            readUTF8();
            break;

          case 0x04: // Player Position
            double x2 = incoming.getDouble();
            double stance2 = incoming.getDouble();
            double y2 = incoming.getDouble();
            double z2 = incoming.getDouble();
            boolean onGround2 = (incoming.get() != 0);
            position.updatePosition(x2, y2, z2, stance2);
            position.updateGround(onGround2);
            positionUpdate();
            break;

          case 0x06: // Update Health
            health = incoming.getFloat();
            incoming.getShort();
            incoming.getFloat();
            if (health <= 0) {
              dead = true;
              respawn();
            }
            break;

          case 0x07: // Respawn
            position.dimension = Dimension.get(incoming.getInt());
            readUnsignedByte();
            readUnsignedByte();
            readUTF8();
            break;

          case 0x08: // Player Position & Look
            double x = incoming.getDouble();
            double stance = incoming.getDouble();
            double y = incoming.getDouble();
            double z = incoming.getDouble();
            float yaw = incoming.getFloat();
            float pitch = incoming.getFloat();
            boolean onGround = (incoming.get() != 0);
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
          case (byte) 0x40: // Disconnect/Kick
            String reason = readUTF8();
            error(reason);
            break;
          default:
            // packet are length prefixed, so just skip the unknowns
            break;
        }
        break;
    }

    lastPacket = packetId;
  }

  protected void die() {
    connected = false;
    if (controller != null) {
      controller.remove(this);
    }
    if (trashdat()) {
      File dat = server.getPlayerFile(name);
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
      print("Bot " + name + " died (" + reason + ")\n");
    }
  }

  public void setController(BotController controller) {
    this.controller = controller;
  }

  protected int decodeVarInt() throws IOException {
    int i = 0;
    int j = 0;

    while (true) {
      int k = ((incoming != null) ? incoming.get() : in.readByte());
      i |= (k & 0x7F) << j++ * 7;

      if (j > 5) {
        throw new IllegalArgumentException("VarInt too big");
      }
      if ((k & 0x80) != 128)
        break;
    }
    return i;
  }

  protected byte[] encodeVarInt(int value) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    while (true) {
      if ((value & 0xFFFFFF80) == 0) {
        buffer.write(value);
        return buffer.toByteArray();
      }
      buffer.write(value & 0x7F | 0x80);
      value >>>= 7;
    }
  }

  protected String readUTF8() throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int length = decodeVarInt();

    for (int i = 0; i < length; i++) {
      buffer.write((incoming != null) ? incoming.get() : in.readByte());
    }
    return new String(buffer.toByteArray(), "UTF-8");
  }

  private byte[] setUTF8(String str) throws IOException {
    // add size of varInt(string.length) + string, append to byte[], return it
    return ByteBuffer.allocate(encodeVarInt(str.length()).length + str.length())
            .put(encodeVarInt(str.length()))
            .put(str.getBytes()).array();
  }

  private int readUnsignedShort() {
    return (incoming.getShort() & 0xFFFF);
  }

  private short readUnsignedByte() {
    return ((short) (incoming.get() & 0xFF));
  }

  private void copyVarInt() throws IOException {
    outgoing.put(encodeVarInt(decodeVarInt()));
  }

  private int copyUnsignedByte() throws IOException {
    return addUnsignedByte(readUnsignedByte());
  }

  private int copyUnsignedShort() throws IOException {
    return addUnsignedShort(readUnsignedShort());
  }

  private int addUnsignedShort(int i) {
    outgoing.putShort((short) (i & 0xFFFF));
    return i;
  }

  private int addUnsignedByte(int i) {
    outgoing.put((byte) (i & 0xFF));
    return i;
  }

  private void sendPacketIndependently(byte id, byte[] data) throws IOException {
    ByteBuffer tmp = ByteBuffer.allocate(data.length + 1);
    tmp.put(id);
    tmp.put(data);

    out.write(encodeVarInt(tmp.limit()));
    tmp.rewind();
    out.write(tmp.array());
    ((OutputStream) out).flush();
  }

  private final class Tunneler extends Thread {
    @Override
    public void run() {
      while (connected) {
        try {
          handlePacket();
          if (!gotFirstPacket) {
            gotFirstPacket = true;
          }
        } catch (IOException e) {
          if (!gotFirstPacket) {
            try {
              connect();
            } catch (Exception e2) {
              error("Socket closed on reconnect");
            }
            break;
          } else {
            error("Socket closed");
          }
        }
      }
    }
  }
}
