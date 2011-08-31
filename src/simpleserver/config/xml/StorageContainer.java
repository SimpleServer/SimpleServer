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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

abstract class StorageContainer extends XMLTag {
  private Map<String, Storage> storages = new LinkedHashMap<String, Storage>();

  StorageContainer(String tag) {
    super(tag);
  }

  void addStorage(String tag, Storage storage) {
    storages.put(tag, storage);
  }

  @Override
  void finish() {
    for (Storage storage : storages.values()) {
      storage.finish();
    }
  }

  @Override
  void addChild(XMLTag child) throws SAXException {
    if (storages.containsKey(child.tag)) {
      storages.get(child.tag).add(child);
    } else {
      super.addChild(child);
    }
  }

  @Override
  void saveChilds(ContentHandler handler) throws SAXException {
    for (Storage storage : storages.values()) {
      Iterator<? extends XMLTag> it = storage.iterator();
      while (it.hasNext()) {
        it.next().save(handler);
      }
    }
    super.saveChilds(handler);
  }
}
