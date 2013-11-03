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
    out.writeByte(2);
    out.writeByte(Main.protocolVersion);
    write(name);
    write("localhost");
    out.writeInt(server.options.getInt("internalPort"));
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
    out.writeByte(0xcd);
    out.writeByte(0);
    writeLock.unlock();
  }

  private void sendSharedKey() throws IOException {
    writeLock.lock();
    out.writeByte(0xfc);
    byte[] key = encryption.getEncryptedSharedKey();
    out.writeShort(key.length);
    out.write(key);
    byte[] challengeTokenResponse = encryption.encryptChallengeToken();
    out.writeShort(challengeTokenResponse.length);
    out.write(challengeTokenResponse);
    out.flush();
    writeLock.unlock();
  }

  private void respawn() throws IOException {
    writeLock.lock();
    out.writeByte(0xcd);
    out.writeByte(1);
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
      case 0x01: // Login Request
        int eid = in.readInt();
        if (playerEntityId == 0) {
          playerEntityId = eid;
        }

        readUTF16();
        in.readByte();
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

      case 0x08: // Update Health
        health = in.readFloat();
        in.readShort();
        in.readFloat();
        if (health <= 0) {
          dead = true;
          respawn();
        }
        break;
      case 0x09: // Respawn
        position.dimension = Dimension.get((byte) in.readInt());
        in.readByte();
        in.readByte();
        in.readShort();
        readUTF16();
        break;
      case (byte) 0xfc: // Encryption Key Response
        byte[] sharedKey = new byte[in.readShort()];
        in.readFully(sharedKey);
        byte[] challengeTokenResponse = new byte[in.readShort()];
        in.readFully(challengeTokenResponse);
        in = new DataInputStream(new BufferedInputStream(encryption.encryptedInputStream(socket.getInputStream())));
        out = new DataOutputStream(new BufferedOutputStream(encryption.encryptedOutputStream(socket.getOutputStream())));
        login();
        break;
      case (byte) 0xfd: // Encryption Key Request (server -> client)
        readUTF16();
        byte[] keyBytes = new byte[in.readShort()];
        in.readFully(keyBytes);
        byte[] challengeToken = new byte[in.readShort()];
        in.readFully(challengeToken);
        encryption.setPublicKey(keyBytes);
        encryption.setChallengeToken(challengeToken);
        sendSharedKey();
        break;
      case (byte) 0xfe: // Server List Ping
        break;
      default:
        error("Unable to handle packet 0x" + Integer.toHexString(packetId)
            + " after 0x" + Integer.toHexString(lastPacket));
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
