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

import static simpleserver.lang.Translations.t;
import static simpleserver.util.Util.print;
import static simpleserver.util.Util.println;

import java.io.*;
import java.nio.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.bytecode.ByteArray;
import simpleserver.*;
import simpleserver.Authenticator.AuthRequest;
import simpleserver.Coordinate.Dimension;
import simpleserver.command.PlayerListCommand;
import simpleserver.config.data.Chests.Chest;
import simpleserver.config.xml.Config.BlockPermission;
import simpleserver.message.Message;
import simpleserver.message.MessagePacket;

public class StreamTunnel {
  private static final boolean EXPENSIVE_DEBUG_LOGGING = Boolean.getBoolean("EXPENSIVE_DEBUG_LOGGING");
  private static final int IDLE_TIME = 30000;
  private static final int BUFFER_SIZE = 1024;
  private static final byte BLOCK_DESTROYED_STATUS = 2;
  private static final Pattern MESSAGE_PATTERN = Pattern.compile("^<([^>]+)> (.*)$");
  private static final Pattern COLOR_PATTERN = Pattern.compile("\u00a7[0-9a-z]");
  private static final Pattern JOIN_PATTERN = Pattern.compile("\u00a7.((\\d|\\w|\\u00a7)*) (joined|left) the game.");
  private static final String CONSOLE_CHAT_PATTERN = "\\[Server:.*\\]";
  private static final int MESSAGE_SIZE = 360;
  private static final int MAXIMUM_MESSAGE_SIZE = 119;
  private static final int STATE_HANDSHAKE = 0;
  private static final int STATE_STATUS = 1;
  private static final int STATE_LOGIN = 2;
  private static final int STATE_PLAY = 3;

  private final boolean isServerTunnel;
  private final String streamType;
  private final Player player;
  private final Server server;
  private final byte[] buffer;
  private final Tunneler tunneler;

  private DataInput in;
  private DataOutput out;
  private InputStream inputStream;
  private OutputStream outputStream;
  private StreamDumper inputDumper;
  private StreamDumper outputDumper;

  private boolean inGame = false;

  private volatile long lastRead;
  private volatile boolean run = true;
  private Byte lastPacket;
  private char commandPrefix;

  private int state = 0;
  private ByteBuffer incoming = null;
  private ByteBuffer outgoing = null;

  public StreamTunnel(InputStream in, OutputStream out, boolean isServerTunnel,
                      Player player) {
    this.isServerTunnel = isServerTunnel;
    if (isServerTunnel) {
      streamType = "ServerStream";
    } else {
      streamType = "PlayerStream";
    }

    this.player = player;
    server = player.getServer();
    commandPrefix = server.options.getBoolean("useSlashes") ? '/' : '!';

    inputStream = in;
    outputStream = out;
    DataInputStream dIn = new DataInputStream(in);
    DataOutputStream dOut = new DataOutputStream(out);
    if (EXPENSIVE_DEBUG_LOGGING) {
      try {
        OutputStream dump = new FileOutputStream(streamType + "Input.debug");
        InputStreamDumper dumper = new InputStreamDumper(dIn, dump);
        inputDumper = dumper;
        this.in = dumper;
      } catch (FileNotFoundException e) {
        System.out.println("Unable to open input debug dump!");
        throw new RuntimeException(e);
      }

      try {
        OutputStream dump = new FileOutputStream(streamType + "Output.debug");
        OutputStreamDumper dumper = new OutputStreamDumper(dOut, dump);
        outputDumper = dumper;
        this.out = dumper;
      } catch (FileNotFoundException e) {
        System.out.println("Unable to open output debug dump!");
        throw new RuntimeException(e);
      }
    } else {
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
    int length = decodeVarInt();

    // read length into byte[], copy into ByteBuffer
    byte[] buf = new byte[length];
    in.readFully(buf, 0, length);
    incoming = ByteBuffer.wrap(buf);
    outgoing = ByteBuffer.allocate(BUFFER_SIZE);

    Byte packetId  = (byte) decodeVarInt();

    System.out.println("state:" + state + (isServerTunnel ? " server " : " client ") +
    String.format("%02x", packetId) + " length: " + length);

    if (state == STATE_HANDSHAKE) {
      if (!isServerTunnel) {
        add(packetId);
        copyVarInt();
        add(readUTF8().getBytes());
        add(readUnsignedShort());
        state = decodeVarInt();
        addVarInt(state);
      }

    } else if (state == STATE_STATUS) {
        switch(packetId) {
          case 0x00: // JSON Response
            add(packetId);

            if (isServerTunnel) {
              add(readUTF8().getBytes());
            }
            break;

          case 0x01: // Ping
            add(packetId);
            add(incoming.getLong());
            break;
      }

    } else if (state == STATE_LOGIN) {
      // @todo add login packets

    } else if (state == STATE_PLAY) {
      // @todo add play packets

    } else {
      throw new ArrayIndexOutOfBoundsException();
    }

    packetFinished();
    lastPacket = (packetId == 0x00) ? lastPacket : packetId;
  }

//  private void copyItem() throws IOException {
//    if (write(in.readShort()) > 0) {
//      write(in.readByte());
//      write(in.readShort());
//      short length;
//      if ((length = write(in.readShort())) > 0) {
//        copyNBytes(length);
//      }
//    }
//  }
//
//  private void skipItem() throws IOException {
//    if (in.readShort() > 0) {
//      in.readByte();
//      in.readShort();
//      short length;
//      if ((length = in.readShort()) > 0) {
//        skipNBytes(length);
//      }
//    }
//  }
//
//  private long copyVLC() throws IOException {
//    long value = 0;
//    int shift = 0;
//    while (true) {
//      int i = write(in.readByte());
//      value |= (i & 0x7F) << shift;
//      if ((i & 0x80) == 0) {
//        break;
//      }
//      shift += 7;
//    }
//    return value;
//  }

  private String readUTF8() throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int length = decodeVarInt();

    for (int i = 0; i < length; i++) {
      buffer.write((incoming != null) ? incoming.get() : in.readByte());
    }
    return new String(buffer.toByteArray(), "UTF-8");
  }

  private void copyVarInt() throws IOException {
   outgoing.put(encodeVarInt(decodeVarInt()));
  }

  private void addVarInt(int i) throws IOException {
    outgoing.put(encodeVarInt(i));
  }

  private void add(int i) throws IOException {
    outgoing.putInt(i);
  }

  private void add(byte[] b) {
    outgoing.put(b);
  }

  private void add(byte b) {
    outgoing.put(b);
  }

  private void add(long l) {
    outgoing.putLong(l);
  }

  private byte[] encodeVarInt(int value) throws IOException {
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

  private int decodeVarInt() throws IOException {
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

  private int readUnsignedShort() {
    return (incoming.getShort() & 0xFFFF);
  }

//  private void lockChest(Coordinate coordinate) {
//    Chest adjacentChest = server.data.chests.adjacentChest(coordinate);
//    if (player.isAttemptLock() || adjacentChest != null && !adjacentChest.isOpen()) {
//      if (adjacentChest != null && !adjacentChest.isOpen()) {
//        server.data.chests.giveLock(adjacentChest.owner, coordinate, adjacentChest.name);
//      } else {
//        if (adjacentChest != null) {
//          adjacentChest.lock(player);
//          adjacentChest.name = player.nextChestName();
//        }
//        server.data.chests.giveLock(player, coordinate, player.nextChestName());
//      }
//      player.setAttemptedAction(null);
//      player.addTMessage(Color.GRAY, "This chest is now locked.");
//    } else if (!server.data.chests.isChest(coordinate)) {
//      server.data.chests.addOpenChest(coordinate);
//    }
//    server.data.save();
//  }
//
//  private void copyPlayerLocation() throws IOException {
//    double x = in.readDouble();
//    double y = in.readDouble();
//    double stance = in.readDouble();
//    double z = in.readDouble();
//    player.position.updatePosition(x, y, z, stance);
//    if (server.options.getBoolean("enableEvents")) {
//      player.checkLocationEvents();
//    }
//    write(x);
//    write(y);
//    write(stance);
//    write(z);
//  }
//
//  private void copyPlayerLook() throws IOException {
//    float yaw = in.readFloat();
//    float pitch = in.readFloat();
//    player.position.updateLook(yaw, pitch);
//    write(yaw);
//    write(pitch);
//  }
//
//  private void copyUnknownBlob() throws IOException {
//    byte item = in.readByte();
//    write(item);
//
//    while (item != 0x7f) {
//      int type = (item & 0xE0) >> 5;
//
//      switch (type) {
//        case 0:
//          write(in.readByte());
//          break;
//        case 1:
//          write(in.readShort());
//          break;
//        case 2:
//          write(in.readInt());
//          break;
//        case 3:
//          write(in.readFloat());
//          break;
//        case 4:
//          write(readUTF8());
//          break;
//        case 5:
//          copyItem();
//          break;
//        case 6:
//          write(in.readInt());
//          write(in.readInt());
//          write(in.readInt());
//      }
//
//      item = in.readByte();
//      write(item);
//    }
//  }
//
//  private void skipUnknownBlob() throws IOException {
//    byte item = in.readByte();
//
//    while (item != 0x7f) {
//      int type = (item & 0xE0) >> 5;
//
//      switch (type) {
//        case 0:
//          in.readByte();
//          break;
//        case 1:
//          in.readShort();
//          break;
//        case 2:
//          in.readInt();
//          break;
//        case 3:
//          in.readFloat();
//          break;
//        case 4:
//          readUTF8();
//          break;
//        case 5:
//          skipItem();
//          break;
//        case 6:
//          in.readInt();
//          in.readInt();
//          in.readInt();
//      }
//
//      item = in.readByte();
//    }
//  }

  private byte write(byte b) throws IOException {
    out.writeByte(b);
    return b;
  }

  private byte[] write(byte[] b) throws IOException {
    out.write(b);
    return b;
  }

  private short write(short s) throws IOException {
    out.writeShort(s);
    return s;
  }

  private int write(int i) throws IOException {
    out.writeInt(i);
    return i;
  }

  private long write(long l) throws IOException {
    out.writeLong(l);
    return l;
  }

  private float write(float f) throws IOException {
    out.writeFloat(f);
    return f;
  }

  private double write(double d) throws IOException {
    out.writeDouble(d);
    return d;
  }

  private String write(String s) throws IOException {
    write((short) s.length());
    out.writeChars(s);
    return s;
  }

  private boolean write(boolean b) throws IOException {
    out.writeBoolean(b);
    return b;
  }

//  private void skipNBytes(int bytes) throws IOException {
//    int overflow = bytes / buffer.length;
//    for (int c = 0; c < overflow; ++c) {
//      in.readFully(buffer, 0, buffer.length);
//    }
//    in.readFully(buffer, 0, bytes % buffer.length);
//  }
//
//  private void copyNBytes(int bytes) throws IOException {
//    int overflow = bytes / buffer.length;
//    for (int c = 0; c < overflow; ++c) {
//      in.readFully(buffer, 0, buffer.length);
//      out.write(buffer, 0, buffer.length);
//    }
//    in.readFully(buffer, 0, bytes % buffer.length);
//    out.write(buffer, 0, bytes % buffer.length);
//  }
//
  private void kick(String reason) throws IOException {
    write((byte) 0xff);
    write(reason);
    packetFinished();
  }
//
//  private String getLastColorCode(String message) {
//    String colorCode = "";
//    int lastIndex = message.lastIndexOf('\u00a7');
//    if (lastIndex != -1 && lastIndex + 1 < message.length()) {
//      colorCode = message.substring(lastIndex, lastIndex + 2);
//    }
//
//    return colorCode;
//  }
//
  private void sendMessage(String message) throws IOException {
    if (message.length() > 0) {
      int end = message.length();
      if (message.charAt(end - 1) == '\u00a7') {
        end--;
      }
      sendMessagePacket(message.substring(0, end));
    }
  }

  private void sendMessagePacket(String message) throws IOException {
    if (message.length() > 0) {
      write((byte) 0x03);
      write(message);
      packetFinished();
    }
  }

  private void packetFinished() throws IOException {
    if (EXPENSIVE_DEBUG_LOGGING) {
      inputDumper.packetFinished();
      outputDumper.packetFinished();
    }

    // reset our incoming buffer, and write the outgoing one
    incoming = null;
    int size = outgoing.position();
    outgoing.limit(size);
    outgoing.rewind();

    byte[] tmp = new byte[size];
    outgoing.get(tmp);

    out.write(size);
    out.write(tmp);
  }

  private void flushAll() throws IOException {
    try {
      ((OutputStream) out).flush();
    } finally {
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
            } else {
              while (player.hasForwardMessages()) {
                sendMessage(player.getForwardMessage());
              }
            }

            flushAll();
          } catch (IOException e) {
            if (run && !player.isRobot()) {
              println(e);
              print(streamType
                  + " error handling traffic for " + player.getIPAddress());
              if (lastPacket != null) {
                System.out.print(" (" + Integer.toHexString(lastPacket) + ")");
              }
              System.out.println();
            }
            break;
          }
        }

        try {
          if (player.isKicked()) {
            kick(player.getKickMsg());
          }
          flushAll();
        } catch (IOException e) {
        }
      } finally {
        if (EXPENSIVE_DEBUG_LOGGING) {
          inputDumper.cleanup();
          outputDumper.cleanup();
        }
      }
    }
  }
}
