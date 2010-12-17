/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package simpleserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.IllegalFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import simpleserver.log.EOFWriter;

public class StreamTunnel {
  public static final int IDLE_TIME = 30 * 1000;
  private static final int BUFFER_SIZE = 128;
  private static final int DESTROY_HITS = 14;
  private static final byte BLOCK_DESTROYED_STATUS = 3;
  private static final Pattern MESSAGE_PATTERN = Pattern.compile("^<([^>]+)> (.*)$");

  private final boolean isServerTunnel;
  private final Player player;
  private final Server server;
  private final DataInputStream in;
  private final DataOutputStream out;
  private final byte[] buffer;

  private boolean run = true;
  private boolean inGame = false;
  protected long lastRead;

  private int motionCounter = 0;

  public StreamTunnel(InputStream in, OutputStream out, boolean isServerTunnel,
                      Player player) {
    this.isServerTunnel = isServerTunnel;

    this.player = player;
    server = player.getServer();

    this.in = new DataInputStream(new BufferedInputStream(in));
    this.out = new DataOutputStream(new BufferedOutputStream(out));

    buffer = new byte[BUFFER_SIZE];
  }

  public void stop() {
    run = false;
  }

  private void handlePacket() throws IOException {
    Byte packetId = in.readByte();
    switch (packetId) {
      case 0: // Keep Alive
        write(packetId);
        break;
      case 0x01: // Login Request/Response
        write(packetId);
        write(in.readInt());
        write(in.readUTF());
        write(in.readUTF());
        write(in.readLong());
        write(in.readByte());
        break;
      case 0x02: // Handshake
        String name = in.readUTF();
        if (isServerTunnel || player.setName(name)) {
          write(packetId);
          write(in.readUTF());
        }
        break;
      case 0x03: // Chat Message
        String message = in.readUTF();
        if (isServerTunnel && server.options.getBoolean("useMsgFormats")) {
          Matcher messageMatcher = MESSAGE_PATTERN.matcher(message);
          if (messageMatcher.find()) {
            Player friend = server.findPlayerExact(messageMatcher.group(1));

            if (friend != null) {
              String color = "f";
              String title = "";
              String format = server.options.get("msgFormat");
              Group group = friend.getGroup();

              if (group != null) {
                color = group.getColor();
                if (group.showTitle()) {
                  title = group.getName();
                  format = server.options.get("msgTitleFormat");
                }
              }

              try {
                message = String.format(format, friend.getName(), title, color)
                    + messageMatcher.group(2);
              }
              catch (IllegalFormatException e) {
                System.out.println("[SimpleServer] There is an error in your msgFormat/msgTitleFormat settings!");
              }
            }
          }
        }

        if (!isServerTunnel) {
          if (player.isMuted() && !message.startsWith("/")
              && !message.startsWith("!")) {
            player.addMessage("You are muted! You may not send messages to all players.");
            break;
          }

          if (server.options.getBoolean("useSlashes")
              && message.startsWith("/") || message.startsWith("!")
              && player.parseCommand(message)) {
            break;
          }
        }

        write(packetId);
        write(message);
        break;
      case 0x04: // Time Update
        write(packetId);
        copyNBytes(8);
        break;
      case 0x05: // Player Inventory
        boolean guest = player.getGroupId() < 0;
        int inventoryType = in.readInt();
        short itemCount = in.readShort();
        if (!guest) {
          write(packetId);
          write(inventoryType);
          write(itemCount);
        }

        for (int c = 0; c < itemCount; ++c) {
          short itemId = in.readShort();
          if (!guest) {
            write(itemId);
          }

          if (itemId != -1) {
            byte itemAmount = in.readByte();
            short itemUses = in.readShort();
            if (!guest) {
              write(itemAmount);
              write(itemUses);
            }

            if (!server.itemWatch.playerAllowed(player, itemId, itemAmount)) {
              server.adminLog.addMessage("ItemWatchList banned player:\t"
                  + player.getName());
              server.banKick(player.getName());
            }
          }
        }
        break;
      case 0x06: // Spawn Position
        write(packetId);
        copyNBytes(12);
        break;
      case 0x07: // Use Entity?
        write(packetId);
        copyNBytes(9);
        break;
      case 0x08: // Update Health
        write(packetId);
        copyNBytes(1);
        break;
      case 0x09: // Respawn
        write(packetId);
        break;
      case 0x0a: // Player
        write(packetId);
        copyNBytes(1);
        if (!inGame && !isServerTunnel) {
          player.sendMOTD();
          inGame = true;
        }
        break;
      case 0x0b: // Player Position
        write(packetId);
        copyPlayerLocation();
        break;
      case 0x0c: // Player Look
        write(packetId);
        copyNBytes(9);
        break;
      case 0x0d: // Player Position & Look
        write(packetId);
        copyPlayerLocation();
        copyNBytes(8);
        break;
      case 0x0e: // Player Digging
        if (!isServerTunnel) {
          if (player.getGroupId() < 0) {
            skipNBytes(11);
            break;
          }

          int status = in.readByte();
          int x = in.readInt();
          int y = in.readByte();
          int z = in.readInt();
          byte face = in.readByte();
          if (server.chests.hasLock(x, y, z) && !player.isAdmin()) {
            break;
          }
          write(packetId);
          write(status);
          write(x);
          write(y);
          write(z);
          write(face);

          if (player.instantDestroyEnabled()) {
            for (int c = 1; c < DESTROY_HITS; ++c) {
              write(packetId);
              write(status);
              write(x);
              write(y);
              write(z);
              write(face);
            }

            write(packetId);
            write(BLOCK_DESTROYED_STATUS);
            write(x);
            write(y);
            write(z);
            write(face);
          }
        }
        else {
          write(packetId);
          copyNBytes(11);
        }
        break;
      // CHECK THIS
      case 0x0f: // Player Block Placement
        short block = readShort();
        if (!isServerTunnel) {
          if (server.blockFirewall.contains(block) || player.getGroupId() < 0) {
            boolean allowed = server.blockFirewall.playerAllowed(player, block);
            if (!allowed || player.getGroupId() < 0) {
              // Remove the packet! : )
              int coord_x = readInt();
              if (!allowed && coord_x != -1) {
                server.runCommand("say",
                                  String.format(server.l.get("BAD_BLOCK"),
                                                player.getName(),
                                                Short.toString(block)));
              }
              skipBytes(6);
              removeBytes(13);
              break;
            }
          }
          if (block == 54) {
            // Check if lock is ready and allowed
            if (player.isAttemptLock()) {
              // calculate coordinates
              int xC0f = readInt();
              int yC0f = readByte();
              int zC0f = readInt();
              int dir = readByte();
              switch (dir) {
                case 0:
                  yC0f--;
                  break;
                case 1:
                  yC0f++;
                  break;
                case 2:
                  zC0f--;
                  break;
                case 3:
                  zC0f++;
                  break;
                case 4:
                  xC0f--;
                  break;
                case 5:
                  xC0f++;
                  break;
              }
              // create chest entry
              if (server.chests.hasLock(xC0f, yC0f, zC0f)) {
                player.addMessage("This block is locked already!");
                player.setAttemptLock(false);
                break;
              }
              if (!server.chests.giveLock(player.getName().toLowerCase(), xC0f,
                                          yC0f, zC0f, false)) {
                player.addMessage("You already have a lock, or this block is locked already!");
              }
              else {
                player.addMessage("Your locked chest is created! Do not add another chest to it!");
              }
              player.setAttemptLock(false);
            }
            else {
              skipBytes(10);
            }
          }
          else {
            skipBytes(10);
          }
        }
        else {
          skipBytes(10);
        }
        break;
      case 0x10: // Holding Change
        write(packetId);
        copyNBytes(6);
        break;
      case 0x11: // Add To Inventory
        write(packetId);
        copyNBytes(5);
        break;
      case 0x12: // Animation
        write(packetId);
        copyNBytes(5);
        break;
      case 0x14: // Named Entity Spawn
        write(packetId);
        write(in.readInt());
        write(in.readUTF());
        copyNBytes(16);
        break;
      case 0x15: // Pickup spawn
        if (player.getGroupId() < 0) {
          skipNBytes(22);
          break;
        }
        write(packetId);
        copyNBytes(22);
        break;
      case 0x16: // Collect Item
        write(packetId);
        copyNBytes(8);
        break;
      case 0x17: // Add Object/Vehicle
        write(packetId);
        copyNBytes(17);
        break;
      case 0x18: // Mob Spawn
        write(packetId);
        copyNBytes(19);
        break;
      case 0x1c: // Entity Velocity?
        write(packetId);
        copyNBytes(10);
        break;
      case 0x1D: // Destroy Entity
        write(packetId);
        copyNBytes(4);
        break;
      case 0x1E: // Entity
        write(packetId);
        copyNBytes(4);
        break;
      case 0x1F: // Entity Relative Move
        write(packetId);
        copyNBytes(7);
        break;
      case 0x20: // Entity Look
        write(packetId);
        copyNBytes(6);
        break;
      case 0x21: // Entity Look and Relative Move
        write(packetId);
        copyNBytes(9);
        break;
      case 0x22: // Entity Teleport
        write(packetId);
        copyNBytes(18);
        break;
      case 0x26: // Entity damage, death and explosion?
        write(packetId);
        copyNBytes(5);
        break;
      case 0x27: // Attach Entity?
        write(packetId);
        copyNBytes(8);
        break;
      case 0x32: // Pre-Chunk
        write(packetId);
        copyNBytes(9);
        break;
      case 0x33: // Map Chunk
        write(packetId);
        copyNBytes(13);
        int chunkSize = in.readInt();
        write(chunkSize);
        copyNBytes(chunkSize);
        break;
      // THIS DOESNT WORK :(
      case 0x34: // Multi Block Change
        skipBytes(8);
        short chunkSize34 = readShort();
        skipBytes(chunkSize34 * 4);
        break;
      case 0x35: // Block Change
        write(packetId);
        copyNBytes(11);
        break;
      case 0x3b: // Complex Entities
        oldCursor = r - 1;
        int xC3b = readInt();
        int yC3b = readShort();
        int zC3b = readInt();
        int arraySize = readShort();
        skipBytes(arraySize);
        if (server.chests.hasLock(xC3b, yC3b, zC3b)) {
          if (!player.isAdmin()) {
            if (!server.chests.ownsLock(xC3b, yC3b, zC3b, player.getName())
                || player.getName() == null) {
              removeBytes(r - oldCursor);
              break;
            }
          }
        }

        if (player.getGroupId() < 0
            && !server.options.getBoolean("guestsCanViewComplex")) {
          removeBytes(r - oldCursor);
          break;
        }

        break;
      case 0x3c: // Explosion
        skipBytes(28);
        skipBytes(readInt() * 3);
        break;
      case 0xff: // Disconnect/Kick
        String discMsg = readString();
        if (discMsg.startsWith("Took too long")) {
          server.addRobot(player);
        }
        player.close();
        break;
      default:
        byte[] cpy = new byte[a];
        System.arraycopy(buf, 0, cpy, 0, a);
        String streamType = "PlayerStream";
        if (isServerTunnel) {
          streamType = "ServerStream";
        }
        new Thread(new EOFWriter(cpy, history, null, streamType + " "
            + player.getName() + " packetid: " + packetid + " totalsize: " + r
            + " amt: " + a)).start();
        throw new InterruptedException();
    }
  }

  private void copyPlayerLocation() throws IOException {
    if (!isServerTunnel) {
      motionCounter++;
    }
    if (!isServerTunnel && motionCounter % 8 == 0) {
      double x = in.readDouble();
      double y = in.readDouble();
      double stance = in.readDouble();
      double z = in.readDouble();
      player.updateLocation(x, y, z, stance);
      copyNBytes(1);
    }
    else {
      copyNBytes(33);
    }
  }

  private void write(byte b) throws IOException {
    out.writeByte(b);
  }

  private void write(short s) throws IOException {
    out.writeShort(s);
  }

  private void write(int i) throws IOException {
    out.writeInt(i);
  }

  private void write(long l) throws IOException {
    out.writeLong(l);
  }

  private void write(float f) throws IOException {
    out.writeFloat(f);
  }

  private void write(double d) throws IOException {
    out.writeDouble(d);
  }

  private void write(String s) throws IOException {
    out.writeUTF(s);
  }

  private void write(boolean b) throws IOException {
    out.writeBoolean(b);
  }

  private void skipNBytes(int bytes) throws IOException {
    in.readFully(buffer, 0, bytes);
  }

  private void copyNBytes(int bytes) throws IOException {
    in.readFully(buffer, 0, bytes);
    out.write(buffer, 0, bytes);
  }

  private final class Tunneler {
    public void run() {
      while (run) {
        lastRead = System.currentTimeMillis();
        handlePacket();

        if (isServerTunnel) {
          if (player.isKicked()) {
            out.write(makePacket((byte) 0xff, player.getKickMsg()));
            out.flush();
            break;
          }
          while (player.hasMessages()) {
            byte[] m = makePacket(player.getMessage());
            out.write(m, 0, m.length);
          }
        }
        else {
          if (player.isKicked()) {
            out.write(makePacket((byte) 0xff, player.getKickMsg()));
            out.flush();
            break;
          }
        }

        if (System.currentTimeMillis() - lastRead > IDLE_TIME) {
          if (!player.isRobot()) {
            System.out.println("[SimpleServer] Disconnecting "
                + player.getIPAddress() + " due to inactivity.");
          }
        }
      }

      try {
        in.close();
      }
      catch (IOException e) {
      }
      try {
        out.close();
      }
      catch (IOException e) {
      }
    }
  }
}
