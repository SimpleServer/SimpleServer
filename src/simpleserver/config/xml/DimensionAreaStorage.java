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
package simpleserver.config.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import simpleserver.Coordinate;

public class DimensionAreaStorage {
  private static DimensionAreaStorage lastInstance;

  static DimensionAreaStorage newInstance() {
    return lastInstance = new DimensionAreaStorage();
  }

  static DimensionAreaStorage getInstance() {
    return lastInstance;
  }

  private SegmentTree<Area> tree = new SegmentTree<Area>(3);
  private Deque<Area> positions = new LinkedList<Area>();

  private DimensionAreaStorage() {
    positions.push(null);
  }

  void addTag(Area area) {
    add(area);

    int position = 0;
    Area parent = null;
    Area top;

    if ((top = positions.pop()) != null) {
      position = top.position + 1;
    }
    if (!positions.isEmpty()) {
      parent = positions.peek();
    }

    area.setInfo(position, positions.size(), parent);
    positions.push(area);
    positions.push(null);
  }

  public void add(Area area) {
    tree.add(new int[] { area.start.y(), area.start.x(), area.start.z() },
             new int[] { area.end.y(), area.end.x(), area.end.z() },
             area);
  }

  public void remove(Area area) {
    tree.remove(area);
  }

  public List<Area> get(Coordinate coord) {
    List<Area> areas = tree.get(coord.y(), coord.x(), coord.z());
    for (Area area : new ArrayList<Area>(areas)) {
      Area parent = area;
      while (parent.parent != null) {
        areas.add(parent = parent.parent);
      }
    }
    Collections.sort(areas);
    LinkedList<Area> active = new LinkedList<Area>();
    int level = 0;
    for (Area area : areas) {
      if (area.level != level) {
        continue;
      }
      if (active.isEmpty() || active.getLast() == area.parent) {
        active.add(area);
        level++;
      }
    }
    return active;
  }

  void decreaseLevel() {
    positions.pop();
  }

  void buildTree() {
    tree.build();
  }
}
