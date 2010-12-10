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

import java.io.IOException;
import java.io.InputStream;

import simpleserver.Server;
import simpleserver.files.ErrorLog;


public class C10TThread implements Runnable {
	Server parent;
	Process c10t;
	String command;
	InputStream stdout;
	InputStream stderr;
	class ErrGobblerThread extends Thread {
		public void run() {
			byte[] buf = new byte[256];
			try {
				while (stderr.read(buf)>=0) ;
			} catch (IOException e) {}
			
		}
	}
	class OutGobblerThread extends Thread {
		public void run() {
			byte[] buf = new byte[256];
			try {
				while (stdout.read(buf)>=0) ;
			} catch (IOException e) {}
			
		}
	}
	public C10TThread(Server parent, String command) {
		this.parent=parent;
		this.command=command;
	}


	public void run() {
		// TODO Auto-generated method stub
		try {
			while (true) {
				Thread.sleep(parent.options.c10tMins*1000*60);
				parent.saveLock.acquire();
				if (parent.requiresBackup()) {
					parent.runCommand("save-off");
					parent.runCommand("say Mapping Server!");
					try {
						c10t = Runtime.getRuntime().exec(command);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						parent.runCommand("say Mapping Complete!");
						parent.runCommand("save-on");
						e.printStackTrace();
						new Thread(new ErrorLog(e,"c10t Failure")).start();
						System.out.println("[SimpleServer] c10t Failed! Bad Command!");
						continue;
					}
					stderr = c10t.getErrorStream();
					stdout = c10t.getInputStream();
					new OutGobblerThread().start();
					new ErrGobblerThread().start();
					int exitCode = c10t.waitFor();
					if (exitCode<0) {
						System.out.println("[SimpleServer] c10t Failed! Exited with code " + exitCode + "!");
					}
					parent.runCommand("say Mapping Complete!");
					parent.runCommand("save-on");
				}
				parent.saveLock.release();
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
