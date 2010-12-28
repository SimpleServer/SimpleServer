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
package simpleserver.config;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import simpleserver.Group;

public class GroupList extends PropertiesConfig {
  private final ConcurrentMap<Integer, Group> groups;

  public GroupList() {
    super("group-list.txt");

    groups = new ConcurrentHashMap<Integer, Group>();
  }

  public boolean groupExists(int group) {
    return groups.containsKey(group);
  }

  public Group getGroup(int id) {
    return groups.get(id);
  }

  @Override
  public void load() {
    super.load();

    groups.clear();
    for (Entry<Object, Object> entry : properties.entrySet()) {
      Integer group;
      try {
        group = Integer.parseInt(entry.getKey().toString());
      }
      catch (NumberFormatException e) {
        System.out.println("Skipping bad group list entry " + entry.getKey());
        continue;
      }

      String[] attributes = entry.getValue().toString().split(",");
      if (attributes.length != 4) {
        System.out.println("Skipping bad group list entry " + entry.getValue());
        continue;
      }
      String name = attributes[0].trim();
      boolean showTitle = Boolean.parseBoolean(attributes[1].trim());
      boolean isAdmin = Boolean.parseBoolean(attributes[2].trim());
      String color = attributes[3].trim();

      groups.put(group, new Group(name, showTitle, isAdmin, color));
    }
  }
}
