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
package simpleserver.message;

import java.util.LinkedList;
import java.util.List;

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.PlayerList;

public abstract class AbstractChat implements Chat {

  private static final String captionFormat = "%s -> %s";

  protected Player sender;
  protected String chatRoom;

  protected AbstractChat(Player sender) {
    this.sender = sender;
  }

  public Player getSender() {
    return sender;
  }

  public List<Player> getRecievers(PlayerList playerList) {
    List<Player> recieverList = new LinkedList<Player>();
    for (Player reciever : playerList.getArray()) {
      if (sendToPlayer(reciever)) {
        recieverList.add(reciever);
      }
    }
    return recieverList;
  }

  public String buildMessage(String message) {
    String caption = String.format(captionFormat, sender.getName(), chatRoom);
    return getCaptionedString(caption, message);
  }

  abstract protected boolean sendToPlayer(Player reciever);

  abstract public void noRecieverFound();

  protected static final String getCaptionedString(String Caption, String message) {
    return String.format("%s%s: %s%s", Color.GRAY, Caption, Color.WHITE, message);
  }

  protected static final String getColoredString(Color color, String message) {
    return String.format("%s%s", color, message);
  }

  @Override
  public String toString() {
    return chatRoom;
  }
}
