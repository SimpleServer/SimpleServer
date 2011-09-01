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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

public class SegmentTree<E> {
  private Node root;
  private List<Segment> segments = new LinkedList<Segment>();
  private boolean built = false;

  public void add(int start, int end, E object) {
    if (!built) {
      segments.add(new Segment(start, end, object));
    }
  }

  public void build() {
    long start = new Date().getTime();

    // find all end points
    TreeSet<Integer> points = new TreeSet<Integer>();
    for (Segment segment : segments) {
      points.add(segment.start);
      points.add(segment.end);
    }

    // create leaves
    List<Node> leaves = new LinkedList<Node>();
    while (points.size() > 1) {
      leaves.add(new Node(points.pollFirst(), points.first()));
    }

    // build tree
    root = createTree(leaves, 0, leaves.size() - 1);

    // insert segments
    for (Segment segment : segments) {
      root.insertSegment(segment);
    }

    // show result
    System.out.println("time: " + (new Date().getTime() - start) + "ms");
    System.out.println(root);

    built = true;
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
    SegmentTree<Integer> tree = new SegmentTree<Integer>();
    Random random = new Random();
    int n = 1000;
    for (int i = 0; i < n; i++) {
      int start = random.nextInt(100);
      int end = random.nextInt(100);
      tree.add(start, end, i);
    }
    tree.build();
  }

  private class Node {
    int start;
    int end;

    Node left;
    Node right;

    List<Segment> segments = new ArrayList<Segment>();

    public Node(int start, int end) {
      this.start = start;
      this.end = end;
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
        for (Segment segment : segments) {
          str.append(" (");
          str.append(segment.start);
          str.append(" - ");
          str.append(segment.end);
          str.append(")");
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
        segments.add(segment);
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

  private class Segment {
    E object;
    int start;
    int end;

    public Segment(int start, int end, E object) {
      this.start = start;
      this.end = end;
      this.object = object;
    }
  }
}
