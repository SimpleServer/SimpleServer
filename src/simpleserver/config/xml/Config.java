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

import java.io.IOException;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class Config extends XMLTag {
  private static final char[] LINE_BREAK = new char[] { '\n' };

  public Config() {
    super("config");
  }

  protected void save(ContentHandler handler, XMLSerializer serializer) throws SAXException, IOException {
    handler.startElement("", "", tag, new AttributesImpl());
    saveChilds(handler, serializer);
    handler.endElement("", "", tag);
  }

  protected void saveChilds(ContentHandler handler, XMLSerializer serializer) throws SAXException, IOException {
    if (childs != null) {
      String tagname = null;
      for (XMLTag child : childs) {
        if (tagname != child.tag) {
          if (tagname != null) {
            handler.characters(LINE_BREAK, 0, LINE_BREAK.length);
          }
          serializer.comment(" " + child.tag + "s ");
        }
        child.save(handler);
        tagname = child.tag;
      }
    }
  }
}
