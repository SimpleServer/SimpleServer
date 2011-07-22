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
import simpleserver.message.Message;
import simpleserver.message.PrivateMessage;

public class TellCommand extends MessageCommand implements
    PlayerCommand {

  public TellCommand() {
    super("tell PLAYER MESSAGE...",
          "Send a message to the named player");
  }

  @Override
  protected Message getMessageInstance(Player sender, String message) {
    String[] arguments = extractArguments(message);

    if (arguments.length > 0) {
      Player reciever = sender.getServer().findPlayer(arguments[0]);
      if (reciever == null) {
        sender.addTMessage(Color.RED, "Player not online (%s)", arguments[0]);
      } else {
        reciever.setReply(sender);
        return new PrivateMessage(sender, reciever);
      }
    } else {
      sender.addTMessage(Color.RED, "No player or message specified.");
    }
    return null;
  }

  @Override
  protected String extractMessage(String message) {
    return extractArgument(message, 1);
  }
}
