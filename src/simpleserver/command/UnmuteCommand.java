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
import simpleserver.Player;
import simpleserver.Server;

public class UnmuteCommand extends PlayerArgCommand {
  public UnmuteCommand() {
    super("unmute PLAYER", "Allow the named player to use normal chat again");
  }

  @Override
  protected void executeWithTarget(Player player, String message, String target) {
    Server server = player.getServer();
    unmute(server, target);
    server.adminLog("Admin " + player.getName() + " unmuted player:\t " + target);
  }

  @Override
  protected void executeWithTarget(Server server, String message, String target, CommandFeedback feedback) {
    unmute(server, target);
    server.adminLog("Console unmuted player:\t " + target);
    feedback.send("Unmuted player %s", target);
  }

  private void unmute(Server server, String target) {
    server.mutelist.removeName(target);
    String msg = t("Player %s has been unmuted!", target);
    server.runCommand("say", msg);
  }
}
