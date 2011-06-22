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

public class GPSCommand extends OnlinePlayerArgCommand {
  public GPSCommand() {
    super("gps [PLAYER]", "Display block coordinates of named player or yourself", true);
  }

  @Override
  protected void executeWithTarget(Player player, String message, Player target) {
    String name = t("Your");
    if (target == null) {
      target = player;
    } else {
      name = t("%s's", target.getName());
    }

    player.addTMessage(Color.GRAY,
                       "%s Latitude: %s %d %s Longitude: %s %d %s Altitude: %s %d %s Dimension: %s %s",
                       name, Color.WHITE, (int) target.getX(), Color.GRAY, Color.WHITE,
                       (int) target.getZ(), Color.GRAY, Color.WHITE, (int) target.getY(), Color.GRAY,
                       Color.WHITE, target.getDimension());
  }
}
