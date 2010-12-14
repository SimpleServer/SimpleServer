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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import simpleserver.config.BlockList;
import simpleserver.config.ChestList;
import simpleserver.config.CommandList;
import simpleserver.config.GroupList;
import simpleserver.config.IPBanList;
import simpleserver.config.IPMemberList;
import simpleserver.config.ItemWatchList;
import simpleserver.config.KitList;
import simpleserver.config.MOTD;
import simpleserver.config.MemberList;
import simpleserver.config.MuteList;
import simpleserver.config.RobotList;
import simpleserver.config.Rules;
import simpleserver.config.WhiteList;
import simpleserver.log.AdminLog;
import simpleserver.rcon.RconServer;
import simpleserver.threads.C10TThread;
import simpleserver.threads.ErrorStreamRouter;
import simpleserver.threads.InputStreamRouter;
import simpleserver.threads.MinecraftMonitor;
import simpleserver.threads.PlayerScanner;
import simpleserver.threads.ServerAutoRestart;
import simpleserver.threads.ServerAutoSave;
import simpleserver.threads.ServerBackup;
import simpleserver.threads.SocketThread;

public class Server {
  private static String version = "RC 6.6.6_stable";
  private static String license = "SimpleServer -- Copyright (C) 2010 Charles Wagner Jr.";
  private static String warranty = "This program is licensed under The MIT License.\nSee file LICENSE for details.";

  private simpleserver.CommandList commandList;

  private boolean open = true;
  public ServerSocket socket;
  public ServerSocket rconSocket;

  private LinkedList<String> outputLog = new LinkedList<String>();

  public Semaphore saveLock = new Semaphore(1);
  public BlockList blockFirewall;
  public GroupList groups;
  public MemberList members;
  private RobotList robots;
  private MOTD motd;
  public KitList kits;
  public ChestList chests;
  private Rules rules;
  public IPMemberList ipMembers;
  public IPBanList ipBans;
  public ItemWatchList itemWatch;
  public WhiteList whitelist;
  public MuteList mutelist;
  public Options options;
  public CommandList commands;

  public AdminLog adminLog;
  private Thread adminLogThread;

  public Language l;

  private InputStreamRouter input;

  private Thread backupThread;
  private Thread autoSaveThread;
  private Thread autoRestartThread;
  private Thread playerScannerThread;
  private Thread shutDownHook;
  private Thread socketThread;

  private C10TThread c10t;
  private Thread c10tThread;

  private ServerBackup backup;
  private ServerAutoSave autosave;
  private ServerAutoRestart autoRestart;
  private PlayerScanner playerScanner;
  private MinecraftMonitor minecraftMonitor;
  public RequestTracker requestTracker;

  private ForceShutdown forceShutdown;
  private ForceRestart forceRestart;

  boolean requireBackup = false;
  private boolean isSaving = false;
  public boolean isRestarting = false;
  private boolean waitingForStart = false;

  private LinkedList<Config> resources = new LinkedList<Config>();
  public LinkedList<Rcon> rcons = new LinkedList<Rcon>();

  // Minecraft Process
  public Process p;

  // Pipe Threads
  private Thread t;
  private Thread t2;
  private Thread t3;

  public static void main(String[] args) {
    System.out.println(license);
    System.out.println(warranty);
    System.out.println(">> Starting SimpleServer " + version);
    new Server();
  }

  private Server() {
    l = new Language();
    l.load();
    options = new Options();
    options.load();

    new Thread(new RconServer(this, false)).start();

    PlayerFactory.initialize(this, options.getInt("maxPlayers"));

    startMinecraft();
    setShutdownHook();
    initResources();
    loadAll();

    backup = new ServerBackup(this);
    backupThread = new Thread(backup);
    backupThread.start();

    autosave = new ServerAutoSave(this);
    autoSaveThread = new Thread(autosave);
    autoSaveThread.start();

    autoRestart = new ServerAutoRestart(this);
    autoRestartThread = new Thread(autoRestart);
    autoRestartThread.start();

    playerScanner = new PlayerScanner();
    playerScannerThread = new Thread(playerScanner);
    playerScannerThread.start();

    forceShutdown = new ForceShutdown(this);
    forceRestart = new ForceRestart(this);

    requestTracker = new RequestTracker(this);
    new Thread(requestTracker).start();

    openSocket();

    if (options.contains("c10tArgs")) {
      c10t = new C10TThread(this, options.get("c10tArgs"));
      c10tThread = new Thread(c10t);
      c10tThread.start();
    }
  }

  private void initResources() {
    resources.add(robots = new RobotList());
    resources.add(ipMembers = new IPMemberList(options.getInt("defaultGroup")));
    resources.add(chests = new ChestList(this));
    resources.add(commands = new CommandList());
    resources.add(blockFirewall = new BlockList());
    resources.add(itemWatch = new ItemWatchList());
    resources.add(groups = new GroupList());
    resources.add(members = new MemberList(this));
    resources.add(motd = new MOTD());
    resources.add(rules = new Rules());
    resources.add(kits = new KitList(this));
    resources.add(ipBans = new IPBanList());
    resources.add(whitelist = new WhiteList());
    resources.add(mutelist = new MuteList());

  }

  private void startMinecraft() {
    new MinecraftOptions(options).save();
    try {
      int minMemory = 1024;
      if (options.getInt("memory") < minMemory) {
        minMemory = options.getInt("memory");
      }
      String jar = "minecraft_server.jar";
      if (options.contains("alternateJarFile")) {
        jar = options.get("alternateJarFile");
      }
      // String[] cmd = {"java", "-Xmx" + options.memory + "M","-Xms" +
      // minMemory + "M", "-jar", alternateJar, "nogui"};
      String cmd = "java " + options.get("javaArguments") + " -Xmx"
          + options.get("memory") + "M -Xms" + minMemory + "M -jar " + jar
          + " nogui";
      // String[] cmd = {"cmd", "/C", "java -Xmx" + options.memory + "M -Xms" +
      // minMemory + "M -jar " + alternateJar +
      // " nogui 2>test1.txt 1>test.txt"};

      p = Runtime.getRuntime().exec(cmd);
    }
    catch (IOException e) {
      e.printStackTrace();
      System.out.println("[SimpleServer] FATAL ERROR: Could not start minecraft_server.jar!");
      System.exit(-1);
    }
    t = new Thread(new ErrorStreamRouter(p.getErrorStream(), this));
    t2 = new Thread(new ErrorStreamRouter(p.getInputStream(), this));
    t.start();
    t2.start();

    if (t3 == null) {
      input = new InputStreamRouter(System.in, p.getOutputStream(), this);
      t3 = new Thread(input);
      t3.start();
    }
    else {
      input.setOut(p.getOutputStream());
    }
    minecraftMonitor = new MinecraftMonitor(this);
    minecraftMonitor.start();

    waitingForStart = true;
    while (waitingForStart) {
      try {
        Thread.sleep(20);
      }
      catch (InterruptedException e) {
      }
    }

    adminLog = new AdminLog();
    adminLogThread = new Thread(adminLog);
    adminLogThread.start();
  }

  private void stopMinecraft() {
    try {
      runCommand("stop");
    }
    catch (InterruptedException e1) {
    }
    long timer = System.currentTimeMillis();
    boolean stopped = false;
    while (System.currentTimeMillis() - timer < 15 * 1000) {
      try {
        p.exitValue();
        stopped = true;
        break;
      }
      catch (Exception e) {
      }
    }
    if (!stopped) {
      p.destroy();
    }
    t.interrupt();
    t2.interrupt();

    adminLogThread.interrupt();
    adminLog = null;

  }

  public void restart() {
    isRestarting = true;
    open = false;
    try {
      socket.close();
    }
    catch (Exception e) {
    }

    kickAllPlayers("Server Restarting!");

    minecraftMonitor.interrupt();
    stopMinecraft();
    startMinecraft();
    setShutdownHook();

    isRestarting = false;
    openSocket();
  }

  private void kickAllPlayers(String msg) {
    if (msg == null) {
      msg = "";
    }
    for (Iterator<Player> itr = PlayerFactory.iterator(); itr.hasNext();) {
      Player p = itr.next();
      p.kick(msg);
      // itr.remove();
    }
  }

  private void kickAllRcons(String msg) {
    if (msg == null) {
      msg = "";
    }
    synchronized (rcons) {
      for (Iterator<Rcon> itr = rcons.iterator(); itr.hasNext();) {
        Rcon p = itr.next();
        p.kick(msg);
        itr.remove();
      }
    }
  }

  public void stop() {
    saveAll();

    isRestarting = true;
    open = false;
    try {
      socket.close();
    }
    catch (Exception e) {
    }

    kickAllPlayers("Server shutting down!");
    kickAllRcons("Server shutting down!");
    System.out.println("Stopping Server...");
    minecraftMonitor.interrupt();
    stopMinecraft();
    Runtime.getRuntime().removeShutdownHook(shutDownHook);
    System.out.println("Server stopped successfully!");
    System.exit(0);
  }

  public void openSocket() {
    if (socketThread != null) {
      socketThread.interrupt();
    }
    open = true;
    socketThread = new Thread(new SocketThread(this));
    socketThread.start();
  }

  public void stopServer() {

    new Thread(forceShutdown).start();
  }

  public void restartServer() {

    if (!isRestarting) {
      if (saveLock.tryAcquire()) {
        saveLock.release();
      }
      else {
        System.out.println("[SimpleServer] Server is currently Backing Up/Saving...");
      }
      new Thread(forceRestart).start();
    }
    else {
      p.destroy();
    }
  }

  public void forceRestart() {
    restartServer();
  }

  private void setShutdownHook() {
    if (shutDownHook != null) {
      Runtime.getRuntime().removeShutdownHook(shutDownHook);
    }
    Runtime.getRuntime()
           .addShutdownHook(shutDownHook = new Thread(new ShutdownHook(p, this)));

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

  public Integer[] getRobotPorts() {
    if (robots != null) {
      return robots.getRobotPorts();
    }
    return null;
  }

  public boolean cmdAllowed(String cmd, Player p) {
    return commands.playerAllowed(cmd, p);
  }

  public int numPlayers() {
    int n = 0;
    for (Iterator<Player> itr = PlayerFactory.iterator(); itr.hasNext();) {
      Player i = itr.next();
      if (i != null) {
        if (i.getName() != null) {
          n++;
        }
      }
    }
    return n;
  }

  public String getMOTD() {
    return motd.getMOTD();
  }

  public String getRules() {
    return rules.getRules();
  }

  public boolean isIPBanned(String ipAddress) {
    return ipBans.isBanned(ipAddress);
  }

  public void banKickIP(String ipAddress, String reason)
      throws InterruptedException {
    if (!isIPBanned(ipAddress)) {
      ipBans.addBan(ipAddress);
    }
    adminLog.addMessage("IP Address " + ipAddress + " was banned:\t " + reason);
    for (Iterator<Player> itr = PlayerFactory.iterator(); itr.hasNext();) {
      Player p = itr.next();
      if (p.getIPAddress().equals(ipAddress)) {
        p.kick(reason);
        adminLog.addMessage("Player " + p.getName() + " was ip-banned:\t "
            + reason);
        // itr.remove();
      }
    }
  }

  public void banKickIP(String ipAddress) throws InterruptedException {
    banKickIP(ipAddress, "Banned!");
  }

  public void banKick(String name, String msg) throws InterruptedException {
    if (name != null) {
      runCommand("ban " + name);
      Player p = PlayerFactory.findPlayer(name);
      if (p != null) {
        adminLog.addMessage("Player " + p.getName() + " was banned:\t " + msg);
        p.kick(msg);
      }
    }
  }

  public void banKick(String name) throws InterruptedException {
    banKick(name, "Banned!");
  }

  public void runCommand(String msg) throws InterruptedException {
    if (input != null) {
      input.runCommand(msg);
    }
  }

  public void sendToAll(String msg) throws InterruptedException {
    if (input != null && msg != null && !msg.equalsIgnoreCase("")) {
      input.runCommand("say " + msg);
    }
  }

  public void notifyClosed(Player player) throws InterruptedException {
    PlayerFactory.removePlayer(player);
  }

  public void loadAll() {
    for (Config i : resources) {
      i.load();
    }
    l.load();
    options.load();
  }

  public void saveAll() {
    for (Config i : resources) {
      i.save();
    }
    options.save();
  }

  public void forceBackup() {
    requireBackup = true;
    backupThread.interrupt();
  }

  public String findName(String prefix) throws InterruptedException {
    Player i = PlayerFactory.findPlayer(prefix);
    if (i != null) {
      return i.getName();
    }
    else {
      return null;
    }
  }

  public Player findPlayer(String prefix) throws InterruptedException {
    return PlayerFactory.findPlayer(prefix);
  }

  public Player findPlayerExact(String exact) throws InterruptedException {
    return PlayerFactory.findPlayerExact(exact);
  }

  public void kick(String name, String reason) throws InterruptedException {
    synchronized (PlayerFactory.playerLock) {
      Player player = PlayerFactory.findPlayer(name);
      if (player != null) {
        player.kick(reason);
      }
    }
  }

  public void updateGroup(String name) throws InterruptedException {
    Player p = PlayerFactory.findPlayer(name);
    if (p != null) {
      p.updateGroup();
    }
  }

  public int localChat(Player p, String msg) {
    String chat = "\302\2477" + p.getName() + " says: " + msg;
    int j = 0;
    if (p.getName() == null) {
      return 0;
    }
    for (Iterator<Player> itr = PlayerFactory.iterator(); itr.hasNext();) {
      Player i = itr.next();
      if (i.getName() != null) {
        int radius = options.getInt("localChatRadius");
        if (i.distanceTo(p) < radius) {
          i.addMessage(chat);
          if (p != i) {
            j++;
          }
        }
      }
    }
    return j;
  }

  public void notifyClosedRcon(Rcon rcon) {
    synchronized (rcons) {
      rcons.remove(rcon);
    }
  }

  public void addOutputLine(String s) {
    synchronized (outputLog) {
      while (outputLog.size() > 30) {
        outputLog.remove();
      }
      outputLog.add(s);
    }
  }

  public String[] getOutputLog() {
    synchronized (outputLog) {
      String[] a = new String[1];
      return outputLog.toArray(a);
    }
  }

  public boolean isRestarting() {
    return isRestarting;
  }

  public boolean requiresBackup() {
    return requireBackup;
  }

  public void requiresBackup(boolean b) {
    requireBackup = b;
  }

  public void isSaving(boolean b) {
    isSaving = b;
  }

  public void waitingForStart(boolean b) {
    waitingForStart = b;
  }

  public boolean isSaving() {
    return isSaving;
  }

  public boolean isOpen() {
    return open;
  }

  public simpleserver.CommandList getCommandList() {
    if (commandList == null) {
      commandList = new simpleserver.CommandList(options);
    }

    return commandList;
  }

}
