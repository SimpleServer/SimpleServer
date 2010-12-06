package mcwrapper;

public class DelayClose implements Runnable{
	Player parent;
	public DelayClose(Player p) {
		parent=p;
	}
	
	public void run() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
		parent.close();
	}
	
}
