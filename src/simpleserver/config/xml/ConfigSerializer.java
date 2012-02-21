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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

@SuppressWarnings("deprecation")
public class ConfigSerializer extends XMLSerializer {
  private String lastElement;
  private boolean first;
  private static final Map<String, String> captions = new HashMap<String, String>();

  static {
    captions.put("property", "Properties");
    captions.put("ip", "IPs");
    captions.put("player", "Players");
    captions.put("group", "Groups");
    captions.put("command", "Commands");
    captions.put("allblocks", "Blocks");
    captions.put("block", "Blocks");
    captions.put("dimension", "Dimensions");
    captions.put("area", "Areas");
    captions.put("event", "Events");
  }

  public ConfigSerializer(FileOutputStream fos, OutputFormat of) {
    super(fos, of);
  }

  @Override
  public void endElementIO(String namespaceURI, String localName, String rawName) throws IOException {
    super.endElementIO(namespaceURI, localName, rawName);
    lastElement = rawName;
    first = false;
  }

  @Override
  public void startElement(String namespaceURI, String localName, String rawName, Attributes attrs) throws SAXException {
    if (!rawName.equals(lastElement) && captions.containsKey(rawName) && !first) {
      try {
        _printer.breakLine();
        _printer.breakLine();
        _printer.printText("<!-- " + captions.get(rawName) + " -->");
      } catch (IOException e) {
        throw new SAXException(e.getMessage());
      }
    }
    super.startElement(namespaceURI, localName, rawName, attrs);
    first = true;
  }
}
