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

import simpleserver.Server;

public class Herobrine extends Bot {

  private Timer timer = new Timer();

  public Herobrine(Server server) {
    super(server, "Herobrine");
  }

  @Override
  protected void ready() throws IOException {
    super.ready();
    timer.schedule(new LookAround(), 0, 100);
  }

  @Override
  protected void handlePacket(byte packetId) throws IOException {
    switch (packetId) {
      case 0x3:
        if (readUTF16().contains("quit")) {
          logout();
        }
        break;
      default:
        super.handlePacket(packetId);
    }
  }

  @Override
  protected void die() {
    timer.cancel();
    super.die();
  }

  private final class LookAround extends TimerTask {
    private float t = 0;
    private float vt = 0;

    @Override
    public void run() {
      try {
        position.updateLook(t, 0);
        walk(0.1);
        vt += Math.random() - 0.5;
        t += vt;
        if (vt > 3 || vt < -3) {
          vt = 0;
        }
        sendPosition();
      } catch (IOException e) {
        error("LookAround failed");
      }
    }
  }
}
