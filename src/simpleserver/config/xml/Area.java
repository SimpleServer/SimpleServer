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

public class Area extends XMLTag {
  public String name;
  public Coordinate start;
  public Coordinate end;
  public String owner;

  public Area() {
    super("area");
  }

  private static final String NAME = "name";
  private static final String START = "start";
  private static final String END = "end";
  private static final String OWNER = "owner";

  @Override
  protected void setAttribute(String name, String value) throws SAXException {
    if (name.equals(NAME)) {
      this.name = value;
    } else if (name.equals(START)) {
      start = getCoord(value, 0);
    } else if (name.equals(END)) {
      end = getCoord(value, 0);
    } else if (name.equals(OWNER)) {
      owner = value;
    }
  }

  private Coordinate getCoord(String value, int defaultY) throws SAXException {
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

  @Override
  protected void saveAttributes(AttributesImpl attributes) {
    addAttribute(attributes, NAME, name);
    addAttribute(attributes, START, start.toString());
    addAttribute(attributes, END, end.toString());
    if (owner != null) {
      addAttribute(attributes, OWNER, owner);
    }

  }
}
