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
import simpleserver.nbt.Inventory.Slot;
import simpleserver.nbt.PlayerFile;

public class Giver extends Bot {

  private Inventory inv;
  private int count;
  private Player player;

  public Giver(Player player) {
    super(player.getServer(), "Giver" + Math.round(100000 * Math.random()));
    inv = new Inventory();
    count = 0;
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
    inv.add(id, count, damage);
    this.count++;
  }

  public void add(Slot slot) {
    inv.add(slot);
    count++;
  }

  @Override
  protected boolean ninja() {
    return true;
  }

  @SuppressWarnings("fallthrough")
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
    for (byte i = 0; i < count; i++) {
      Slot slot = inv.get(i);
      out.writeByte(0x66);
      out.writeByte(0);
      out.writeShort(Inventory.networkSlot(i));
      out.writeByte(0);
      out.writeShort(i * 2);
      out.writeBoolean(false);
      slot.write(out);
      out.flush();
      out.writeByte(0x66);
      out.writeByte(0);
      out.writeShort(-999);
      out.writeByte(0);
      out.writeShort(i * 2 + 1);
      out.writeBoolean(false);
      out.writeShort(-1);
      out.flush();
    }
    writeLock.unlock();
    logout();
  }

}
