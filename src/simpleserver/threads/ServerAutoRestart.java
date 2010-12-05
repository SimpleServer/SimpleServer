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
