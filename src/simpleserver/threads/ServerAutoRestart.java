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

import simpleserver.Server;


public class ServerAutoRestart implements Runnable {
	Server parent;
	public ServerAutoRestart(Server parent) {
		this.parent=parent;
	}

	public void run() {
		// TODO Auto-generated method stub
		try {
			while (true) {
				while(parent.options.autoRestart) {
					Thread.sleep(parent.options.autoRestartMins*1000*60);
					parent.saveLock.acquire();
					//parent.runCommand("say Server is restarting in 60 seconds!");
					parent.sendToAll(parent.l.get("SERVER_RESTART_60"));
					Thread.sleep(30*1000);
					//parent.runCommand("say Server is restarting in 30 seconds!");
					parent.sendToAll(parent.l.get("SERVER_RESTART_30"));
					Thread.sleep(27*1000);
					
					//parent.runCommand("say Server is restarting in 3 seconds!");
					parent.sendToAll(parent.l.get("SERVER_RESTART_3"));
					Thread.sleep(3*1000);

					if (!parent.isRestarting())
						parent.restart();
					parent.saveLock.release();
				}
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			parent.saveLock.release();
			// TODO Auto-generated catch block
			if (!parent.isRestarting()) {
				e.printStackTrace();
				System.out.println("[WARNING] Automated Server Restart Failure! Server will no longer automatically restart until SimpleServer is manually restarted!");
		
			}
		}
	}

}
