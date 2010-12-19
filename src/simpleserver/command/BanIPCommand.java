/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package simpleserver.command;

import simpleserver.Player;
import simpleserver.Server;

public class BanIPCommand extends AbstractCommand implements PlayerCommand {
  public BanIPCommand() {
    super("banip");
  }

  public void execute(Player player, String message) {
    String[] arguments = extractArguments(message);
    Server server = player.getServer();

    if (arguments.length >= 1) {
      Player p = player.getServer().findPlayer(arguments[0]);
      if (p == null) {
        server.ipBans.addBan(arguments[0]);
        player.addMessage("IP Address " + arguments[0] + " has been banned!");
        server.adminLog.addMessage("User " + player.getName()
            + " banned ip:\t " + arguments[0]);
      }
      else {
        server.ipBans.addBan(p.getIPAddress());
        server.kick(p.getName(), "IP Banned!");
        server.runCommand("say", "Player " + p.getName()
            + " has been IP banned!");
        server.adminLog.addMessage("User " + player.getName()
            + " banned ip:\t " + arguments[0] + "\t(" + p.getName() + ")");
      }
    }
    else {
      player.addMessage("\302\247cNo player or IP specified.");
    }
  }
}
