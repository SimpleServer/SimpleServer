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

import java.util.Set;

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.config.xml.AllBlocks;
import simpleserver.config.xml.Area;
import simpleserver.config.xml.Chests;
import simpleserver.config.xml.Config;
import simpleserver.config.xml.Config.AreaStoragePair;
import simpleserver.config.xml.DimensionConfig;
import simpleserver.config.xml.Permission;

public class MyAreaCommand extends AbstractCommand implements PlayerCommand {
  private static final byte DEFAULT_SIZE = 50;

  public MyAreaCommand() {
    super("myarea [start|end|save|unsave|rename]",
          "Manage your personal area");
  }

  private boolean areaSizeOk(Player player, int[] size) {
    return (Math.abs(player.areastart.x() - player.areaend.x()) < size[0])
        && (Math.abs(player.areastart.z() - player.areaend.z()) < size[1])
        && player.areaend.dimension() == player.areastart.dimension();
  }

  private int[] getAreaMax(Player player) {
    // Get the maximum area sizes from config.xml
    int[] size = { Math.abs(player.getServer().config.properties.getInt("areaMaxX")),
        Math.abs(player.getServer().config.properties.getInt("areaMaxZ")) };

    // Check to make sure the configuration is valid
    // If not, reset to default size
    for (byte i = 0; i < size.length; i++) {
      if (size[i] <= 0 || Integer.valueOf(size[i]) == null) {
        size[i] = DEFAULT_SIZE;
      }
    }

    return size;
  }

  public void execute(Player player, String message) {
    // Set up an integer array to hold the maximum area size
    int[] maxSize = getAreaMax(player); // X, Z

    Config config = player.getServer().config;
    String arguments[] = extractArguments(message);

    if (arguments.length == 0) {
      player.addTCaptionedMessage("Usage", commandPrefix() + "myarea [start|end|save|unsave|rename]");
      return;
    }

    if (arguments[0].equals("start")) {
      player.areastart = player.position();
      player.areastart = player.areastart.setY(0); // no height limit
      player.addTMessage(Color.GRAY, "Start coordinate set.");
    } else if (arguments[0].equals("end")) {
      player.areaend = player.position();
      player.areaend = player.areaend.setY(255); // no height limit
      player.addTMessage(Color.GRAY, "End coordinate set.");
    } else if (arguments[0].equals("save")) {
      if (player.areastart == null || player.areaend == null) {
        player.addTMessage(Color.RED, "Define start and end coordinates for your area first!");
        return;
      }
      if (!areaSizeOk(player, maxSize)) {
        player.addTMessage(Color.RED, "Your area is allowed to have a maximum size of " +
            maxSize[0] + "x" + maxSize[1] + "!");
        return;
      }
      if (player.getServer().config.playerArea(player) != null) {
        player.addTMessage(Color.RED, "New area can not be saved before you remove your old one!");
        return;
      }
      Area area = createPlayerArea(player);
      Set<Area> overlaps = config.dimensions.overlaps(area);
      if (!overlaps.isEmpty()) {
        player.addTMessage(Color.RED, "Your area overlaps with other areas and could therefore not be saved!");
        StringBuilder str = new StringBuilder();
        for (Area overlap : overlaps) {
          str.append(overlap.name);
          str.append(", ");
        }
        str.delete(str.length() - 2, str.length() - 1);
        player.addTCaptionedMessage("Overlapping areas", "%s", str);
        return;
      }
      saveArea(area, player);
      player.addTMessage(Color.GRAY, "Your area has been saved!");
    } else if (arguments[0].equals("unsave") || arguments[0].equals("remove")) {
      AreaStoragePair area = config.playerArea(player);
      if (area == null) {
        player.addTMessage(Color.RED, "You currently have no personal area which can be removed!");
        return;
      }

      area.storage.remove(area.area);
      player.addTMessage(Color.GRAY, "Your area has been removed!");
      player.getServer().saveConfig();
    } else if (arguments[0].equals("rename")) {
      AreaStoragePair area = config.playerArea(player);
      if (area == null) {
        player.addTMessage(Color.RED, "You currently have no personal area which can be renamed!");
        return;
      }

      String label = extractArgument(message, 1);
      if (label != null) {
        area.area.name = label;
        player.addTMessage(Color.GRAY, "Your area has been renamed!");
        player.getServer().saveConfig();
      } else {
        player.addTMessage(Color.RED, "Please supply an area name.");
      }
    } else {
      player.addTMessage(Color.RED, "You entered an invalid argument.");
    }
  }

  private Area createPlayerArea(Player player) {
    Area area = new Area(t("%s's area", player.getName()), player.areastart, player.areaend);
    area.owner = player.getName().toLowerCase();
    Permission perm = new Permission(player);
    AllBlocks blocks = new AllBlocks();
    blocks.destroy = perm;
    blocks.place = perm;
    blocks.use = perm;
    area.allblocks.blocks = blocks;
    area.chests.chests = new Chests(perm);
    return area;
  }

  private void saveArea(Area area, Player player) {
    DimensionConfig dimension = player.getServer().config.dimensions.get(area.start.dimension());
    if (dimension == null) {
      dimension = player.getServer().config.dimensions.add(area.start.dimension());
    }
    dimension.add(area);
    player.getServer().saveConfig();
  }
}
