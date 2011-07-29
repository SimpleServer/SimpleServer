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
import simpleserver.Server;

public class ReloadCommand extends AbstractCommand implements PlayerCommand,
    ServerCommand {
  public ReloadCommand() {
    super("reload", "Reread the configuration files from disk");
  }

  public void execute(Player player, String message) {
    player.getServer().loadResources();
    if (player.getServer().permissions.loadsuccess) {
      player.addTMessage(Color.GRAY, "Resources Reloaded!");
    } else {
      player.addTMessage(Color.RED, "Error while reloading %s. See %s for detailed information.",
                         "permissions.xml", "server.log");
    }
  }

  public void execute(Server server, String message) {
    server.loadResources();
    if (server.permissions.loadsuccess) {
      System.out.println("Resources Reloaded!");
    }
  }

  @Override
  public void usage(Player player) {
    player.addTMessage(Color.GRAY, "Reload all the configuration files (e.g. permissions.xml)");
    player.addTMessage(Color.GRAY, "This does not affect the map");
  }
}
