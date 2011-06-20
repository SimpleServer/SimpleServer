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
package simpleserver.bot;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import simpleserver.Coordinate;
import simpleserver.Player;

public class Teleporter extends Bot {

  private Timer timer = new Timer();
  private Coordinate coordinate;
  private Player player;
  private PlayerFile dat;

  public Teleporter(Player player, Coordinate coordinate) {
    super(player.getServer(), "Teleporter" + Math.round(100000 * Math.random()));
    this.coordinate = coordinate;
    this.player = player;
    prepare();
  }

  @Override
  protected void prepare() {
    dat = new PlayerFile(name, server);
    dat.setPosition(coordinate);
    dat.save();
  }

  @Override
  protected void ready() throws IOException {
    super.ready();
    timer.schedule(new LookAround(), 0, 500);
    server.runCommand("tp", player.getName() + " " + name);
    timer.schedule(new Logout(), 3000);
  }

  @Override
  protected void handlePacket(byte packetId) throws IOException {
    switch (packetId) {
      default:
        super.handlePacket(packetId);
    }
  }

  @Override
  protected void die() {
    timer.cancel();
    super.die();
    dat.unlink();
  }

  private final class LookAround extends TimerTask {
    private int t = 0;

    @Override
    public void run() {
      writeLock.lock();
      try {
        out.writeByte(0x0c);
        out.writeFloat(40 * t++);
        out.writeFloat((float) (Math.sin(t / 10) * 45));
        out.writeBoolean(true);
      } catch (IOException e) {
        error("LookAround failed");
      } finally {
        writeLock.unlock();
      }

    }
  }

  private final class Logout extends TimerTask {
    @Override
    public void run() {
      writeLock.lock();
      try {
        logout();
      } catch (IOException e) {
        error("Logout failed");
      } finally {
        writeLock.unlock();
      }

    }
  }

}
