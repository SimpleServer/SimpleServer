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
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import simpleserver.bot.BotController;
import simpleserver.command.ExternalCommand;
import simpleserver.command.PlayerCommand;
import simpleserver.config.GiveAliasList;
import simpleserver.config.HelpText;
import simpleserver.config.IPBanList;
import simpleserver.config.KitList;
import simpleserver.config.MOTD;
import simpleserver.config.MuteList;
import simpleserver.config.ReadFiles;
import simpleserver.config.RobotList;
import simpleserver.config.Rules;
import simpleserver.config.WhiteList;
import simpleserver.config.data.GlobalData;
import simpleserver.config.xml.CommandConfig;
import simpleserver.config.xml.Config;
import simpleserver.config.xml.GlobalConfig;
import simpleserver.config.xml.Group;
import simpleserver.events.EventHost;
import simpleserver.export.CustAuthExport;
import simpleserver.lang.Translations;
import simpleserver.log.AdminLog;
import simpleserver.log.ConnectionLog;
import simpleserver.log.ErrorLog;
import simpleserver.log.MessageLog;
import simpleserver.message.Chat;
import simpleserver.message.Messager;
import simpleserver.minecraft.MinecraftWrapper;
import simpleserver.nbt.WorldFile;
import simpleserver.options.Options;
import simpleserver.rcon.RconServer;
import simpleserver.stream.Encryption.ClientEncryption;
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

  public static final LocalAddressFactory addressFactory = new LocalAddressFactory();

  public Options options;
  public MOTD motd;
  public KitList kits;
  public Rules rules;
  public HelpText helptext;
  public IPBanList ipBans;
  public WhiteList whitelist;
  public MuteList mutelist;
  public GiveAliasList giveAliasList;
  public GlobalData data;
  private RobotList robots;
  public ReadFiles docs;
  public Config config;
  private GlobalConfig globalConfig;

  private SecureRandom random = new SecureRandom();

  public PlayerList playerList;
  public Authenticator authenticator;
  private List<Resource> resources;

  private CommandParser commandParser;
  private Messager messager;
  private AdminLog adminLog;
  private ErrorLog errorLog;
  private ConnectionLog connectionLog;
  private MessageLog messageLog;
  private SystemInputQueue systemInput;

  public CustAuthExport custAuthExport;

  private MinecraftWrapper minecraft;
  private RconServer rconServer;
  private TelnetServer telnetServer;
  private AutoRun c10t;
  public AutoFreeSpaceChecker autoSpaceCheck;
  private AutoBackup autoBackup;
  private AutoSave autosave;
  private AutoRestart autoRestart;
  public RequestTracker requestTracker;

  public EventHost eventhost;

  public long mapSeed;

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

  public String nextHash() {
    return Long.toHexString(random.nextLong() & 0x7fffffff);
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

  public boolean loadResources() {
    for (Resource resource : resources) {
      resource.load();
    }

    if (playerList != null) {
      playerList.updatePlayerGroups();
    }

    if (globalConfig.loadsuccess) {
      config = globalConfig.config;
    } else {
      print("Syntax error in config.xml! Config was not reloaded.");
      return false;
    }

    if (!Translations.getInstance().setLanguage(config.properties.get("serverLanguage"))) {
      options.set("serverLanguage", "en");
      options.save();
    }

    addressFactory.toggle(!config.properties.getBoolean("disableAddressFactory"));

    // reload events from config
    if (eventhost != null) {
      eventhost.loadEvents();
    }

    saveResources();

    return globalConfig.loadsuccess;
  }

  public void saveResources() {
    if (eventhost != null) {
      eventhost.saveGlobalVars();
    }

    for (Resource resource : resources) {
      resource.save();
    }
  }

  public void saveConfig() {
    globalConfig.save();
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
    int radius = config.properties.getInt("localChatRadius");
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

  public PlayerCommand resolvePlayerCommand(String commandName, Group groupObject) {
    CommandConfig cmdconfig = config.commands.getTopConfig(commandName);
    String originalName = cmdconfig == null ? commandName : cmdconfig.originalName;

    PlayerCommand command;
    if (cmdconfig == null) {
      command = commandParser.getPlayerCommand(commandName);
      if (command != null && !command.hidden()) {
        command = null;
      }
    } else {
      command = commandParser.getPlayerCommand(originalName);
    }

    if (command == null) {
      if ((groupObject != null && groupObject.forwardUnknownCommands) || cmdconfig != null) {
        command = new ExternalCommand(commandName);
      } else {
        command = commandParser.getPlayerCommand((String) null);
      }
    }

    return command;
  }

  public PlayerCommand resolvePlayerCommand(String commandName) {
    return resolvePlayerCommand(commandName, null);
  }

  public void runCommand(String command, String arguments) {
    minecraft.execute(command, arguments);
  }

  public Messager getMessager() {
    return messager;
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

  public void messageLog(Chat chat, String message) {
    messageLog.addMessage(chat, message);
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

    resources.add(options = new Options());
    resources.add(globalConfig = new GlobalConfig(options));
    resources.add(robots = new RobotList());
    resources.add(motd = new MOTD());
    resources.add(rules = new Rules());
    resources.add(helptext = new HelpText());
    resources.add(kits = new KitList());
    resources.add(ipBans = new IPBanList());
    resources.add(whitelist = new WhiteList());
    resources.add(mutelist = new MuteList());
    resources.add(giveAliasList = new GiveAliasList());
    resources.add(data = new GlobalData());
    resources.add(docs = new ReadFiles());

    time = new Time(this);
    bots = new BotController(this);

    systemInput = new SystemInputQueue();
    adminLog = new AdminLog();
    errorLog = new ErrorLog();
    connectionLog = new ConnectionLog();

    commandParser = new CommandParser(options);
  }

  private void cleanup() {
    systemInput.stop();
    adminLog.stop();
    errorLog.stop();
    connectionLog.stop();
    messageLog.stop();
    time.unfreeze();
    bots.cleanup();
  }

  private void startup() {
    restart = false;

    loadResources();

    if (!globalConfig.loadsuccess) {
      print("Syntax error in config.xml! Emergency shutdown...");
      System.exit(1);
    }

    authenticator = new Authenticator(this);
    playerList = new PlayerList(this);
    requestTracker = new RequestTracker(this);
    messager = new Messager(this);

    if (options.getBoolean("enableCustAuthExport")) {
      resources.add(custAuthExport = new CustAuthExport(this));
      custAuthExport.load();
    }

    messageLog = new MessageLog(config.properties.get("logMessageFormat"), config.properties.getBoolean("logMessages"));

    minecraft = new MinecraftWrapper(this, options, systemInput);
    if (!minecraft.prepareServerJar()) {
      print("Please download minecraft_server.jar to the folder with SimpleServer.jar.");
      System.exit(1);
    }

    try {
      minecraft.start();
    } catch (InterruptedException e) {
      // Severe error happened while starting up.
      // Already on track to stop/restart.
    }

    try {
      ClientEncryption.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      print("Error while generating RSA key pair");
      e.printStackTrace();
      System.exit(1);
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
    if (data.freezeTime() >= 0) {
      time.freeze(data.freezeTime());
    }

    if (options.getBoolean("enableEvents")) {
      eventhost = new EventHost(this);
    }

    bots.ready();
  }

  private void shutdown() {
    print("Stopping Server...");
    save = false;

    bots.stop();

    if (!saveLock.tryAcquire()) {
      print("Server is currently Backing Up/Saving...");
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
    authenticator.finalize();
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
    print("Server stopped successfully!");
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
            print(e);
            print("Invalid listening address " + ip);
            break;
          }
        }

        try {
          socket = new ServerSocket(port, 0, address);
        } catch (IOException e) {
          print(e);
          print("Could not listen on port " + port
              + "!\nIs it already in use? Exiting application...");
          break;
        }

        print("Wrapper listening on "
            + socket.getInetAddress().getHostAddress() + ":"
            + socket.getLocalPort() + " (connect here)");
        if (socket.getInetAddress().getHostAddress().equals("0.0.0.0")) {
          print("Note: 0.0.0.0 means all"
              + " IP addresses; you want this.");
        }

        try {
          while (run) {
            Socket client;
            try {
              client = socket.accept();
            } catch (IOException e) {
              if (run && !restart) {
                print(e);
                print("Accept failed on port "
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

  public void setMapSeed(long seed) {
    if (mapSeed != seed) {
      mapSeed = seed;
      // System.out.println("[MAP SEED] " + mapSeed);
    }
  }

  public long getMapSeed() {
    return mapSeed;
  }

  public static final class LocalAddressFactory {
    private static final int[] octets = { 0, 0, 1 };
    private static Boolean canCycle = null;
    private static boolean enabled = true;

    private void toggle(boolean enabled) {
      LocalAddressFactory.enabled = enabled;
    }

    public synchronized String getNextAddress() {
      if (!enabled || !canCycle()) {
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

}
