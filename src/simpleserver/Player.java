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
import static simpleserver.util.Util.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xml.sax.SAXException;

import simpleserver.Coordinate.Dimension;
import simpleserver.bot.BotController.ConnectException;
import simpleserver.bot.Giver;
import simpleserver.bot.Teleporter;
import simpleserver.command.ExternalCommand;
import simpleserver.command.PlayerCommand;
import simpleserver.config.KitList.Kit;
import simpleserver.config.data.Stats.StatField;
import simpleserver.config.xml.Area;
import simpleserver.config.xml.CommandConfig;
import simpleserver.config.xml.CommandConfig.Forwarding;
import simpleserver.config.xml.Event;
import simpleserver.config.xml.Group;
import simpleserver.config.xml.Permission;
import simpleserver.message.AbstractChat;
import simpleserver.message.Chat;
import simpleserver.message.GlobalChat;
import simpleserver.stream.Encryption;
import simpleserver.stream.Encryption.ClientEncryption;
import simpleserver.stream.Encryption.ServerEncryption;
import simpleserver.stream.StreamTunnel;

public class Player {
  private final long connected;
  private final Socket extsocket;
  private final Server server;

  private Socket intsocket;
  private StreamTunnel serverToClient;
  private StreamTunnel clientToServer;
  private Watchdog watchdog;

  public ServerEncryption serverEncryption = new Encryption.ServerEncryption();
  public ClientEncryption clientEncryption = new Encryption.ClientEncryption();

  private String name = null;
  private String renameName = null;
  private String connectionHash;
  private boolean closed = false;
  private boolean isKicked = false;
  private Action attemptedAction;
  private boolean instantDestroy = false;
  private boolean godMode = false;
  private String kickMsg = null;
  public Position position;
  private Position deathPlace;
  private short health = 0;
  private short experience = 0;
  private int group = 0;
  private int entityId = 0;
  private Group groupObject = null;
  private boolean isRobot = false;
  // player is not authenticated with minecraft.net:
  private boolean guest = false;
  private boolean usedAuthenticator = false;
  private int blocksPlaced = 0;
  private int blocksDestroyed = 0;
  private Player reply = null;
  private String lastCommand = "";

  private AbstractChat chatType;
  private Queue<String> messages = new ConcurrentLinkedQueue<String>();
  private Queue<String> forwardMessages = new ConcurrentLinkedQueue<String>();
  private Queue<PlayerVisitRequest> visitreqs = new ConcurrentLinkedQueue<PlayerVisitRequest>();

  private Coordinate chestPlaced;

  private Coordinate chestOpened;

  private String nextChestName;

  // temporary coordinate storage for /myarea command
  public Coordinate areastart;
  public Coordinate areaend;

  private long lastTeleport;
  private short experienceLevel;

  public ConcurrentHashMap<String, String> vars; // temporary player-scope
                                                 // Script variables
  private long lastEvent;
  private HashSet<Area> currentAreas = new HashSet<Area>();

  public Player(Socket inc, Server parent) {
    connected = System.currentTimeMillis();
    position = new Position();
    server = parent;
    chatType = new GlobalChat(this);
    extsocket = inc;

    vars = new ConcurrentHashMap<String, String>();

    if (server.isRobot(getIPAddress())) {
      print("Robot Heartbeat: " + getIPAddress()
          + ".");
      isRobot = true;
    } else {
      print("IP Connection from " + getIPAddress()
          + "!");
    }

    if (server.isIPBanned(getIPAddress())) {
      print("IP " + getIPAddress() + " is banned!");

      cleanup();
      return;
    }
    server.requestTracker.addRequest(getIPAddress());

    try {
      InetAddress localAddress = InetAddress.getByName(Server.addressFactory.getNextAddress());
      intsocket = new Socket(InetAddress.getByName(null),
                             server.options.getInt("internalPort"),
                             localAddress, 0);
    } catch (Exception e) {
      try {
        intsocket = new Socket(InetAddress.getByName(null), server.options.getInt("internalPort"));
      } catch (Exception E) {
        e.printStackTrace();
        if (server.config.properties.getBoolean("exitOnFailure")) {
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
    renameName = server.data.players.getRenameName(name);

    name = name.trim();
    if (name.length() == 0 || this.name != null) {
      kick(t("Invalid Name!"));
      return false;
    }

    if (name == "Player") {
      kick(t("Too many guests in server!"));
      return false;
    }

    if (!guest && server.config.properties.getBoolean("useWhitelist")
        && !server.whitelist.isWhitelisted(name)) {
      kick(t("You are not whitelisted!"));
      return false;
    }

    if (server.playerList.findPlayerExact(name) != null) {
      kick(t("Player already in server!"));
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
    return renameName;
  }

  public String getName(boolean original) {
    return (original) ? name : renameName;
  }

  public String getRealName() {
    return server.data.players.getRealName(name);
  }

  public void updateRealName(String name) {
    server.data.players.setRealName(name);
  }

  public String getConnectionHash() {
    if (connectionHash == null) {
      connectionHash = server.nextHash();
    }
    return connectionHash;
  }

  public String getLoginHash() throws NoSuchAlgorithmException, UnsupportedEncodingException {
    return clientEncryption.getLoginHash(getConnectionHash());
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

  public void setChat(AbstractChat chat) {
    chatType = chat;
  }

  public String getChatRoom() {
    return chatType.toString();
  }

  public void sendMessage(String message) {
    sendMessage(chatType, message);
  }

  public void sendMessage(String message, boolean build) {
    sendMessage(chatType, message, build);
  }

  public void sendMessage(Chat messageType, String message) {
    server.getMessager().propagate(messageType, message);
  }

  public void sendMessage(Chat messageType, String message, boolean build) {
    server.getMessager().propagate(messageType, message, build);
  }

  public void forwardMessage(String message) {
    forwardMessages.add(message);
  }

  public boolean hasForwardMessages() {
    return !forwardMessages.isEmpty();
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

  public String getForwardMessage() {
    return forwardMessages.remove();
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

  public void setGuest(boolean guest) {
    this.guest = guest;
  }

  public boolean isGuest() {
    return guest;
  }

  public void setUsedAuthenticator(boolean usedAuthenticator) {
    this.usedAuthenticator = usedAuthenticator;
  }

  public boolean usedAuthenticator() {
    return usedAuthenticator;
  }

  public String getIPAddress() {
    return extsocket.getInetAddress().getHostAddress();
  }

  public InetAddress getInetAddress() {
    return extsocket.getInetAddress();
  }

  public boolean ignoresChestLocks() {
    return groupObject.ignoreChestLocks;
  }

  private void setDeathPlace(Position deathPosition) {
    deathPlace = deathPosition;
  }

  public Position getDeathPlace() {
    return deathPlace;
  }

  public short getHealth() {
    return health;
  }

  public void updateHealth(short health) {
    this.health = health;
    if (health <= 0) {
      setDeathPlace(new Position(position()));
    }
  }

  public short getExperience() {
    return experience;
  }

  public short getExperienceLevel() {
    return experienceLevel;
  }

  public void updateExperience(float bar, short level, short experience) {
    experienceLevel = level;
    this.experience = experience;
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

  public String parseCommand(String message, boolean overridePermissions) {
    // TODO: Handle aliases of external commands

    if (closed) {
      return null;
    }

    // Repeat last command
    if (message.equals(server.getCommandParser().commandPrefix() + "!")) {
      message = lastCommand;
    } else {
      lastCommand = message;
    }

    String commandName = message.split(" ")[0].substring(1).toLowerCase();
    String args = commandName.length() + 1 >= message.length() ? "" : message.substring(commandName.length() + 2);
    CommandConfig config = server.config.commands.getTopConfig(commandName);
    String originalName = config == null ? commandName : config.originalName;

    PlayerCommand command = server.resolvePlayerCommand(originalName, groupObject);

    if (config != null && !overridePermissions) {
      Permission permission = server.config.getCommandPermission(config.name, args, position.coordinate());
      if (!permission.contains(this)) {
        addTMessage(Color.RED, "Insufficient permission.");
        return null;
      }
    }

    try {
      if (server.options.getBoolean("enableEvents") && config.event != null) {
        Event e = server.eventhost.findEvent(config.event);
        if (e != null) {
          ArrayList<String> arguments = new ArrayList<String>();
          if (!args.equals("")) {
            arguments = new ArrayList<String>(java.util.Arrays.asList(args.split("\\s+")));
          }
          server.eventhost.execute(e, this, true, arguments);
        } else {
          System.out.println("Error in player command " + originalName + ": Event " + config.event + " not found!");
        }
      }
    } catch (NullPointerException e) {
      System.out.println("Error evaluating player command: " + originalName);
    }

    if (!(command instanceof ExternalCommand) && (config == null || config.forwarding != Forwarding.ONLY)) {
      command.execute(this, message);
    }

    if (command instanceof ExternalCommand) {
      // commands with bound events have to be forwarded explicitly
      // (to prevent unknown command error by server)
      if (config.event != null && config.forwarding == Forwarding.NONE) {
        return null;
      } else {
        return "/" + originalName + " " + args;
      }
    } else if ((config != null && config.forwarding != Forwarding.NONE) || server.config.properties.getBoolean("forwardAllCommands")) {
      return message;
    } else {
      return null;
    }
  }

  public void execute(Class<? extends PlayerCommand> c) {
    execute(c, "");
  }

  public void execute(Class<? extends PlayerCommand> c, String arguments) {
    server.getCommandParser().getPlayerCommand(c).execute(this, "a " + arguments);
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

  public void give(int id, int amount) {
    String baseCommand = getName() + " " + id + " ";
    for (int c = 0; c < amount / 64; ++c) {
      server.runCommand("give", baseCommand + 64);
    }
    if (amount % 64 != 0) {
      server.runCommand("give", baseCommand + amount % 64);
    }
  }

  public void give(int id, short damage, int amount) throws ConnectException {
    if (damage == 0) {
      give(id, amount);
    } else {
      Giver giver = new Giver(this);
      for (int c = 0; c < amount / 64; ++c) {
        giver.add(id, 64, damage);
      }
      if (amount % 64 != 0) {
        giver.add(id, amount % 64, damage);
      }
      server.bots.connect(giver);
    }
  }

  public void give(Kit kit) throws ConnectException {
    Giver giver = new Giver(this);
    int invSize = 45;
    int slot = invSize;

    for (Kit.Entry e : kit.items) {
      if (e.damage() == 0) {
        give(e.item(), e.amount());
      } else {
        int restAmount = e.amount();
        while (restAmount > 0 && --slot >= 0) {
          giver.add(e.item(), Math.min(restAmount, 64), e.damage());
          restAmount -= 64;

          if (slot == 0) {
            slot = invSize;
            server.bots.connect(giver);
            giver = new Giver(this);
          }
        }
      }
    }

    if (slot != invSize) {
      server.bots.connect(giver);
    }
  }

  public void updateGroup() {
    try {
      groupObject = server.config.getGroup(this);
    } catch (SAXException e) {
      print("A player could not be assigned to any group. (" + e + ")");
      kick("You could not be asigned to any group.");
      return;
    }
    group = groupObject.id;
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
    stats[1] = server.data.players.stats.get(this, StatField.PLAY_TIME) + stats[0];
    stats[2] = server.data.players.stats.add(this, StatField.BLOCKS_PLACED, blocksPlaced);
    stats[3] = server.data.players.stats.add(this, StatField.BLOCKS_DESTROYED, blocksDestroyed);

    blocksPlaced = 0;
    blocksDestroyed = 0;
    server.data.save();

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
      server.authenticator.unbanLogin(this);
      if (usedAuthenticator) {
        if (guest) {
          server.authenticator.releaseGuestName(name);
        } else {
          server.authenticator.rememberAuthentication(name, getIPAddress());
        }
      } else if (guest) {
        if (isKicked) {
          server.authenticator.releaseGuestName(name);
        } else {
          server.authenticator.rememberGuest(name, getIPAddress());
        }
      }

      server.data.players.stats.add(this, StatField.PLAY_TIME, (int) (System.currentTimeMillis() - connected) / 1000 / 60);
      server.data.players.stats.add(this, StatField.BLOCKS_DESTROYED, blocksDestroyed);
      server.data.players.stats.add(this, StatField.BLOCKS_PLACED, blocksPlaced);
      server.data.save();

      server.playerList.removePlayer(this);
      name = renameName = null;
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
        print("Socket Closed: "
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
          print("Disconnecting " + getIPAddress()
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
    position.updateDimension(dimension);
  }

  public Dimension getDimension() {
    return position.dimension();
  }

  public void teleport(Coordinate coordinate) throws ConnectException, IOException {
    teleport(new Position(coordinate));
  }

  public void teleport(Position position) throws ConnectException, IOException {
    if (position.dimension() == getDimension()) {
      server.bots.connect(new Teleporter(this, position));
    } else {
      addTMessage(Color.RED, "You're not in the same dimension as the specified warppoint.");
    }
  }

  public void teleportSelf(Coordinate coordinate) {
    teleportSelf(new Position(coordinate));
  }

  public void teleportSelf(Position position) {
    try {
      teleport(position);
    } catch (Exception e) {
      addTMessage(Color.RED, "Teleporting failed.");
      return;
    }
    lastTeleport = System.currentTimeMillis();
  }

  private int cooldownLeft() {
    int cooldown = getGroup().cooldown();
    if (lastTeleport > System.currentTimeMillis() - cooldown) {
      return (int) (cooldown - System.currentTimeMillis() + lastTeleport);
    } else {
      return 0;
    }
  }

  public synchronized void teleportWithWarmup(Coordinate coordinate) {
    teleportWithWarmup(new Position(coordinate));
  }

  public synchronized void teleportWithWarmup(Position position) {
    int cooldown = cooldownLeft();
    if (lastTeleport < 0) {
      addTMessage(Color.RED, "You are already waiting for a teleport.");
    } else if (cooldown > 0) {
      addTMessage(Color.RED, "You have to wait %d seconds before you can teleport again.", cooldown / 1000);
    } else {
      int warmup = getGroup().warmup();
      if (warmup > 0) {
        lastTeleport = -1;
        Timer timer = new Timer();
        timer.schedule(new Warmup(position), warmup);
        addTMessage(Color.GRAY, "You will be teleported in %s seconds.", warmup / 1000);
      } else {
        teleportSelf(position);
      }
    }
  }

  public void checkAreaEvents() {
    HashSet<Area> areas = new HashSet<Area>(server.config.dimensions.areas(position()));
    HashSet<Area> areasCopy = new HashSet<Area>(areas);
    HashSet<Area> oldAreas = currentAreas;

    areasCopy.removeAll(oldAreas); // -> now contains only newly entered areas
    oldAreas.removeAll(areas); // -> now contains only areas not present anymore

    for (Area a : areasCopy) { // run area onenter events
      if (a.event == null) {
        continue;
      }
      Event e = server.eventhost.findEvent(a.event);
      if (e != null) {
        ArrayList<String> args = new ArrayList<String>();
        args.add("enter");
        args.add(a.name);
        server.eventhost.execute(e, this, true, args);
      } else {
        System.out.println("Error in area " + a.name + "/event: Event " + a.event + " not found!");
      }
    }

    for (Area a : oldAreas) { // run area onleave events
      if (a.event == null) {
        continue;
      }
      Event e = server.eventhost.findEvent(a.event);
      if (e != null) {
        ArrayList<String> args = new ArrayList<String>();
        args.add("leave");
        args.add(a.name);
        server.eventhost.execute(e, this, true, args);
      } else {
        System.out.println("Error in area " + a.name + "/event: Event " + a.event + " not found!");
      }
    }

    currentAreas = areas;
  }

  public void checkLocationEvents() {
    checkAreaEvents();

    long currtime = System.currentTimeMillis();
    if (currtime < lastEvent + 500) {
      return;
    }

    Iterator<Event> it = server.eventhost.events.keySet().iterator();
    while (it.hasNext()) {
      Event ev = it.next();
      if (!ev.type.equals("plate") || ev.coordinate == null) {
        continue;
      }
      if (position.coordinate().equals(ev.coordinate)) { // matching -> execute
        server.eventhost.execute(ev, this, false, null);
        lastEvent = currtime;
      }
    }
  }

  public void checkButtonEvents(Coordinate c) {
    long currtime = System.currentTimeMillis();
    if (currtime < lastEvent + 500) {
      return;
    }

    Iterator<Event> it = server.eventhost.events.keySet().iterator();
    while (it.hasNext()) {
      Event ev = it.next();
      if (!ev.type.equals("button") || ev.coordinate == null) {
        continue;
      }
      if ((new Coordinate(c.x(), c.y(), c.z(), position.dimension())).equals(ev.coordinate)) { // matching
                                                                                               // ->
                                                                                               // execute
        server.eventhost.execute(ev, this, false, null);
        lastEvent = currtime;
      }
    }
  }

  private final class Warmup extends TimerTask {
    private final Position position;

    private Warmup(Position position) {
      super();
      this.position = position;
    }

    @Override
    public void run() {
      teleportSelf(position);
    }
  }
}
