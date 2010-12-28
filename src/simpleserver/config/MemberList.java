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

import simpleserver.Server;

public class MemberList extends PropertiesConfig {
  private final Server server;
  private final ConcurrentMap<String, Integer> members;

  public MemberList(Server server) {
    super("member-list.txt");

    this.server = server;
    members = new ConcurrentHashMap<String, Integer>();
  }

  public int getGroup(String name) {
    Integer group = members.get(name.toLowerCase());
    if (group != null) {
      return group;
    }
    return server.options.getInt("defaultGroup");
  }

  public void setGroup(String name, int group) {
    if (!server.groups.groupExists(group)) {
      return;
    }
    members.put(name.toLowerCase(), group);
    properties.setProperty(name.toLowerCase(), Integer.toString(group));

    server.updateGroup(name);
    save();
  }

  @Override
  public void load() {
    super.load();

    members.clear();
    for (Entry<Object, Object> entry : properties.entrySet()) {
      Integer group;
      try {
        group = Integer.parseInt(entry.getValue().toString());
      }
      catch (NumberFormatException e) {
        System.out.println("Skipping bad member list entry " + entry.getValue());
        continue;
      }

      members.put(entry.getKey().toString().toLowerCase(), group);
    }

    properties.clear();
    for (Entry<String, Integer> entry : members.entrySet()) {
      properties.setProperty(entry.getKey(), entry.getValue().toString());
    }
  }
}
