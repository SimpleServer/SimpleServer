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
package simpleserver.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import simpleserver.Coordinate;
import simpleserver.Player;
import simpleserver.Position;
import simpleserver.Server;
import simpleserver.config.xml.Event;
import simpleserver.nbt.PlayerFile;

public class NpcBot extends Bot {
  private Position position;
  private PlayerFile dat;
  private String purename;

  private HashMap<Integer, DroppedItem> dropped = new HashMap<Integer, DroppedItem>();
  private String ev = null;

  private Timer timer = new Timer();

  public NpcBot(String name, String e, Server s, Coordinate coordinate) throws IOException {
    this(name, e, s, new Position(coordinate));
  }

  public NpcBot(String name, String e, Server s, Position position) throws IOException {
    super(s, "npc" + name);
    purename = name;
    ev = e;
    this.position = position;
    prepare();
  }

  protected void prepare() throws IOException {
    dat = new PlayerFile(name, server);
    dat.setPosition(position);
    dat.save();
  }

  @Override
  protected void handlePacket(byte packetId) throws IOException {
    switch (packetId) {
      case 0x08: // respawn
        super.handlePacket(packetId);
        if (dead) {
          respawnEvent();
        }
        break;
      case 0x15: // pickup spawn
        addDroppedItem();
        break;
      case 0x16: // collect item
        collectItem();
        break;
      case 0x1d: // destroy entity
        destroyEntity();
        break;

      default:
        super.handlePacket(packetId);
    }
  }

  private void collectItem() throws IOException {
    int what = in.readInt();
    int who = in.readInt();

    // System.out.println("Collected " + what + " Collector:" + who +
    // " real player id:" + playerEntityId); // DEBUG

    if (who != playerEntityId) {
      return;
    }
    DroppedItem i = dropped.get(what);
    if (i != null) {
      itemCollectEvent(i);
    }

  }

  private void addDroppedItem() throws IOException {
    int eid = in.readInt();
    short id = in.readShort();
    byte count = in.readByte();
    short dat = in.readShort();
    int x = in.readInt();
    int y = in.readInt();
    int z = in.readInt();

    in.readByte(); // rotation
    in.readByte(); // pitch
    in.readByte(); // roll

    // System.out.println("Dropped item " + eid + " itemid:" + id + " amount:" +
    // count); // DEBUG

    dropped.put(eid, new DroppedItem(eid, id, count, dat,
                                     (double) x / 32, (double) y / 32, (double) z / 32));
  }

  public void destroyEntity() throws IOException {
    byte destroyCount = in.readByte();
    if (destroyCount > 0) {
      for (int i = 0; i < destroyCount; i++) {
        int eid = in.readInt();
        dropped.remove(eid);
      }
    }
  }

  private Player nearestPlayer() {
    Player[] players = server.playerList.getArray();
    Player nearest = null;
    double bestdistance = 255;
    for (Player p : players) {
      // fugly as hell :D
      double distx = Math.abs(p.x() - position.x());
      double disty = Math.abs(p.y() - position.y());
      double distz = Math.abs(p.z() - position.z());
      double distxy = Math.sqrt(Math.pow(distx, 2) + Math.pow(disty, 2));
      double distxyz = Math.sqrt(Math.pow(distxy, 2) + Math.pow(distz, 2));

      if (distxyz < bestdistance) {
        bestdistance = distxyz;
        nearest = p;
      }

    }

    return nearest;
  }

  private void itemCollectEvent(DroppedItem i) {
    if (i == null || ev == null) {
      return;
    }

    Event e = server.eventhost.findEvent(ev);
    if (e == null) {
      System.out.println("NPC " + purename + ": Event " + ev + " not found!");
      return;
    }

    Player nearest = nearestPlayer();

    ArrayList<String> args = new ArrayList<String>();
    args.add(purename); // NPC name
    args.add("collect"); // NPC trigger
    // collect trigger arguments
    args.add(String.valueOf(i.id)); // item id
    args.add(String.valueOf(i.count)); // item count
    args.add(String.valueOf(i.data)); // meta data
    server.eventhost.execute(e, nearest, true, args);
  }

  private void loginEvent() {
    Player nearest = nearestPlayer();

    Event e = server.eventhost.findEvent(ev);
    if (e == null) {
      System.out.println("NPC " + purename + ": Event " + ev + " not found!");
      return;
    }

    ArrayList<String> args = new ArrayList<String>();
    args.add(purename); // NPC name
    args.add("login"); // NPC trigger
    server.eventhost.execute(e, nearest, true, args);
  }

  private void logoutEvent() {
    Player nearest = nearestPlayer();

    Event e = server.eventhost.findEvent(ev);
    if (e == null) {
      System.out.println("NPC " + purename + ": Event " + ev + " not found!");
      return;
    }

    ArrayList<String> args = new ArrayList<String>();
    args.add(purename); // NPC name
    args.add("logout"); // NPC trigger
    server.eventhost.execute(e, nearest, true, args);
  }

  private void respawnEvent() {
    try {
      Thread.sleep(1000);
    } catch (Exception e) {
    }

    Player nearest = nearestPlayer();

    Event e = server.eventhost.findEvent(ev);
    if (e == null) {
      System.out.println("NPC " + purename + ": Event " + ev + " not found!");
      return;
    }

    ArrayList<String> args = new ArrayList<String>();
    args.add(purename); // NPC name
    args.add("respawn"); // NPC trigger
    server.eventhost.execute(e, nearest, true, args);
  }

  @Override
  protected void ready() throws IOException {
    super.ready();
    server.runCommand("gamemode", "1 " + name); // creative mode
    timer.schedule(new GhostWalk(), 0, 1000); // send movement updates

    loginEvent();
  }

  @Override
  protected void die() {
    logoutEvent();
    server.eventhost.npcs.remove(purename);

    timer.cancel();
    super.die();
  }

  @Override
  boolean ninja() {
    return false;
  }

  @SuppressWarnings("unused")
  private class DroppedItem {
    public int eid;

    public short id;
    public byte count;
    public short data;

    public double x;
    public double y;
    public double z;

    public DroppedItem(int eid, short id, byte count, short data, double x, double y, double z) {
      this.eid = eid;

      this.id = id;
      this.count = count;
      this.data = data;
      this.x = x;
      this.y = y;
      this.z = z;
    }

  }

  private final class GhostWalk extends TimerTask {
    @Override
    public void run() {
      try {
        if (!dead) {
          position.updateLook(0, 0);
          writeLock.lock();
          walk(0.1);
          sendPosition();
          walk(-0.1);
          sendPosition();
          writeLock.unlock();
        }
      } catch (IOException e) {
        error("GhostWalk failed");
      }
    }
  }

}
