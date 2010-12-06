package mcwrapper;

public class PlayerScanner implements Runnable {
	Server parent;
	public PlayerScanner(Server server) {
		parent=server;
	}
	public void run() {
		while(true) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {}
			for(Player i: parent.players) {
				i.testTimeout();
			}
		}
	}
	
}
