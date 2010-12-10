/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
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
