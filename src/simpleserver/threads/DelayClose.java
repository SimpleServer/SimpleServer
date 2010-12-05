package simpleserver.threads;

import simpleserver.Player;

public class DelayClose implements Runnable{
	Player parent;
	public DelayClose(Player p) {
		parent=p;
	}
	
	public void run() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		try {
			parent.close();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		parent=null;
	}
	
}
