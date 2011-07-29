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
package simpleserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import simpleserver.nbt.PlayerFile;

public class JailRelease {
  private HashMap<String, Timer> timers = new HashMap<String, Timer>();

  public void addRelease(Player player, long afterMillis) {
    String playerName = player.getName();

    if (timers.containsKey(playerName)) {
      timers.get(playerName).cancel();
    }

    Timer timer = new Timer("UnjailTimer-" + playerName, true);
    TimerTask task = new Releaser(player.getServer(), playerName);

    timers.put(playerName, timer);
    timer.schedule(task, afterMillis);
  }

  private class Releaser extends TimerTask {
    private final Server server;
    private final String playerName;

    Releaser(Server server, String playerName) {
      this.server = server;
      this.playerName = playerName;
    }

    @Override
    public void run() {
      Player target = server.findPlayer(playerName);

      if (target != null) {
        if (target.getIsJailed()) {
          target.unjail();
        }
      } else {
        server.data.players.jail.get(playerName).unjail();
        PlayerFile dat = new PlayerFile(playerName, server);
        dat.setPosition(server.world.spawnPoint());
        try {
          dat.save();
        } catch (IOException e) {
          System.out.println("[SimpleServer] " + e.getMessage());
          System.out.println("[SimpleServer] Could not teleport " + playerName + " out of jail!");
        }
      }
    }
  }
}
