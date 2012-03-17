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

import org.xml.sax.SAXException;

public class CommandConfig extends StorageContainer implements Comparable<CommandConfig> {
  public String name;
  public String originalName;
  public List<String> aliases;
  public Forwarding forwarding = Forwarding.NONE;
  public Permission allow;
  public boolean hidden;
  public boolean disabled = false;
  public String event;

  private ArgumentStorage arguments;

  private static final String NAME = "name";
  private static final String ALLOW = "allow";
  private static final String RENAME = "rename";
  private static final String ALIAS = "alias";
  private static final String FORWARD = "forward";
  private static final String FORWARD_ONLY = "forwardonly";
  private static final String HIDDEN = "hidden";
  private static final String DISABLED = "disabled";
  private static final String EVENT = "event";

  CommandConfig() {
    super("command");
    acceptAttribute(RENAME);
    acceptAttribute(ALIAS);
    acceptAttribute(FORWARD);
    acceptAttribute(FORWARD_ONLY);
    acceptAttribute(HIDDEN);
    acceptAttribute(DISABLED);
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
      if (value == null || value.equals("true")) {
        forwarding = Forwarding.BOTH;
      } else if (value.equals("only")) {
        forwarding = Forwarding.ONLY;
      }
    } else if (name.equals(FORWARD_ONLY) && (value == null || !value.equals("false"))) {
      forwarding = Forwarding.ONLY;
    } else if (name.equals(HIDDEN) && (value == null || !value.equals("false"))) {
      hidden = true;
    } else if (name.equals(ALIAS) && value != null) {
      if (aliases == null) {
        aliases = new ArrayList<String>();
      }
      for (String alias : value.split(",")) {
        if (alias.trim().length() > 0) {
          aliases.add(alias.trim());
        }
      }
    } else if (name.equals(RENAME)) {
      this.name = value;
    } else if (name.equals(DISABLED) && (value == null || !value.equals("false"))) {
      disabled = true;
    } else if (name.equals(EVENT)) {
      event = value;
    }
  }

  @Override
  void saveAttributes(AttributeList attributes) {
    attributes.addAttribute(NAME, originalName);
    attributes.addAttribute(ALLOW, allow);
    if (hidden) {
      attributes.addAttributeElement(HIDDEN);
    }
    if (disabled) {
      attributes.addAttributeElement(DISABLED);
    }
    if (forwarding == Forwarding.BOTH) {
      attributes.addAttributeElement(FORWARD);
    } else if (forwarding == Forwarding.ONLY) {
      attributes.addAttributeElement(FORWARD_ONLY);
    }
    if (!name.equals(originalName)) {
      attributes.addAttributeElement(RENAME, name);
    }
    if (aliases != null) {
      for (String alias : aliases) {
        attributes.addAttributeElement(ALIAS, alias);
      }
    }
    if (event != null) {
      attributes.addAttribute(EVENT, event);
    }
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
