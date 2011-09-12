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
package simpleserver.config.xml.legacy;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import simpleserver.Coordinate;
import simpleserver.Coordinate.Dimension;
import simpleserver.config.xml.Area;
import simpleserver.config.xml.Config;
import simpleserver.config.xml.DimensionAreaStorage;
import simpleserver.config.xml.PermissionContainer;

public class AreaConverter extends TagConverter {

  AreaConverter() {
    super("area");
  }

  @Override
  void convert(Attributes attributes, Stack<PermissionContainer> stack) throws SAXException {
    PermissionContainer container = stack.peek();

    String[] parts = attributes.getValue("start").split(";");
    String[] coords = parts[0].split(",");
    Coordinate start;
    if (coords.length == 2) {
      start = new Coordinate(getInt(coords[0]), 0, getInt(coords[1]));
    } else if (coords.length >= 3) {
      start = new Coordinate(getInt(coords[0]), getInt(coords[1]), getInt(coords[2]));
    } else {
      throw new SAXException("Invalid coordinate: " + parts[0]);
    }

    Dimension dimension = (parts.length >= 2) ? Dimension.get(parts[1]) : Dimension.EARTH;

    parts = attributes.getValue("end").split(";");
    coords = parts[0].split(",");
    Coordinate end;
    if (coords.length == 2) {
      end = new Coordinate(getInt(coords[0]), 127, getInt(coords[1]));
    } else if (coords.length >= 3) {
      end = new Coordinate(getInt(coords[0]), getInt(coords[1]), getInt(coords[2]));
    } else {
      throw new SAXException("Invalid coordinate: " + parts[0]);
    }

    if (!((Config) stack.firstElement()).dimensions.contains(dimension)) {
      ((Config) stack.firstElement()).dimensions.add(dimension);
    }

    DimensionAreaStorage.setInstance(((Config) stack.firstElement()).dimensions.get(dimension).areas);

    Area area = new Area(attributes.getValue("name"), start, end);
    if (attributes.getIndex("owner") >= 0) {
      area.owner = attributes.getValue("owner").toLowerCase();
    }
    area.fullInit();

    if (container instanceof Config) {
      ((Config) container).dimensions.get(dimension).add(area);
    } else {
      ((Area) container).areas.add(area);
    }

    stack.push(area);
  }

  @Override
  void end(Stack<PermissionContainer> stack) {
    stack.pop();
  }
}
