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
package simpleserver.config;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class RobotList extends PropertiesConfig {
  private Semaphore robotLock = new Semaphore(1);
  private LinkedList<UnconfirmedRobot> potentialRobots = new LinkedList<UnconfirmedRobot>();
  private LinkedList<Integer> activeRobots = new LinkedList<Integer>();

  private static final class UnconfirmedRobot {
    private final String ipAddress;
    private int tries = 0;

    public UnconfirmedRobot(String ipAddress) {
      this.ipAddress = ipAddress;
    }
  }

  public RobotList() {
    super("robot-list.txt");
  }

  public void addRobotPort(int port) {
    try {
      robotLock.acquire();
    }
    catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    activeRobots.add(port);
    robotLock.release();
  }

  public void removeRobotPort(int port) {
    try {
      robotLock.acquire();
    }
    catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    activeRobots.remove((Integer) port);
    robotLock.release();
  }

  public Integer[] getRobotPorts() {
    try {
      robotLock.acquire();
    }
    catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Integer[] ports = new Integer[10];
    ports = activeRobots.toArray(ports);
    robotLock.release();
    return ports;
  }

  public boolean isRobot(String ip) {
    return properties.getProperty(ip) != null;
  }

  private boolean isPotentialRobot(String ip) {
    for (UnconfirmedRobot i : potentialRobots) {
      if (ip.equals(i.ipAddress)) {
        return true;
      }
    }
    return false;
  }

  public synchronized void addRobot(String ip) {
    if (!isRobot(ip)) {
      if (!isPotentialRobot(ip)) {
        potentialRobots.add(new UnconfirmedRobot(ip));
      }
      else {
        for (UnconfirmedRobot i : potentialRobots) {
          if (ip.equals(i.ipAddress)) {
            i.tries++;
            if (i.tries > 30) {
              properties.setProperty(ip, "");
              potentialRobots.remove(i);
              save();
            }
          }
        }
      }
    }
  }
}
