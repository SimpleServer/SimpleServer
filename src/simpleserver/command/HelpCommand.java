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
package simpleserver.command;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.config.xml.CommandConfig;
import simpleserver.config.xml.PermissionContainer;

public class HelpCommand extends AbstractCommand implements PlayerCommand {
  public HelpCommand() {
    super("help [COMMAND]", "List commands or get help for one command");
  }

  public void execute(Player player, String message) {
    String[] arguments = extractArguments(message);

    if (arguments.length > 0) {
      String prefix = commandPrefix();
      String commandName = arguments[0];
      if (!commandName.startsWith(prefix)) {
        commandName = prefix + commandName;
      }
      PlayerCommand command = parser.getPlayerCommand(commandName);
      player.addMessage(command.getHelpText(prefix));

      List<String> aliases = player.getServer().config.commands.get(command.getName()).aliases;
      if (aliases != null) {
        StringBuffer line = new StringBuffer();
        for (String alias : aliases) {
          line.append(commandPrefix());
          line.append(alias);
          line.append(" ");
        }
        player.addTCaptionedMessage("Aliases", line.toString());
      }
    } else {
      List<PermissionContainer> containers = player.getServer().config.containers(player.position());
      Set<CommandConfig> commands = new TreeSet<CommandConfig>();

      for (PermissionContainer container : containers) {
        for (CommandConfig command : container.commands) {
          if (command.allow.contains(player)) {
            commands.add(command);
          }
        }
      }

      StringBuffer line = new StringBuffer();
      String prefix = commandPrefix();

      for (CommandConfig cmd : commands) {
        Command command = parser.getPlayerCommand(cmd.originalName);
        System.out.println(cmd.name);

        if (cmd.hidden || (command != null && command.hidden())) {
          continue;
        }

        line.append(prefix);
        line.append(cmd.name);
        line.append(" ");
      }

      player.addTCaptionedMessage("Available Commands", line.toString());

      player.addTMessage(Color.GRAY, "Say %s command for details of a specific command.",
                         prefix + "help");

      // additional custom help text from helptext.txt
      String[] helplines = player.getServer().helptext.getHelpText().split("\n");
      if (helplines.length > 0) {
        player.addMessage(" ");
        for (String l : helplines) {
          player.addMessage(Color.WHITE, l);
        }
      }
    }
  }
}
