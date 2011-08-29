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
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLTagResolver extends DefaultHandler {
  private HashMap<String, XMLTag> tags = new HashMap<String, XMLTag>();
  private Stack<XMLTag> stack = new Stack<XMLTag>();
  private Config root;

  public XMLTagResolver() throws SAXException {
    loadTags();
  }

  public Config root() {
    return root;
  }

  @Override
  public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
    XMLTag tag;
    try {
      tag = getTag(qName);
    } catch (SAXException e) {
      if (root != null && stack.peek().acceptChildAttribute(qName)) {
        tag = new AttributeElement(qName);
        stack.push(tag);
        return;
      } else {
        throw e;
      }
    }
    if (root == null) {
      root = (Config) tag;
    } else {
      stack.peek().addChild(tag);
    }
    stack.push(tag);
    tag.setAttributes(atts);
  }

  @Override
  public void endElement(String uri, String name, String qName) throws SAXException {
    XMLTag tag = stack.pop();
    tag.finish();
    if (tag instanceof AttributeElement) {
      stack.peek().setAttribute(((AttributeElement) tag).name, ((AttributeElement) tag).value);
    }
  }

  @Override
  public void characters(char[] chars, int start, int length) throws SAXException {
    stack.peek().content(new String(chars, start, length));
  }

  @Override
  public InputSource resolveEntity(String name, String publicId) throws SAXException, IOException {
    return new InputSource(getClass().getResourceAsStream("config.dtd"));
  }

  @Override
  public void warning(SAXParseException exception) throws SAXException {
    System.out.println("warning:");
    exception.printStackTrace();
  }

  @Override
  public void error(SAXParseException exception) throws SAXException {
    throw exception;
  }

  private XMLTag getTag(String tagName) throws SAXException {
    if (!tags.containsKey(tagName)) {
      throw new SAXException(String.format("Found unknown tag \"%s\"", tagName));
    }
    try {
      return tags.get(tagName).clone();
    } catch (CloneNotSupportedException e) {
      throw new SAXException(String.format("Unabled to load tag \"%s\"", tagName));
    }
  }

  private void loadTags() throws SAXException {
    Reflections r = new Reflections("simpleserver", new SubTypesScanner());
    Set<Class<? extends XMLTag>> classes = r.getSubTypesOf(XMLTag.class);

    for (Class<? extends XMLTag> tag : classes) {
      if (Modifier.isAbstract(tag.getModifiers())) {
        continue;
      }

      XMLTag instance;
      try {
        instance = tag.getConstructor().newInstance();
      } catch (Exception e) {
        throw new SAXException("Unable to load tags (" + tag.getSimpleName() + ")");
      }
      tags.put(instance.tag, instance);
    }
  }

  private static class AttributeElement extends XMLTag {
    public String name;
    public String value;

    public AttributeElement() {
      super(null);
    }

    public AttributeElement(String name) {
      this();
      this.name = name.toLowerCase();
    }

    @Override
    protected void content(String content) {
      if (value == null) {
        value = content;
      } else {
        value += content;
      }
    }
  }
}
