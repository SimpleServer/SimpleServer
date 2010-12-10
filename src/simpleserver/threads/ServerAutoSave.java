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


public class ServerAutoSave implements Runnable {
	Server parent;
	public ServerAutoSave(Server parent) {
		this.parent=parent;
	}


	public void run() {
		// TODO Auto-generated method stub
		try {
			while (true) {
				while(parent.options.autoSave) {
					Thread.sleep(parent.options.autoSaveMins*1000*60);
					parent.saveLock.acquire();
					if (parent.requiresBackup()) {
						//parent.runCommand("say Saving Map...");
						parent.sendToAll(parent.l.get("SAVING_MAP"));
						parent.isSaving(true);
						parent.runCommand("save-all");
					
					}
					while (parent.isSaving()) {
						try {
							Thread.sleep(20);
						}catch(Exception e) {
							break;
						}
					}
					parent.saveLock.release();
				}
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			if (!parent.isRestarting()) {
				e.printStackTrace();
				System.out.println("[WARNING] Automated Server Save Failure! Please run save-all and restart server!");
		
			}
		}
	}

}
