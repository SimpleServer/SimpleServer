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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IpStorage implements Storage {
  private Map<InetAddress, Ip> ips = new HashMap<InetAddress, Ip>();

  public void add(Ip ip) {
    ips.put(ip.address, ip);
  }

  public void setGroup(InetAddress ip, int group) {
    if (contains(ip)) {
      get(ip).group = group;
    } else {
      ips.put(ip, new Ip(ip, group));
    }
  }

  public void remove(InetAddress ip) {
    if (contains(ip)) {
      ips.remove(ip);
    }
  }

  public boolean contains(InetAddress ip) {
    return ips.containsKey(ip);
  }

  public Ip get(InetAddress ip) {
    return contains(ip) ? ips.get(ip) : null;
  }

  public Iterator<Ip> iterator() {
    return ips.values().iterator();
  }

  public void add(XMLTag child) {
    add((Ip) child);
  }
}
