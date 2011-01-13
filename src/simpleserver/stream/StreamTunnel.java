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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.IllegalFormatException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import simpleserver.Group;
import simpleserver.Player;
import simpleserver.Server;

public class StreamTunnel {
  private static final boolean EXPENSIVE_DEBUG_LOGGING = Boolean.getBoolean("EXPENSIVE_DEBUG_LOGGING");
  private static final int IDLE_TIME = 30000;
  private static final int BUFFER_SIZE = 1024;
  private static final int DESTROY_HITS = 14;
  private static final byte BLOCK_DESTROYED_STATUS = 3;
  private static final Pattern MESSAGE_PATTERN = Pattern.compile("^<([^>]+)> (.*)$");
  private static final Pattern COLOR_PATTERN = Pattern.compile("\u00a7[0-9a-f]");

  private final boolean isServerTunnel;
  private final String streamType;
  private final Player player;
  private final Server server;
  private final byte[] buffer;
  private final Tunneler tunneler;

  private DataInput in;
  private DataOutput out;
  private StreamDumper inputDumper;
  private StreamDumper outputDumper;

  private int motionCounter = 0;
  private boolean inGame = false;

  private volatile long lastRead;
  private volatile boolean run = true;

  public StreamTunnel(InputStream in, OutputStream out, boolean isServerTunnel,
                      Player player) {
    this.isServerTunnel = isServerTunnel;
    if (isServerTunnel) {
      streamType = "ServerStream";
    }
    else {
      streamType = "PlayerStream";
    }

    this.player = player;
    server = player.getServer();

    DataInputStream dIn = new DataInputStream(new BufferedInputStream(in));
    DataOutputStream dOut = new DataOutputStream(new BufferedOutputStream(out));
    if (EXPENSIVE_DEBUG_LOGGING) {
      try {
        OutputStream dump = new FileOutputStream(streamType + "Input.debug");
        InputStreamDumper dumper = new InputStreamDumper(dIn, dump);
        inputDumper = dumper;
        this.in = dumper;
      }
      catch (FileNotFoundException e) {
        System.out.println("Unable to open input debug dump!");
        throw new RuntimeException(e);
      }

      try {
        OutputStream dump = new FileOutputStream(streamType + "Output.debug");
        OutputStreamDumper dumper = new OutputStreamDumper(dOut, dump);
        outputDumper = dumper;
        this.out = dumper;
      }
      catch (FileNotFoundException e) {
        System.out.println("Unable to open output debug dump!");
        throw new RuntimeException(e);
      }
    }
    else {
      this.in = dIn;
      this.out = dOut;
    }

    buffer = new byte[BUFFER_SIZE];

    tunneler = new Tunneler();
    tunneler.start();

    lastRead = System.currentTimeMillis();
  }

  public void stop() {
    run = false;
  }

  public boolean isAlive() {
    return tunneler.isAlive();
  }

  public boolean isActive() {
    return System.currentTimeMillis() - lastRead < IDLE_TIME
        || player.isRobot();
  }

  private void handlePacket() throws IOException {
    Byte packetId = in.readByte();
    switch (packetId) {
      case 0x00: // Keep Alive
        write(packetId);
        break;
      case 0x01: // Login Request/Response
        write(packetId);
        if (isServerTunnel) {
          player.setEntityId(in.readInt());
          write(player.getEntityId());
        }
        else {
          write(in.readInt());
        }
        write(in.readUTF());
        write(in.readUTF());
        write(in.readLong());
        write(in.readByte());
        break;
      case 0x02: // Handshake
        String name = in.readUTF();

        if (isServerTunnel || player.setName(name)) {
          tunneler.setName(streamType + "-" + player.getName());
          write(packetId);
          write(name);
        }
        break;
      case 0x03: // Chat Message
        String message = in.readUTF();
        if (isServerTunnel && server.options.getBoolean("useMsgFormats")) {
          Matcher colorMatcher = COLOR_PATTERN.matcher(message);
          String cleanMessage = colorMatcher.replaceAll("");

          Matcher messageMatcher = MESSAGE_PATTERN.matcher(cleanMessage);
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
            player.addMessage("\u00a7cYou are muted! You may not send messages to all players.");
            break;
          }

          if (player.parseCommand(message)) {
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
        write(packetId);
        write(in.readInt());
        write(in.readShort());
        write(in.readShort());
        write(in.readShort());
        break;
      case 0x06: // Spawn Position
        write(packetId);
        copyNBytes(12);
        break;
      case 0x07: // Use Entity?
        int user = in.readInt();
        int target = in.readInt();
        Player targetPlayer = server.playerList.findPlayer(target);
        if (targetPlayer != null) {
          if (targetPlayer.godModeEnabled()) {
            in.readBoolean();
            break;
          }
        }
        write(packetId);
        write(user);
        write(target);
        write(in.readBoolean());
        break;
      case 0x08: // Update Health
        write(packetId);
        copyNBytes(2);
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
          }
          else {
            byte status = in.readByte();
            int x = in.readInt();
            byte y = in.readByte();
            int z = in.readInt();
            byte face = in.readByte();

            if (!server.chests.hasLock(x, y, z) || player.isAdmin()) {
              if (server.chests.hasLock(x, y, z)
                  && status == BLOCK_DESTROYED_STATUS) {
                server.chests.releaseLock(x, y, z);
              }

              write(packetId);
              write(status);
              write(x);
              write(y);
              write(z);
              write(face);

              if (player.instantDestroyEnabled()) {
                for (int c = 1; c < DESTROY_HITS; ++c) {
                  packetFinished();
                  write(packetId);
                  write(status);
                  write(x);
                  write(y);
                  write(z);
                  write(face);
                }

                packetFinished();
                write(packetId);
                write(BLOCK_DESTROYED_STATUS);
                write(x);
                write(y);
                write(z);
                write(face);
              }
            }
          }
        }
        else {
          write(packetId);
          copyNBytes(11);
        }
        break;
      case 0x0f: // Player Block Placement
        final int x = in.readInt();
        final byte y = in.readByte();
        final int z = in.readInt();
        final byte direction = in.readByte();
        final short dropItem = in.readShort();

        byte itemCount = 0;
        short uses = 0;
        if (dropItem != -1) {
          itemCount = in.readByte();
          uses = in.readShort();
        }

        boolean writePacket = true;
        if (isServerTunnel) {
          // continue
        }
        else if (server.chests.hasLock(x, y, z) && !player.isAdmin()
            && !server.chests.ownsLock(player.getName(), x, y, z)) {
          player.addMessage("\u00a7cThis chest is locked!");
          writePacket = false;
        }
        else if ((player.getGroupId() < 0)
            || !server.blockFirewall.playerAllowed(player, dropItem)) {
          String badBlock = String.format(server.l.get("BAD_BLOCK"),
                                          player.getName(),
                                          Short.toString(dropItem));
          server.runCommand("say", badBlock);
          writePacket = false;
        }
        else if (dropItem == 54) {
          int xPosition = x;
          byte yPosition = y;
          int zPosition = z;
          switch (direction) {
            case 0:
              --yPosition;
              break;
            case 1:
              ++yPosition;
              break;
            case 2:
              --zPosition;
              break;
            case 3:
              ++zPosition;
              break;
            case 4:
              --xPosition;
              break;
            case 5:
              ++xPosition;
              break;
          }

          if (server.chests.hasAdjacentLock(xPosition, yPosition, zPosition)) {
            player.addMessage("\u00a7cThe adjacent chest is locked!");
            writePacket = false;
          }
          else if (player.isAttemptLock()) {
            if (server.chests.hasLock(xPosition, yPosition, zPosition)) {
              player.addMessage("\u00a7cThis block is locked already!");
            }
            else if (server.chests.giveLock(player.getName(), xPosition,
                                            yPosition, zPosition, false)) {
              player.addMessage("\u00a77Your locked chest is created! Do not add another chest to it!");
            }
            else {
              player.addMessage("\u00a7cYou already have a lock, or this block is locked already!");
            }
            player.setAttemptLock(false);
          }
        }

        if (writePacket) {
          write(packetId);
          write(x);
          write(y);
          write(z);
          write(direction);
          write(dropItem);

          if (dropItem != -1) {
            write(itemCount);
            write(uses);
          }
        }
        else {
          // Drop the item in hand. This keeps the client state in-sync with the
          // server. This generally prevents empty-hand clicks by the client
          // from placing blocks the server thinks the client has in hand.
          write((byte) 0x0e);
          write((byte) 0x04);
          write(x);
          write(y);
          write(z);
          write(direction);
        }
        break;
      case 0x10: // Holding Change
        write(packetId);
        copyNBytes(2);
        break;
      case 0x12: // Animation
        write(packetId);
        copyNBytes(5);
        break;
      case 0x13: // ???
        write(packetId);
        write(in.readInt());
        write(in.readByte());
        break;
      case 0x14: // Named Entity Spawn
        write(packetId);
        write(in.readInt());
        write(in.readUTF());
        copyNBytes(16);
        break;
      case 0x15: // Pickup spawn
        if (player.getGroupId() < 0) {
          skipNBytes(24);
          break;
        }
        write(packetId);
        copyNBytes(24);
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
        write(in.readInt());
        write(in.readByte());
        write(in.readInt());
        write(in.readInt());
        write(in.readInt());
        write(in.readByte());
        write(in.readByte());

        copyUnknownBlob();
        break;
      case 0x19: // ???
        write(packetId);
        write(in.readInt());
        write(in.readUTF());
        write(in.readInt());
        write(in.readInt());
        write(in.readInt());
        write(in.readInt());
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
      case 0x26: // Entity status?
        write(packetId);
        copyNBytes(5);
        break;
      case 0x27: // Attach Entity?
        write(packetId);
        copyNBytes(8);
        break;
      case 0x28: // ???
        write(packetId);
        write(in.readInt());

        copyUnknownBlob();
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
      case 0x34: // Multi Block Change
        write(packetId);
        copyNBytes(8);
        short arraySize = in.readShort();
        write(arraySize);
        copyNBytes(arraySize * 4);
        break;
      case 0x35: // Block Change
        write(packetId);
        copyNBytes(11);
        break;
      case 0x36: // ???
        write(packetId);
        copyNBytes(12);
        break;
      case 0x3c: // Explosion
        write(packetId);
        copyNBytes(28);
        int recordCount = in.readInt();
        write(recordCount);
        copyNBytes(recordCount * 3);
        break;
      case 0x64:
        write(packetId);
        write(in.readByte());
        write(in.readByte());
        write(in.readUTF());
        write(in.readByte());
        break;
      case 0x65:
        write(packetId);
        write(in.readByte());
        break;
      case 0x66: // Inventory Item Move
        byte typeFrom = in.readByte();
        short slotFrom = in.readShort();
        byte typeTo = in.readByte();
        short slotTo = in.readShort();
        if ((typeFrom < 0 && typeTo < 0) || player.getGroupId() >= 0) {
          write(packetId);
          write(typeFrom);
          write(slotFrom);
          write(typeTo);
          write(slotTo);
          short moveItem = in.readShort();
          write(moveItem);
          if (moveItem != -1) {
            write(in.readByte());
            write(in.readShort());
          }
        }
        else {
          short moveItem = in.readShort();
          if (moveItem != -1) {
            in.readByte();
            in.readShort();
          }
        }
        break;
      case 0x67: // Inventory Item Update
        byte type67 = in.readByte();
        if (type67 < 0 || player.getGroupId() >= 0) {
          write(packetId);
          short slot = in.readShort();
          write(type67);
          write(slot);
          short setItem = in.readShort();
          write(setItem);
          if (setItem != -1) {
            write(in.readByte());
            write(in.readShort());
          }
        }
        else {
          in.readShort();
          short setItem = in.readShort();
          if (setItem != -1) {
            in.readByte();
            in.readShort();
          }
        }
        break;
      case 0x68: // Inventory
        byte type = in.readByte();
        if (type < 0 || player.getGroupId() >= 0) {
          write(packetId);
          write(type);
          short count = in.readShort();
          write(count);
          for (int c = 0; c < count; ++c) {
            short item = in.readShort();
            write(item);

            if (item != -1) {
              write(in.readByte());
              write(in.readShort());
            }
          }
        }
        else {
          short count = in.readShort();
          for (int c = 0; c < count; ++c) {
            short item = in.readShort();
            if (item != -1) {
              in.readByte();
              in.readShort();
            }
          }
        }
        break;
      case 0x69:
        write(packetId);
        write(in.readByte());
        write(in.readShort());
        write(in.readShort());
        break;
      case 0x6a:
        write(packetId);
        write(in.readByte());
        write(in.readShort());
        write(in.readByte());
        break;
      case (byte) 0x82: // Update Sign
        write(packetId);
        write(in.readInt());
        write(in.readShort());
        write(in.readInt());
        write(in.readUTF());
        write(in.readUTF());
        write(in.readUTF());
        write(in.readUTF());
        break;
      case (byte) 0xff: // Disconnect/Kick
        write(packetId);
        String reason = in.readUTF();
        write(reason);
        if (reason.startsWith("Took too long")) {
          server.addRobot(player);
        }
        player.close();
        break;
      default:
        if (EXPENSIVE_DEBUG_LOGGING) {
          while (true) {
            skipNBytes(1);
            flushAll();
          }
        }
        else {
          throw new IOException("Unable to parse unknown " + streamType
              + " packet 0x" + Integer.toHexString(packetId) + " for player "
              + player.getName());
        }
    }
    packetFinished();
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
      write(x);
      write(y);
      write(stance);
      write(z);
      copyNBytes(1);
    }
    else {
      copyNBytes(33);
    }
  }

  private void copyUnknownBlob() throws IOException {
    byte unknown = in.readByte();
    write(unknown);

    while (unknown != 0x7f) {
      int type = (unknown & 0xE0) >> 5;

      switch (type) {
        case 0:
          write(in.readByte());
          break;
        case 1:
          write(in.readShort());
          break;
        case 2:
          write(in.readInt());
          break;
        case 3:
          write(in.readFloat());
          break;
        case 4:
          write(in.readUTF());
          break;
        case 5:
          write(in.readShort());
          write(in.readByte());
          write(in.readShort());
      }

      unknown = in.readByte();
      write(unknown);
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
    int overflow = bytes / buffer.length;
    for (int c = 0; c < overflow; ++c) {
      in.readFully(buffer, 0, buffer.length);
    }
    in.readFully(buffer, 0, bytes % buffer.length);
  }

  private void copyNBytes(int bytes) throws IOException {
    int overflow = bytes / buffer.length;
    for (int c = 0; c < overflow; ++c) {
      in.readFully(buffer, 0, buffer.length);
      out.write(buffer, 0, buffer.length);
    }
    in.readFully(buffer, 0, bytes % buffer.length);
    out.write(buffer, 0, bytes % buffer.length);
  }

  private void kick(String reason) throws IOException {
    write((byte) 0xff);
    write(reason);
    packetFinished();
  }

  private void sendMessage(String message) throws IOException {
    write(0x03);
    write(message);
    packetFinished();
  }

  private void packetFinished() throws IOException {
    if (EXPENSIVE_DEBUG_LOGGING) {
      inputDumper.packetFinished();
      outputDumper.packetFinished();
    }
  }

  private void flushAll() throws IOException {
    try {
      ((OutputStream) out).flush();
    }
    finally {
      if (EXPENSIVE_DEBUG_LOGGING) {
        inputDumper.flush();
      }
    }
  }

  private final class Tunneler extends Thread {
    @Override
    public void run() {
      try {
        while (run) {
          lastRead = System.currentTimeMillis();

          try {
            handlePacket();

            if (isServerTunnel) {
              while (player.hasMessages()) {
                sendMessage(player.getMessage());
              }
            }

            flushAll();
          }
          catch (IOException e) {
            if (run && !player.isRobot()) {
              e.printStackTrace();
              System.out.println(streamType + " error handling traffic for "
                  + player.getIPAddress());
            }
            break;
          }
        }

        try {
          if (player.isKicked()) {
            kick(player.getKickMsg());
          }
          flushAll();
        }
        catch (IOException e) {
        }
      }
      finally {
        if (EXPENSIVE_DEBUG_LOGGING) {
          inputDumper.cleanup();
          outputDumper.cleanup();
        }
      }
    }
  }
}
