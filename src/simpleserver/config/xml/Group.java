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

import org.xml.sax.SAXException;

public class Group extends XMLTag {
  public int id;
  public String name;
  public char color = 'f';

  public boolean ignoreChestLocks;
  public boolean ignoreAreas;
  public boolean forwardUnknownCommands;
  public boolean showTitle;
  public int cooldown;
  public int warmup;

  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String COLOR = "color";
  private static final String IGNORE_CHESTS = "ignoreChestLocks";
  private static final String IGNORE_AREAS = "ignoreAreas";
  private static final String FORWARD_UNKNOWN = "forwardUnknownCommands";
  private static final String SHOW_TITLE = "showTitle";
  private static final String COOLDOWN = "cooldown";
  private static final String WARMUP = "warmup";

  Group() {
    super("group");
    acceptAttribute(COOLDOWN);
    acceptAttribute(WARMUP);
    acceptAttribute(IGNORE_CHESTS);
    acceptAttribute(IGNORE_AREAS);
    acceptAttribute(FORWARD_UNKNOWN);
    acceptAttribute(SHOW_TITLE);
  }

  public Group(int id, String name) {
    this();
    this.id = id;
    this.name = name;
  }

  public int warmup() {
    return warmup * 1000;
  }

  public int cooldown() {
    return cooldown * 1000;
  }

  @Override
  void setAttribute(String name, String value) throws SAXException {
    if (name.equals(ID)) {
      id = getInt(value);
    } else if (name.equals(NAME)) {
      this.name = value;
    } else if (name.equals(COLOR)) {
      if (value.length() > 0) {
        color = value.charAt(0);
      }
    } else if (name.equals(IGNORE_CHESTS) && (value == null || !value.equals("false"))) {
      ignoreChestLocks = true;
    } else if (name.equals(IGNORE_AREAS) && (value == null || !value.equals("false"))) {
      ignoreAreas = true;
    } else if (name.equals(FORWARD_UNKNOWN) && (value == null || !value.equals("false"))) {
      forwardUnknownCommands = true;
    } else if (name.equals(SHOW_TITLE) && (value == null || !value.equals("false"))) {
      showTitle = true;
    } else if (name.equals(COOLDOWN)) {
      cooldown = getInt(value);
    } else if (name.equals(WARMUP)) {
      warmup = getInt(value);
    }
  }

  @Override
  void saveAttributes(AttributeList attributes) {
    attributes.addAttribute(ID, id);
    attributes.addAttribute(NAME, name);
    attributes.addAttribute(COLOR, Character.toString(color));
    if (ignoreChestLocks) {
      attributes.addAttributeElement(IGNORE_CHESTS);
    }
    if (ignoreAreas) {
      attributes.addAttributeElement(IGNORE_AREAS);
    }
    if (showTitle) {
      attributes.addAttributeElement(SHOW_TITLE);
    }
    if (forwardUnknownCommands) {
      attributes.addAttributeElement(FORWARD_UNKNOWN);
    }
    if (cooldown != 0) {
      attributes.addAttributeElement(COOLDOWN, cooldown);
    }
    if (warmup != 0) {
      attributes.addAttributeElement(WARMUP, warmup);
    }
  }
}
