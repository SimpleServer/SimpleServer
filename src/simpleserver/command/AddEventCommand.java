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
import simpleserver.Coordinate;

import simpleserver.config.xml.Event;

public class AddEventCommand extends AbstractCommand implements PlayerCommand {
  public AddEventCommand() {
    super("addevent NAME", "Add an empty placeholder event on current block below player");
  }

  public void execute(Player player, String message) {
    String[] args = extractArguments(message);
    if (args.length == 0) {
      player.addTMessage(Color.RED, "No name given!");
      return;
    }

    if (player.getServer().config.events.contains(args[0])) {
      player.addTMessage(Color.RED, "This event name is already taken!");
      return;
    }

    Event e = new Event(args[0],
              new Coordinate((int)player.x(), (int)player.y(), (int)player.z(), player.getDimension()));
    player.getServer().config.events.add(e);
    player.getServer().saveConfig();

    player.addTMessage(Color.GRAY, "Event created!");
  }
}
