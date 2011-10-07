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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class SegmentTree<E> {
  private Node root;
  private List<HyperSegment> segments = new LinkedList<HyperSegment>();
  private NodeCache cache;
  private boolean built = false;
  private int dimensions;

  public SegmentTree(int dimensions) {
    this.dimensions = dimensions;
  }

  public void add(int[] start, int[] end, E object) {
    segments.add(new HyperSegment(start, end, object));
    if (built) {
      build();
    }
  }

  public void remove(E object) {
    for (HyperSegment segment : segments) {
      if (segment.object == object) {
        segments.remove(segment);
        if (built) {
          build();
        }
        return;
      }
    }
  }

  public void build() {
    cache = new NodeCache();
    root = build(segments, 0);
    cache = null;
    built = true;
  }

  public List<E> get(int... point) {
    return root.find(point);
  }

  public Set<E> overlaps(int[] start, int[] end) {
    return overlaps(new HyperSegment(start, end, null));
  }

  private Set<E> overlaps(HyperSegment segment) {
    return root.overlaps(segment);
  }

  private Node build(List<HyperSegment> segments, int dimension) {
    if (segments.size() == 1 && cache.contains(segments.get(0), dimension)) {
      return cache.get(segments.get(0), dimension);
    }

    // find all end points
    TreeMap<Integer, Boolean> points = new TreeMap<Integer, Boolean>();
    for (HyperSegment hyperSegment : segments) {
      Segment segment = hyperSegment.segments[dimension];
      points.put(segment.start, false);
      points.put(segment.end, segment.start == segment.end);
    }

    if (points.isEmpty()) {
      return new Node(0, 0);
    }

    // create leaves
    List<Node> leaves = new LinkedList<Node>();
    while (points.size() > 1) {
      int point = points.firstKey();
      if (points.remove(point)) {
        leaves.add(new Node(point, point));
      }
      leaves.add(new Node(point, points.firstKey()));
    }

    Entry<Integer, Boolean> lastPoint = points.firstEntry();
    if (lastPoint.getValue()) {
      leaves.add(new Node(lastPoint.getKey(), lastPoint.getKey()));
    }

    // build tree
    Node root = createTree(leaves, 0, leaves.size() - 1);

    // insert segments
    for (HyperSegment segment : segments) {
      root.insertSegment(segment.segments[dimension]);
    }

    // build higher dimensions
    if (++dimension < dimensions) {
      root.buildDimension(dimension);
    }

    if (segments.size() == 1) {
      cache.put(root, segments.get(0), dimension - 1);
    }

    return root;
  }

  private Node createTree(List<Node> leaves, int start, int end) {
    if (start == end) {
      return leaves.get(start);
    }
    Node node = new Node(leaves.get(start).start, leaves.get(end).end);
    int middle = (end - start) / 2 + start;
    node.left = createTree(leaves, start, middle);
    node.right = createTree(leaves, middle + 1, end);
    return node;
  }

  private class Node {
    int start;
    int end;

    Node left;
    Node right;

    Node nextDimension;

    List<HyperSegment> segments = new ArrayList<HyperSegment>();

    Node(int start, int end) {
      this.start = start;
      this.end = end;
    }

    void buildDimension(int dimension) {
      if (!segments.isEmpty()) {
        nextDimension = build(segments, dimension);
      }
      if (left != null) {
        left.buildDimension(dimension);
      }
      if (right != null) {
        right.buildDimension(dimension);
      }
    }

    List<E> find(int[] point) {
      List<E> list = new LinkedList<E>();
      find(point, list, 0);
      return list;
    }

    void find(int[] point, List<E> list, int dimension) {
      if (point[dimension] > end || point[dimension] < start) {
        return;
      } else {
        if (dimension == dimensions - 1) {
          for (HyperSegment segment : segments) {
            list.add(segment.object);
          }
        } else if (nextDimension != null) {
          nextDimension.find(point, list, dimension + 1);
        }
        if (left != null) {
          left.find(point, list, dimension);
        }
        if (right != null) {
          right.find(point, list, dimension);
        }
      }
    }

    public Set<E> overlaps(HyperSegment segment) {
      Set<E> set = new HashSet<E>();
      overlaps(segment, set, 0);
      return set;
    }

    private void overlaps(HyperSegment segment, Set<E> set, int dimension) {
      if (segment.segments[dimension].start > end || segment.segments[dimension].end < start) {
        return;
      } else {
        if (dimension == dimensions - 1) {
          for (HyperSegment s : segments) {
            set.add(s.object);
          }
        } else if (nextDimension != null) {
          nextDimension.overlaps(segment, set, dimension + 1);
        }
        if (left != null) {
          left.overlaps(segment, set, dimension);
        }
        if (right != null) {
          right.overlaps(segment, set, dimension);
        }
      }
    }

    public void insertSegment(Segment segment) {
      if (segment.start <= start && segment.end >= end) {
        segments.add(segment.parent);
      } else {
        if (left != null) {
          left.insertSegment(segment);
        }
        if (right != null) {
          right.insertSegment(segment);
        }
      }
    }
  }

  private class HyperSegment {
    E object;
    Segment[] segments;

    @SuppressWarnings("unchecked")
    HyperSegment(int[] start, int[] end, E object) {
      this.object = object;
      segments = (Segment[]) Array.newInstance(Segment.class, dimensions);
      for (int d = 0; d < dimensions; d++) {
        if (start[d] <= end[d]) {
          segments[d] = new Segment(start[d], end[d], this);
        } else {
          segments[d] = new Segment(end[d], start[d], this);
        }
      }
    }
  }

  private class Segment {
    HyperSegment parent;
    int start;
    int end;

    Segment(int start, int end, HyperSegment parent) {
      this.start = start;
      this.end = end;
      this.parent = parent;
    }
  }

  private class NodeCache {
    private HashMap<Segment, Node> cache = new HashMap<Segment, Node>();

    boolean contains(Segment segment) {
      return cache.containsKey(segment);
    }

    boolean contains(HyperSegment segment, int dimension) {
      return contains(segment.segments[dimension]);
    }

    void put(Node node, Segment segment) {
      cache.put(segment, node);
    }

    public void put(Node node, HyperSegment segment, int dimension) {
      put(node, segment.segments[dimension]);
    }

    Node get(Segment segment) {
      return cache.get(segment);
    }

    Node get(HyperSegment segment, int dimension) {
      return get(segment.segments[dimension]);
    }
  }
}
