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

  public void setState(int i) {
    state = i;
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
    outgoing = ByteBuffer.allocate(length * 2);

    Byte packetId  = (byte) decodeVarInt();

    System.out.println("state:" + state + (isServerTunnel ? " server " : " client ") +
    String.format("%02x", packetId) + " length: " + length);

    if (state == STATE_HANDSHAKE) {
      if (!isServerTunnel) {
        add(packetId);
        copyVarInt();
        add(readUTF8());
        addUnsignedShort(readUnsignedShort());
        state = decodeVarInt();
        player.setState(state);
        addVarInt(state);
      }
    } else if (state == STATE_STATUS) {
        switch(packetId) {
          case 0x00: // JSON Response
            add(packetId);

            if (isServerTunnel) {
              add(readUTF8()); // @todo handle changing of max-player, server name, etc.
            }
            break;

          case 0x01: // Ping
            add(packetId);
            add(incoming.getLong());
            break;
      }

    } else if (state == STATE_LOGIN) {
      switch(packetId) {
        case 0x00: // Disconnect / Login Start
          add(packetId);
          add(readUTF8());
          break;

        case 0x01: // Encryption Request / Response
          if (isServerTunnel) {
            add(packetId);
            tunneler.setName(streamType + "-" + player.getUuid());
            String serverId = readUTF8();
            if (!server.authenticator.useCustAuth(player)) {
              serverId = "-";
            } else {
              serverId = player.getConnectionHash();
            }
            add(serverId);
            // @todo fixup sizes
            byte[] keyBytes = new byte[incoming.getShort()];
            outgoing.put(keyBytes);

            byte[] challengeToken = new byte[incoming.getShort()];
            outgoing.put(challengeToken);

            player.serverEncryption.setPublicKey(keyBytes);
            byte[] key = player.clientEncryption.getPublicKey();
            add((short) key.length);
            add(key);
            add((short) challengeToken.length);
            add(challengeToken);
            player.serverEncryption.setChallengeToken(challengeToken);
            player.clientEncryption.setChallengeToken(challengeToken);
          } else {
            byte[] sharedKey = new byte[incoming.getShort()];
            outgoing.put(sharedKey);
            byte[] challengeTokenResponse = new byte[incoming.getShort()];
            outgoing.put(challengeTokenResponse);

            if (!player.clientEncryption.checkChallengeToken(challengeTokenResponse)) {
              player.kick("Invalid client response");
              break;
            }
            player.clientEncryption.setEncryptedSharedKey(sharedKey);
            sharedKey = player.serverEncryption.getEncryptedSharedKey();

            if (server.authenticator.useCustAuth(player)
                    && !server.authenticator.onlineAuthenticate(player)) {
              player.kick(t("%s Failed to login: User not premium", "[CustAuth]"));
              break;
            }
            add(packetId);
            add((short) sharedKey.length);
            add(sharedKey);
            challengeTokenResponse = player.serverEncryption.encryptChallengeToken();
            add((short) challengeTokenResponse.length);
            add(challengeTokenResponse);
            in = new DataInputStream(new BufferedInputStream(player.clientEncryption.encryptedInputStream(inputStream)));
            out = new DataOutputStream(new BufferedOutputStream(player.serverEncryption.encryptedOutputStream(outputStream)));
          }
          break;

        case 0x02: // Login Success
          add(packetId);
          String uuid = readUTF8();
          String name = readUTF8();

          boolean nameSet = false;

          if (name.contains(";")) {
            name = name.substring(0, name.indexOf(";"));
          }
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

          if (player.isGuest() && !server.authenticator.allowGuestJoin()) {
            player.kick(t("Failed to login: User not authenticated"));
            nameSet = false;
          }

          tunneler.setName(streamType + "-" + player.getName());
          player.setUuid(uuid);
          player.setState(3);
          add(uuid);
          add(player.getName());
          break;

      }
    } else if (state == STATE_PLAY) {

      int x, z;
      byte y, dimension;
      Coordinate coordinate;

      switch(packetId) {
        case 0x00: // Keep-Alive
          add(packetId);
          add(incoming.getInt());
          break;

        case 0x01: // Join-Game / Chat-Message
          add(packetId);
          if (isServerTunnel) {
            player.setEntityId(add(incoming.getInt()));
            copyUnsignedByte();
            dimension = incoming.get();
            add(dimension);
            copyUnsignedByte();
            copyUnsignedByte();
            //readUnsignedByte(); @todo uncomment once fix 0x00 of status
            //addUnsignedByte(server.config.properties.getInt("maxPlayers"));
            add(readUTF8());
          } else {
            add(readUTF8()); // @todo handle chat message
          }
          break;

        case 0x02: // Chat-Message / Use Entity
          add(packetId);
          if (isServerTunnel) {
            add(readUTF8());
          } else {
            add(incoming.getInt());
            add(incoming.get());
          }
          break;

        case 0x03: // Time-Update / Player
          add(packetId);
          if (isServerTunnel) {
            add(incoming.getLong());
            long time = incoming.getLong();
            server.setTime(time);
            add(time);
          } else {
            add(incoming.get());
          }
          break;

        case 0x04: // Entity-Equipment / Player Position
          add(packetId);
          if (isServerTunnel) {
            add(incoming.getInt());
            add(incoming.getShort());
            copyItem();
          } else {
            copyPlayerPosition(true);
          }
          break;

        case 0x05: // Spawn Position / Player Look
          add(packetId);
          if (isServerTunnel) {
            add(incoming.getInt());
            add(incoming.getInt());
            add(incoming.getInt());
            if (server.options.getBoolean("enableEvents")) {
              server.eventhost.execute(server.eventhost.findEvent("onPlayerConnect"), player, true, null);
            }
          } else {
            add(incoming.getFloat());
            add(incoming.getFloat());
            add(incoming.get());
          }
          break;

        case 0x06: // Update Health / Player Position & Look
          add(packetId);
          if (isServerTunnel) {
            player.updateHealth(add(incoming.getFloat()));
            player.getHealth();
            add(incoming.getShort());
            add(incoming.getFloat());
          } else {
            copyPlayerPosition(false);
            copyPlayerLook(true);
          }
          break;

        case 0x07: // Respawn / Player Digging
          if (isServerTunnel) {
            add(packetId);
            player.setDimension(Dimension.get(add(incoming.getInt())));
            copyUnsignedByte();
            copyUnsignedByte();
            add(readUTF8());
            if (server.options.getBoolean("enableEvents") && isServerTunnel) {
              server.eventhost.execute(server.eventhost.findEvent("onPlayerRespawn"), player, true, null);
            }
          } else {
            byte status = incoming.get();
            x = incoming.getInt();
            y = incoming.get();
            z = incoming.getInt();
            byte face = incoming.get();

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

              add(packetId);
              add(status);
              add(x);
              add(y);
              add(z);
              add(face);

              if (player.instantDestroyEnabled()) {
                packetFinished();
                add(packetId);
                add(BLOCK_DESTROYED_STATUS);
                add(x);
                add(y);
                add(z);
                add(face);
              }

              if (status == BLOCK_DESTROYED_STATUS) {
                player.destroyedBlock();
              }
            }
          }
          break;

        case 0x08: // Player Position & Look / Player Block Placement
          if (isServerTunnel) {
            add(packetId);
            copyPlayerPosition(false);
            copyPlayerLook(true);
          } else {
            x = incoming.getInt();
            y = (byte) readUnsignedByte();
            z = incoming.getInt();
            coordinate = new Coordinate(x, y, z, player);
            final byte direction = incoming.get();
            final short dropItem = incoming.getShort();
            byte itemCount = 0;
            short uses = 0;
            byte[] data = null;
            if (dropItem != -1) {
              itemCount = incoming.get();
              uses = incoming.getShort();
              short dataLength = incoming.getShort();
              if (dataLength != -1) {
                data = new byte[dataLength];
                incoming.get(data);
              }
            }
            byte blockX = incoming.get();
            byte blockY = incoming.get();
            byte blockZ = incoming.get();

            boolean writePacket = true;
            boolean drop = false;

            BlockPermission perm = server.config.blockPermission(player, coordinate, dropItem);

            if (server.options.getBoolean("enableEvents")) {
              player.checkButtonEvents(new Coordinate(x + (x < 0 ? 1 : 0), y + 1, z + (z < 0 ? 1 : 0)));
            }

            if (server.data.chests.isChest(coordinate)) {
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
              add(packetId);
              add(x);
              add(y);
              add(z);
              add(direction);
              add(dropItem);

              if (dropItem != -1) {
                add(itemCount);
                add(uses);
                if (data != null) {
                  add((short) data.length);
                  add(data);
                } else {
                  add((short) -1);
                }

                if (dropItem <= 94 && direction >= 0) {
                  player.placedBlock();
                }
              }
              add(blockX);
              add(blockY);
              add(blockZ);

              player.openingChest(coordinate);

            } else if (drop) {
              // Drop the item in hand. This keeps the client state in-sync with the
              // server. This generally prevents empty-hand clicks by the client
              // from placing blocks the server thinks the client has in hand.
              add((byte) 0x0e); // @todo figure out how to drop items in new protocol
              add((byte) 0x04);
              add(x);
              add(y);
              add(z);
              add(direction);
            }
          }
          break;

        case 0x09: // Held Item Change / Held Item Change
          add(packetId);
          if (isServerTunnel) {
            add(incoming.get());
          } else {
            add(incoming.getShort());
          }
          break;

        case 0x0A: // Animation / Use Bed
          add(packetId);
          if (isServerTunnel) {
            add(incoming.getInt());
            add(incoming.get());
          } else {
            add(incoming.getInt());
            add(incoming.getInt());
            copyUnsignedByte();
            add(incoming.getInt());
          }
          break;

        case 0x0B: // Entity Action / Animation
          add(packetId);
          if (isServerTunnel) {
            add(incoming.getInt());
            add(incoming.get());
            add(incoming.getInt());
          } else {
            copyVarInt();
            copyUnsignedByte();
          }
          break;

        case 0x0C: // Steer Vehicle / Spawn Player
          add(packetId);
          if (isServerTunnel) {
            add(incoming.getFloat());
            add(incoming.getFloat());
            add(incoming.get());
            add(incoming.get());
          } else {
            copyVarInt();
            add(readUTF8());
            add(readUTF8());
            add(incoming.getInt());
            add(incoming.getInt());
            add(incoming.getInt());
            add(incoming.get());
            add(incoming.get());
            add(incoming.getShort());
            copyEntityMetadata();
          }
          break;

        case 0x0D: // Close Window / Collect Item
          add(packetId);
          if (isServerTunnel) {
            add(incoming.get());
          } else {
            add(incoming.getInt());
            add(incoming.getInt());
          }
          break;

        case 0x0E: // Click Window / Spawn Object
          if (isServerTunnel) {
            add(packetId);
            add(incoming.get());
            add(incoming.getShort());
            add(incoming.get());
            add(incoming.getShort());
            add(incoming.get());
            copyItem();
          } else {
            int eid = decodeVarInt();
            String name = readUTF8();

            if (!server.bots.ninja(name)) {
              add(packetId);
              add(eid);
              add(name);
              add(incoming.getInt());
              add(incoming.getInt());
              add(incoming.getInt());
              add(incoming.get());
              add(incoming.get());
              copyEntityMetadata();
            } else {
              incoming.getInt();
              incoming.getInt();
              incoming.getInt();
              incoming.get();
              incoming.get();
              skipEntityMetadata();
            }
          }
          break;

        case 0x0F: // Confirm Transaction / Spawn Mob
          add(packetId);
          if (isServerTunnel) {
            add(incoming.get());
            add(incoming.getShort());
            add(incoming.get());
          } else {
            copyVarInt();
            copyUnsignedByte();
            add(incoming.getInt());
            add(incoming.getInt());
            add(incoming.getInt());
            add(incoming.get());
            add(incoming.get());
            add(incoming.get());
            add(incoming.getShort());
            add(incoming.getShort());
            add(incoming.getShort());
            copyEntityMetadata();
          }
          break;

        case 0x10: // Creative Inventory Action / Spawn Painting
          add(packetId);
          if (isServerTunnel) {
            add(incoming.getShort());
            copyItem();
          } else {
            copyVarInt();
            add(readUTF8());
            add(incoming.getInt());
            add(incoming.getInt());
            add(incoming.getInt());
            add(incoming.getInt());
          }
          break;

        case 0x11: // Enchant Item / Spawn Experience Orb
          add(packetId);
          if (isServerTunnel) {
            add(incoming.get());
            add(incoming.get());
          } else {
            copyVarInt();
            add(incoming.getInt());
            add(incoming.getInt());
            add(incoming.getInt());
            add(incoming.getShort());
          }
          break;

        case 0x12: // Update Sign / Entity Velocity
          add(packetId);
          if (isServerTunnel) {
            add(incoming.getInt());
            add(incoming.getShort());
            add(incoming.getInt());
            add(readUTF8());
            add(readUTF8());
            add(readUTF8());
            add(readUTF8());
          } else {
            add(incoming.getInt());
            add(incoming.getShort());
            add(incoming.getShort());
            add(incoming.getShort());
          }
          break;

        case 0x13: // Player Abilities / Destroy Entities
          add(packetId);
          if (isServerTunnel) {
            add(incoming.get());
            add(incoming.getFloat());
            add(incoming.getFloat());
          } else {
            byte destroyCount = add(incoming.get());
            if (destroyCount > 0) {
              copyNBytes(destroyCount * 4);
            }
          }
          break;

        case 0x14: // Tab Complete / Entity
          add(packetId);
          if (isServerTunnel) {
            add(readUTF8());
          } else {
            add(incoming.getInt());
          }
          break;

        case 0x15: // Client Settings / Entity Relative Move
          add(packetId);
          if (isServerTunnel) {
            add(readUTF8());
            add(incoming.get());
            add(incoming.get());
            add(incoming.get());
            add(incoming.get());
            add(incoming.get());
          } else {
            add(incoming.getInt());
            add(incoming.get());
            add(incoming.get());
            add(incoming.get());
          }
          break;

        case 0x16: // Client Status / Entity Look
          add(packetId);
          if (isServerTunnel) {
            add(incoming.get());
          } else {
            add(incoming.getInt());
            add(incoming.get());
            add(incoming.get());
          }
          break;

        case 0x17: // Plugin Message / Entity Look & Relative Move
          add(packetId);
          if (isServerTunnel) {
            add(readUTF8());
            short size = incoming.getShort();
            copyNBytes(size);
          } else {
            add(incoming.getInt());
            add(incoming.get());
            add(incoming.get());
            add(incoming.get());
            add(incoming.get());
            add(incoming.get());
          }
          break;

        case 0x18: // Entity Teleport
          add(packetId);
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.get());
          add(incoming.get());
          break;

        case 0x19: // Entity Head Lock
          add(packetId);
          add(incoming.getInt());
          add(incoming.get());
          break;

        case 0x1A: // Entity Status
          add(packetId);
          add(incoming.getInt());
          add(incoming.get());
          break;

        case 0x1B: // Attach Entity
          add(packetId);
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.get());
          break;

        case 0x1C: // Entity Metdata
          add(packetId);
          add(incoming.getInt());
          copyEntityMetadata();
          break;

        case 0x1D: // Entity Effect
          add(packetId);
          add(incoming.getInt());
          add(incoming.get());
          add(incoming.get());
          add(incoming.getShort());
          break;

        case 0x1E: // Remove Entity Effect
          add(packetId);
          add(incoming.getInt());
          add(incoming.get());
          break;

        case 0x1F: // Set Experience
          add(packetId);
          player.updateExperience(add(incoming.getFloat()),add(incoming.getShort()),add(incoming.getShort()));
          break;

        case 0x20: // Entity Properties
          add(packetId);
          add(incoming.getInt());
          int properties_count = incoming.getInt();
          short list_length = 0;
          add(properties_count);

          // loop for every property key/value pair
          for (int i = 0; i < properties_count; i++) {
            add(readUTF8());
            add(in.readDouble());

            // grab list elements
            list_length = in.readShort();
            add(list_length);
            if (list_length > 0) {
              for (int k = 0; k < list_length; k++) {
                add(in.readLong());
                add(in.readLong());
                add(in.readDouble());
                add(in.readByte());
              }
            }
          }
          break;

        case 0x21: // Chunk Data
          add(packetId);
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.get());
          copyUnsignedShort();
          copyUnsignedShort();
          copyNBytes(add(incoming.getInt()));
          break;

        case 0x22: // Multi Block Change
          add(packetId);
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.getShort());
          copyNBytes(add(incoming.getInt()));
          break;

        case 0x23: // Block Change
          add(packetId);
          x = incoming.getInt();
          y = (byte) readUnsignedByte();
          z = incoming.getInt();
          int blockType = decodeVarInt();
          byte metadata = (byte) readUnsignedByte();

          coordinate = new Coordinate(x, y, z, player);

          if (blockType == 54 && player.placedChest(coordinate)) {
            lockChest(coordinate);
            player.placingChest(null);
          }

          add(x);
          add(y);
          add(z);
          add(blockType);
          add(metadata);
          break;

        case 0x24: // Block Action
          add(packetId);
          add(incoming.getInt());
          add(incoming.getShort());
          add(incoming.getInt());
          copyUnsignedByte();
          copyUnsignedByte();
          copyVarInt();
          break;

        case 0x25: // Block Break Animation
          add(packetId);
          copyVarInt();
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.get());
          break;

        case 0x26: // Map Chunk Bulk
          add(packetId);
          short chunkCount = incoming.getShort();
          int dataLength = incoming.getInt();
          add(chunkCount);
          add(dataLength);
          add(incoming.get());
          copyNBytes(chunkCount * 12 + dataLength);
          break;

        case 0x27: // Explosion
          add(packetId);
          add(incoming.getFloat());
          add(incoming.getFloat());
          add(incoming.getFloat());
          int recordCount = incoming.getInt();
          add(recordCount);
          copyNBytes(recordCount * 3);
          add(incoming.getFloat());
          add(incoming.getFloat());
          add(incoming.getFloat());
          break;

        case 0x28: // Effect
          add(packetId);
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.get());
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.get());
          break;

        case 0x29: // Sound Effect
          add(packetId);
          add(readUTF8());
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.getFloat());
          copyUnsignedByte();
          break;

        case 0x2A: // Particle
          add(packetId);
          add(readUTF8());
          add(incoming.getFloat());
          add(incoming.getFloat());
          add(incoming.getFloat());
          add(incoming.getFloat());
          add(incoming.getFloat());
          add(incoming.getFloat());
          add(incoming.getFloat());
          add(incoming.getInt());
          break;

        case 0x2B: // Change Game State
          add(packetId);
          copyUnsignedByte();
          add(incoming.getFloat());
          break;

        case 0x2C: // Spawn Global Entity
          add(packetId);
          copyVarInt();
          add(incoming.get());
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.getInt());
          break;

        case 0x2D: // Open Window
          boolean allow = true;
          byte id = incoming.get();
          byte invtype = incoming.get();
          String title = readUTF8();
          byte number = incoming.get();
          byte provided = incoming.get();
          int unknown = 0;
          if (invtype == 11) {
            unknown = incoming.getInt();
          }
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
                  title = t("Open Chest");
                } else {
                  title = server.data.chests.chestName(player.openedChest());
                }
              } else {
                title = t("Open Chest");
                if (player.isAttemptLock()) {
                  lockChest(player.openedChest());
                  title = (player.nextChestName() == null) ? t("Locked Chest") : player.nextChestName();
                }
              }

            } else {
              player.addTMessage(Color.RED, "This chest is locked!");
              allow = false;
            }
          }
          if (!allow) {
            add((byte) 0x2E);
            add(id);
          } else {
            add(packetId);
            add(id);
            add(invtype);
            add(title);
            add(number);
            add(provided);
            if (invtype == 11) {
              add(unknown);
            }
          }
          break;

        case 0x2E: // Close Window
          add(packetId);
          copyUnsignedByte();
          break;

        case 0x2F: // Set Slot
          add(packetId);
          copyUnsignedByte();
          add(incoming.getShort());
          copyItem();
          break;

        case 0x30: // Window Items
          add(packetId);
          copyUnsignedByte();
          short count = add(incoming.getShort());
          for (int c = 0; c < count; ++c) {
            copyItem();
          }
          break;

        case 0x31: // Window Property
          add(packetId);
          copyUnsignedByte();
          add(incoming.getShort());
          add(incoming.getShort());
          break;

        case 0x32: // Confirm Transaction
          add(packetId);
          copyUnsignedByte();
          add(incoming.getShort());
          add(incoming.get());
          break;

        case 0x33: // Update Sign
          add(packetId);
          add(incoming.getInt());
          add(incoming.getShort());
          add(incoming.getInt());
          add(readUTF8());
          add(readUTF8());
          add(readUTF8());
          add(readUTF8());
          break;

        case 0x34: // Maps
          add(packetId);
          copyVarInt();
          copyNBytes(add(incoming.getShort()));
          break;

        case 0x35: // Update Block Entity
          add(packetId);
          add(incoming.getInt());
          add(incoming.getShort());
          add(incoming.getInt());
          copyUnsignedByte();
          short len = incoming.getShort();
          add(len);
          if (len > 0) {
            copyNBytes(len);
          }
          break;

        case 0x36: // Sign Editor Open
          add(packetId);
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.getInt());
          break;

        case 0x37: // Statistics
          add(packetId);
          copyVarInt();
          add(readUTF8());
          copyVarInt();
          break;

        case 0x38: // Player List Item
          add(packetId);
          add(readUTF8());
          add(incoming.get());
          add(incoming.getShort());
          break;

        case 0x39: // Player Abilities
          add(packetId);
          add(incoming.get());
          add(incoming.getFloat());
          add(incoming.getFloat());
          break;

        case 0x3A: // Tab-Complete
          add(packetId);
          copyVarInt();
          add(readUTF8());
          break;

        case 0x3B: // Scoreboard Objective
          add(packetId);
          add(readUTF8());
          add(readUTF8());
          add(incoming.get());
          break;

        case 0x3C: // Update Score
          add(packetId);
          add(readUTF8());
          add(incoming.get());
          add(readUTF8());
          add(incoming.get());
          break;

        case 0x3D: // Display Scoreboard
          add(packetId);
          add(incoming.get());
          add(readUTF8());
          break;

        case 0x3E: // Teams
          add(packetId);
          add(readUTF8());
          byte mode = incoming.get();
          short playerCount = -1;
          add(mode);

          if (mode == 0 || mode == 2) {
            write(readUTF8()); // team display name
            write(readUTF8()); // team prefix
            write(readUTF8()); // team suffix
            write(incoming.get()); // friendly fire
          }

          // only ran if 0,3,4
          if (mode == 0 || mode == 3 || mode == 4) {
            playerCount = incoming.getShort();
            write(playerCount);

            if (playerCount != -1) {
              for (int i = 0; i < playerCount; i++) {
                write(readUTF8());
              }
            }
          }
          break;

        case 0x3F: // Plugin Message
          add(packetId);
          add(readUTF8());
          short size = incoming.getShort();
          copyNBytes(size);
          break;

        case 0x40: // Disconnect
          add(packetId);
          add(readUTF8());
          break;
      }

    } else {
      throw new ArrayIndexOutOfBoundsException();
    }

    packetFinished();
    lastPacket = (packetId == 0x00) ? lastPacket : packetId;
  }

  private void copyItem() throws IOException {
    if (add(incoming.getShort()) > 0) {
      add(incoming.get());
      add(incoming.getShort());
      short length;
      if ((length = add(incoming.getShort())) > 0) {
        copyNBytes(length);
      }
    }
  }

  private void skipItem() throws IOException {
    if (incoming.getShort() > 0) {
      incoming.get();
      incoming.getShort();
      short length;
      if ((length = incoming.getShort()) > 0) {
        copyNBytes(length);
      }
    }
  }

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

  private int addVarInt(int i) throws IOException {
    outgoing.put(encodeVarInt(i));
    return i;
  }

  private int addUnsignedShort(int i) {
    outgoing.putShort((short) (i & 0xFFFF));
    return i;
  }

  private int addUnsignedByte(int i) {
    outgoing.put((byte) (i & 0xFF));
    return i;
  }

  private int add(int i) throws IOException {
    outgoing.putInt(i);
    return i;
  }

  private String add(String s) throws IOException {
   addVarInt(s.length());
   add(s.getBytes());
   return s;
  }

  private byte[] add(byte[] b) throws IOException {
    outgoing.put(b);
    return b;
  }

  private byte add(byte b) throws IOException {
    outgoing.put(b);
    return b;
  }

  private long add(long l) throws IOException {
    outgoing.putLong(l);
    return l;
  }

  private short add(short s) throws IOException {
    outgoing.putShort(s);
    return s;
  }

  private double add(double d) throws IOException {
    outgoing.putDouble(d);
    return d;
  }

  private float add(float f) throws IOException {
    outgoing.putFloat(f);
    return f;
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

  private void copyPlayerPosition(boolean on_ground) throws IOException {
    double x = incoming.getDouble();
    double y = incoming.getDouble();
    double stance = incoming.getDouble();
    double z = incoming.getDouble();
    player.position.updatePosition(x, y, z, stance);
    if (server.options.getBoolean("enableEvents")) {
      player.checkLocationEvents();
    }
    add(x);
    add(y);
    add(stance);
    add(z);

    if (on_ground) {
      add(incoming.get()); // on ground (bool)
    }
  }

  private void copyPlayerLook(boolean on_ground) throws IOException {
    float yaw = incoming.getFloat();
    float pitch = incoming.getFloat();
    player.position.updateLook(yaw, pitch);
    add(yaw);
    add(pitch);

    if (on_ground) {
      add(incoming.get());
    }
  }

  private void copyEntityMetadata() throws IOException {
    byte item = incoming.get();
    add(item);

    while (item != 0x7F) {
      int type = (item & 0xE0) >> 5;

      switch (type) {
        case 0:
          add(incoming.get());
          break;
        case 1:
          add(incoming.getShort());
          break;
        case 2:
          add(incoming.getInt());
          break;
        case 3:
          add(incoming.getFloat());
          break;
        case 4:
          add(readUTF8());
          break;
        case 5:
          copyItem();
          break;
        case 6:
          add(incoming.getInt());
          add(incoming.getInt());
          add(incoming.getInt());
          break;
      }

      item = incoming.get();
      add(item);
    }
  }

  private void skipEntityMetadata() throws IOException {
    byte item = incoming.get();

    while (item != 0x7F) {
      int type = (item & 0xE0) >> 5;

      switch (type) {
        case 0:
          incoming.get();
          break;
        case 1:
          incoming.getShort();
          break;
        case 2:
          incoming.getInt();
          break;
        case 3:
          incoming.getFloat();
          break;
        case 4:
          readUTF8();
          break;
        case 5:
          skipItem();
          break;
        case 6:
          incoming.getInt();
          incoming.getInt();
          incoming.getInt();
          break;
      }

      item = incoming.get();
    }
  }

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
  private void copyNBytes(int bytes) throws IOException {
    int overflow = bytes / buffer.length;
    for (int c = 0; c < overflow; ++c) {
      incoming.get(buffer, 0, buffer.length);
      outgoing.put(buffer, 0, buffer.length);
    }
    incoming.get(buffer, 0, bytes % buffer.length);
    outgoing.put(buffer, 0, bytes % buffer.length);
  }

  private void kick(String reason) throws IOException {
    write((byte) 0x40);
    write(reason);
    packetFinished();
  }

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
      write((byte) 0x01);
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

    if (size > 0) {
      outgoing.limit(size);
      outgoing.rewind();
      outgoing.order(ByteOrder.BIG_ENDIAN);

      byte[] tmp = new byte[size];
      outgoing.get(tmp);

      System.out.println("outbound size: " + size );

      out.write(size);
      out.write(tmp);
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
