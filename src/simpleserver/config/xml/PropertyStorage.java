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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xml.sax.SAXException;

public class PropertyStorage extends Storage implements Iterable<Property> {
  private Map<String, Property> properties = new HashMap<String, Property>();
  private PropertyStorage defaults;

  void add(Property property) {
    properties.put(property.name, property);
  }

  void setDefaults(PropertyStorage storage) {
    defaults = storage;
  }

  public void set(String name, String value) {
    if (contains(name)) {
      properties.get(name).value = value;
    } else {
      properties.put(name, new Property(name, value));
    }
  }

  public void set(String name, boolean value) {
    set(name, value ? "true" : "false");
  }

  public void remove(String name) {
    if (contains(name)) {
      properties.remove(name);
    }
  }

  public boolean contains(String name) {
    return properties.containsKey(name);
  }

  public boolean getBoolean(String name) {
    if (contains(name)) {
      String value = properties.get(name).value;
      if (value.equals("true")) {
        return true;
      } else if (value.equals("false")) {
        return false;
      }
    }
    if (defaults != null && defaults != this) {
      return defaults.getBoolean(name);
    } else {
      return false;
    }
  }

  public String get(String name) {
    return contains(name) ? properties.get(name).value : null;
  }

  public int getInt(String name) {
    try {
      return getIntValue(get(name));
    } catch (SAXException e) {
      if (defaults != null) {
        return defaults.getInt(name);
      } else {
        return 0;
      }
    }
  }

  @Override
  public Iterator<Property> iterator() {
    return properties.values().iterator();
  }

  @Override
  void add(XMLTag child) {
    add((Property) child);
  }
}
