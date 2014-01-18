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

import java.util.List;

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.config.xml.Area;
import simpleserver.config.xml.Config;

public class AreaChat extends AbstractChat {

  private Config config;
  private List<Area> areas;

  public AreaChat(Player sender) {
    super(sender);

    config = sender.getServer().config;
    areas = config.dimensions.areas(sender.position());
    if (areas != null && !areas.isEmpty()) {
      chatRoom = areas.get(0).name;
    }
  }

  @Override
  protected boolean sendToPlayer(Player reciever) {
    if (noArea()) {
      return false;
    }
    if (reciever == sender) {
      return true;
    } else if (areas == null || areas.isEmpty()) {
      return false;
    }
    List<Area> recAreas = config.dimensions.areas(reciever.position());
    for (Area area : recAreas) {
      if (areas.contains(area)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void noRecieverFound() {
    if (noArea()) {
      sender.addTMessage(Color.RED, "You are in no area at the moment");
    } else {
      sender.addTMessage(Color.RED, "Nobody is in this area to hear you");
    }
  }

  public boolean noArea() {
    return areas.isEmpty();
  }
}
