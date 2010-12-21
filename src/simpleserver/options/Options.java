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
package simpleserver.options;

import java.util.Scanner;

import simpleserver.config.CommandList;

public class Options extends AbstractOptions {
  private static final String[] ranks = new String[] { "warpPlayerRank",
      "warpPlayerRank", "teleportRank", "homeCommandRank", "giveRank",
      "givePlayerRank", "muteRank", "muteRank", "setRankRank", "useWarpRank",
      "createWarpRank" };
  private static final String[] rankConversions = new String[] { "warptome",
      "warpmeto", "tp", "home", "give", "giveplayer", "mute", "unmute",
      "setgroup", null, null };

  public Options() {
    super("simpleserver.properties");
  }

  public void set(String option, String value) {
    options.setProperty(option, value);
  }

  @Override
  public void load() {
    super.load();

    if (get("msgFormat").equals("")) {
      set("msgFormat", defaultOptions.getProperty("msgFormat"));
    }
    if (get("msgTitleFormat").equals("")) {
      set("msgTitleFormat", defaultOptions.getProperty("msgTitleFormat"));
    }

    for (String rank : ranks) {
      if (options.contains(rank)) {
        conversion();
        break;
      }
    }

    if (getInt("internalPort") == getInt("port")) {
      System.out.println("OH NO! Your 'internalPort' and 'port' properties are the same! Edit simpleserver.properties and change them to different values. 'port' is recommended to be 25565, the default port of minecraft, and will be the port you actually connect to.");
      System.out.println("Press enter to continue...");
      Scanner in = new Scanner(System.in);
      in.nextLine();
      System.exit(0);
    }
  }

  @Override
  protected void missingFile() {
    super.missingFile();

    System.out.println("Properties file not found! Created simpleserver.properties! Adjust values and then start the server again!");
    System.out.println("Press enter to continue...");
    Scanner in = new Scanner(System.in);
    in.nextLine();
    System.exit(0);
  }

  private void conversion() {
    CommandList cmds = new CommandList();
    cmds.load();
    for (int c = 0; c < ranks.length; ++c) {
      if (rankConversions[c] != null) {
        String value = options.getProperty(ranks[c]);
        if (value != null) {
          try {
            cmds.setGroup(rankConversions[c], Integer.parseInt(value));
          }
          catch (NumberFormatException e) {
          }
        }
      }
    }
    cmds.save();

    for (String rank : ranks) {
      options.remove(rank);
    }
    save();

    System.out.println("The Properties file format has changed! Command ranks are now set in command-list.txt!");
    System.out.println("Your previous settings for commands have been saved, and cleared from simpleserver.properties!");
    System.out.println("Press enter to continue...");
    Scanner in = new Scanner(System.in);
    in.nextLine();
    System.exit(0);
  }
}
