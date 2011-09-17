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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.xml.sax.SAXException;

public class BlockStorage extends Storage {
  TreeMap<Integer, Interval> intervals = new TreeMap<Integer, Interval>();

  public Block get(int id) {
    Entry<Integer, Interval> node = intervals.lowerEntry(id + 1);
    return node == null || !node.getValue().contains(id) ? null : node.getValue().block;
  }

  @Override
  public void add(XMLTag child) throws SAXException {
    Block block = (Block) child;
    String[] parts = block.id.split(",");
    for (String interval : parts) {
      String[] numbers = interval.split("-");
      if (numbers.length > 2 || numbers.length < 1) {
        throw new SAXException("Invalid block id: " + block.id);
      }
      int start = getIntValue(numbers[0].trim());
      int end = start;
      if (numbers.length == 2) {
        end = getIntValue(numbers[1].trim());
      }

      Entry<Integer, Interval> nearInterval = intervals.higherEntry(start - 1);
      if (nearInterval != null && nearInterval.getKey() <= end) {
        illegalInterval(start, end, nearInterval.getKey(), nearInterval.getValue().end);
      }

      nearInterval = intervals.lowerEntry(start);
      if (nearInterval != null && nearInterval.getValue().end >= start) {
        illegalInterval(start, end, nearInterval.getKey(), nearInterval.getValue().end);
      }

      intervals.put(start, new Interval(start, end, block));
    }
  }

  private void illegalInterval(int start1, int end1, int start2, int end2) throws SAXException {
    throw new SAXException(String.format("The intervals %s-%s and %s-%s overlap. Block definitions must be unique.", start1, end1, start2, end2));
  }

  @Override
  Iterator<Block> iterator() {
    Set<Block> set = new LinkedHashSet<Block>();
    for (Interval interval : intervals.values()) {
      set.add(interval.block);
    }
    return set.iterator();
  }

  private static class Interval implements Comparable<Interval> {
    int start;
    int end;
    Block block;

    Interval(int start, int end, Block block) {
      if (start <= end) {
        this.start = start;
        this.end = end;
      } else {
        this.start = end;
        this.end = start;
      }
      this.block = block;
    }

    boolean contains(int id) {
      return id <= end && id >= start;
    }

    public int compareTo(Interval interval) {
      if (interval.start > start) {
        return 1;
      } else if (interval.start < start) {
        return -1;
      } else {
        return 0;
      }
    }
  }
}
