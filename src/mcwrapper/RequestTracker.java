package mcwrapper;

import java.util.LinkedList;

public class RequestTracker implements Runnable {
	public static final int MAX_REQUESTS=30;
	public static final int CLEAR_SECONDS=60;
	class Request {
		String ipAddress;
		int requests;
		Request(String ipAddress) {
			this.ipAddress=ipAddress;
			this.requests=1;
		}
		public void addRequest() {
			requests++;
		}
	}
	Server parent;
	LinkedList<Request> requests = new LinkedList<Request>();
	public RequestTracker(Server p) {
		parent=p;
	}
	public synchronized void addRequest(String ipAddress) {
		for (Request i: requests) {
			if (i.ipAddress.equals(ipAddress)) {
				i.addRequest();
				if (i.requests>MAX_REQUESTS) {
					parent.banKickIP(ipAddress);
				}
				return;
			}
		}
		requests.add(new Request(ipAddress));
	}
	public void run() {
		while(true) {
			try {
				Thread.sleep(CLEAR_SECONDS*1000);
			} catch (InterruptedException e) {
			}
			requests.clear();
			
		}
	}
}
