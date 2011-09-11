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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xml.sax.SAXException;

import simpleserver.Coordinate;
import simpleserver.Coordinate.Dimension;

public class DimensionStorage extends Storage implements Iterable<DimensionConfig> {
  Map<Dimension, DimensionConfig> dimensions = new LinkedHashMap<Dimension, DimensionConfig>();

  @Override
  void add(XMLTag child) throws SAXException {
    dimensions.put(((DimensionConfig) child).dimension, (DimensionConfig) child);
  }

  @Override
  public Iterator<DimensionConfig> iterator() {
    return dimensions.values().iterator();
  }

  public boolean contains(Dimension dimension) {
    return dimensions.containsKey(dimension);
  }

  public DimensionConfig get(Dimension dimension) {
    return dimensions.get(dimension);
  }

  public List<Area> areas(Coordinate coordinate) {
    DimensionConfig dim = get(coordinate.dimension());
    if (dim != null) {
      return dim.areas.get(coordinate);
    }
    return null;
  }

  public Set<Area> overlaps(Area area) {
    DimensionConfig dim = get(area.start.dimension());
    if (dim != null) {
      return dim.areas.overlaps(area);
    }
    return new HashSet<Area>(0);
  }

  public DimensionConfig add(Dimension dimension) {
    DimensionConfig newDimension = new DimensionConfig();
    newDimension.dimension = dimension;
    newDimension.init();
    dimensions.put(dimension, newDimension);
    return newDimension;
  }

}
