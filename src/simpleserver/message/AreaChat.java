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

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.config.PermissionConfig;

public class AreaChat extends AbstractChat {

  PermissionConfig perm;

  public AreaChat(Player sender) {
    // TODO: notice that area is changed when walking around...
    super(sender);

    perm = sender.getServer().permissions;
    chatRoom = perm.getCurrentArea(sender);
  }

  @Override
  protected boolean sendToPlayer(Player reciever) {
    return !perm.getCurrentArea(sender).isEmpty() && (perm.getCurrentArea(reciever) == perm.getCurrentArea(sender));
  }

  @Override
  public void noRecieverFound() {
    if (perm.getCurrentArea(sender).isEmpty()) {
      sender.addTMessage(Color.RED, "You are in no area at the moment");
    } else {
      sender.addTMessage(Color.RED, "Nobody is in this area to hear you");
    }
  }
}
