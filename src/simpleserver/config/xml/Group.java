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

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class Group extends XMLTag {
  public int id;
  public String name;

  public boolean ignoreChestLocks;
  public boolean ignoreAreas;
  public boolean forwardUnknownCommands;
  public boolean showTitle;
  public int cooldown;
  public int warmup;

  private static final String IGNORE_CHESTS = "ignorechestlocks";
  private static final String IGNORE_AREAS = "ignoreareas";
  private static final String FORWARD_UNKNOWN = "forwardunknowncommands";
  private static final String SHOW_TITLE = "showtitle";
  private static final String COOLDOWN = "cooldown";
  private static final String WARMUP = "warmup";

  public Group() {
    super("group");
    acceptAttribute(COOLDOWN);
    acceptAttribute(WARMUP);
    acceptAttribute(IGNORE_CHESTS);
    acceptAttribute(IGNORE_AREAS);
    acceptAttribute(FORWARD_UNKNOWN);
    acceptAttribute(SHOW_TITLE);
  }

  @Override
  protected void setAttribute(String name, String value) throws SAXException {
    if (name.equals("id")) {
      id = getInt(value);
    } else if (name.equals(IGNORE_CHESTS)) {
      ignoreChestLocks = true;
    } else if (name.equals(IGNORE_AREAS)) {
      ignoreAreas = true;
    } else if (name.equals(FORWARD_UNKNOWN)) {
      forwardUnknownCommands = true;
    } else if (name.equals(SHOW_TITLE)) {
      showTitle = true;
    } else if (name.equals(COOLDOWN)) {
      cooldown = getInt(value);
    } else if (name.equals(WARMUP)) {
      warmup = getInt(value);
    }
  }

  @Override
  protected void saveAttributeElements(ContentHandler handler) throws SAXException {
    if (ignoreChestLocks) {
      saveAttributeElement(handler, IGNORE_CHESTS);
    }
    if (ignoreAreas) {
      saveAttributeElement(handler, IGNORE_AREAS);
    }
    if (showTitle) {
      saveAttributeElement(handler, SHOW_TITLE);
    }
    if (cooldown != 0) {
      saveAttributeElement(handler, COOLDOWN, Integer.toString(cooldown));
    }
    if (warmup != 0) {
      saveAttributeElement(handler, WARMUP, Integer.toString(warmup));
    }
  }

  @Override
  protected void saveAttributes(AttributesImpl attributes) {
    addAttribute(attributes, "id", Integer.toString(id));
  }
}
