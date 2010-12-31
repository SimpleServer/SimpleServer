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

import simpleserver.Player;
import simpleserver.Server;

public class SaveCommand extends AbstractCommand implements PlayerCommand,
    ServerCommand {
  public SaveCommand() {
    super("save", "Store configuration to disk and force a map save");
  }

  @Override
  public boolean shouldPassThroughToSMPAPI() {
    return true;
  }

  public void execute(Player player, String message) {
    player.getServer().saveResources();
    player.getServer().runCommand("save-all", null);
    player.addMessage("\u00a77Resources Saved!");
  }

  public void execute(Server server, String message) {
    server.saveResources();
    server.runCommand("save-all", null);
    System.out.println("Resources Saved!");
  }
}
