package simpleserver.threads;

import java.util.Iterator;

import simpleserver.Player;
import simpleserver.PlayerFactory;
import simpleserver.Server;

public class PlayerScanner implements Runnable {
	Server parent;
	boolean timedOut;
	public PlayerScanner(Server server) {
		parent=server;
	}
	public void run() {
		while(true) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {}
			//synchronized(parent.players) {
			for (Iterator<Player> itr = PlayerFactory.iterator(); itr.hasNext();) {
				Player i = itr.next();
					timedOut = i.testTimeout();
					if (timedOut) {
						itr.remove();
						if (!i.isRobot())
							System.out.println("[SimpleServer] Disconnecting " + i.getIPAddress() + " due to inactivity.");
						try {
							//This is required to make it not try to call notifyClosed()
							i.isKicked(true);
							i.close();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {	}	
				}
			//}
		}
	}
	
}
