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
package simpleserver.config.xml;

import static simpleserver.config.xml.XMLTag.getInt;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

import simpleserver.Player;

public class Permission {
  private static Pattern GROUP_PATTERN = Pattern.compile("(-?\\d+)-(-?\\d+)");

  private Set<String> allowedPlayers = new HashSet<String>(0);
  private Set<String> disallowedPlayers = new HashSet<String>(0);
  private TreeMap<Integer, Integer> allowedGroups = new TreeMap<Integer, Integer>();
  private TreeMap<Integer, Integer> disallowedGroups = new TreeMap<Integer, Integer>();

  public Permission(String permission) throws SAXException {
    set(permission);
  }

  public Permission() {
    allowedGroups.put(Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  public Permission(String allow, String disallow) throws SAXException {
    if (disallow == null) {
      disallow = "";
    }
    if (allow == null) {
      allow = "";
    }

    String[] allowParts = allow.split(";");
    String[] disallowParts = disallow.split(";");

    StringBuilder perm = new StringBuilder(allowParts[0]);

    for (String part : disallowParts[0].split(",")) {
      if (part.length() == 0) {
        continue;
      }
      perm.append(',');
      perm.append('!');
      perm.append(part);
    }
    if (allowParts[0].length() == 0) {
      perm.deleteCharAt(0);
    }

    if (allowParts.length >= 2 || disallowParts.length >= 2) {
      perm.append(';');

      if (allowParts.length >= 2) {
        perm.append(allowParts[1]);
      }

      if (disallowParts.length >= 2) {
        for (String part : disallowParts[1].split(",")) {
          if (perm.charAt(perm.length() - 1) != ';') {
            perm.append(',');
          }
          perm.append('!');
          perm.append(part);
        }
      }
    }

    set(perm.toString());
  }

  private void set(String permission) throws SAXException {
    if (permission.equals("-")) {
      return;
    }
    String[] parts = permission.split(";");
    for (String group : parts[0].split(",")) {
      if (group.length() == 0) {
        continue;
      }
      boolean allow = true;
      if (group.startsWith("!") || group.startsWith("~") || group.startsWith("¬")) {
        allow = false;
        group = group.substring(1);
      }

      int start;
      int end;
      Matcher matcher;

      try {
        if (group.endsWith("+")) {
          start = getInt(group.substring(0, group.length() - 1));
          end = Integer.MAX_VALUE;
        } else if (group.equals("*")) {
          start = Integer.MIN_VALUE;
          end = Integer.MAX_VALUE;
        } else if ((matcher = GROUP_PATTERN.matcher(group)).matches()) {
          start = getInt(matcher.group(1));
          end = getInt(matcher.group(2));
        } else {
          start = end = getInt(group);
        }
      } catch (SAXException e) {
        throw new SAXException("Error while parsing permission \"" + permission + "\":");
      }

      if (allow) {
        allowedGroups.put(start, end);
      } else {
        disallowedGroups.put(start, end);
      }
    }

    if (parts.length >= 2) {
      for (String player : parts[1].split(",")) {
        if (player.startsWith("!") || player.startsWith("~") || player.startsWith("¬")) {
          disallowedPlayers.add(player.substring(1).toLowerCase());
        } else {
          allowedPlayers.add(player.toLowerCase());
        }
      }
    }

    if (allowedGroups.isEmpty() && allowedPlayers.isEmpty()) {
      allowedGroups.put(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
  }

  public boolean contains(Player player) {
    if (disallowedPlayers.contains(player.getName().toLowerCase())) {
      return false;
    }
    if (contains(disallowedGroups, player.getGroupId())) {
      return false;
    }
    return allowedPlayers.contains(player.getName().toLowerCase()) || contains(allowedGroups, player.getGroupId());
  }

  private static boolean contains(TreeMap<Integer, Integer> ranges, int value) {
    Entry<Integer, Integer> entry = ranges.lowerEntry(value + 1);
    return entry != null && entry.getValue() >= value;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    groupList(allowedGroups, str, "");
    if (str.equals("*,") && allowedPlayers.isEmpty()) {
      str = new StringBuilder();
    }
    groupList(disallowedGroups, str, "¬");
    if (str.length() > 0) {
      str.deleteCharAt(str.length() - 1);
    }
    if (!allowedPlayers.isEmpty() || !disallowedPlayers.isEmpty()) {
      str.append(';');
      for (String player : allowedPlayers) {
        str.append(player);
        str.append(',');
      }
      for (String player : disallowedPlayers) {
        str.append('¬');
        str.append(player);
        str.append(',');
      }
      str.deleteCharAt(str.length() - 1);
    }
    return str.length() == 0 ? "-" : str.toString();
  }

  private static void groupList(Map<Integer, Integer> groups, StringBuilder str, String prefix) {
    for (Entry<Integer, Integer> entry : groups.entrySet()) {
      str.append(prefix);
      int start = entry.getKey();
      int end = entry.getValue();
      if (start == Integer.MIN_VALUE) {
        str.append('*');
      } else if (end == Integer.MAX_VALUE) {
        str.append(start);
        str.append('+');
      } else if (start == end) {
        str.append(start);
      } else {
        str.append(start);
        str.append('-');
        str.append(end);
      }
      str.append(',');
    }
  }

}
