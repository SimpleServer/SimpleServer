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

import simpleserver.Color;
import simpleserver.Player;

public class HelpCommand extends AbstractCommand implements PlayerCommand {
  public HelpCommand() {
    super("help [COMMAND]");
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
      player.addMessage(command.getName(prefix));
      command.usage(player);

      String[] aliases = player.getServer().permissions.getCommandAliases(command.getName());
      if (aliases.length > 0) {
        StringBuffer line = new StringBuffer();
        for (String alias : aliases) {
          line.append(commandPrefix());
          line.append(alias);
          line.append(" ");
        }
        player.addTCaptionedMessage("Aliases", line.toString());
      }
    } else {
      StringBuffer line = new StringBuffer();

      String prefix = commandPrefix();

      for (Object cmd : player.getServer().permissions.getAllCommands()) {
        String commandName = cmd.toString();
        parser.getPlayerCommand(commandName);

        if (player.getServer().permissions.commandIsHidden(commandName)
            || !player.commandAllowed(commandName)) {
          continue;
        }

        line.append(prefix);
        line.append(commandName);
        line.append(" ");
      }

      player.addTCaptionedMessage("Available Commands", line.toString());

      player.addTMessage(Color.GRAY, "Say %s command %s for details of a specific command.", Color.WHITE + prefix + "help", Color.GRAY);

      // additional custom help text from helptext.txt
      String[] helplines = player.getServer().helptext.getHelpText().split("\n");
      player.addMessage(" ");
      for (String l : helplines) {
        player.addMessage(Color.WHITE, l);
      }
    }
  }

  @Override
  public void usage(Player player) {
    player.addTMessage(Color.GRAY, "List all commands or get help information for a specific command");
    // FIXME Needed? // execute(player, parser.commandPrefix() + "help");
  }
}
