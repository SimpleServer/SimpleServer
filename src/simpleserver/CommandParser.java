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
package simpleserver;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import simpleserver.command.AbstractCommand;
import simpleserver.command.Command;
import simpleserver.command.PlayerCommand;
import simpleserver.command.ServerCommand;
import simpleserver.options.Options;

@SuppressWarnings("rawtypes")
public class CommandParser {
  private final Map<String, PlayerCommand> playerCommands;
  private final Map<Class, PlayerCommand> playerCommandClasses;
  private final Map<String, ServerCommand> serverCommands;
  private final Map<Class, ServerCommand> serverCommandClasses;
  private final Options options;

  public CommandParser(Options options) {
    this.options = options;

    playerCommands = new HashMap<String, PlayerCommand>();
    playerCommandClasses = new HashMap<Class, PlayerCommand>();
    serverCommands = new HashMap<String, ServerCommand>();
    serverCommandClasses = new HashMap<Class, ServerCommand>();
    loadCommands(PlayerCommand.class, playerCommands, playerCommandClasses);
    loadCommands(ServerCommand.class, serverCommands, serverCommandClasses);
  }

  public PlayerCommand getPlayerCommand(Class<? extends PlayerCommand> c) {
    if (playerCommandClasses.containsKey(c)) {
      return playerCommandClasses.get(c);
    } else {
      return null;
    }
  }

  public PlayerCommand getPlayerCommand(String name) {
    return playerCommands.get(name);
  }

  public ServerCommand getServerCommand(String name) {
    return serverCommands.get(name);
  }

  public ServerCommand getServerCommand(Class<? extends ServerCommand> c) {
    if (serverCommandClasses.containsKey(c)) {
      return serverCommandClasses.get(c);
    } else {
      return null;
    }
  }

  public Collection<PlayerCommand> getPlayerCommands() {
    return playerCommands.values();
  }

  public String commandPrefix() {
    if (options.getBoolean("useSlashes")) {
      return "/";
    } else {
      return "!";
    }
  }

  private <T extends Command> void loadCommands(Class<T> type, Map<String, T> commands, Map<Class, T> commandClasses) {
    Reflections r = new Reflections("simpleserver", new SubTypesScanner());
    Set<Class<? extends T>> types = r.getSubTypesOf(type);

    for (Class<? extends T> commandType : types) {
      if (Modifier.isAbstract(commandType.getModifiers())) {
        continue;
      }

      T command;
      try {
        command = commandType.getConstructor().newInstance(new Object[] {});
        ((AbstractCommand) command).setParser(this);
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Unexpected exception.  Skipping command "
            + commandType.getName());
        continue;
      }

      commands.put(command.getName(), command);
      commandClasses.put(command.getClass(), command);
    }
  }
}
