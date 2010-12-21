/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package simpleserver.config;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import simpleserver.Group;
import simpleserver.Player;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

public class CommandList extends PropertiesConfig {
  private final ConcurrentMap<String, ImmutableSet<Integer>> commands;

  public CommandList() {
    super("command-list.txt");

    commands = new ConcurrentHashMap<String, ImmutableSet<Integer>>();

    loadDefaults();
  }

  public boolean playerAllowed(String command, Player player) {
    ImmutableSet<Integer> groups = commands.get(command);
    if (groups != null) {
      return Group.isMember(groups, player);
    }

    return false;
  }

  public void setGroup(String command, int group) {
    commands.put(command, ImmutableSortedSet.of(group));
    setProperty(command, Integer.toString(group));
  }

  @Override
  public void load() {
    super.load();

    commands.clear();
    for (Entry<Object, Object> entry : entrySet()) {
      commands.put(entry.getKey().toString(),
                   Group.parseGroups(entry.getValue().toString()));
    }
  }
}
