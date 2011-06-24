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
import java.net.UnknownHostException;

import simpleserver.Player;
import simpleserver.nbt.Inventory;
import simpleserver.nbt.PlayerFile;

public class Giver extends Bot {

  private Inventory inv;
  private int[] counts;
  private Player player;
  int slot;

  public Giver(Player player) {
    super(player.getServer(), "Giver" + Math.round(100000 * Math.random()));
    inv = new Inventory();
    counts = new int[9];
    slot = 0;
    this.player = player;
  }

  @Override
  void connect() throws UnknownHostException, IOException {
    PlayerFile dat = new PlayerFile(name, server);
    dat.setInventory(inv);
    dat.setPosition(player.position());
    dat.setLook(player.yaw(), player.pitch());
    dat.save();
    super.connect();
  }

  public void add(int id, int count, int damage) {
    if (slot < 9) {
      inv.add(id, count, damage);
      counts[slot++] = count;
    }
  }

  @Override
  protected boolean ninja() {
    return true;
  }

  @Override
  protected void handlePacket(byte packetId) throws IOException {
    switch (packetId) {
      case 0x68:
        drop();
      default:
        super.handlePacket(packetId);
    }
  }

  protected void drop() throws IOException {
    super.ready();
    writeLock.lock();
    for (int i = 0; i < 9; i++) {
      if (counts[i] > 0) {
        out.writeByte(0x10);
        out.writeShort(i);
        for (int j = 0; j < counts[i]; j++) {
          out.writeByte(0x0e);
          out.writeByte(0x4);
          out.writeInt(0);
          out.writeByte(0);
          out.writeInt(0);
          out.writeByte(0);
        }
      }
      out.flush();
    }
    writeLock.unlock();
    logout();
  }

}
