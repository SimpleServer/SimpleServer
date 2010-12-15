/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package simpleserver.config;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MuteList extends PropertiesConfig {
  public MuteList() {
    super("mute-list.txt");
  }

  public boolean isMuted(String name) {
    return getProperty(name.toLowerCase()) != null;
  }

  public void addName(String name) {
    if (setProperty(name.toLowerCase(), "") == null) {
      save();
    }
  }

  public boolean removeName(String name) {
    return setProperty(name.toLowerCase(), null) != null;
  }

  @Override
  public void load() {
    super.load();

    List<String> names = new LinkedList<String>();
    Set<Object> rawNames = keySet();

    for (Object name : rawNames) {
      rawNames.remove(name);
      names.add(((String) name).toLowerCase());
    }

    for (String name : names) {
      setProperty(name, "");
    }
  }
}
