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

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import simpleserver.Coordinate;

public class Area extends StorageContainer implements Comparable<Area> {
  public String name;
  public Coordinate start;
  public Coordinate end;
  public String owner;

  AreaStorage areas;

  int position;
  int priority;
  int level;
  Area parent;

  private static final String NAME = "name";
  private static final String START = "start";
  private static final String END = "end";
  private static final String OWNER = "owner";

  Area() {
    super("area");
  }

  @Override
  void addStorages() {
    addStorage("area", areas = new AreaStorage());
  }

  @Override
  void loadedAttributes() {
    areas.setOwner(this);
  }

  @Override
  void finish() {
    GlobalAreaStorage.getInstance().decreaseLevel();
  }

  @Override
  void setAttribute(String name, String value) throws SAXException {
    if (name.equals(NAME)) {
      this.name = value;
    } else if (name.equals(START)) {
      start = getCoord(value, 0);
    } else if (name.equals(END)) {
      end = getCoord(value, 128);
    } else if (name.equals(OWNER)) {
      owner = value;
    }
  }

  void setInfo(int position, int level, Area parent) {
    this.position = position;
    this.level = level;
    this.parent = parent;
  }

  @Override
  void saveAttributes(AttributesImpl attributes) {
    addAttribute(attributes, NAME, name);
    addAttribute(attributes, START, start.toString());
    addAttribute(attributes, END, end.toString());
    if (owner != null) {
      addAttribute(attributes, OWNER, owner);
    }
  }

  public int compareTo(Area area) {
    int compared;
    if ((compared = new Integer(level).compareTo(area.level)) != 0) {
      return compared;
    }
    if ((compared = -new Integer(priority).compareTo(area.priority)) != 0) {
      return compared;
    }
    return new Integer(position).compareTo(area.position);
  }

  private static Coordinate getCoord(String value, int defaultY) throws SAXException {
    String[] parts = value.split(",");
    if (parts.length < 2 || parts.length > 3) {
      throw new SAXException("Malformed coordinate: " + value);
    }

    int x = getInt(parts[0]);
    int y = defaultY;
    int z;
    if (parts.length == 2) {
      z = getInt(parts[1]);
    } else {
      y = getInt(parts[1]);
      z = getInt(parts[2]);
    }

    return new Coordinate(x, (byte) y, z);
  }
}
