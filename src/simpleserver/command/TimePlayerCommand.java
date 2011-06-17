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
import simpleserver.lang.Translations;

public class TimePlayerCommand extends TimeCommand implements PlayerCommand {
  private Player player;

  public synchronized void execute(Player player, String message) {
    this.player = player;
    execute(player.getServer(), message);
  }

  @Override
  protected void captionedInfo(String caption, String message, Object... args) {
    player.addCaptionedMessage(caption, message, args);
  }

  @Override
  protected void error(String message) {
    player.addMessage(Color.RED, message);
  }

  @Override
  protected void info(String message) {
    player.addMessage(Color.GRAY, message);
  }

  @Override
  protected void tCaptionedInfo(String caption, String message, Object... args) {
    captionedInfo(Translations.getInstance().get(caption), message, args);
  }

  @Override
  protected void tError(String message) {
    error(Translations.getInstance().get(message));
  }

  @Override
  protected void tInfo(String message) {
    info(Translations.getInstance().get(message));
  }
}
