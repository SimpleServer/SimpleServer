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
