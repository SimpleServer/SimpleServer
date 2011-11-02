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

import static simpleserver.lang.Translations.t;
import simpleserver.Color;
import simpleserver.Player;
import simpleserver.Server;

public abstract class PlayerArgCommand extends AbstractCommand implements PlayerCommand, ServerCommand {
  protected PlayerArgCommand(String name, String commandCode) {
    super(name, commandCode);
  }

  public void execute(Player player, String message) {
    String target = completeName(message, player.getServer());

    if (target != null) {
      executeWithTarget(player, message, target);
    } else {
      player.addTMessage(Color.RED, noTargetSpecified());
    }
  }

  public void execute(Server server, String message, CommandFeedback feedback) {
    String target = completeName(message, server);

    if (target != null) {
      executeWithTarget(server, message, target, feedback);
    } else {
      feedback.send(noTargetSpecified());
    }
  }

  private String completeName(String message, Server server) {
    String[] arguments = extractArguments(message);

    if (arguments.length > 0) {
      String name = server.findName(arguments[0]);
      if (name == null) {
        return arguments[0];
      } else {
        return name;
      }
    } else {
      return null;
    }
  }

  protected abstract void executeWithTarget(Player player, String message, String target);

  protected abstract void executeWithTarget(Server server, String message, String target, CommandFeedback feedback);

  protected String noTargetSpecified() {
    return "No player specified.";
  }

  @Override
  public String getHelpText(String prefix) {
    return super.getHelpText(prefix) + " " + t("(case-insensitive, name prefix works for online players)");
  }
}
