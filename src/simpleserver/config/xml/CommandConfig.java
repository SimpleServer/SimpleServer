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

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class CommandConfig extends StorageContainer implements Comparable<CommandConfig> {
  public String name;
  public String originalName;
  public List<String> aliases;
  public Forwarding forwarding = Forwarding.NONE;
  public Permission allow;
  public boolean hidden;

  private ArgumentStorage arguments;

  private static final String NAME = "name";
  private static final String ALLOW = "allow";
  private static final String RENAME = "rename";
  private static final String ALIAS = "alias";
  private static final String FORWARD = "forward";
  private static final String FORWARD_ONLY = "forwardonly";
  private static final String HIDDEN = "hidden";

  CommandConfig() {
    super("command");
    acceptAttribute(RENAME);
    acceptAttribute(ALIAS);
    acceptAttribute(FORWARD);
    acceptAttribute(FORWARD_ONLY);
    acceptAttribute(HIDDEN);
  }

  public CommandConfig(String name) {
    this();
    this.name = originalName = name.toLowerCase();
  }

  @Override
  void addStorages() {
    addStorage("argument", arguments = new ArgumentStorage());
  }

  @Override
  public void finish() {
    if (allow == null) {
      allow = new Permission();
    }
  }

  @Override
  void setAttribute(String name, String value) throws SAXException {
    if (name.equals(NAME)) {
      this.name = originalName = value.toLowerCase();
    } else if (name.equals(ALLOW)) {
      allow = new Permission(value);
    } else if (name.equals(FORWARD)) {
      forwarding = Forwarding.BOTH;
    } else if (name.equals(FORWARD_ONLY)) {
      forwarding = Forwarding.ONLY;
    } else if (name.equals(HIDDEN)) {
      hidden = true;
    } else if (name.equals(ALIAS)) {
      if (aliases == null) {
        aliases = new ArrayList<String>();
      }
      aliases.add(value.toLowerCase());
    } else if (name.equals(RENAME)) {
      this.name = value;
    }
  }

  @Override
  void saveAttributeElements(ContentHandler handler) throws SAXException {
    if (hidden) {
      saveAttributeElement(handler, HIDDEN);
    }
    if (forwarding == Forwarding.BOTH) {
      saveAttributeElement(handler, FORWARD);
    } else if (forwarding == Forwarding.ONLY) {
      saveAttributeElement(handler, FORWARD_ONLY);
    }
    if (!name.equals(originalName)) {
      saveAttributeElement(handler, RENAME, name);
    }
    if (aliases != null) {
      for (String alias : aliases) {
        saveAttributeElement(handler, ALIAS, alias);
      }
    }
  }

  @Override
  void saveAttributes(AttributesImpl attributes) {
    addAttribute(attributes, NAME, originalName);
    addAttribute(attributes, ALLOW, allow);
  }

  public Permission allow(String args) {
    Permission perm = arguments.permission(args);
    return perm == null ? allow : perm;
  }

  public boolean alias(String alias) {
    return aliases != null && aliases.contains(alias);
  }

  public int compareTo(CommandConfig command) {
    return originalName.compareTo(command.originalName);
  }

  public static enum Forwarding {
    NONE,
    BOTH,
    ONLY;
  }
}
