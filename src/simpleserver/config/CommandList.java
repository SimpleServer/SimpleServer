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

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import simpleserver.Group;
import simpleserver.Player;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

public class CommandList extends PropertiesConfig {
  private final ConcurrentMap<String, ImmutableSet<Integer>> commands;
  private final ConcurrentMap<String, String> aliases;

  public CommandList() {
    super("command-list.txt", true);

    commands = new ConcurrentHashMap<String, ImmutableSet<Integer>>();
    aliases = new ConcurrentHashMap<String, String>();
  }

  public String[] getAliases(String command) {
    command = command.toLowerCase();

    List<String> names = new LinkedList<String>();
    for (Entry<String, String> alias : aliases.entrySet()) {
      if (alias.getValue().equals(command)) {
        names.add(alias.getKey());
      }
    }

    return names.toArray(new String[names.size()]);
  }

  public String lookupCommand(String name) {
    name = name.toLowerCase();

    String alias = aliases.get(name);
    if (alias != null) {
      name = alias;
    }
    else if (!commands.containsKey(name)) {
      name = null;
    }

    return name;
  }

  public boolean playerAllowed(String command, Player player) {
    ImmutableSet<Integer> groups = commands.get(command);
    return (groups != null) && Group.isMember(groups, player);
  }

  public void setGroup(String command, int group) {
    commands.put(command, ImmutableSortedSet.of(group));
    properties.setProperty(command, Integer.toString(group));
  }

  @Override
  public void load() {
    super.load();

    commands.clear();
    aliases.clear();
    for (Entry<Object, Object> entry : properties.entrySet()) {
      String command = entry.getKey().toString().toLowerCase();
      String[] options = entry.getValue().toString().split(";");

      String groups;
      if (options.length == 1) {
        groups = options[0];
      }
      else if (options.length == 2) {
        for (String alias : options[0].split(",")) {
          aliases.put(alias, command);
        }
        groups = options[1];
      }
      else {
        System.out.println("Skipping bad command entry: " + command);
        continue;
      }

      commands.put(command, Group.parseGroups(groups));
    }
  }
}
