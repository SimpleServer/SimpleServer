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

import simpleserver.Server;
import simpleserver.nbt.Inventory;
import simpleserver.nbt.PlayerFile;

public class EnchantmentDetector extends Bot {
  private int start;

  public EnchantmentDetector(Server server, int start) throws IOException {
    super(server, "Detector" + start);
    this.start = start;
    Inventory inv = new Inventory();
    for (int id = start + 27; id < start + 35; id++) {
      inv.add(id, 1);
    }
    inv.add(1, 1);
    for (int id = start; id < start + 27; id++) {
      inv.add(id, 1);
    }
    PlayerFile dat = new PlayerFile(name, server);
    dat.setInventory(inv);
    dat.save();
  }

  @Override
  protected void handlePacket(byte packetId) throws IOException {
    switch (packetId) {
      case 0x68:
        in.readByte();
        in.readShort();
        short last = 0;
        short now = 0;
        while (last != 1) {
          short id = in.readShort();
          if (id == -1) {
            now += 1;
          } else {
            if (last != 0 && now + 1 != id && (id != 1 || now != start + 34)) {
              server.data.enchantable.add(last);
            }
            last = now = id;
            in.readByte();
            in.readShort();
          }
        }
        logout();
      default:
        super.handlePacket(packetId);
    }
  }
}
