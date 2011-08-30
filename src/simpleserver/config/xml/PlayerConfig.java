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
import org.xml.sax.helpers.AttributesImpl;

public class PlayerConfig extends XMLTag {
  public String name;
  public int group;

  public PlayerConfig() {
    super("player");
  }

  public PlayerConfig(String name, int group) {
    this();
    this.name = name;
    this.group = group;
  }

  @Override
  protected void setAttribute(String name, String value) throws SAXException {
    if (name.equals("group")) {
      group = getInt(value);
    }
  }

  @Override
  protected void content(String content) {
    name = (name == null) ? content.toLowerCase() : name + content.toLowerCase();
  }

  @Override
  protected String saveContent() {
    return name;
  }

  @Override
  protected void saveAttributes(AttributesImpl attributes) {
    addAttribute(attributes, "group", Integer.toString(group));
  }
}
