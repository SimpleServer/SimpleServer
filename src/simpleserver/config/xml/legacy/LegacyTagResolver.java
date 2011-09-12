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
package simpleserver.config.xml.legacy;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import simpleserver.config.xml.Config;
import simpleserver.config.xml.PermissionContainer;

class LegacyTagResolver extends DefaultHandler {
  private Map<String, TagConverter> tags = new HashMap<String, TagConverter>();

  private Stack<PermissionContainer> stack = new Stack<PermissionContainer>();

  LegacyTagResolver() throws SAXException {
    loadTags();
  }

  @Override
  public void endElement(String uri, String name, String qName) throws SAXException {
    if (!tags.containsKey(qName)) {
      return;
    }
    getTag(qName).end(stack);
  }

  @Override
  public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
    if (!tags.containsKey(qName)) {
      return;
    }
    getTag(qName).convert(atts, stack);
  }

  private TagConverter getTag(String tagName) throws SAXException {
    if (!tags.containsKey(tagName)) {
      throw new SAXException(String.format("Found unknown tag \"%s\"", tagName));
    }

    return tags.get(tagName);
  }

  private void loadTags() throws SAXException {
    Reflections r = new Reflections("simpleserver", new SubTypesScanner());
    Set<Class<? extends TagConverter>> classes = r.getSubTypesOf(TagConverter.class);

    for (Class<? extends TagConverter> tag : classes) {
      if (Modifier.isAbstract(tag.getModifiers())) {
        continue;
      }

      TagConverter instance;
      try {
        instance = tag.getDeclaredConstructor().newInstance();
      } catch (Exception e) {
        throw new SAXException("Unable to load tag \"" + tag.getSimpleName() + "\" (after " + tags.size() + " tags)");
      }
      tags.put(instance.tag, instance);
    }
  }

  public Config config() {
    return (Config) stack.pop();
  }
}
