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

import static simpleserver.util.Util.*;

import java.io.File;

import simpleserver.Coordinate;
import simpleserver.Coordinate.Dimension;

public class WorldFile {
  private String filename;
  private NBTCompound data;

  public WorldFile(String world) throws Exception {
    filename = world + File.separator + "level.dat";
    try {
      NBTFile nbt = new GZipNBTFile(filename);
      data = nbt.root().getCompound("Data");
    } catch (Exception e) {
      println("Can't read level.dat: " + e.getMessage() + " (This is normal if the world was just created!)");
      throw new Exception();
    }
  }

  public long seed() {
    return data.getLong("RandomSeed").get();
  }

  public Coordinate spawnPoint() {
    int x, y, z;
    try {
      x = data.getInt("SpawnX").get();
      y = data.getInt("SpawnY").get();
      z = data.getInt("SpawnZ").get();
    } catch (Exception e) {
      x = z = 0;
      y = 62;
    }
    return new Coordinate(x, y, z, Dimension.EARTH);
  }
}
