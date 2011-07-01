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
package simpleserver.config;

import java.io.File;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LegacyStats extends PropertiesConfig {
  public final ConcurrentMap<String, Statistic> stats;

  public LegacyStats() {
    super("stats.txt");

    stats = new ConcurrentHashMap<String, Statistic>();
  }

  @Override
  public void save() {
    File stats = new File("simpleserver" + File.separator + "stats.txt");
    File oldconf = new File("simpleserver" + File.separator + "oldconf");
    oldconf.mkdir();
    stats.renameTo(new File(oldconf.getPath() + File.separator + "stats.txt"));
  }

  @Override
  public void load() {
    super.load();

    stats.clear();
    for (Entry<Object, Object> entry : properties.entrySet()) {
      String objects[] = entry.getValue().toString().split(",");
      if (objects.length < 3) {
        System.out.println("Skipping bad statistics for " + entry.getKey());
        continue;
      }

      Integer[] ints = new Integer[3];

      try {
        for (int i = 0; i < 3; i++) {
          ints[i] = Integer.parseInt(objects[i]);
        }
      } catch (NumberFormatException e) {
        System.out.println("Skipping bad statistics fory " + entry.getKey());
        continue;
      }

      stats.put(entry.getKey().toString().toLowerCase(), new Statistic(ints));
    }

    properties.clear();
    for (Entry<String, Statistic> entry : stats.entrySet()) {
      properties.setProperty(entry.getKey(), entry.getValue().toString());
    }
  }

  @Override
  protected void missingFile() {
  }

  public class Statistic {
    public int minutes;
    public int blocksPlaced;
    public int blocksDestroyed;

    public Statistic(Integer[] stats) {
      if (stats.length >= 3) {
        minutes = stats[0];
        blocksPlaced = stats[1];
        blocksDestroyed = stats[2];
      }
    }
  }
}
