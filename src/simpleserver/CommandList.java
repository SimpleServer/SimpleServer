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
package simpleserver;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

public class CommandList {
  private Map<String, Command> commands;
  private Map<String, String> aliases;
  private Options options;

  public CommandList(Options options) {
    this.options = options;

    loadCommands();
  }

  public Command getCommand(String message) {
    if (message.startsWith(commandPrefix())) {
      int splitIndex = message.indexOf(" ");
      if (splitIndex == -1) {
        splitIndex = message.length();
      }

      String name = message.substring(1, splitIndex).toLowerCase();
      Command command = commands.get(name);
      if (command == null) {
        command = commands.get(aliases.get(name));
      }

      return command;
    }

    return null;
  }

  public Command[] getCommands() {
    return commands.values().toArray(new Command[commands.size()]);
  }

  public String commandPrefix() {
    if (options.getBoolean("useSlashes")) {
      return "/";
    }
    else {
      return "!";
    }
  }

  private void loadCommands() {
    commands = new HashMap<String, Command>();
    aliases = new HashMap<String, String>();

    Reflections r = new Reflections("simpleserver");
    Set<Class<? extends Command>> commandTypes = r.getSubTypesOf(Command.class);

    for (Class<? extends Command> commandType : commandTypes) {
      if (Modifier.isAbstract(commandType.getModifiers())) {
        continue;
      }

      Command command;
      try {
        command = commandType.getConstructor().newInstance(new Object[] {});
      }
      catch (Exception e) {
        e.printStackTrace();
        System.out.println("Unexpected exception.  Skipping command "
            + commandType.getName());
        continue;
      }

      commands.put(command.getName(), command);
      for (String alias : command.getAliases()) {
        aliases.put(alias, command.getName());
      }
    }
  }
}
