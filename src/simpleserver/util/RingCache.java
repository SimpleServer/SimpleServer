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
package simpleserver.util;

import java.lang.reflect.Array;

public class RingCache<E> {
  private E[] items;
  private final int capacity;
  private int writeIndex = 0;

  @SuppressWarnings("unchecked")
  public RingCache(Class<E> c, int capacity) {
    this.capacity = capacity;
    items = (E[]) Array.newInstance(c, capacity);
  }

  public RingCache(Class<E> c) {
    this(c, 10);
  }

  public void put(E item) {
    items[writeIndex] = item;
    writeIndex = (writeIndex + 1) % capacity;
  }

  public E getLast() {
    return items[(writeIndex + (capacity - 1)) % capacity];
  }

  public E getOldest() {
    int i = (writeIndex + 1) % capacity;
    while (items[i] == null && i != writeIndex) {
      i = (i + 1) % capacity;
    }

    return items[i];
  }

  public boolean has(String item) {
    int modAdder = capacity - 1;
    int end = writeIndex;
    int start = (end + modAdder) % capacity;
    for (int i = start; i != end; i = (i + modAdder) % capacity) {
      if (items[i] != null && items[i].equals(item)) {
        return true;
      }
    }
    return (items[end] != null && items[end].equals(item));
  }
}
