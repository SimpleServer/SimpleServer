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
