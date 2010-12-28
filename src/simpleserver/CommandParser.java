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

import simpleserver.command.Command;
import simpleserver.command.PlayerCommand;
import simpleserver.command.ServerCommand;
import simpleserver.config.CommandList;
import simpleserver.options.Options;

public class CommandParser {
  private final Map<String, PlayerCommand> playerCommands;
  private final Map<String, ServerCommand> serverCommands;
  private final Options options;
  private final CommandList commandList;

  public CommandParser(Options options, CommandList commandList) {
    this.options = options;
    this.commandList = commandList;

    playerCommands = new HashMap<String, PlayerCommand>();
    serverCommands = new HashMap<String, ServerCommand>();
    loadCommands(PlayerCommand.class, playerCommands);
    loadCommands(ServerCommand.class, serverCommands);
  }

  public PlayerCommand getPlayerCommand(String message) {
    if (message.startsWith(commandPrefix())) {
      PlayerCommand command = playerCommands.get(extractName(message, 1));
      if (command == null) {
        System.out.println("Illegal command configuration for: " + message);
        command = playerCommands.get(null);
      }

      return command;
    }

    return null;
  }

  public ServerCommand getServerCommand(String message) {
    return serverCommands.get(extractName(message, 0));
  }

  public Collection<PlayerCommand> getPlayerCommands() {
    return playerCommands.values();
  }

  public String commandPrefix() {
    if (options.getBoolean("useSlashes")) {
      return "/";
    }
    else {
      return "!";
    }
  }

  private String extractName(String message, int offset) {
    int splitIndex = message.indexOf(" ");
    if (splitIndex == -1) {
      splitIndex = message.length();
    }

    String name = message.substring(offset, splitIndex).toLowerCase();
    return commandList.lookupCommand(name);
  }

  private <T extends Command> void loadCommands(Class<T> type,
                                                Map<String, T> commands) {
    Reflections r = new Reflections("simpleserver", new SubTypesScanner());
    Set<Class<? extends T>> types = r.getSubTypesOf(type);

    for (Class<? extends T> commandType : types) {
      if (Modifier.isAbstract(commandType.getModifiers())) {
        continue;
      }

      T command;
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
    }
  }
}
