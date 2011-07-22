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

public class Group {
  private final String groupName;
  private final int groupId;
  private final boolean showTitle;
  private final boolean isAdmin;
  private final String color;
  private final boolean forwardsCommands;
  private final int warmupSecs;
  private final int cooldownSecs;

  public Group(String groupName, int groupId, boolean showTitle,
               boolean isAdmin, String color, boolean forwardsCommands,
               int warmupSecs, int cooldownSecs) {
    this.groupName = groupName;
    this.groupId = groupId;
    this.showTitle = showTitle;
    this.isAdmin = isAdmin;
    this.color = color;
    this.forwardsCommands = forwardsCommands;
    this.warmupSecs = warmupSecs;
    this.cooldownSecs = cooldownSecs;
  }

  public String getName() {
    return groupName;
  }

  public int getId() {
    return groupId;
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

  public boolean getForwardsCommands() {
    return forwardsCommands;
  }

  public int getWarmupMillis() {
    return warmupSecs * 1000;
  }

  public int getCooldownMillis() {
    return cooldownSecs * 1000;
  }
}
