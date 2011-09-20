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

public class CommandStorage extends Storage implements Iterable<CommandConfig> {
  private Map<String, CommandConfig> commands = new HashMap<String, CommandConfig>();

  public void add(CommandConfig command) {
    commands.put(command.name, command);
  }

  public boolean contains(String name) {
    return commands.containsKey(name);
  }

  public CommandConfig get(String name) {
    return contains(name) ? commands.get(name) : null;
  }

  @Override
  public Iterator<CommandConfig> iterator() {
    return new TreeSet<CommandConfig>(commands.values()).iterator();
  }

  @Override
  void add(XMLTag child) {
    add((CommandConfig) child);
  }

  public CommandConfig getTopConfig(String name) {
    if (commands.containsKey(name) && !commands.get(name).disabled) {
      return commands.get(name);
    }
    for (CommandConfig command : commands.values()) {
      if (!command.disabled && command.alias(name)) {
        return command;
      }
    }
    return null;
  }
}
