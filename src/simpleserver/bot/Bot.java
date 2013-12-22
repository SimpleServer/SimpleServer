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
    out.writeByte(0x0);
    out.writeInt(keepAliveId);
    writeLock.unlock();
  }

  private void handshake() throws IOException {
    writeLock.lock();

    byte[] s = encodeVarInt(Main.protocolVersion);
    byte[] v = encodeVarInt(("localhost").length());
    byte[] t = "localhost".getBytes();
    short  w = (short) (server.options.getInt("internalPort") & 0xFFFF);
    byte[] q = encodeVarInt(2);

    ByteBuffer handshake = ByteBuffer.allocate(s.length + v.length + t.length + 2 + q.length);
    handshake.put(s).put(v).put(t).putShort(w).put(q);
    sendPacketIndependently((byte) 0x00, handshake.array());
    this.state = 2;
    writeLock.unlock();
  }

  public void logout() throws IOException {
    die();
    expectDisconnect = true;
//    out.writeByte(0xff);
//    write("quitting");
//    out.flush();
  }

  protected void login() throws IOException {
    writeLock.lock();
    byte[] uuid_s = encodeVarInt(4);
    byte[] uuid = String.valueOf(this.playerEntityId).getBytes();
    byte[] name_s = encodeVarInt(this.name.length());
    byte[] name = this.name.getBytes();

    ByteBuffer login = ByteBuffer.allocate(uuid_s.length + uuid.length + name_s.length + name.length);
    sendPacketIndependently((byte) 0x02, login.array());
    writeLock.unlock();
  }

  private void sendSharedKey() throws IOException {
    writeLock.lock();
    //out.writeByte(0xfc);
    //byte[] key = encryption.getEncryptedSharedKey();
    //out.writeShort(key.length);
    //out.write(key);
    //byte[] challengeTokenResponse = encryption.encryptChallengeToken();
    //out.writeShort(challengeTokenResponse.length);
    //out.write(challengeTokenResponse);
    //out.flush();
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

    // System.out.println("Packet: 0x" + Integer.toHexString(packetId));
    switch (this.state) {
      case 0: // handshake

        break;

      case 1: // status

        break;

      case 2: // login

        break;

      case 3: // play
        switch (packetId) {
          case 0x01: // Login Request
            int eid = in.readInt();
            if (playerEntityId == 0) {
              playerEntityId = eid;
            }

            readUTF8();
            incoming.get();
            position.dimension = Dimension.get(incoming.get());
            incoming.get();
            incoming.get();
            incoming.get();
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
          case (byte) 0xff: // Disconnect/Kick
            String reason = readUTF8();
            error(reason);
            break;
          case 0x00: // Keep Alive
            keepAlive(incoming.getInt());
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
          case (byte) 0xfe: // Server List Ping
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
          out.flush();
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
