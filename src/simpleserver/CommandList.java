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

import simpleserver.command.AbstractCommand;
import simpleserver.options.Options;

public class CommandList {
  private Map<String, AbstractCommand> abstractCommands;
  private Map<String, String> aliases;
  private Options options;

  public CommandList(Options options) {
    this.options = options;

    loadCommands();
  }

  public AbstractCommand getCommand(String message) {
    if (message.startsWith(commandPrefix())) {
      int splitIndex = message.indexOf(" ");
      if (splitIndex == -1) {
        splitIndex = message.length();
      }

      String name = message.substring(1, splitIndex).toLowerCase();
      AbstractCommand abstractCommand = abstractCommands.get(name);
      if (abstractCommand == null) {
        abstractCommand = abstractCommands.get(aliases.get(name));
      }

      return abstractCommand;
    }

    return null;
  }

  public AbstractCommand[] getCommands() {
    return abstractCommands.values()
                           .toArray(new AbstractCommand[abstractCommands.size()]);
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
    abstractCommands = new HashMap<String, AbstractCommand>();
    aliases = new HashMap<String, String>();

    Reflections r = new Reflections("simpleserver");
    Set<Class<? extends AbstractCommand>> commandTypes = r.getSubTypesOf(AbstractCommand.class);

    for (Class<? extends AbstractCommand> commandType : commandTypes) {
      if (Modifier.isAbstract(commandType.getModifiers())) {
        continue;
      }

      AbstractCommand abstractCommand;
      try {
        abstractCommand = commandType.getConstructor()
                                     .newInstance(new Object[] {});
      }
      catch (Exception e) {
        e.printStackTrace();
        System.out.println("Unexpected exception.  Skipping command "
            + commandType.getName());
        continue;
      }

      abstractCommands.put(abstractCommand.getName(), abstractCommand);
      for (String alias : abstractCommand.getAliases()) {
        aliases.put(alias, abstractCommand.getName());
      }
    }
  }
}
