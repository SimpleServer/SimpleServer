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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import simpleserver.config.xml.Config;

public class LegacyPermissionConfig {

  public static Config load() {
    File file = new File("simpleserver", "permissions.xml");

    if (!file.exists()) {
      return null;
    }

    System.out.println("[SimpleServer] Converting permisisons.xml to config.xml");

    XMLReader xml;
    try {
      xml = XMLReaderFactory.createXMLReader();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }

    LegacyTagResolver handler;
    try {
      handler = new LegacyTagResolver();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    xml.setContentHandler(handler);
    xml.setErrorHandler(handler);

    try {
      xml.setFeature("http://xml.org/sax/features/validation", false);
      xml.setEntityResolver(handler);
      xml.parse(new InputSource(new FileReader(file)));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    return handler.config();
  }
}
