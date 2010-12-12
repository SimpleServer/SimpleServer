/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import simpleserver.Group;
import simpleserver.Player;

public class CommandList extends PropertiesConfig {
  public static final Map<String, String> synonyms = new HashMap<String, String>();
  static {
    synonyms.put("releaselock", "unlock");
    synonyms.put("l", "local");
  }

  private boolean useSlashes;
  private Map<String, int[]> commands;

  public CommandList(boolean useSlashes) {
    super("command-list.txt");

    this.useSlashes = useSlashes;
    this.commands = new HashMap<String, int[]>();
  }

  public boolean playerAllowed(String command, Player player) {
    int[] groups = commands.get(command);
    if (groups != null) {
      return Group.contains(groups, player);
    }

    String synonym = synonyms.get(command);
    if (synonym != null) {
      return playerAllowed(synonym, player);
    }

    return false;
  }

  public int[] getGroups(String command) {
    int[] groups = commands.get(command);
    if (groups != null) {
      return groups;
    }

    String synonym = synonyms.get(command);
    if (synonym != null) {
      return getGroups(synonym);
    }

    return null;
  }

  public void setGroup(String command, int group) {
    commands.put(command, new int[] { group });
    setProperty(command, Integer.toString(group));
  }

  public String getCommandList(Player player) {
    StringBuilder commandList = new StringBuilder();
    String prefix = "!";
    if (useSlashes) {
      prefix = "/";
    }

    for (Entry<String, int[]> entry : commands.entrySet()) {
      if (Group.contains(entry.getValue(), player)) {
        commandList.append(prefix);
        commandList.append(entry.getKey());
        commandList.append(", ");
      }
    }

    return commandList.substring(0, commandList.length() - 2);
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
