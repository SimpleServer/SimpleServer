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

import simpleserver.CommandParser;
import simpleserver.Player;

public class HelpCommand extends AbstractCommand implements PlayerCommand {
  public HelpCommand() {
    super("help [COMMAND]", "List commands or get help for one command");
  }

  @Override
  public boolean shouldPassThroughToSMPAPI() {
    return true;
  }

  public void execute(Player player, String message) {
    String[] arguments = extractArguments(message);

    CommandParser commandParser = player.getServer().getCommandParser();
    if (arguments.length > 0) {
      String prefix = commandParser.commandPrefix();
      String commandName = arguments[0];
      if (!commandName.startsWith(prefix)) {
        commandName = prefix + commandName;
      }
      PlayerCommand command = commandParser.getPlayerCommand(commandName);
      player.addMessage(command.getHelpText(prefix));

      String[] aliases = player.getServer().commands.getAliases(command.getName());
      if (aliases.length > 0) {
        StringBuffer line = new StringBuffer();
        line.append("\u00a77Aliases:\u00a7f ");
        for (String alias : aliases) {
          line.append(commandParser.commandPrefix());
          line.append(alias);
          line.append(" ");
        }
        player.addMessage(line.toString());
      }
    }
    else {
      StringBuffer line = new StringBuffer();
      line.append("\u00a77Available Commands:\u00a7f ");

      String prefix = commandParser.commandPrefix();
      for (PlayerCommand command : commandParser.getPlayerCommands()) {
        if (command.isHidden() || !player.commandAllowed(command.getName())) {
          continue;
        }

        line.append(prefix);
        line.append(command.getName());
        line.append(" ");
      }

      player.addMessage(line.toString());
    }
  }
}
