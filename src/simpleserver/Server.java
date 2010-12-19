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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import simpleserver.config.BlockList;
import simpleserver.config.ChestList;
import simpleserver.config.CommandList;
import simpleserver.config.GiveAliasList;
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
import simpleserver.minecraft.MinecraftWrapper;
import simpleserver.options.Language;
import simpleserver.options.Options;
import simpleserver.rcon.RconServer;
import simpleserver.threads.C10TThread;
import simpleserver.threads.RequestTracker;
import simpleserver.threads.ServerAutoRestart;
import simpleserver.threads.ServerAutoSave;
import simpleserver.threads.ServerBackup;
import simpleserver.threads.SystemInputQueue;

public class Server {
  private static final String version = "RC 6.6.6_stable";
  private static final String license = "SimpleServer -- Copyright (C) 2010 Charles Wagner Jr.";
  private static final String warranty = "This program is licensed under The MIT License.\nSee file LICENSE for details.";

  private final Listener listener;

  private simpleserver.CommandList commandList;

  private ServerSocket socket;

  private List<String> outputLog = new LinkedList<String>();

  public Semaphore saveLock = new Semaphore(1);

  public Language l;
  public Options options;
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
  public GiveAliasList giveAliasList;

  public CommandList commands;

  public AdminLog adminLog;
  private SystemInputQueue systemInput;

  private Thread autoRestartThread;

  private MinecraftWrapper minecraft;
  private RconServer rconServer;
  private C10TThread c10t;
  private ServerBackup serverBackup;
  private ServerAutoSave autosave;
  private ServerAutoRestart autoRestart;
  public RequestTracker requestTracker;

  private boolean run = true;
  private boolean restart = false;
  private boolean save = false;

  private List<Resource> resources;
  public PlayerList playerList;

  public static void main(String[] args) {
    System.out.println(license);
    System.out.println(warranty);
    System.out.println(">> Starting SimpleServer " + version);
    new Server();
  }

  private Server() {
    listener = new Listener();
    listener.start();
  }

  public void restart() {
    restart = true;
    stop();
  }

  public void stop() {
    run = restart;

    try {
      socket.close();
    }
    catch (IOException e) {
    }
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
    return playerList.size();
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

  public void banKickIP(String ipAddress, String reason) {
    if (!isIPBanned(ipAddress)) {
      ipBans.addBan(ipAddress);
    }
    adminLog.addMessage("IP Address " + ipAddress + " was banned:\t " + reason);
    for (Player player : playerList.getArray()) {
      if (player.getIPAddress().equals(ipAddress)) {
        player.kick(reason);
        adminLog.addMessage("Player " + player.getName() + " was ip-banned:\t "
            + reason);
      }
    }
  }

  public void banKickIP(String ipAddress) {
    banKickIP(ipAddress, "Banned!");
  }

  public void banKick(String name, String msg) {
    if (name != null) {
      runCommand("ban", name);
      Player p = playerList.findPlayer(name);
      if (p != null) {
        adminLog.addMessage("Player " + p.getName() + " was banned:\t " + msg);
        p.kick(msg);
      }
    }
  }

  public void banKick(String name) {
    banKick(name, "Banned!");
  }

  public void loadResources() {
    for (Resource resource : resources) {
      resource.load();
    }
  }

  public void saveResources() {
    for (Resource resource : resources) {
      resource.save();
    }
    autosave.forceSave();
  }

  public void forceBackup() {
    serverBackup.forceBackup();
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

  public void kick(String name, String reason) {
    Player player = playerList.findPlayer(name);
    if (player != null) {
      player.kick(reason);
    }
  }

  public void updateGroup(String name) {
    Player p = playerList.findPlayer(name);
    if (p != null) {
      p.updateGroup();
    }
  }

  public int localChat(Player player, String msg) {
    String chat = "\302\2477" + player.getName() + " says: " + msg;
    int localPlayers = 0;
    for (Player friend : playerList.getArray()) {
      int radius = options.getInt("localChatRadius");
      if (friend.distanceTo(player) < radius) {
        friend.addMessage(chat);
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

  public simpleserver.CommandList getCommandList() {
    return commandList;
  }

  public void runCommand(String command, String arguments) {
    minecraft.execute(command, arguments);
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

  private void kickAllPlayers() {
    String message = "Server shutting down!";
    if (restart) {
      message = "Server restarting!";
    }

    for (Player p : playerList.getArray()) {
      p.kick(message);
    }
  }

  private void initialize() {
    resources = new LinkedList<Resource>();
    resources.add(l = new Language());
    resources.add(options = new Options());
    resources.add(robots = new RobotList());
    resources.add(ipMembers = new IPMemberList(options));
    resources.add(chests = new ChestList());
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
    resources.add(giveAliasList = new GiveAliasList());

    systemInput = new SystemInputQueue();
    adminLog = new AdminLog();

    commandList = new simpleserver.CommandList(options);

    autoRestart = new ServerAutoRestart(this);
    autoRestartThread = new Thread(autoRestart);
    autoRestartThread.start();
  }

  private void cleanup() {
    systemInput.stop();
    adminLog.stop();
  }

  private void startup() {
    restart = false;

    loadResources();
    playerList = new PlayerList();
    requestTracker = new RequestTracker(this);

    minecraft = new MinecraftWrapper(this, options, systemInput);
    minecraft.start();

    rconServer = new RconServer(this);
    serverBackup = new ServerBackup(this);
    autosave = new ServerAutoSave(this);
    c10t = new C10TThread(this, options.get("c10tArgs"));
  }

  private void shutdown() {
    System.out.println("Stopping Server...");
    save = false;

    if (!saveLock.tryAcquire()) {
      System.out.println("[SimpleServer] Server is currently Backing Up/Saving...");
      while (true) {
        try {
          saveLock.acquire();
          break;
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    kickAllPlayers();
    rconServer.stop();
    serverBackup.stop();
    autosave.stop();
    requestTracker.stop();
    c10t.stop();
    saveResources();

    while (playerList.size() > 0) {
      try {
        Thread.sleep(100);
      }
      catch (InterruptedException e) {
      }
    }

    minecraft.stop();
    System.out.println("Server stopped successfully!");
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
        }
        else {
          try {
            address = InetAddress.getByName(ip);
          }
          catch (UnknownHostException e) {
            e.printStackTrace();
            System.out.println("Invalid listening address " + ip);
            break;
          }
        }

        try {
          socket = new ServerSocket(port, 0, address);
        }
        catch (IOException e) {
          e.printStackTrace();
          System.out.println("Could not listen on port " + port
              + "!\nIs it already in use? Exiting application...");
          break;
        }

        try {
          while (true) {
            Socket client;
            try {
              client = socket.accept();
            }
            catch (IOException e) {
              if (run && !restart) {
                e.printStackTrace();
                System.out.println("Accept failed on port " + port + "!");
              }
              break;
            }
            new Player(client, Server.this);
          }
        }
        finally {
          try {
            socket.close();
          }
          catch (IOException e) {
          }
        }

        shutdown();
      }

      cleanup();
    }
  }
}
