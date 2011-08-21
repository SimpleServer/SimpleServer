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

public class MuteCommand extends PlayerArgCommand {
  public MuteCommand() {
    super("mute PLAYER", "Block the named player from normal chat");
  }

  @Override
  protected void executeWithTarget(Player player, String message, String name) {
    Server server = player.getServer();
    server.mutelist.addName(name);

    server.adminLog("Admin " + player.getName() + " muted player:\t " + name);
    String msg = t("Player %s has been muted!", name);
    server.runCommand("say", msg);
  }

  @Override
  public void usage(Player player) {
    player.addTMessage(Color.GRAY, "Forbid the player to use the normal world chat until he/she is unmuted");
  }
}
