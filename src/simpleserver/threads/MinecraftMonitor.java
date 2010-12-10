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

public class MinecraftMonitor extends Thread {
	Server server;
	public MinecraftMonitor(Server p) {
		server=p;
	}
	public void run() {
		try {
			server.p.waitFor();
			if (Thread.interrupted())
				return;
			System.out.println("[SimpleServer] Minecraft process stopped unexpectedly! Automatically restarting...");
			server.forceRestart();
		} catch (InterruptedException e) {
			//We are only interrupted if the server is restarting.
			return;
		}
	}
}
