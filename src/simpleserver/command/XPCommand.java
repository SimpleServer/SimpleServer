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

public class XPCommand extends PlayerArgCommand {

  public XPCommand() {
    super("xp [PLAYER] AMOUNT", "Gives XP to a player");
  }

  @Override
  protected void executeWithTarget(Player player, String message, String target) {
    String[] parts = message.trim().split(" ");
    String amount;
    if (parts.length <= 2) {
      target = player.getName();
      amount = parts[1];
    } else {
      amount = parts[2];
    }
    try {
      give(player.getServer(), target, amount);
    } catch (Exception e) {
      player.addMessage(Color.RED, e.getMessage());
    }
  }

  @Override
  protected void executeWithTarget(Server server, String message, String target, CommandFeedback feedback) {
    String[] parts = message.trim().split(" ");
    if (parts.length < 3) {
      feedback.send("No amount specified");
      return;
    }
    try {
      give(server, target, parts[2]);
    } catch (Exception e) {
      feedback.send(e.getMessage());
    }
  }

  private void give(Server server, String target, String amount) throws Exception {
    if (server.playerList.findPlayerExact(target) == null) {
      throw new Exception(t("Player %s is not online", target));
    }
    if (!amount.matches("^-?\\d+L?$")) {
      throw new Exception(t("Invalid XP amount"));
    }
    server.runCommand("xp", amount + " " + target);
  }

  @Override
  protected String noTargetSpecified() {
    return "No player or amount specified.";
  }
}
