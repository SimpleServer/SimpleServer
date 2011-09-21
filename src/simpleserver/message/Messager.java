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

import simpleserver.Player;
import simpleserver.Server;
import simpleserver.util.RingCache;

public class Messager {
  private static final int MAXIMUM_MESSAGE_SIZE = 119;
  private static final int MESSAGE_SIZE = 60;

  private Server server;
  private RingCache<String> forwardedMessages = new RingCache<String>(String.class, 10);

  public Messager(Server server) {
    this.server = server;
  }

  public void propagate(Chat chat, String message, boolean build) {
    int recieverCount = 0;
    String builtMessage = build ? chat.buildMessage(message) : message;

    if (chat.broadcast(server)) {
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
      if (server.config.properties.getBoolean("forwardChat")) {
        forwardToServer(chat, message);
      }
    } else {
      forwardToServer(chat, message, false);
    }

    if (server.config.properties.getBoolean("logMessages")) {
      server.messageLog(chat, message);
    }

  }

  public void propagate(Chat chat, String message) {
    propagate(chat, message, true);
  }

  private void forwardToServer(Chat chat, String message) {
    forwardToServer(chat, message, true);
  }

  private void forwardToServer(Chat chat, String message, boolean absorbOnReturn) {
    Player sender = chat.getSender();
    String forwardMessage = String.format(server.config.properties.get("msgForwardFormat"), chat, message);

    for (String msgPart : warpMessage(forwardMessage)) {
      if (absorbOnReturn) {
        forwardedMessages.put(msgPart);
      }
      sender.forwardMessage(msgPart);
    }
  }

  public boolean wasForwarded(String message) {
    return forwardedMessages.hasLike(message);
  }

  private LinkedList<String> warpMessage(String message) {
    LinkedList<String> messages = new LinkedList<String>();

    int msgSize = MESSAGE_SIZE;
    if (!server.config.properties.getBoolean("msgWrap")) {
      msgSize = MAXIMUM_MESSAGE_SIZE;
    }
    if (message.length() > 0) {
      while (message.length() > msgSize) {
        int end = msgSize - 1;
        while (end > 0 && message.charAt(end) != ' ') {
          end--;
        }
        if (end == 0) {
          end = msgSize;
        } else {
          end++;
        }

        if (end > 0 && message.charAt(end) == '\u00a7') {
          end--;
        }

        messages.add(message.substring(0, end).trim());
        message = message.substring(end);
      }

      int end = message.length();
      if (message.length() > 0 && message.charAt(end - 1) == '\u00a7') {
        end--;
      }
      if (end > 0) {
        messages.add(message.substring(0, end).trim());
      }
    }
    return messages;
  }
}
