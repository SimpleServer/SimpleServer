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
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import simpleserver.Authenticator.AuthRequest;
import simpleserver.Color;
import simpleserver.Coordinate;
import simpleserver.Coordinate.Dimension;
import simpleserver.Player;
import simpleserver.Server;
import simpleserver.command.PlayerListCommand;
import simpleserver.config.data.Chests.Chest;
import simpleserver.config.xml.Config.BlockPermission;

public class StreamTunnel {
  private static final boolean EXPENSIVE_DEBUG_LOGGING = Boolean.getBoolean("EXPENSIVE_DEBUG_LOGGING");
  private static final int IDLE_TIME = 30000;
  private static final int BUFFER_SIZE = 1024;
  private static final byte BLOCK_DESTROYED_STATUS = 2;
  private static final Pattern MESSAGE_PATTERN = Pattern.compile("^<([^>]+)> (.*)$");
  private static final Pattern COLOR_PATTERN = Pattern.compile("\u00a7[0-9a-f]");
  private static final Pattern JOIN_PATTERN = Pattern.compile("\u00a7.((\\d|\\w)*) (joined|left) the game.");
  private static final String CONSOLE_CHAT_PATTERN = "\\(CONSOLE:.*\\)";
  private static final int MESSAGE_SIZE = 60;
  private static final int MAXIMUM_MESSAGE_SIZE = 119;
  public static final HashSet<Short> ENCHANTABLE = new HashSet<Short>();

  static {
    ENCHANTABLE.add((short) 0x15a); // Fishing rod
    ENCHANTABLE.add((short) 0x167); // Shears
    ENCHANTABLE.add((short) 0x105); // Bow
    // Tools
    for (short id = 256; id <= 259; id++) {
      ENCHANTABLE.add(id);
    }
    for (short id = 267; id <= 279; id++) {
      ENCHANTABLE.add(id);
    }
    for (short id = 283; id <= 286; id++) {
      ENCHANTABLE.add(id);
    }
    for (short id = 290; id <= 294; id++) {
      ENCHANTABLE.add(id);
    }
    // Armour
    for (short id = 298; id <= 317; id++) {
      ENCHANTABLE.add(id);
    }
  };

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

  private boolean inGame = false;

  private volatile long lastRead;
  private volatile boolean run = true;
  private Byte lastPacket;
  private char commandPrefix;

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

    DataInputStream dIn = new DataInputStream(new BufferedInputStream(in));
    DataOutputStream dOut = new DataOutputStream(new BufferedOutputStream(out));
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
    Byte packetId = in.readByte();
    int x;
    byte y;
    int z;
    byte dimension;
    Coordinate coordinate;
    switch (packetId) {
      case 0x00: // Keep Alive
        write(packetId);
        write(in.readInt()); // random number that is returned from server
        break;
      case 0x01: // Login Request/Response
        write(packetId);
        if (isServerTunnel) {
          if (server.authenticator.useCustAuth(player)
              && !server.authenticator.onlineAuthenticate(player)) {
            player.kick(t("%s Failed to login: User not premium", "[CustAuth]"));
            break;
          }
          player.setEntityId(write(in.readInt()));
          write(readUTF16());
          server.setMapSeed(write(in.readLong()));
        } else {
          write(in.readInt());
          readUTF16(); // and throw away
          write(player.getName());
          write(in.readLong());
        }

        write(readUTF16());
        write(in.readInt());

        dimension = in.readByte();
        if (isServerTunnel) {
          player.setDimension(Dimension.get(dimension));
        }
        write(dimension);
        write(in.readByte());
        write(in.readByte());
        if (isServerTunnel) {
          in.readByte();
          write((byte) server.config.properties.getInt("maxPlayers"));
        } else {
          write(in.readByte());
        }

        break;
      case 0x02: // Handshake
        String name = readUTF16();
        boolean nameSet = false;

        if (isServerTunnel) {
          if (!server.authenticator.useCustAuth(player)) {
            name = "-";
          } else if (!server.authenticator.vanillaOnlineMode()) {
            name = player.getConnectionHash();
          }
        } else {
          if (name.equals("Player") || !server.authenticator.isMinecraftUp) {
            AuthRequest req = server.authenticator.getAuthRequest(player.getIPAddress());
            if (req != null) {
              name = req.playerName;
              nameSet = server.authenticator.completeLogin(req, player);
            }
            if (req == null || !nameSet) {
              if (!name.equals("Player")) {
                player.addTMessage(Color.RED, "Login verification failed.");
                player.addTMessage(Color.RED, "You were logged in as guest.");
              }
              name = server.authenticator.getFreeGuestName();
              player.setGuest(true);
              nameSet = player.setName(name);
            }
          } else {
            nameSet = player.setName(name);
            if (nameSet) {
              player.updateRealName(name);
            }
          }
        }

        if (player.isGuest() && !server.authenticator.allowGuestJoin()) {
          player.kick(t("Failed to login: User not authenticated"));
          nameSet = false;
        }

        if (isServerTunnel || nameSet) {
          tunneler.setName(streamType + "-" + player.getName());
          write(packetId);
          write(name);
        }

        break;
      case 0x03: // Chat Message
        String message = readUTF16();

        Matcher joinMatcher = JOIN_PATTERN.matcher(message);
        if (isServerTunnel && joinMatcher.find()) {
          if (server.bots.ninja(joinMatcher.group(1))) {
            break;
          }
          if (message.contains("join")) {
            player.addTMessage(Color.YELLOW, "%s joined the game.", joinMatcher.group(1));
          } else {
            player.addTMessage(Color.YELLOW, "%s left the game.", joinMatcher.group(1));
          }
          break;
        }
        if (isServerTunnel && server.config.properties.getBoolean("useMsgFormats")) {
          if (server.config.properties.getBoolean("forwardChat") && server.getMessager().wasForwarded(message)) {
            break;
          }

          Matcher colorMatcher = COLOR_PATTERN.matcher(message);
          String cleanMessage = colorMatcher.replaceAll("");

          Matcher messageMatcher = MESSAGE_PATTERN.matcher(cleanMessage);
          if (messageMatcher.find()) {

          } else if (cleanMessage.matches(CONSOLE_CHAT_PATTERN) && !server.config.properties.getBoolean("chatConsoleToOps")) {
            break;
          }

          if (server.config.properties.getBoolean("msgWrap")) {
            sendMessage(message);
          } else {
            if (message.length() > MAXIMUM_MESSAGE_SIZE) {
              message = message.substring(0, MAXIMUM_MESSAGE_SIZE);
            }
            write(packetId);
            write(message);
          }
        } else if (!isServerTunnel) {

          if (player.isMuted() && !message.startsWith("/")
              && !message.startsWith("!")) {
            player.addTMessage(Color.RED, "You are muted! You may not send messages to all players.");
            break;
          }

          if (message.charAt(0) == commandPrefix) {
            message = player.parseCommand(message);
            if (message == null) {
              break;
            }
            write(packetId);
            write(message);
            return;
          }

          player.sendMessage(message);
        }
        break;

      case 0x04: // Time Update
        write(packetId);
        long time = in.readLong();
        server.setTime(time);
        write(time);
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
      case 0x07: // Use Entity
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
        copyNBytes(1);
        break;
      case 0x08: // Update Health
        write(packetId);
        player.updateHealth(write(in.readShort()));
        player.getHealth();
        write(in.readShort());
        write(in.readFloat());
        break;
      case 0x09: // Respawn
        write(packetId);
        player.setDimension(Dimension.get(write(in.readByte())));
        write(in.readByte());
        write(in.readByte());
        write(in.readShort());
        write(in.readLong());
        write(readUTF16());
        break;
      case 0x0a: // Player
        write(packetId);
        copyNBytes(1);
        if (!inGame && !isServerTunnel) {
          player.sendMOTD();

          if (server.config.properties.getBoolean("showListOnConnect")) {
            // display player list if enabled in config
            player.execute(PlayerListCommand.class);
          }

          inGame = true;
        }
        break;
      case 0x0b: // Player Position
        write(packetId);
        copyPlayerLocation();
        copyNBytes(1);
        break;
      case 0x0c: // Player Look
        write(packetId);
        copyPlayerLook();
        copyNBytes(1);
        break;
      case 0x0d: // Player Position & Look
        write(packetId);
        copyPlayerLocation();
        copyPlayerLook();
        copyNBytes(1);
        break;
      case 0x0e: // Player Digging
        if (!isServerTunnel) {
          byte status = in.readByte();
          x = in.readInt();
          y = in.readByte();
          z = in.readInt();
          byte face = in.readByte();

          coordinate = new Coordinate(x, y, z, player);

          if (!player.getGroup().ignoreAreas) {
            BlockPermission perm = server.config.blockPermission(player, coordinate);

            if (!perm.use && status == 0) {
              player.addTMessage(Color.RED, "You can not use this block here!");
              break;
            }
            if (!perm.destroy && status == BLOCK_DESTROYED_STATUS) {
              player.addTMessage(Color.RED, "You can not destroy this block!");
              break;
            }
          }

          boolean locked = server.data.chests.isLocked(coordinate);

          if (!locked || player.ignoresChestLocks() || server.data.chests.canOpen(player, coordinate)) {
            if (locked && status == BLOCK_DESTROYED_STATUS) {
              server.data.chests.releaseLock(coordinate);
              server.data.save();
            }

            write(packetId);
            write(status);
            write(x);
            write(y);
            write(z);
            write(face);

            if (player.instantDestroyEnabled()) {
              packetFinished();
              write(packetId);
              write(BLOCK_DESTROYED_STATUS);
              write(x);
              write(y);
              write(z);
              write(face);
            }

            if (status == BLOCK_DESTROYED_STATUS) {
              player.destroyedBlock();
            }
          }
        } else {
          write(packetId);
          copyNBytes(11);
        }
        break;
      case 0x0f: // Player Block Placement
        x = in.readInt();
        y = in.readByte();
        z = in.readInt();
        coordinate = new Coordinate(x, y, z, player);
        final byte direction = in.readByte();
        final short dropItem = in.readShort();
        byte itemCount = 0;
        short uses = 0;
        byte[] data = null;
        if (dropItem != -1) {
          itemCount = in.readByte();
          uses = in.readShort();
          if (ENCHANTABLE.contains(dropItem)) {
            short dataLength = in.readShort();
            if (dataLength != -1) {
              data = new byte[dataLength];
              in.readFully(data);
            }
          }
        }

        boolean writePacket = true;
        boolean drop = false;

        BlockPermission perm = server.config.blockPermission(player, coordinate, dropItem);

        if (isServerTunnel || server.data.chests.isChest(coordinate)) {
          // continue
        } else if (!player.getGroup().ignoreAreas && ((dropItem != -1 && !perm.place) || !perm.use)) {
          if (!perm.use) {
            player.addTMessage(Color.RED, "You can not use this block here!");
          } else {
            player.addTMessage(Color.RED, "You can not place this block here!");
          }

          writePacket = false;
          drop = true;
        } else if (dropItem == 54) {
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

          Coordinate targetBlock = new Coordinate(xPosition, yPosition, zPosition, player);

          Chest adjacentChest = server.data.chests.adjacentChest(targetBlock);

          if (adjacentChest != null && !adjacentChest.isOpen() && !adjacentChest.ownedBy(player)) {
            player.addTMessage(Color.RED, "The adjacent chest is locked!");
            writePacket = false;
            drop = true;
          } else {
            player.placingChest(targetBlock);
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
            if (ENCHANTABLE.contains(dropItem)) {
              if (data != null) {
                write((short) data.length);
                out.write(data);
              } else {
                write((short) -1);
              }
            }

            if (dropItem <= 94 && direction >= 0) {
              player.placedBlock();
            }
          }

          player.openingChest(coordinate);

        } else if (drop) {
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
      case 0x11: // Use Bed
        write(packetId);
        copyNBytes(14);
        break;
      case 0x12: // Animation
        write(packetId);
        copyNBytes(5);
        break;
      case 0x13: // Entity Action
        write(packetId);
        write(in.readInt());
        write(in.readByte());
        break;
      case 0x14: // Named Entity Spawn
        int eid = in.readInt();
        name = readUTF16();
        if (!server.bots.ninja(name)) {
          write(packetId);
          write(eid);
          write(name);
          copyNBytes(16);
        } else {
          skipNBytes(16);
        }
        break;
      case 0x15: // Pickup Spawn
        write(packetId);
        copyNBytes(24);
        break;
      case 0x16: // Collect Item
        write(packetId);
        copyNBytes(8);
        break;
      case 0x17: // Add Object/Vehicle
        write(packetId);
        write(in.readInt());
        write(in.readByte());
        write(in.readInt());
        write(in.readInt());
        write(in.readInt());
        int flag = in.readInt();
        write(flag);
        if (flag > 0) {
          write(in.readShort());
          write(in.readShort());
          write(in.readShort());
        }
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
        write(in.readByte());

        copyUnknownBlob();
        break;
      case 0x19: // Entity: Painting
        write(packetId);
        write(in.readInt());
        write(readUTF16());
        write(in.readInt());
        write(in.readInt());
        write(in.readInt());
        write(in.readInt());
        break;
      case 0x1a: // Experience Orb
        write(packetId);
        write(in.readInt());
        write(in.readInt());
        write(in.readInt());
        write(in.readInt());
        write(in.readShort());
        break;
      case 0x1c: // Entity Velocity
        write(packetId);
        copyNBytes(10);
        break;
      case 0x1d: // Destroy Entity
        write(packetId);
        copyNBytes(4);
        break;
      case 0x1e: // Entity
        write(packetId);
        copyNBytes(4);
        break;
      case 0x1f: // Entity Relative Move
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
      case 0x23: // ???, added in 12w03a
        write(in.readInt());
        write(in.readByte());
        break;
      case 0x26: // Entity Status
        write(packetId);
        copyNBytes(5);
        break;
      case 0x27: // Attach Entity
        write(packetId);
        copyNBytes(8);
        break;
      case 0x28: // Entity Metadata
        write(packetId);
        write(in.readInt());

        copyUnknownBlob();
        break;
      case 0x29: // Entity Effect
        write(packetId);
        write(in.readInt());
        write(in.readByte());
        write(in.readByte());
        write(in.readShort());
        break;
      case 0x2a: // Remove Entity Effect
        write(packetId);
        write(in.readInt());
        write(in.readByte());
        break;
      case 0x2b: // Experience
        write(packetId);
        player.updateExperience(write(in.readFloat()), write(in.readShort()), write(in.readShort()));
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
        x = in.readInt();
        y = in.readByte();
        z = in.readInt();
        byte blockType = in.readByte();
        byte metadata = in.readByte();
        coordinate = new Coordinate(x, y, z, player);

        if (blockType == 54 && player.placedChest(coordinate)) {
          lockChest(coordinate);
          player.placingChest(null);
        }

        write(x);
        write(y);
        write(z);
        write(blockType);
        write(metadata);

        break;
      case 0x36: // Block Action
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
      case 0x3d: // Sound/Particle Effect
        write(packetId);
        write(in.readInt());
        write(in.readInt());
        write(in.readByte());
        write(in.readInt());
        write(in.readInt());
        break;
      case 0x46: // New/Invalid State
        write(packetId);
        write(in.readByte());
        write(in.readByte());
        break;
      case 0x47: // Thunderbolt
        write(packetId);
        copyNBytes(17);
        break;
      case 0x64: // Open Window
        boolean allow = true;
        byte id = in.readByte();
        byte invtype = in.readByte();
        String typeString = readUTF16();
        byte unknownByte = in.readByte();
        if (invtype == 0) {
          Chest adjacent = server.data.chests.adjacentChest(player.openedChest());
          if (!server.data.chests.isChest(player.openedChest())) {
            if (adjacent == null) {
              server.data.chests.addOpenChest(player.openedChest());
            } else {
              server.data.chests.giveLock(adjacent.owner, player.openedChest(), adjacent.name);
            }
            server.data.save();
          }
          if (!player.getGroup().ignoreAreas && (!server.config.blockPermission(player, player.openedChest()).chest || (adjacent != null && !server.config.blockPermission(player, adjacent.coordinate).chest))) {
            player.addTMessage(Color.RED, "You can't use chests here");
            allow = false;
          } else if (server.data.chests.canOpen(player, player.openedChest()) || player.ignoresChestLocks()) {
            if (server.data.chests.isLocked(player.openedChest())) {
              if (player.isAttemptingUnlock()) {
                server.data.chests.unlock(player.openedChest());
                server.data.save();
                player.setAttemptedAction(null);
                player.addTMessage(Color.RED, "This chest is no longer locked!");
                typeString = t("Open Chest");
              } else {
                typeString = server.data.chests.chestName(player.openedChest());
              }
            } else {
              typeString = t("Open Chest");
              if (player.isAttemptLock()) {
                lockChest(player.openedChest());
                typeString = (player.nextChestName() == null) ? t("Locked Chest") : player.nextChestName();
              }
            }

          } else {
            player.addTMessage(Color.RED, "This chest is locked!");
            allow = false;
          }
        }
        if (!allow) {
          write((byte) 0x65);
          write(id);
        } else {
          write(packetId);
          write(id);
          write(invtype);
          write(typeString);
          write(unknownByte);
        }
        break;
      case 0x65: // Close Window
        write(packetId);
        write(in.readByte());
        break;
      case 0x66: // Window Click
        write(packetId);
        write(in.readByte());
        write(in.readShort());
        write(in.readByte());
        write(in.readShort());
        write(in.readBoolean());
        copyItem();
        break;
      case 0x67: // Set Slot
        write(packetId);
        write(in.readByte());
        write(in.readShort());
        copyItem();
        break;
      case 0x68: // Window Items
        write(packetId);
        write(in.readByte());
        short count = write(in.readShort());
        for (int c = 0; c < count; ++c) {
          copyItem();
        }
        break;
      case 0x69: // Update Window Property
        write(packetId);
        write(in.readByte());
        write(in.readShort());
        write(in.readShort());
        break;
      case 0x6a: // Transaction
        write(packetId);
        write(in.readByte());
        write(in.readShort());
        write(in.readByte());
        break;
      case 0x6b: // Creative Inventory Action
        write(packetId);
        write(in.readShort());
        copyItem();
        break;
      case 0x6c: // Enchant Item
        write(packetId);
        write(in.readByte());
        write(in.readByte());
        break;
      case (byte) 0x82: // Update Sign
        write(packetId);
        write(in.readInt());
        write(in.readShort());
        write(in.readInt());
        write(readUTF16());
        write(readUTF16());
        write(readUTF16());
        write(readUTF16());
        break;
      case (byte) 0x83: // Item Data
        write(packetId);
        write(in.readShort());
        write(in.readShort());
        byte length = in.readByte();
        write(length);
        copyNBytes(0xff & length);
        break;
      case (byte) 0xc3: // BukkitContrib
        write(packetId);
        write(in.readInt());
        copyNBytes(write(in.readInt()));
        break;
      case (byte) 0xc8: // Increment Statistic
        write(packetId);
        copyNBytes(5);
        break;
      case (byte) 0xc9: // Player List Item
        write(packetId);
        write(readUTF16());
        write(in.readByte());
        write(in.readShort());
        break;
      case (byte) 0xd3: // Red Power (mod by Eloraam)
        write(packetId);
        copyNBytes(1);
        copyVLC();
        copyVLC();
        copyVLC();
        copyNBytes((int) copyVLC());
        break;
      case (byte) 0xe6: // ModLoaderMP by SDK
        write(packetId);
        write(in.readInt()); // mod
        write(in.readInt()); // packet id
        copyNBytes(write(in.readInt()) * 4); // ints
        copyNBytes(write(in.readInt()) * 4); // floats
        int sizeString = write(in.readInt()); // strings
        for (int i = 0; i < sizeString; i++) {
          copyNBytes(write(in.readInt()));
        }
        break;
      case (byte) 0xfa: // Plugin Message
        write(packetId);
        write(readUTF16());
        short arrayLength = in.readShort();
        write(arrayLength);
        copyNBytes(0xff & arrayLength);
        break;
      case (byte) 0xfe: // Server List Ping
        write(packetId);
        break;
      case (byte) 0xff: // Disconnect/Kick
        // server list answer 'serverText§playerOnline§maxPlayers'
        write(packetId);
        String reason = readUTF16();
        if (reason.contains("\u00a7")) {
          reason = String.format("%s\u00a7%s\u00a7%s",
                                 server.config.properties.get("serverDescription"),
                                 server.playerList.size(),
                                 server.config.properties.getInt("maxPlayers"));
        }
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
        } else {
          if (lastPacket != null) {
            throw new IOException("Unable to parse unknown " + streamType
                + " packet 0x" + Integer.toHexString(packetId) + " for player "
                + player.getName() + " (after 0x" + Integer.toHexString(lastPacket));
          } else {
            throw new IOException("Unable to parse unknown " + streamType
                + " packet 0x" + Integer.toHexString(packetId) + " for player "
                + player.getName());
          }
        }
    }
    packetFinished();
    lastPacket = (packetId == 0x00) ? lastPacket : packetId;
  }

  private void copyItem() throws IOException {
    short id;
    if ((id = write(in.readShort())) > 0) {
      write(in.readByte());
      write(in.readShort());
      if (ENCHANTABLE.contains(id)) {
        short length;
        if ((length = write(in.readShort())) > 0) {
          copyNBytes(length);
        }
      }
    }
  }

  private long copyVLC() throws IOException {
    long value = 0;
    int shift = 0;
    while (true) {
      int i = write(in.readByte());
      value |= (i & 0x7F) << shift;
      if ((i & 0x80) == 0) {
        break;
      }
      shift += 7;
    }
    return value;
  }

  private String readUTF16() throws IOException {
    short length = in.readShort();
    byte[] bytes = new byte[length * 2 + 2];
    in.readFully(bytes, 2, length * 2);
    bytes[0] = (byte) 0xfffffffe;
    bytes[1] = (byte) 0xffffffff;
    return new String(bytes, "UTF-16");
  }

  private void lockChest(Coordinate coordinate) {
    Chest adjacentChest = server.data.chests.adjacentChest(coordinate);
    if (player.isAttemptLock() || adjacentChest != null && !adjacentChest.isOpen()) {
      if (adjacentChest != null && !adjacentChest.isOpen()) {
        server.data.chests.giveLock(adjacentChest.owner, coordinate, adjacentChest.name);
      } else {
        if (adjacentChest != null) {
          adjacentChest.lock(player);
          adjacentChest.name = player.nextChestName();
        }
        server.data.chests.giveLock(player, coordinate, player.nextChestName());
      }
      player.setAttemptedAction(null);
      player.addTMessage(Color.GRAY, "This chest is now locked.");
    } else if (!server.data.chests.isChest(coordinate)) {
      server.data.chests.addOpenChest(coordinate);
    }
    server.data.save();
  }

  private void copyPlayerLocation() throws IOException {
    double x = in.readDouble();
    double y = in.readDouble();
    double stance = in.readDouble();
    double z = in.readDouble();
    player.position.updatePosition(x, y, z, stance);
    write(x);
    write(y);
    write(stance);
    write(z);
  }

  private void copyPlayerLook() throws IOException {
    float yaw = in.readFloat();
    float pitch = in.readFloat();
    player.position.updateLook(yaw, pitch);
    write(yaw);
    write(pitch);
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
          write(readUTF16());
          break;
        case 5:
          write(in.readShort());
          write(in.readByte());
          write(in.readShort());
          break;
        case 6:
          write(in.readInt());
          write(in.readInt());
          write(in.readInt());
      }

      unknown = in.readByte();
      write(unknown);
    }
  }

  private byte write(byte b) throws IOException {
    out.writeByte(b);
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

  private String getLastColorCode(String message) {
    String colorCode = "";
    int lastIndex = message.lastIndexOf('\u00a7');
    if (lastIndex != -1 && lastIndex + 1 < message.length()) {
      colorCode = message.substring(lastIndex, lastIndex + 2);
    }

    return colorCode;
  }

  private void sendMessage(String message) throws IOException {
    if (message.length() > 0) {
      if (message.length() > MESSAGE_SIZE) {
        int end = MESSAGE_SIZE - 1;
        while (end > 0 && message.charAt(end) != ' ') {
          end--;
        }
        if (end == 0) {
          end = MESSAGE_SIZE;
        } else {
          end++;
        }

        if (end > 0 && message.charAt(end) == '\u00a7') {
          end--;
        }

        String firstPart = message.substring(0, end);
        sendMessagePacket(firstPart);
        sendMessage(getLastColorCode(firstPart) + message.substring(end));
      } else {
        int end = message.length();
        if (message.charAt(end - 1) == '\u00a7') {
          end--;
        }
        sendMessagePacket(message.substring(0, end));
      }
    }
  }

  private void sendMessagePacket(String message) throws IOException {
    if (message.length() > MESSAGE_SIZE) {
      System.out.println("[SimpleServer] Invalid message size: " + message);
      return;
    }
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
              System.out.println("[SimpleServer] " + e);
              System.out.print("[SimpleServer] " + streamType
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
