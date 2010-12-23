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
package simpleserver;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

public class Group {
  private final String groupName;
  private final boolean showTitle;
  private final boolean isAdmin;
  private final String color;

  public Group(String groupName, boolean showTitle, boolean isAdmin,
               String color) {
    this.groupName = groupName;
    this.showTitle = showTitle;
    this.isAdmin = isAdmin;
    this.color = color;
  }

  public String getName() {
    return groupName;
  }

  public boolean showTitle() {
    return showTitle;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public String getColor() {
    return color;
  }

  public static boolean isMember(ImmutableSet<Integer> groups, Player player) {
    return groups.contains(player.getGroupId()) || groups.contains(-1)
        || (groups.contains(0) && (player.getGroupId() != -1));
  }

  public static ImmutableSet<Integer> parseGroups(String idString) {
    return parseGroups(idString, ",");
  }

  public static ImmutableSet<Integer> parseGroups(String idString,
                                                  String delimiter) {
    String[] segments = idString.split(delimiter);
    ImmutableSortedSet.Builder<Integer> groups = ImmutableSortedSet.naturalOrder();
    for (String segment : segments) {
      if (segment.matches("^\\s*$")) {
        continue;
      }

      try {
        groups.add(Integer.valueOf(segment));
        continue;
      }
      catch (NumberFormatException e) {
      }

      String[] range = segment.split("-");
      if (range.length != 2) {
        System.out.println("Unable to parse group: " + segment);
        continue;
      }

      int low;
      int high;
      try {
        low = Integer.valueOf(range[0]);
        high = Integer.valueOf(range[1]);
      }
      catch (NumberFormatException e) {
        System.out.println("Unable to parse group: " + segment);
        continue;
      }

      if (low > high) {
        System.out.println("Unable to parse group: " + segment);
        continue;
      }

      for (int k = low; k <= high; ++k) {
        groups.add(k);
      }
    }

    return groups.build();
  }
}
