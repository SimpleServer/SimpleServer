package simpleserver;

public class ForceRestart implements Runnable {
	Server parent;
	public ForceRestart(Server s) {
		parent=s;
	}
	public void run() {
		try {
			parent.saveLock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		parent.restart();			
		parent.saveLock.release();
	}
}
