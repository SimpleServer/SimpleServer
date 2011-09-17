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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

abstract class XMLTag implements Cloneable {
  final String tag;
  protected Collection<XMLTag> childs;
  private Set<String> acceptedAttributes;

  XMLTag(String tag) {
    this.tag = tag;
  }

  // hooks
  void init() {
  }

  void loadedAttributes() {
  }

  void finish() throws SAXException {
  }

  // load data
  void setAttribute(String name, String value) throws SAXException {
  }

  void content(String content) {
  }

  // save data
  void saveAttributes(AttributeList attributes) {
  }

  // setup
  boolean acceptChildAttribute(String name) {
    return acceptedAttributes != null && acceptedAttributes.contains(name.toLowerCase());
  }

  void acceptAttribute(String name) {
    if (acceptedAttributes == null) {
      acceptedAttributes = new HashSet<String>();
    }
    acceptedAttributes.add(name.toLowerCase());
  }

  public void fullInit() throws SAXException {
    init();
    loadedAttributes();
    finish();
  }

  // helper
  static int getInt(String value) throws SAXException {
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException e) {
      throw new SAXException("Not a valid number: " + value);
    }
  }

  // internal methods
  XMLTag callInit() {
    init();
    return this;
  }

  void clean() {
    acceptedAttributes = null;
  }

  void setAttributes(Attributes attributes) throws SAXException {
    for (int i = 0; i < attributes.getLength(); i++) {
      setAttribute(attributes.getLocalName(i), attributes.getValue(i));
    }
    loadedAttributes();
  }

  void addChild(XMLTag child) throws SAXException {
    if (childs == null) {
      childs = new LinkedList<XMLTag>();
    }
    childs.add(child);
  }

  protected void save(ContentHandler handler, boolean childs) throws SAXException {
    AttributeList attributes = new AttributeList(childs);
    saveAttributes(attributes);
    handler.startElement("", "", tag, attributes.inlineAttributes());
    for (AttributeList.Attribute attribute : attributes.childAttributes()) {
      saveAttributeElement(handler, attribute.name, attribute.value);
    }
    saveChilds(handler, childs);
    String content = saveContent();
    if (content != null) {
      handler.characters(content.toCharArray(), 0, content.length());
    }
    handler.endElement("", "", tag);
  }

  String saveContent() {
    return null;
  }

  void saveChilds(ContentHandler handler, boolean childAttributes) throws SAXException {
    if (childs != null) {
      for (XMLTag child : childs) {
        child.save(handler, childAttributes);
      }
    }
  }

  private void saveAttributeElement(ContentHandler handler, String name, String content) throws SAXException {
    handler.startElement("", "", name, new AttributesImpl());
    if (content != null) {
      handler.characters(content.toCharArray(), 0, content.length());
    }
    handler.endElement("", "", name);
  }

  @Override
  public XMLTag clone() throws CloneNotSupportedException {
    return ((XMLTag) super.clone()).callInit();
  }

  static class AttributeList {
    private AttributesImpl attributes = new AttributesImpl();
    private List<Attribute> childs = new LinkedList<Attribute>();
    private boolean childAttributes;

    public AttributeList(boolean childAttributes) {
      this.childAttributes = childAttributes;
    }

    public void addAttribute(String name, String value) {
      if (attributes.getIndex(name) >= 0) {
        value = attributes.getValue(name) + "," + value;
        attributes.removeAttribute(attributes.getIndex(name));
      }
      attributes.addAttribute("", "", name, "CDATA", value == null ? "true" : value);
    }

    public void addAttribute(String name) {
      addAttribute(name, null);
    }

    public void addAttribute(String name, Object object) {
      addAttribute(name, object.toString());
    }

    public void addAttribute(String name, int number) {
      addAttribute(name, Integer.toString(number));
    }

    public void addAttributeElement(String name, String value) {
      if (childAttributes) {
        childs.add(new Attribute(name, value));
      } else {
        addAttribute(name, value);
      }
    }

    public void addAttributeElement(String name, int number) {
      addAttributeElement(name, Integer.toString(number));
    }

    public void addAttributeElement(String name) {
      addAttributeElement(name, null);
    }

    public Attributes inlineAttributes() {
      return attributes;
    }

    public List<Attribute> childAttributes() {
      return childs;
    }

    static class Attribute {
      public String name;
      public String value;

      Attribute(String name, String value) {
        this.name = name;
        this.value = value;
      }
    }
  }
}
