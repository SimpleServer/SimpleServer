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

import java.util.Hashtable;

import simpleserver.Player;
import simpleserver.Server;

public class Messager {
  private Server server;

  private Hashtable<String, Integer> forwardedMessages = new Hashtable<String, Integer>();

  public Messager(Server server) {
    this.server = server;
  }

  public void propagate(Chat chat, String message) {
    int recieverCount = 0;
    String builtMessage = chat.buildMessage(message);

    for (Player reciever : chat.getRecievers(server.playerList)) {
      reciever.addMessage(builtMessage);

      if (!reciever.equals(chat.getSender())) {
        recieverCount++;
      }
    }

    if (recieverCount == 0) {
      chat.noRecieverFound();
      return;
    }

    if (server.options.getBoolean("forwardChat")) {
      forwardToServer(chat, message);
    }
  }

  private void forwardToServer(Chat chat, String message) {
    Player sender = chat.getSender();
    String forwardMessage = String.format(server.options.get("msgForwardFormat"), message, chat);

    forwardedMessages.put(String.format("<%s> %s", sender.getName(), forwardMessage), server.numPlayers());
    sender.forwardMessage(forwardMessage);
  }

  public boolean wasForwarded(String message) {
    if (forwardedMessages.containsKey(message)) {
      int toSuppress = forwardedMessages.get(message) - 1;
      if (toSuppress > 0) {
        forwardedMessages.put(message, toSuppress);
      } else {
        forwardedMessages.remove(message);
      }
      return true;
    }
    return false;
  }
}
