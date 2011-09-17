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

class PlayerConfig extends XMLTag {
  String name;
  int group;

  PlayerConfig() {
    super("player");
  }

  PlayerConfig(String name, int group) {
    this();
    this.name = name;
    this.group = group;
  }

  @Override
  void setAttribute(String name, String value) throws SAXException {
    if (name.equals("group")) {
      group = getInt(value);
    } else if (name.equals("name")) {
      content(value);
    }
  }

  @Override
  void content(String content) {
    name = (name == null) ? content.toLowerCase() : name + content.toLowerCase();
  }

  @Override
  void saveAttributes(AttributeList attributes) {
    attributes.setValue("name", name);
    attributes.addAttribute("group", Integer.toString(group));
  }
}
