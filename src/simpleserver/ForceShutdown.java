package simpleserver;

import java.util.concurrent.TimeUnit;

public class ForceShutdown implements Runnable {
	Server server;
	public ForceShutdown(Server s) {
		server=s;
	}
	public void run() {
		try {
			server.saveLock.tryAcquire(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		server.stop();
	}
	
}
