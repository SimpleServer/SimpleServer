/*******************************************************************************
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
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
  private boolean godMode = false;
  private String kickMsg = null;
  private double x, y, z;
  private int group = 0;
  private int entityId = 0;
  private Group groupObject = null;
  private boolean isRobot = false;

  private StreamTunnel serverToClient;
  private StreamTunnel clientToServer;
  private final Watchdog watchdog;

  private Queue<String> messages = new ConcurrentLinkedQueue<String>();

  public Player(Socket inc, Server parent) {
    server = parent;
    extsocket = inc;
    if (server.isRobot(getIPAddress())) {
      System.out.println("[SimpleServer] Robot Heartbeat: " + getIPAddress()
          + ".");
      isRobot = true;
    }
    else {
      System.out.println("[SimpleServer] IP Connection from " + getIPAddress()
          + "!");
    }

    if (server.isIPBanned(getIPAddress())) {
      System.out.println("[SimpleServer] IP " + getIPAddress() + " is banned!");
      kick("Banned IP!");
    }
    server.requestTracker.addRequest(getIPAddress());

    try {
      intsocket = new Socket("localhost", server.options.getInt("internalPort"));
    }
    catch (Exception e) {
      e.printStackTrace();
      if (server.options.getBoolean("exitOnFailure")) {
        server.stop();
      }
      else {
        server.restart();
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
      server.addRobotPort(intsocket.getLocalPort());
    }

    watchdog = new Watchdog();
    watchdog.start();
  }

  public double distanceTo(Player player) {
    return Math.sqrt(Math.pow(x - player.x, 2) + Math.pow(x - player.y, 2)
        + Math.pow(x - player.z, 2));
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
    this.name = name;
    updateGroup();

    watchdog.setName("PlayerWatchdog-" + name);
    server.connectionLog("player", extsocket, name);
    server.playerList.addPlayer(this);
    return true;
  }

  public String getName() {
    return name;
  }

  public boolean hasMessages() {
    return !messages.isEmpty();
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

  public String getKickMsg() {
    return kickMsg;
  }

  public boolean isMuted() {
    return server.mutelist.isMuted(name);
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
      addMessage("\u00a7cInsufficient permission.");
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

  public void updateGroup() {
    int nameGroup = server.members.getGroup(name);
    int ipGroup = server.ipMembers.getGroup(this);
    int defaultGroup = server.options.getInt("defaultGroup");

    if (ipGroup >= nameGroup) {
      group = ipGroup;
    }
    else {
      group = nameGroup;
    }

    if (nameGroup == -1 || ipGroup == -1 && defaultGroup != -1) {
      group = -1;
    }

    if (server.groups.groupExists(group)) {
      groupObject = server.groups.getGroup(group);
    }
    else {
      groupObject = null;
    }
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

  public void close() {
    serverToClient.stop();
    clientToServer.stop();
  }

  private void cleanup() {
    if (!closed) {
      closed = true;
      entityId = 0;
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
        addMessage("\u00a7cItem ID must be positive!");
        success = false;
      }
    }
    catch (NumberFormatException e) {
      addMessage("\u00a7cItem ID must map to a number!");
      success = false;
    }

    int amount = 1;
    if (rawAmount != null) {
      try {
        amount = Integer.parseInt(rawAmount);

        if ((amount < 1) || (amount > 1000)) {
          addMessage("\u00a7cAmount must be within 1-1000!");
          success = false;
        }
      }
      catch (NumberFormatException e) {
        addMessage("\u00a7cAmount must be a number!");
        success = false;
      }
    }

    if (!success) {
      addMessage("\u00a7cUnable to give " + rawItem);
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

  public boolean godModeEnabled() {
    return godMode;
  }

  public void toggleGodMode() {
    godMode = !godMode;
  }

  /**
   * @return
   */
  public int getEntityId() {
    return entityId;
  }

  /**
   * @param readInt
   */
  public void setEntityId(int readInt) {
    entityId = readInt;
  }
}
