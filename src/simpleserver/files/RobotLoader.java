/*******************************************************************************
 * Copyright (C) 2010 Charles Wagner Jr..
 * spiegalpwns@gmail.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package simpleserver.files;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class RobotLoader extends FileLoader {
	Semaphore robotLock = new Semaphore(1);
	LinkedList<String> robots = new LinkedList<String>();
	LinkedList<UnconfirmedRobot> potentialRobots = new LinkedList<UnconfirmedRobot>();
	LinkedList<Integer> activeRobots = new LinkedList<Integer>();
	static class UnconfirmedRobot {
		String ipAddress;
		int tries=0;
		public UnconfirmedRobot(String ipAddress) {
			this.ipAddress=ipAddress;
		}
	}
	public RobotLoader() {
		this.filename="robot-list.txt";
	}
	public  void addRobotPort(int port) {
		try {
			robotLock.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		activeRobots.add((Integer)port);
		robotLock.release();
	}
	public  void removeRobotPort(int port) {
		try {
			robotLock.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		activeRobots.remove((Integer)port);
		robotLock.release();
	}
	public Integer[] getRobotPorts() {
		try {
			robotLock.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Integer[] ports = new Integer[10];
		ports = activeRobots.toArray(ports);
		robotLock.release();
		return ports;
	}
	public boolean isRobot(String ip) {
		for (String i: robots) {
			if (ip.equals(i)) {
				return true;
			}
		}
		return false;
	}
	public boolean isPotentialRobot(String ip) {
		for (UnconfirmedRobot i: potentialRobots) {
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
				for (UnconfirmedRobot i: potentialRobots) {
					if (ip.equals(i.ipAddress)) {
						i.tries++;
						if (i.tries>30) {
							robots.add(ip);
							potentialRobots.remove(i);
							save();
						}
					}
				}
			}
		}
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		robots.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		if (line!=null)
			if (line.length()>0)
				robots.add(line);
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (String i: robots) {
        	line+=i + "\r\n";
        }
		return line;
	}
}
