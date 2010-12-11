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
