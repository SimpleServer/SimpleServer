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

public class Property extends XMLTag {
  public String name;
  public String value;

  public Property() {
    super("property");
    acceptAttribute("true");
    acceptAttribute("false");
  }

  @Override
  protected void setAttribute(String name, String value) {
    if (name.equals("name")) {
      this.name = value;
    } else if (name.equals("true") || name.equals("false")) {
      this.value = name;
    }
  }

  @Override
  protected void content(String content) {
    value = (value == null) ? content : value + content;
  }

  @Override
  protected void finish() {
    String lowvalue = value.toLowerCase();
    if (lowvalue.equals("true") || lowvalue.equals("1") || lowvalue.equals("yes") ||
          lowvalue.equals("on")) {
      value = "true";
    } else if (lowvalue.equals("false") || lowvalue.equals("not") || lowvalue.equals("0") ||
          lowvalue.equals("no") || lowvalue.equals("off")) {
      value = "false";
    }
  }

  @Override
  protected String saveContent() {
    return (isBoolean()) ? null : value;
  }

  @Override
  protected void saveAttributeElements(ContentHandler handler) throws SAXException {
    if (isBoolean()) {
      saveAttributeElement(handler, value);
    }
  }

  private boolean isBoolean() {
    return value.equals("true") | value.equals("false");
  }

  @Override
  protected void saveAttributes(AttributesImpl attributes) {
    addAttribute(attributes, "name", name);
  }
}
