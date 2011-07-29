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

public class TellCommand extends OnlinePlayerArgCommand implements PlayerCommand {
  public TellCommand() {
    super("tell PLAYER MESSAGE...", "Send a message to the named player");
  }

  @Override
  protected void executeWithTarget(Player player, String message, Player target) {
    String chat = extractArgument(message, 1);
    if (chat != null) {
      String caption = player.getName() + " -> " + target.getName();
      player.addCaptionedMessage(caption, chat);
      target.addCaptionedMessage(caption, chat);

      target.setReply(player);
    } else {
      player.addTMessage(Color.RED, "Please supply a message.");
    }
  }

  @Override
  protected void noTargetSpecified(Player player, String message) {
    player.addTMessage(Color.RED, "No player or message specified.");
  }

  @Override
  public void usage(Player player) {
    player.addTMessage(Color.GRAY, "Send a player a quiet message (whisper) that only he receives");
  }
}
