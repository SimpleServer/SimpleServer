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
package simpleserver.nbt;

import java.io.File;
import java.io.FileNotFoundException;

import simpleserver.Coordinate;
import simpleserver.Server;

public class PlayerFile {
  private String path;
  private NBT nbt;

  public PlayerFile(String name, Server server) {
    path = server.options.get("levelName") + File.separator + "players" + File.separator + name + ".dat";
    try {
      nbt = new NBT(path);
    } catch (FileNotFoundException e) {
      nbt = new NBT(getClass().getResourceAsStream("template.dat"));
    }
  }

  public void setPosition(Coordinate coord) {
    NBTag pos = nbt.root().get("Pos");
    ((NBTDouble) ((NBTList) pos).get(0)).set(coord.x());
    ((NBTDouble) ((NBTList) pos).get(1)).set(coord.y());
    ((NBTDouble) ((NBTList) pos).get(2)).set(coord.z());
    ((NBTInt) nbt.root().get("Dimension")).set(coord.dimension().index());
  }

  public void save() {
    nbt.save(path);
  }

  public File file() {
    return new File(path);
  }

  @Override
  public String toString() {
    return nbt.toString();
  }
}
