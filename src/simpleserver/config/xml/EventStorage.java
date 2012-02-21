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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

public class EventStorage extends Storage implements Iterable<Event> {
  private Map<String, Event> events = new HashMap<String, Event>();

  public void add(Event event) {
    events.put(event.name, event);
  }

  public boolean contains(String name) {
    return events.containsKey(name);
  }

  public Event get(String name) {
    return contains(name) ? events.get(name) : null;
  }

  @Override
  public Iterator<Event> iterator() {
    return new TreeSet<Event>(events.values()).iterator();
  }

  @Override
  void add(XMLTag child) {
    add((Event) child);
  }

  public Event getTopConfig(String name) {
    if (events.containsKey(name)) {
      return events.get(name);
    }
    for (Event event : events.values()) {
      return event;
    }
    return null;
  }
}
