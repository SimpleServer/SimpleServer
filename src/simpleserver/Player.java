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
package simpleserver;

import static simpleserver.lang.Translations.t;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import simpleserver.Coordinate.Dimension;
import simpleserver.bot.Teleporter;
import simpleserver.bot.BotController.ConnectException;
import simpleserver.command.ExternalCommand;
import simpleserver.command.PlayerCommand;
import simpleserver.stream.StreamTunnel;

public class Player {
  private static final LocalAddressFactory addressFactory = new LocalAddressFactory();

  private final long connected;
  private final Socket extsocket;
  private final Server server;

  private Socket intsocket;
  private StreamTunnel serverToClient;
  private StreamTunnel clientToServer;
  private Watchdog watchdog;

  private String name = null;
  private boolean closed = false;
  private boolean isKicked = false;
  private Action attemptedAction;
  private boolean instantDestroy = false;
  private boolean godMode = false;
  private String kickMsg = null;
  public Position position;
  private Dimension dimension;
  private int group = 0;
  private int entityId = 0;
  private Group groupObject = null;
  private boolean isRobot = false;
  private boolean localChat = false;
  private int blocksPlaced = 0;
  private int blocksDestroyed = 0;
  private Player reply = null;
  private String lastCommand = "";

  private Queue<String> messages = new ConcurrentLinkedQueue<String>();
  private Queue<PlayerVisitRequest> visitreqs = new ConcurrentLinkedQueue<PlayerVisitRequest>();

  private Coordinate chestPlaced;

  private Coordinate chestOpened;

  private String nextChestName;

  // temporary coordinate storage for !myarea command
  public Coordinate areastart;
  public Coordinate areaend;

  public Player(Socket inc, Server parent) {
    connected = System.currentTimeMillis();
    position = new Position();
    server = parent;
    extsocket = inc;
    if (server.isRobot(getIPAddress())) {
      System.out.println("[SimpleServer] Robot Heartbeat: " + getIPAddress()
          + ".");
      isRobot = true;
    } else {
      System.out.println("[SimpleServer] IP Connection from " + getIPAddress()
          + "!");
    }

    if (server.isIPBanned(getIPAddress())) {
      System.out.println("[SimpleServer] IP " + getIPAddress() + " is banned!");

      cleanup();
      return;
    }
    server.requestTracker.addRequest(getIPAddress());

    try {
      InetAddress localAddress = InetAddress.getByName(addressFactory.getNextAddress());
      intsocket = new Socket(InetAddress.getByName(null),
                             server.options.getInt("internalPort"),
                             localAddress, 0);
    } catch (Exception e) {
      try {
        intsocket = new Socket(InetAddress.getByName(null), server.options.getInt("internalPort"));
      } catch (Exception E) {
        e.printStackTrace();
        if (server.options.getBoolean("exitOnFailure")) {
          server.stop();
        } else {
          server.restart();
        }

        cleanup();
        return;
      }
    }

    watchdog = new Watchdog();
    try {
      serverToClient = new StreamTunnel(intsocket.getInputStream(),
                                        extsocket.getOutputStream(), true, this);
      clientToServer = new StreamTunnel(extsocket.getInputStream(),
                                        intsocket.getOutputStream(), false,
                                        this);
    } catch (IOException e) {
      e.printStackTrace();
      cleanup();
      return;
    }

    if (isRobot) {
      server.addRobotPort(intsocket.getLocalPort());
    }
    watchdog.start();
  }

  public boolean setName(String name) {

    name = name.trim();
    if (name.length() == 0 || this.name != null) {
      kick("Invalid Name!");
      return false;
    }

    if (server.options.getBoolean("useWhitelist")
        && !server.whitelist.isWhitelisted(name)) {
      kick("You are not whitelisted!");
      return false;
    }

    if (server.playerList.findPlayerExact(name) != null) {
      kick("Player already in server!");
      return false;
    }

    this.name = name;
    updateGroup();

    watchdog.setName("PlayerWatchdog-" + name);
    server.connectionLog("player", extsocket, name);

    if (server.numPlayers() == 0) {
      server.time.set();
    }

    server.playerList.addPlayer(this);
    return true;
  }

  public String getName() {
    return name;
  }

  public double distanceTo(Player player) {
    return Math.sqrt(Math.pow(x() - player.x(), 2) + Math.pow(y() - player.y(), 2) + Math.pow(z() - player.z(), 2));
  }

  public long getConnectedAt() {
    return connected;
  }

  public boolean isAttemptLock() {
    return attemptedAction == Action.Lock;
  }

  public void setAttemptedAction(Action action) {
    attemptedAction = action;
  }

  public boolean instantDestroyEnabled() {
    return instantDestroy;
  }

  public void toggleInstantDestroy() {
    instantDestroy = !instantDestroy;
  }

  public Server getServer() {
    return server;
  }

  public boolean hasMessages() {
    return !messages.isEmpty();
  }

  public void addMessage(Color color, String format, Object... args) {
    addMessage(color, String.format(format, args));
  }

  public void addMessage(Color color, String message) {
    addMessage(color + message);
  }

  public void addMessage(String format, Object... args) {
    addMessage(String.format(format, args));
  }

  public void addCaptionedMessage(String caption, String format, Object... args) {
    addMessage("%s%s: %s%s", Color.GRAY, caption, Color.WHITE, String.format(format, args));
  }

  public void addMessage(String msg) {
    messages.add(msg);
  }

  public void addTMessage(Color color, String format, Object... args) {
    addMessage(color + t(format, args));
  }

  public void addTMessage(Color color, String message) {
    addMessage(color + t(message));
  }

  public void addTMessage(String msg) {
    addMessage(t(msg));
  }

  public void addTCaptionedTMessage(String caption, String format, Object... args) {
    addMessage("%s%s: %s%s", Color.GRAY, t(caption), Color.WHITE, t(format, args));
  }

  public void addTCaptionedMessage(String caption, String format, Object... args) {
    addMessage("%s%s: %s%s", Color.GRAY, t(caption),
               Color.WHITE, String.format(format, args));
  }

  public String getMessage() {
    return messages.remove();
  }

  public void addVisitRequest(Player source) {
    visitreqs.add(new PlayerVisitRequest(source));
  }

  public void handleVisitRequests() {
    while (visitreqs.size() > 0) {
      PlayerVisitRequest req = visitreqs.remove();
      if (System.currentTimeMillis() < req.timestamp + 10000 && server.findPlayerExact(req.source.getName()) != null) {
        req.source.addTMessage(Color.GRAY, "Request accepted!");
        req.source.teleportTo(this);
      }
    }
  }

  public void kick(String reason) {
    kickMsg = reason;
    isKicked = true;

    serverToClient.stop();
    clientToServer.stop();
  }

  public boolean isKicked() {
    return isKicked;
  }

  public void setKicked(boolean b) {
    isKicked = b;
  }

  public String getKickMsg() {
    return kickMsg;
  }

  public boolean isMuted() {
    return server.mutelist.isMuted(name);
  }

  public boolean isRobot() {
    return isRobot;
  }

  public boolean godModeEnabled() {
    return godMode;
  }

  public void toggleGodMode() {
    godMode = !godMode;
  }

  public int getEntityId() {
    return entityId;
  }

  public void setEntityId(int readInt) {
    entityId = readInt;
  }

  public int getGroupId() {
    return group;
  }

  public Group getGroup() {
    return groupObject;
  }

  public boolean isAdmin() {
    if (groupObject != null) {
      return groupObject.isAdmin();
    }
    return false;
  }

  public String getIPAddress() {
    return extsocket.getInetAddress().getHostAddress();
  }

  public double x() {
    return position.x;
  }

  public double y() {
    return position.y;
  }

  public double z() {
    return position.z;
  }

  public Coordinate position() {
    return position.coordinate();
  }

  public float yaw() {
    return position.yaw;
  }

  public float pitch() {
    return position.pitch;
  }

  public void setLocalChat(boolean mode) {
    localChat = mode;
  }

  public boolean localChat() {
    return localChat;
  }

  public boolean parseCommand(String message) {
    if (closed) {
      return true;
    }

    // Repeat last command
    if (message.equals(server.getCommandParser().commandPrefix() + "!")) {
      message = lastCommand;
    }

    PlayerCommand command = server.getCommandParser().getPlayerCommand(message);
    if (command == null) {
      return false;
    }

    boolean invalidCommand = command.getName() == null;

    if (!invalidCommand && !commandAllowed(command.getName())) {
      addTMessage(Color.RED, "Insufficient permission.");
      return true;
    }

    if (server.permissions.commandShouldBeExecuted(command.getName())) {
      command.execute(this, message);
    }
    lastCommand = message;

    return !((server.permissions.commandShouldPassThroughToMod(command.getName())
            || server.options.getBoolean("forwardAllCommands")
            || invalidCommand || command instanceof ExternalCommand)
            && server.options.contains("alternateJarFile") && getGroup().getForwardsCommands());
  }

  public void execute(Class<? extends PlayerCommand> c) {
    execute(c, "");
  }

  public void execute(Class<? extends PlayerCommand> c, String arguments) {
    server.getCommandParser().getPlayerCommand(c).execute(this, "a " + arguments);
  }

  public boolean commandAllowed(String command) {
    return server.permissions.playerCommandAllowed(command, this);
  }

  public void teleportTo(Player target) {
    server.runCommand("tp", getName() + " " + target.getName());
  }

  public void sendMOTD() {
    String[] lines = server.motd.getMOTD().split("\\r?\\n");
    for (String line : lines) {
      addMessage(line);
    }
  }

  public boolean give(String rawItem, String rawAmount) {
    boolean success = true;

    int item = 0;
    try {
      item = Integer.parseInt(rawItem);

      if (item < 0) {
        addTMessage(Color.RED, "Item ID must be positive!");
        success = false;
      }
    } catch (NumberFormatException e) {
      addTMessage(Color.RED, "Item ID must map to a number!");
      success = false;
    }

    int amount = 1;
    if (rawAmount != null) {
      try {
        amount = Integer.parseInt(rawAmount);

        if ((amount < 1) || (amount > 1000)) {
          addTMessage(Color.RED, "Amount must be within 1-1000!");
          success = false;
        }
      } catch (NumberFormatException e) {
        addTMessage(Color.RED, "Amount must be a number!");
        success = false;
      }
    }

    if (!success) {
      addTMessage(Color.RED, "Unable to give %s", rawItem);
      return false;
    }

    String baseCommand = getName() + " " + item + " ";
    for (int c = 0; c < amount / 64; ++c) {
      server.runCommand("give", baseCommand + 64);
    }
    if (amount % 64 != 0) {
      server.runCommand("give", baseCommand + amount % 64);
    }

    return true;
  }

  public void updateGroup() {
    group = server.permissions.getPlayerGroup(this);
    groupObject = server.permissions.getGroup(group);
  }

  public void placedBlock() {
    blocksPlaced += 1;
  }

  public void destroyedBlock() {
    blocksDestroyed += 1;
  }

  public Integer[] stats() {
    Integer[] stats = new Integer[4];

    stats[0] = (int) (System.currentTimeMillis() - connected) / 1000 / 60;
    stats[1] = server.stats.getMinutes(this) + stats[0];
    stats[2] = server.stats.addPlacedBlocks(this, blocksPlaced);
    stats[3] = server.stats.addDestroyedBlocks(this, blocksDestroyed);

    blocksPlaced = 0;
    blocksDestroyed = 0;
    server.stats.save();

    return stats;
  }

  public void setReply(Player answer) {
    // set Player to reply with !reply command
    reply = answer;
  }

  public Player getReply() {
    return reply;
  }

  public void close() {
    if (serverToClient != null) {
      serverToClient.stop();
    }

    if (clientToServer != null) {
      clientToServer.stop();
    }

    if (name != null) {
      server.stats.addOnlineMinutes(this, (int) (System.currentTimeMillis() - connected) / 1000 / 60);
      server.stats.addDestroyedBlocks(this, blocksDestroyed);
      server.stats.addPlacedBlocks(this, blocksPlaced);
      server.stats.save();

      server.playerList.removePlayer(this);
      name = null;
    }
  }

  private void cleanup() {
    if (!closed) {
      closed = true;
      entityId = 0;

      close();

      try {
        extsocket.close();
      } catch (Exception e) {
      }
      try {
        intsocket.close();
      } catch (Exception e) {
      }

      if (!isRobot) {
        System.out.println("[SimpleServer] Socket Closed: "
            + extsocket.getInetAddress().getHostAddress());
      }
    }
  }

  private class PlayerVisitRequest {
    public Player source;
    public long timestamp;

    public PlayerVisitRequest(Player source) {
      timestamp = System.currentTimeMillis();
      this.source = source;
    }
  }

  private final class Watchdog extends Thread {
    @Override
    public void run() {
      while (serverToClient.isAlive() || clientToServer.isAlive()) {
        if (!serverToClient.isActive() || !clientToServer.isActive()) {
          System.out.println("[SimpleServer] Disconnecting " + getIPAddress()
              + " due to inactivity.");
          close();
          break;
        }

        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
      }

      cleanup();
    }
  }

  public static final class LocalAddressFactory {
    private static final int[] octets = { 0, 0, 1 };
    private static Boolean canCycle = null;

    public synchronized String getNextAddress() {
      if (!canCycle()) {
        return "127.0.0.1";
      }

      if (octets[2] >= 255) {
        if (octets[1] >= 255) {
          if (octets[0] >= 255) {
            octets[0] = 0;
          } else {
            ++octets[0];
          }
          octets[1] = 0;
        } else {
          ++octets[1];
        }
        octets[2] = 2;
      } else {
        ++octets[2];
      }

      return "127." + octets[0] + "." + octets[1] + "." + octets[2];
    }

    private boolean canCycle() {
      if (canCycle == null) {
        InetAddress testDestination;
        InetAddress testSource;
        try {
          testDestination = InetAddress.getByName(null);
          testSource = InetAddress.getByName("127.0.1.2");
        } catch (UnknownHostException e) {
          canCycle = false;
          return false;
        }

        try {
          Socket testSocket = new Socket(testDestination, 80, testSource, 0);
          testSocket.close();
        } catch (BindException e) {
          canCycle = false;
          return false;
        } catch (IOException e) {
          // Probably nothing listening on port 80
        }

        canCycle = true;
      }

      return canCycle;
    }
  }

  public void placingChest(Coordinate coord) {
    chestPlaced = coord;
  }

  public boolean placedChest(Coordinate coordinate) {
    return chestPlaced != null && chestPlaced.equals(coordinate);
  }

  public void openingChest(Coordinate coordinate) {
    chestOpened = coordinate;
  }

  public Coordinate openedChest() {
    return chestOpened;
  }

  public void setChestName(String name) {
    nextChestName = name;
  }

  public String nextChestName() {
    return nextChestName;
  }

  public enum Action {
    Lock, Unlock, Rename;
  }

  public boolean isAttemptingUnlock() {
    return attemptedAction == Action.Unlock;
  }

  public void setDimension(Dimension dimension) {
    this.dimension = dimension;
  }

  public Dimension getDimension() {
    return dimension;
  }

  public void teleport(Coordinate coordinate) throws IOException, ConnectException {
    server.bots.connect(new Teleporter(this, coordinate));
  }

  public void teleport(Position position) throws IOException, ConnectException {
    server.bots.connect(new Teleporter(this, position));
  }
}
