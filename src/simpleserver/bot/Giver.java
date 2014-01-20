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
import java.nio.ByteBuffer;

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

  @Override
  protected void handlePacket(byte packetId) throws IOException {

    switch (packetId) {
      case (byte) 0x0E:
        drop();
        break;

      default:
        super.handlePacket(packetId);
        break;
    }
  }

  protected void drop() throws IOException {
    super.ready();
    writeLock.lock();
    ByteBuffer b = ByteBuffer.allocate(24);
    ByteBuffer c = ByteBuffer.allocate(24);
    for (byte i = 0; i < count; i++) {
      Slot slot = inv.get(i);
      b.put((byte) 0);
      b.putShort(Inventory.networkSlot(i));
      b.put((byte) 0);
      b.putShort((short) (i * 2));
      b.put((byte) 0);
      slot.write(b);
      super.sendPacketIndependently((byte) 0x0E, b.array());
      c.put((byte) 0);
      c.putShort((short) -999);
      c.put((byte) 0);
      c.putShort((short) (i * 2 + 1));
      c.put((byte) 0);
      c.putShort((short) -1);
      super.sendPacketIndependently((byte) 0x0E, c.array());
    }
    writeLock.unlock();
    logout();
  }
}
