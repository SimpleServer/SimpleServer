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

import java.io.File;

import simpleserver.Coordinate;
import simpleserver.Server;
import simpleserver.bot.NBT.NBTDouble;
import simpleserver.bot.NBT.NBTInt;
import simpleserver.bot.NBT.NBTList;
import simpleserver.bot.NBT.NBTag;

public class PlayerFile {
  private String path;
  private NBT nbt;

  public PlayerFile(String name, Server server) {
    path = server.options.get("levelName") + File.separator + "players" + File.separator + name + ".dat";
    File file = new File(path);
    if (file.exists()) {
      nbt = new NBT(path);
    } else {
      nbt = new NBT(getClass().getResourceAsStream("template.dat"));
    }
  }

  public void setPosition(Coordinate coord) {
    NBTag pos = nbt.root().find("Pos");
    ((NBTDouble) ((NBTList) pos).get(0)).set(coord.x());
    ((NBTDouble) ((NBTList) pos).get(1)).set(coord.y());
    ((NBTDouble) ((NBTList) pos).get(2)).set(coord.z());
    ((NBTInt) nbt.root().find("Dimension")).set(coord.dimension().index());
  }

  public void save() {
    nbt.save(path);
  }

  public File file() {
    return new File(path);
  }
}
