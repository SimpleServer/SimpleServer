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
import simpleserver.config.ReadFiles;

public class ReadCommand extends AbstractCommand implements PlayerCommand {

  public ReadCommand() {
    super("read DOCUMENT", "display a specified text document");
  }

  public void execute(Player player, String message) {
    String filename = extractArgument(message);
    ReadFiles d = player.getServer().docs;

    if (filename == null) {
      // no arguments given -> display list of available files

      StringBuilder s = new StringBuilder();
      for (String file : d.getList()) {
        s.append(file);
        s.append(", ");
      }
      if (s.length() > 2) {
        s.delete(s.length() - 2, s.length());
      }
      player.addTCaptionedMessage("Avilable text files", s.toString());
      return;
    }

    String text = d.getText(filename);
    if (text != null) {
      player.addTCaptionedMessage("Loaded document", filename);

      String[] textlines = text.split("\n");
      for (String l : textlines) {
        player.addMessage(Color.WHITE, l);
      }
    } else {
      player.addTMessage(Color.RED, "Document %s does not exist", filename);
    }
  }

}
