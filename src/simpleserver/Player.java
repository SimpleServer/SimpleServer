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
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import simpleserver.command.PlayerCommand;
import simpleserver.stream.StreamTunnel;

public class Player {
  private Socket intsocket;
  private Socket extsocket;
  private Server server;
  private String name = null;
  private boolean closed = false;
  private boolean isKicked = false;
  private boolean attemptLock = false;
  private boolean instantDestroy = false;
  private String kickMsg = null;
  private double x, y, z;
  private int group = 0;
  private Group groupObject = null;
  private boolean isRobot = false;

  private StreamTunnel serverToClient;
  private StreamTunnel clientToServer;
  private final Watchdog watchdog;

  private Queue<String> messages = new ConcurrentLinkedQueue<String>();

  public double distanceTo(Player player) {
    return Math.sqrt(Math.pow(x - player.x, 2) + Math.pow(x - player.y, 2)
        + Math.pow(x - player.z, 2));
  }

  public boolean hasExternalConnection() {
    return extsocket != null;
  }

  public boolean hasInternalConnection() {
    return intsocket != null;
  }

  public void updateLocation(double x, double y, double z, double stance) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public boolean isAttemptLock() {
    return attemptLock;
  }

  public void setAttemptLock(boolean state) {
    attemptLock = state;
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

  public boolean setName(String name) {
    if (name == null) {
      kick("Invalid Name!");
      return false;
    }
    if (name.trim().compareTo("") == 0 || name.length() == 0
        || name.trim().length() == 0 || this.name != null) {
      kick("Invalid Name!");
      return false;
    }
    if (server.options.getBoolean("useWhitelist")) {
      if (!server.whitelist.isWhitelisted(name)) {
        kick("You are not whitelisted!");
        return false;
      }
    }
    updateGroup(name.trim());
    this.name = name.trim();

    server.playerList.addPlayer(this);
    return true;
  }

  public void addMessage(String msg) {
    messages.add(msg);
  }

  public String getMessage() {
    return messages.remove();
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

  public boolean isMuted() {
    return server.mutelist.isMuted(name);
  }

  public String getKickMsg() {
    return kickMsg;
  }

  public boolean hasMessages() {
    return !messages.isEmpty();
  }

  public String getName() {
    return name;
  }

  public boolean parseCommand(String message) {
    if (closed) {
      return true;
    }

    PlayerCommand command = server.getCommandList().getPlayerCommand(message);
    if (command == null) {
      return false;
    }

    if (command.getName() != null && !commandAllowed(command.getName())) {
      addMessage("\302\247cInsufficient permission.");
      return true;
    }

    command.execute(this, message);
    return !(command.passThrough() && server.options.getBoolean("useSMPAPI"))
        || message.startsWith("/");
  }

  public boolean commandAllowed(String command) {
    return server.commands.playerAllowed(command, this);
  }

  public int getGroupId() {
    return group;
  }

  public Group getGroup() {
    return groupObject;
  }

  private void updateGroup(String name) {
    int nameGroup = server.members.getGroup(name);
    int ipGroup = server.ipMembers.getGroup(this);
    int defaultGroup = server.options.getInt("defaultGroup");
    if ((nameGroup == -1 || ipGroup == -1 && defaultGroup != -1)
        || (nameGroup == -1 && ipGroup == -1 && defaultGroup == -1)) {
      group = -1;
      if (server.groups.groupExists(group)) {
        groupObject = server.groups.getGroup(group);
      }
      else {
        groupObject = null;
      }
      return;
    }

    if (ipGroup >= nameGroup) {
      group = ipGroup;
    }
    else {
      group = nameGroup;
    }

    if (server.groups.groupExists(group)) {
      groupObject = server.groups.getGroup(group);
    }
    else {
      group = 0;
    }
  }

  public void updateGroup() {
    updateGroup(name);
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

  public Player(Socket inc, Server parent) {
    server = parent;
    extsocket = inc;
    if (parent.isRobot(extsocket.getInetAddress().getHostAddress())) {
      System.out.println("[SimpleServer] Robot Heartbeat: "
          + extsocket.getInetAddress().getHostAddress() + ".");
      isRobot = true;
    }
    if (!isRobot) {
      System.out.println("[SimpleServer] IP Connection from "
          + extsocket.getInetAddress().getHostAddress() + "!");
    }
    parent.requestTracker.addRequest(extsocket.getInetAddress()
                                              .getHostAddress());
    if (parent.isIPBanned(extsocket.getInetAddress().getHostAddress())) {
      System.out.println("[SimpleServer] IP "
          + extsocket.getInetAddress().getHostAddress() + " is banned!");
      kick("Banned IP!");
    }
    try {
      intsocket = new Socket("localhost", parent.options.getInt("internalPort"));
    }
    catch (Exception e) {
      e.printStackTrace();
      if (parent.options.getBoolean("exitOnFailure")) {
        server.stop();
      }
      else {
        parent.restart();
      }
    }

    try {
      serverToClient = new StreamTunnel(intsocket.getInputStream(),
                                        extsocket.getOutputStream(), true, this);
      clientToServer = new StreamTunnel(extsocket.getInputStream(),
                                        intsocket.getOutputStream(), false,
                                        this);
    }
    catch (IOException e) {
      e.printStackTrace();
      cleanup();
    }

    if (isRobot) {
      parent.addRobotPort(intsocket.getLocalPort());
    }

    watchdog = new Watchdog();
    watchdog.start();
  }

  public boolean isClosed() {
    return closed;
  }

  public void close() {
    serverToClient.stop();
    clientToServer.stop();
  }

  public void cleanup() {
    if (!closed) {
      closed = true;
      serverToClient.stop();
      clientToServer.stop();

      if (name != null) {
        server.playerList.removePlayer(this);
      }

      try {
        extsocket.close();
      }
      catch (IOException e) {
      }
      try {
        intsocket.close();
      }
      catch (IOException e) {
      }

      if (!isRobot) {
        System.out.println("[SimpleServer] Socket Closed: "
            + extsocket.getInetAddress().getHostAddress());
      }
    }
  }

  public boolean isRobot() {
    return isRobot;
  }

  public boolean give(String rawItem, String rawAmount) {
    boolean success = true;

    int item = 0;
    try {
      item = Integer.parseInt(rawItem);

      if (item < 0) {
        addMessage("\302\247cItem ID must be positive!");
        success = false;
      }
    }
    catch (NumberFormatException e) {
      addMessage("\302\247cItem ID must be a number!");
      success = false;
    }

    int amount = 1;
    if (rawAmount != null) {
      try {
        amount = Integer.parseInt(rawAmount);

        if ((amount < 1) || (amount > 1000)) {
          addMessage("\302\247cAmount must be within 1-1000!");
          success = false;
        }
      }
      catch (NumberFormatException e) {
        addMessage("\302\247cAmount must be a number!");
        success = false;
      }
    }

    if (!success) {
      addMessage("\302\247cUnable to give " + rawItem);
      return false;
    }

    String baseCommand = getName() + " " + item + " ";
    for (int c = 0; c < amount / 64; ++c) {
      server.runCommand("give", baseCommand + 64);
    }
    server.runCommand("give", baseCommand + amount % 64);

    return true;
  }

  public void teleportTo(Player target) {
    server.runCommand("tp", getName() + " " + target.getName());
  }

  public void sendMOTD() {
    String[] lines = server.getMOTD().split("\\r?\\n");
    for (int i = 0; i < lines.length; i++) {
      addMessage(lines[i]);
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
        }
        catch (InterruptedException e) {
        }
      }

      cleanup();
    }
  }
}
