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

import simpleserver.Authenticator;
import simpleserver.Color;
import simpleserver.Player;

public class LoginCommand extends AbstractCommand implements PlayerCommand {

  public LoginCommand() {
    super("login PLAYER PASSWORD", "Login using CustAuth.");
  }

  public void execute(Player player, String message) {

    Authenticator auth = player.getServer().authenticator;
    if (!allowUse(player, auth)) {
      return;
    }

    String[] arguments = extractArguments(message);

    if (arguments.length != 2) {
      player.addTMessage(Color.RED, "Wrong number of arguments!");
      return;
    }

    String userName = arguments[0];
    String password = arguments[1];

    if (player.getServer().playerList.findPlayerExact(userName) != null) {
      player.addTMessage(Color.RED, "Login failed! Player already in server.");
      return;
    }

    if (auth.isRegistered(userName)) {
      if (auth.loginBanTimeOver(player)) {
        if (auth.login(player, userName, password)) {
          player.addTMessage(Color.GRAY, "Login successfull!");
          player.addTMessage(Color.GRAY, "Please reconnect to the server within %s seconds to complete the CustAuth process.", Authenticator.REQUEST_EXPIRATION);
          player.setUsedAuthenticator(true);
        } else {
          player.addTMessage(Color.RED, "Login failed! Password missmatch.");
          auth.banLogin(player);
        }
      } else {
        player.addTMessage(Color.RED, "Login currently not allowed. Try again in %s seconds", auth.banTimeEnd(player));
      }
    } else {
      player.addTMessage(Color.RED, "You are not registered!");
      player.addTMessage(Color.RED, "Use the %s command to register.", (commandPrefix() + "register"));
      return;
    }
  }

  private boolean allowUse(Player player, Authenticator auth) {
    if (!auth.allowLogin()) {
      player.addTMessage(Color.RED, "Login failed! CustAuth login currently not allowed.");
      return false;
    }
    if (!player.isGuest()) {
      player.addTMessage(Color.RED, "You can only use CustAuth login if you're a guest.");
      return false;
    }
    return true;
  }
}
