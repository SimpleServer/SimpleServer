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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import simpleserver.bot.BotController;
import simpleserver.config.ChestList;
import simpleserver.config.GiveAliasList;
import simpleserver.config.GlobalData;
import simpleserver.config.HelpText;
import simpleserver.config.IPBanList;
import simpleserver.config.KitList;
import simpleserver.config.MOTD;
import simpleserver.config.MuteList;
import simpleserver.config.PermissionConfig;
import simpleserver.config.RobotList;
import simpleserver.config.Rules;
import simpleserver.config.Stats;
import simpleserver.config.WhiteList;
import simpleserver.lang.Translations;
import simpleserver.log.AdminLog;
import simpleserver.log.ConnectionLog;
import simpleserver.log.ErrorLog;
import simpleserver.minecraft.MinecraftWrapper;
import simpleserver.nbt.WorldFile;
import simpleserver.options.Options;
import simpleserver.rcon.RconServer;
import simpleserver.telnet.TelnetServer;
import simpleserver.thread.AutoBackup;
import simpleserver.thread.AutoFreeSpaceChecker;
import simpleserver.thread.AutoRestart;
import simpleserver.thread.AutoRun;
import simpleserver.thread.AutoSave;
import simpleserver.thread.RequestTracker;
import simpleserver.thread.SystemInputQueue;

public class Server {

  private final Listener listener;

  private ServerSocket socket;
  private List<String> outputLog = new LinkedList<String>();

  public Options options;
  public MOTD motd;
  public KitList kits;
  public ChestList chests;
  public Rules rules;
  public HelpText helptext;
  public IPBanList ipBans;
  public WhiteList whitelist;
  public MuteList mutelist;
  public GiveAliasList giveAliasList;
  public Stats stats;
  public GlobalData data;
  private RobotList robots;

  public PermissionConfig permissions;

  public PlayerList playerList;
  private List<Resource> resources;
  private CommandParser commandParser;

  private AdminLog adminLog;
  private ErrorLog errorLog;
  private ConnectionLog connectionLog;
  private SystemInputQueue systemInput;

  private MinecraftWrapper minecraft;
  private RconServer rconServer;
  private TelnetServer telnetServer;
  private AutoRun c10t;
  public AutoFreeSpaceChecker autoSpaceCheck;
  private AutoBackup autoBackup;
  private AutoSave autosave;
  private AutoRestart autoRestart;
  public RequestTracker requestTracker;

  private boolean run = true;
  private boolean restart = false;
  private boolean save = false;

  public Semaphore saveLock = new Semaphore(1);

  public Time time;
  public BotController bots;
  public WorldFile world;

  public Server() {
    listener = new Listener();
    listener.start();
    listener.setName("SimpleServerListener");
  }

  public void restart() {
    restart = true;
    stop();
  }

  public void stop() {
    run = restart;

    try {
      socket.close();
    } catch (Exception e) {
    }

    listener.interrupt();
  }

  public void addRobot(Player p) {
    robots.addRobot(p.getIPAddress());
  }

  public boolean isRobot(String ipAddress) {
    return robots.isRobot(ipAddress);
  }

  public void addRobotPort(int port) {
    robots.addRobotPort(port);
  }

  public void removeRobotPort(int port) {
    robots.removeRobotPort(port);
  }

  public List<Resource> getResources() {
    return resources;
  }

  public Integer[] getRobotPorts() {
    if (robots != null) {
      return robots.getRobotPorts();
    }
    return null;
  }

  public boolean cmdAllowed(String cmd, Player p) {
    return permissions.playerCommandAllowed(cmd, p);
  }

  public int numPlayers() {
    return playerList.size();
  }

  public boolean isIPBanned(String ipAddress) {
    return ipBans.isBanned(ipAddress);
  }

  public void banKickIP(String ipAddress, String reason) {
    if (!isIPBanned(ipAddress)) {
      ipBans.addBan(ipAddress);
    }
    adminLog("IP Address " + ipAddress + " was banned:\t " + reason);
    for (Player player : playerList.getArray()) {
      if (player.getIPAddress().equals(ipAddress)) {
        player.kick(reason);
        adminLog("Player " + player.getName() + " was ip-banned:\t " + reason);
      }
    }
  }

  public void banKickIP(String ipAddress) {
    banKickIP(ipAddress, t("Banned!"));
  }

  public void banKick(String name, String msg) {
    if (name != null) {
      runCommand("ban", name);
      Player p = playerList.findPlayer(name);
      if (p != null) {
        adminLog("Player " + p.getName() + " was banned:\t " + msg);
        p.kick(msg);
      }
    }
  }

  public void banKick(String name) {
    banKick(name, t("Banned!"));
  }

  public void kick(String name, String reason) {
    Player player = playerList.findPlayer(name);
    if (player != null) {
      player.kick(reason);
    }
  }

  public void loadResources() {
    for (Resource resource : resources) {
      resource.load();
    }
    if (playerList != null) {
      playerList.updatePlayerGroups(); // reflect changes of permission.xml
      // without player relogin
    }

    if (!Translations.getInstance().setLanguage(options.get("serverLanguage"))) {
      options.set("serverLanguage", "en");
      options.save();
    }

    commandParser.reload();
  }

  public void saveResources() {
    for (Resource resource : resources) {
      resource.save();
    }
  }

  public String findName(String prefix) {
    Player i = playerList.findPlayer(prefix);
    if (i != null) {
      return i.getName();
    }

    return null;
  }

  public Player findPlayer(String prefix) {
    return playerList.findPlayer(prefix);
  }

  public Player findPlayerExact(String exact) {
    return playerList.findPlayerExact(exact);
  }

  public void updateGroup(String name) {
    Player p = playerList.findPlayer(name);
    if (p != null) {
      p.updateGroup();
    }
  }

  public void updateGroups() {
    playerList.updatePlayerGroups();
  }

  public int localChat(Player player, String msg) {
    int localPlayers = 0;
    int radius = options.getInt("localChatRadius");
    for (Player friend : playerList.getArray()) {
      if (friend.distanceTo(player) < radius) {
        friend.addCaptionedMessage(t("%s says", player.getName()), msg);
        if (player != friend) {
          localPlayers++;
        }
      }
    }
    return localPlayers;
  }

  public void addOutputLine(String s) {
    synchronized (outputLog) {
      int size = outputLog.size();
      for (int c = 0; c <= size - 30; ++c) {
        outputLog.remove(0);
      }
      outputLog.add(s);
    }
  }

  public String[] getOutputLog() {
    synchronized (outputLog) {
      return outputLog.toArray(new String[outputLog.size()]);
    }
  }

  public CommandParser getCommandParser() {
    return commandParser;
  }

  public void runCommand(String command, String arguments) {
    minecraft.execute(command, arguments);
  }

  public void adminLog(String message) {
    adminLog.addMessage(message);
  }

  public void errorLog(Exception exception, String message) {
    errorLog.addMessage(exception, message);
  }

  public void connectionLog(String type, Socket socket, String comments) {
    connectionLog.addMessage(type, socket, comments);
  }

  public boolean isRestarting() {
    return restart;
  }

  public boolean isStopping() {
    return !run;
  }

  public boolean isSaving() {
    return save;
  }

  public void setSaving(boolean save) {
    this.save = save;
  }

  public void forceBackup() {
    autoBackup.forceBackup();
  }

  private void kickAllPlayers() {
    String message = t("Server shutting down!");
    if (restart) {
      message = t("Server restarting!");
    }

    for (Player player : playerList.getArray()) {
      player.kick(message);
    }
  }

  private void initialize() {
    resources = new LinkedList<Resource>();

    resources.add(permissions = new PermissionConfig(this));
    resources.add(options = new Options());
    resources.add(robots = new RobotList());
    resources.add(chests = new ChestList());
    resources.add(motd = new MOTD());
    resources.add(rules = new Rules());
    resources.add(helptext = new HelpText());
    resources.add(kits = new KitList(this));
    resources.add(ipBans = new IPBanList());
    resources.add(whitelist = new WhiteList());
    resources.add(mutelist = new MuteList());
    resources.add(giveAliasList = new GiveAliasList());
    resources.add(stats = new Stats());
    resources.add(data = new GlobalData());

    time = new Time(this);
    bots = new BotController(this);

    systemInput = new SystemInputQueue();
    adminLog = new AdminLog();
    errorLog = new ErrorLog();
    connectionLog = new ConnectionLog();

    commandParser = new CommandParser(options, permissions);
  }

  private void cleanup() {
    systemInput.stop();
    adminLog.stop();
    errorLog.stop();
    connectionLog.stop();
    time.unfreeze();
    bots.cleanup();
  }

  private void startup() {
    restart = false;

    loadResources();

    if (permissions.loadsuccess == false) {
      System.out.println("[SimpleServer] Syntax error in permissions.xml! Emergency shutdown...");
      System.exit(1);
    }

    playerList = new PlayerList(options);
    requestTracker = new RequestTracker(this);

    minecraft = new MinecraftWrapper(this, options, systemInput);
    if (!minecraft.prepareServerJar()) {
      System.out.println("[SimpleServer] Please download minecraft_server.jar to the folder with SimpleServer.jar.");
      System.exit(1);
    }

    try {
      minecraft.start();
    } catch (InterruptedException e) {
      // Severe error happened while starting up.
      // Already on track to stop/restart.
    }

    if (options.getBoolean("enableTelnet")) {
      telnetServer = new TelnetServer(this);
    }
    if (options.getBoolean("enableRcon")) {
      rconServer = new RconServer(this);
    }
    world = new WorldFile(options.get("levelName"));
    autoSpaceCheck = new AutoFreeSpaceChecker(this);
    autoBackup = new AutoBackup(this);
    autosave = new AutoSave(this);
    autoRestart = new AutoRestart(this);
    c10t = new AutoRun(this, options.get("c10tArgs"));
    if (options.contains("freezeTime")) {
      try {
        time.freeze(time.parse(options.get("freezeTime")));
      } catch (Exception e) {
        System.out.println("[SimpleServer] Warning: freezeTime option is not valid");
      }
    }

    bots.ready();
  }

  private void shutdown() {
    System.out.println("[SimpleServer] Stopping Server...");
    save = false;

    bots.stop();

    if (!saveLock.tryAcquire()) {
      System.out.println("[SimpleServer] Server is currently Backing Up/Saving...");
      while (true) {
        try {
          saveLock.acquire();
          break;
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    kickAllPlayers();
    if (telnetServer != null) {
      telnetServer.stop();
    }
    if (rconServer != null) {
      rconServer.stop();
    }
    autoSpaceCheck.cleanup();
    autoBackup.stop();
    autosave.stop();
    autoRestart.stop();
    requestTracker.stop();
    c10t.stop();
    saveResources();

    playerList.waitUntilEmpty();
    minecraft.stop();
    System.out.println("[SimpleServer] Server stopped successfully!");
    saveLock.release();
  }

  private final class Listener extends Thread {
    @Override
    public void run() {
      initialize();

      while (run) {
        startup();

        String ip = options.get("ipAddress");
        int port = options.getInt("port");

        InetAddress address;
        if (ip.equals("0.0.0.0")) {
          address = null;
        } else {
          try {
            address = InetAddress.getByName(ip);
          } catch (UnknownHostException e) {
            System.out.println("[SimpleServer] " + e);
            System.out.println("[SimpleServer] Invalid listening address " + ip);
            break;
          }
        }

        try {
          socket = new ServerSocket(port, 0, address);
        } catch (IOException e) {
          System.out.println("[SimpleServer] " + e);
          System.out.println("[SimpleServer] Could not listen on port " + port
              + "!\nIs it already in use? Exiting application...");
          break;
        }

        System.out.println("[SimpleServer] Wrapper listening on "
            + socket.getInetAddress().getHostAddress() + ":"
            + socket.getLocalPort() + " (connect here)");
        if (socket.getInetAddress().getHostAddress().equals("0.0.0.0")) {
          System.out.println("[SimpleServer] Note: 0.0.0.0 means all"
              + " IP addresses; you want this.");
        }

        try {
          while (run) {
            Socket client;
            try {
              client = socket.accept();
            } catch (IOException e) {
              if (run && !restart) {
                System.out.println("[SimpleServer] " + e);
                System.out.println("[SimpleServer] Accept failed on port "
                    + port + "!");
              }
              break;
            }
            new Player(client, Server.this);
          }
        } finally {
          try {
            socket.close();
          } catch (IOException e) {
          }
        }

        shutdown();
      }

      cleanup();
    }
  }

  public void setTime(long time) {
    this.time.is(time);
  }

  public long time() {
    return time.get();
  }

}
