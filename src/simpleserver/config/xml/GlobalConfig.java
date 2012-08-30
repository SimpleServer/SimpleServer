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

import static simpleserver.util.Util.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import simpleserver.config.AbstractConfig;
import simpleserver.config.xml.legacy.LegacyPermissionConfig;
import simpleserver.options.Options;
import simpleserver.util.UnicodeReader;

@SuppressWarnings("deprecation")
public class GlobalConfig extends AbstractConfig {
  public Config config;
  public boolean loadsuccess;
  private static final Lock lock = new ReentrantLock();

  private Options options;
  private Config defaults;

  public GlobalConfig(Options options) {
    super("config.xml");
    this.options = options;

    try {
      defaults = loadDefaults();
    } catch (Exception e) {
      print("Error while loading default config.xml");
      e.printStackTrace();
      return;
    }
  }

  @Override
  public void load() {
    loadsuccess = false;

    if (defaults == null) {
      return;
    }

    lock.lock();

    if (!getFile().exists()) {
      Config config;
      if ((config = LegacyPermissionConfig.load()) != null) {
        this.config = config;
        completeConfig(config, defaults);
        print("Converted permisisons.xml to config.xml");
      } else {
        this.config = defaults;
        print("Loaded default config.xml");
      }
    } else {
      try {
        config = load(new FileInputStream(getFile()));
      } catch (Exception e) {
        print("Error in config.xml: " + e);
        e.printStackTrace();
        lock.unlock();
        return;
      }
      completeConfig(config, defaults);
    }

    config.properties.setDefaults(defaults.properties);
    loadsuccess = true;
    lock.unlock();
  }

  private Config loadDefaults() throws SAXException, IOException {
    return load(getClass().getResourceAsStream(filename));
  }

  private Config load(InputStream stream) throws SAXException, IOException {
    XMLReader xml = XMLReaderFactory.createXMLReader();
    XMLTagResolver handler = new XMLTagResolver();
    xml.setContentHandler(handler);
    xml.setErrorHandler(handler);
    xml.setFeature("http://xml.org/sax/features/validation", true);
    xml.setEntityResolver(handler);
    xml.parse(new InputSource(new UnicodeReader(stream)));

    return handler.root();
  }

  private void completeConfig(Config config, Config defaults) {
    String[] fallbackIfEmpty = new String[] { "msgFormat", "msgTitleFormat", "msgForwardFormat", "logMessageFormat" };

    // Properties
    for (Property prop : defaults.properties) {
      if (!config.properties.contains(prop.name)) {
        if (options.contains(prop.name)) {
          config.properties.set(prop.name, options.get(prop.name));
        } else {
          config.properties.add(prop);
        }
      }
      options.remove(prop.name);
    }

    for (String prop : fallbackIfEmpty) {
      if (config.properties.get(prop).equals("")) {
        config.properties.set(prop, defaults.properties.get(prop));
      }
    }

    // Commands
    Set<String> commands = new HashSet<String>();
    for (CommandConfig cmd : config.commands) {
      commands.add(cmd.originalName);
    }
    for (CommandConfig cmd : defaults.commands) {
      if (!commands.contains(cmd.originalName)) {
        config.commands.add(cmd);
      }
    }
  }

  @Override
  public void save() {
    lock.lock();
    FileOutputStream fos;
    try {
      fos = new FileOutputStream(getFile());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      lock.unlock();
      return;
    }
    OutputFormat of = new OutputFormat("XML", "UTF-8", true);
    of.setIndent(1);
    of.setIndenting(true);
    of.setLineWidth(200);
    of.setDoctype(null, "http://simpleserver.ceilingcat.ch/resources/config.2.dtd");
    XMLSerializer serializer = new ConfigSerializer(fos, of);
    ContentHandler hd;
    try {
      hd = serializer.asContentHandler();

      hd.startDocument();
      config.save(hd, serializer);
      hd.endDocument();
      fos.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected void loadHeader() {
  }
}
