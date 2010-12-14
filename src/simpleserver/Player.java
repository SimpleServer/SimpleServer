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

import simpleserver.command.AbstractCommand;
import simpleserver.threads.DelayClose;

public class Player {
  private Socket intsocket;
  private Socket extsocket;
  private Thread t1;
  private Thread t2;
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

  public void sendHome() {
    clientToServer.addPacket(new byte[] { 0x03, 0x07, '/', 'h', 'o', 'm', 'e' });
  }

  public boolean setName(String name) throws InterruptedException {
    t1.setName(t1.getName() + "-serverToClient-" + name);
    t2.setName(t2.getName() + "-clientToServer-" + name);
    // timeout.setName("timeoutThread-"+name);
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
    server.requireBackup = true;
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
    delayClose();
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

  public boolean parseCommand(String message) throws InterruptedException,
      IOException {
    if (closed) {
      return true;
    }

    AbstractCommand abstractCommand = server.getCommandList()
                                            .getCommand(message);
    if (abstractCommand == null) {
      return false;
    }

    if (abstractCommand.getName() != null
        && !commandAllowed(abstractCommand.getName())) {
      addMessage("\302\247cInsufficient permission.");
      return true;
    }

    abstractCommand.execute(this, message);
    return !(abstractCommand.passThrough() && server.options.getBoolean("useSMPAPI"))
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
    int nameGroup = server.members.checkName(name);
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

  public void delayClose() {
    new Thread(new DelayClose(this)).start();
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
    try {
      parent.requestTracker.addRequest(extsocket.getInetAddress()
                                                .getHostAddress());
    }
    catch (InterruptedException e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }
    if (parent.isIPBanned(extsocket.getInetAddress().getHostAddress())) {
      System.out.println("[SimpleServer] IP "
          + extsocket.getInetAddress().getHostAddress() + " is banned!");
      kick("Banned IP!");
    }
    try {
      intsocket = new Socket("localhost", parent.options.getInt("internalPort"));
    }
    catch (Exception e2) {
      e2.printStackTrace();
      if (parent.options.getBoolean("exitOnFailure")) {
        System.exit(-1);
      }
      else {
        parent.forceRestart();
      }
    }

    try {

      t1 = new Thread(
                      serverToClient = new StreamTunnel(
                                                        intsocket.getInputStream(),
                                                        extsocket.getOutputStream(),
                                                        true, this, 2048));
      t2 = new Thread(
                      clientToServer = new StreamTunnel(
                                                        extsocket.getInputStream(),
                                                        intsocket.getOutputStream(),
                                                        false, this));
      t1.start();
      t2.start();
    }
    catch (Exception e) {
      e.printStackTrace();
      try {
        close();
      }
      catch (InterruptedException e1) {
        e1.printStackTrace();
      }
    }

    if (isRobot) {
      parent.addRobotPort(intsocket.getLocalPort());
    }
  }

  // Public Methods:
  public boolean testTimeout() {
    if (!closed) {
      if (System.currentTimeMillis() - serverToClient.lastRead > StreamTunnel.IDLE_TIME
          || System.currentTimeMillis() - clientToServer.lastRead > StreamTunnel.IDLE_TIME) {
        /*
        if (!isRobot)
          System.out.println("[SimpleServer] Disconnecting " + getIPAddress() + " due to inactivity.");
        try {
          close();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        */
        return true;
      }
    }
    return false;
  }

  public boolean isClosed() {
    return closed;
  }

  public void close() throws InterruptedException {
    // Don't spam the console! : )
    // And don't close if we're already closing!
    if (!isKicked && server != null) {
      server.notifyClosed(this);
    }
    if (!closed) {
      closed = true;
      cleanup();
    }
  }

  public void cleanup() {
    try {
      t1.interrupt();
    }
    catch (Exception e) {
    }
    try {
      t2.interrupt();
    }
    catch (Exception e) {
    }
    try {
      extsocket.close();
    }
    catch (Exception e) {
    }
    try {
      intsocket.close();
    }
    catch (Exception e) {
    }
    intsocket = null;
    if (!isRobot && extsocket != null) {
      System.out.println("[SimpleServer] Socket Closed: "
          + extsocket.getInetAddress().getHostAddress());
    }
    extsocket = null;
    t1 = null;
    t2 = null;
    clientToServer = null;
    serverToClient = null;
    name = null;

  }

  public void reinitialize(Socket inc) {
    extsocket = inc;
    isRobot = false;
    name = null;
    closed = false;
    isKicked = false;
    attemptLock = false;
    instantDestroy = false;
    kickMsg = null;
    x = 0;
    y = 0;
    z = 0;
    group = 0;
    groupObject = null;

    if (server.isRobot(extsocket.getInetAddress().getHostAddress())) {
      System.out.println("[SimpleServer] Robot Heartbeat: "
          + extsocket.getInetAddress().getHostAddress() + ".");
      isRobot = true;
    }
    if (!isRobot) {
      System.out.println("[SimpleServer] IP Connection from "
          + extsocket.getInetAddress().getHostAddress() + "!");
    }
    try {
      server.requestTracker.addRequest(extsocket.getInetAddress()
                                                .getHostAddress());
    }
    catch (InterruptedException e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }
    if (server.isIPBanned(extsocket.getInetAddress().getHostAddress())) {
      System.out.println("[SimpleServer] IP "
          + extsocket.getInetAddress().getHostAddress() + " is banned!");
      kick("Banned IP!");
    }
    try {
      intsocket = new Socket("localhost", server.options.getInt("internalPort"));
    }
    catch (Exception e2) {
      e2.printStackTrace();
      if (server.options.getBoolean("exitOnFailure")) {
        System.exit(-1);
      }
      else {
        server.forceRestart();
      }
    }

    try {

      t1 = new Thread(
                      serverToClient = new StreamTunnel(
                                                        intsocket.getInputStream(),
                                                        extsocket.getOutputStream(),
                                                        true, this, 2048));
      t2 = new Thread(
                      clientToServer = new StreamTunnel(
                                                        extsocket.getInputStream(),
                                                        intsocket.getOutputStream(),
                                                        false, this));
      t1.start();
      t2.start();
    }
    catch (Exception e) {
      e.printStackTrace();
      try {
        close();
      }
      catch (InterruptedException e1) {
        e1.printStackTrace();
      }
    }

    if (isRobot) {
      server.addRobotPort(intsocket.getLocalPort());
    }
  }

  public boolean isRobot() {
    return isRobot;
  }

  public boolean give(String rawItem, String rawAmount)
      throws InterruptedException {
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

    String baseCommand = "give " + getName() + " " + item + " ";
    for (int c = 0; c < amount / 64; ++c) {
      server.runCommand(baseCommand + 64);
    }
    server.runCommand(baseCommand + amount % 64);

    return true;
  }

  public void teleportTo(Player target) throws InterruptedException {
    server.runCommand("tp " + getName() + " " + target.getName());
  }

  public void sendMOTD() {
    String[] lines = server.getMOTD().split("\\r?\\n");
    for (int i = 0; i < lines.length; i++) {
      addMessage(lines[i]);
    }
  }
}
