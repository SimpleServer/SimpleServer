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

public class AllBlocks extends XMLTag {
  public Permission place;
  public Permission destroy;
  public Permission use;
  public Permission give;

  private static final String PLACE = "place";
  private static final String DESTROY = "destroy";
  private static final String USE = "use";
  private static final String GIVE = "give";

  public AllBlocks() {
    super("allblocks");
  }

  @Override
  void setAttribute(String name, String value) throws SAXException {
    if (name.equals(PLACE)) {
      place = new Permission(value);
    } else if (name.equals(DESTROY)) {
      destroy = new Permission(value);
    } else if (name.equals(USE)) {
      use = new Permission(value);
    } else if (name.equals(GIVE)) {
      give = new Permission(value);
    }
  }

  @Override
  void saveAttributes(AttributesImpl attributes) {
    if (place != null) {
      addAttribute(attributes, PLACE, place);
    }
    if (destroy != null) {
      addAttribute(attributes, DESTROY, destroy);
    }
    if (use != null) {
      addAttribute(attributes, USE, use);
    }
    if (give != null) {
      addAttribute(attributes, GIVE, give);
    }
  }
}
