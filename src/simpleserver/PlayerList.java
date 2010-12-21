/*******************************************************************************
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
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
package simpleserver;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlayerList {
  private final ConcurrentMap<String, Player> players;

  public PlayerList() {
    players = new ConcurrentHashMap<String, Player>();
  }

  public synchronized Collection<Player> getArray() {
    return players.values();
  }

  public int size() {
    return players.size();
  }

  public Player findPlayer(int entityId) {
    for (Player p : players.values()) {
      if (p.getEntityId() == entityId) {
        return p;
      }
    }
    return null;
  }

  public Player findPlayer(String prefix) {
    prefix = prefix.toLowerCase();
    for (String name : players.keySet()) {
      if (name.startsWith(prefix)) {
        return players.get(name);
      }
    }
    return null;
  }

  public Player findPlayerExact(String name) {
    return players.get(name.toLowerCase());
  }

  public synchronized void removePlayer(Player player) {
    players.remove(player.getName().toLowerCase());
  }

  public synchronized void addPlayer(Player player) {
    players.put(player.getName().toLowerCase(), player);
  }
}
