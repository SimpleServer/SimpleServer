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

public class RegisterCommand extends AbstractCommand implements PlayerCommand {

  public RegisterCommand() {
    super("register [OLD PASSWORD] NEW_PASSWORD NEW_PASSWORD", "Set or change password for CustAuth.");
  }

  public void execute(Player player, String message) {

    Authenticator auth = player.getServer().authenticator;
    if (!auth.allowRegistration()) {
      player.addTMessage(Color.RED, "Registration failed! CustAuth registration currently not allowed.");
      return;
    }
    if (player.isGuest()) {
      player.addTMessage(Color.RED, "You cannot use the CustAuth registration if you are a Guest on the Server.");
      return;
    }

    String[] arguments = extractArguments(message);
    if (arguments.length == 2) {
      // registration
      registration(player, arguments, auth);
    } else if (arguments.length == 3) {
      // password change
      changePassword(player, arguments, auth);
    } else {
      player.addTMessage(Color.RED, "Wrong number of arguments!");
    }
  }

  private void changePassword(Player player, String[] arguments, Authenticator auth) {
    String oldPw = arguments[0];
    String newPw1 = arguments[1];
    String newPw2 = arguments[2];

    if (!auth.isRegistered(player.getName())) {
      player.addTMessage(Color.RED, "You are not registered yet!");
      return;
    }

    if (checkPasswordFormat(player, newPw1)) {
      if (newPw1.equals(newPw2)) {

        if (auth.changePassword(player, oldPw, newPw1)) {
          player.addTMessage(Color.GRAY, "Passwords successfully changed!");
        } else {
          player.addTMessage(Color.RED, "Your old password seems incorrect!");
        }
      } else {
        player.addTMessage(Color.RED, "New passwords do not match!");
      }
    }

  }

  private void registration(Player player, String[] arguments, Authenticator auth) {
    String newPw1 = arguments[0];
    String newPw2 = arguments[1];

    if (auth.isRegistered(player.getName())) {
      player.addTMessage(Color.RED, "You are already registered!");
      return;
    }

    if (checkPasswordFormat(player, newPw1)) {
      if (newPw1.equals(newPw2)) {

        auth.register(player, newPw1);
        player.addTMessage(Color.GRAY, "Registration successful!");
      } else {
        player.addTMessage(Color.RED, "New passwords do not match!");
      }
    }
  }

  private boolean checkPasswordFormat(Player player, String pw) {
    boolean r = (pw.length() >= 4 && pw.length() <= 10);
    if (!r) {
      player.addTMessage(Color.RED, "The password must contain 4 to 10 characters.");
    }
    return r;
  }
}
