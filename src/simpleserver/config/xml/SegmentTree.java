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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

public class SegmentTree<E> {
  private Node root;
  private List<HyperSegment> segments = new LinkedList<HyperSegment>();
  private NodeCache cache = new NodeCache();
  private boolean built = false;
  private int dimensions;

  public int cacheCounter;

  public SegmentTree(int dimensions) {
    this.dimensions = dimensions;
  }

  public void add(int[] start, int[] end, E object) {
    if (!built) {
      segments.add(new HyperSegment(start, end, object));
    }
  }

  public void build() {
    root = build(segments, 0);
  }

  public List<E> get(int... point) {
    return root.find(point);
  }

  private Node build(List<HyperSegment> segments, int dimension) {
    System.out.println("Dimension: " + (dimension + 1));

    if (segments.size() == 1 && cache.contains(segments.get(0), dimension)) {
      System.out.println("CACHED " + segments.get(0) + "\n");
      cacheCounter++;
      return cache.get(segments.get(0), dimension);
    }

    // find all end points
    TreeSet<Integer> points = new TreeSet<Integer>();
    for (HyperSegment hyperSegment : segments) {
      Segment segment = hyperSegment.segments[dimension];
      points.add(segment.start);
      points.add(segment.end);
    }

    // create leaves
    List<Node> leaves = new LinkedList<Node>();
    while (points.size() > 1) {
      leaves.add(new Node(points.pollFirst(), points.first()));
    }

    if (leaves.isEmpty()) {
      leaves.add(new Node(points.first(), points.first()));
    }

    // build tree
    Node root = createTree(leaves, 0, leaves.size() - 1);

    // insert segments
    for (HyperSegment segment : segments) {
      root.insertSegment(segment.segments[dimension]);
    }

    // show result
    System.out.println(root);

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

  public static void main(String[] args) {
    int d = 3;
    int n = 100;
    int m = 100;

    SegmentTree<String> tree = new SegmentTree<String>(d);
    Random random = new Random();
    for (int i = 0; i < n; i++) {
      int[] start = new int[d];
      int[] end = new int[d];
      System.out.print(i + ": ");
      StringBuilder str = new StringBuilder();
      for (int j = 0; j < d; j++) {
        start[j] = random.nextInt(100);
        end[j] = random.nextInt(100);
        str.append("(" + start[j] + " - " + end[j] + ") ");
      }
      System.out.println(str);
      tree.add(start, end, str.toString());
    }
    System.out.println();

    long memory = getMemory();
    long start = new Date().getTime();
    tree.build();
    long buildTime = (new Date().getTime() - start);
    memory = getMemory() - memory;

    start = new Date().getTime();
    for (int i = 0; i < m; i++) {
      int[] point = new int[d];
      System.out.println("\nLooking for ");
      for (int j = 0; j < d; j++) {
        point[j] = random.nextInt(100);
        if (j != 0) {
          System.out.print('/');
        }
        System.out.print(point[j]);
      }
      System.out.println();
      List<String> result = tree.get(point);
      for (String obj : result) {
        System.out.println(obj);
      }
    }
    System.out.println("\nMemory used: " + memory + "KB");
    System.out.println("Nodes saved through caching: " + tree.cacheCounter);
    System.out.println("Total build time: " + buildTime + "ms");
    System.out.println("Total query time: " + (new Date().getTime() - start) + "ms");
  }

  private static final long getMemory() {
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    return (runtime.totalMemory() - runtime.freeMemory()) / 1000;
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
      List<E> list = new LinkedList();
      find(point, list, 0);
      return list;
    }

    void find(int[] point, List<E> list, int dimension) {
      if (point[dimension] >= end || point[dimension] < start) {
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

    @Override
    public String toString() {
      return toString(0);
    }

    public String toString(int in) {
      StringBuilder str = new StringBuilder();
      for (int i = 0; i < in; i++) {
        str.append(' ');
      }
      str.append(start);
      str.append(" - ");
      str.append(end);
      if (!segments.isEmpty()) {
        str.append(" <=");
        for (HyperSegment segment : segments) {
          str.append(' ');
          str.append(segment);
        }
      }
      str.append('\n');
      if (left != null) {
        str.append(left.toString(in + 1));
      }
      if (right != null) {
        str.append(right.toString(in + 1));
      }
      return str.toString();
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

    @Override
    public String toString() {
      StringBuilder str = new StringBuilder();
      str.append('[');
      for (Segment segment : segments) {
        str.append(segment);
      }
      str.append(']');
      return str.toString();
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

    @Override
    public String toString() {
      return "(" + start + " - " + end + ")";
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
