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
import simpleserver.config.xml.Property;

public class SetOptCommand extends AbstractCommand implements PlayerCommand {
  public SetOptCommand() {
    super("setopt KEY [VALUE]", "List or change configuration options");
  }

  public void execute(Player player, String message) {
    Server server = player.getServer();
    String arguments[] = extractArguments(message);

    if (arguments.length == 0) {
      String setoptCommand = commandPrefix() + "setopt";
      player.addTMessage(Color.GRAY, "Usage:");
      player.addTMessage(Color.GRAY, "%s KEY: search for configuration option", setoptCommand);
      player.addTMessage(Color.GRAY, "%s KEY VALUE: set configuration option", setoptCommand);
      player.addTMessage(Color.GRAY, "For a list of valid KEY values, see the wiki/README");
      player.addTMessage(Color.GRAY, "Some keys cannot be changed using this command");
      return;
    }

    if (arguments.length == 1) {
      int matches = 0;
      String key = arguments[0];

      player.addTMessage(Color.GRAY, "Properties:");

      for (Property property : server.config.properties) {
        if (property.name.toLowerCase().contains(key.toLowerCase()) && !property.value.contains("\u00a7")) {
          player.addTMessage(Color.WHITE, "%s => %s", property.name, property.value);
          matches++;
        }
      }

      player.addTMessage(Color.GRAY, "%d matches", matches);
    } else {
      String key = arguments[0];
      String value = message.substring(message.indexOf(arguments[1]));

      boolean flag = false;

      // Map types are case sensitive, do search manually
      for (Property property : server.config.properties) {
        if (property.name.toLowerCase().equals(key.toLowerCase())) {
          key = property.name;
          flag = true;
          break;
        }
      }

      if (!flag) {
        player.addTMessage(Color.RED, "Invalid key specified.");
        return;
      }

      String currentValue = server.config.properties.get(key);

      if (currentValue.contains("\u00a7")) {
        player.addTMessage(Color.RED, "Selected field contains an escape character that cannot be altered by this command.");
        return;
      }

      boolean currentValueIsBoolean = currentValue.toLowerCase().equals("true") || currentValue.toLowerCase().equals("false");
      boolean newValueIsBoolean = value.toLowerCase().equals("true") || value.toLowerCase().equals("false");

      if (currentValueIsBoolean != newValueIsBoolean) {
        player.addTMessage(Color.RED, "Type mismatch, key or value is boolean.");
        return;
      }

      server.config.properties.set(key, value);
      server.saveConfig();

      server.adminLog("User " + player.getName() + " changed configuration " + key + ": " + currentValue + " => " + value);

      player.addTMessage(Color.GRAY, "Configuration updated.");
    }
  }
}
