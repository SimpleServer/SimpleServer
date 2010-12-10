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
package simpleserver.threads;

import java.util.Iterator;

import simpleserver.Player;
import simpleserver.PlayerFactory;
import simpleserver.Server;

public class PlayerScanner implements Runnable {
	Server parent;
	boolean timedOut;
	public PlayerScanner(Server server) {
		parent=server;
	}
	public void run() {
		while(true) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {}
			//synchronized(parent.players) {
			for (Iterator<Player> itr = PlayerFactory.iterator(); itr.hasNext();) {
				Player i = itr.next();
					timedOut = i.testTimeout();
					if (timedOut) {
						itr.remove();
						if (!i.isRobot())
							System.out.println("[SimpleServer] Disconnecting " + i.getIPAddress() + " due to inactivity.");
						try {
							//This is required to make it not try to call notifyClosed()
							i.isKicked(true);
							i.close();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {	}	
				}
			//}
		}
	}
	
}
