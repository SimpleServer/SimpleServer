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

import simpleserver.Coordinate.Dimension;
import simpleserver.Player;
import simpleserver.message.Chat;
import simpleserver.message.DimensionChat;

public class DimensionSayCommand extends MessageCommand implements PlayerCommand {

  String chatMessage;

  public DimensionSayCommand() {
    super("dimension [DIMENSION] MESSAGE", "Send message only in specified dimension and to yourself");
  }

  @Override
  protected Chat getMessageInstance(Player sender, String rawMessage) {
    Dimension dim = sender.getDimension();
    chatMessage = rawMessage;

    String[] arguments = extractArguments(rawMessage);
    if (arguments.length > 1 && Dimension.get(arguments[0]) != Dimension.LIMBO) {
      dim = Dimension.get(arguments[0]);
      chatMessage = extractArgument(rawMessage);
    }
    return new DimensionChat(sender, dim);
  }

  @Override
  protected String extractMessage(String message) {
    return extractArgument(chatMessage);
  }

}
