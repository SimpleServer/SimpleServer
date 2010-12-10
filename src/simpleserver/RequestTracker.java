/*******************************************************************************
 * Copyright (C) 2010 Charles Wagner Jr..
 * spiegalpwns@gmail.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package simpleserver;

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
	public synchronized void addRequest(String ipAddress) throws InterruptedException {
		for (Request i: requests) {
			if (i.ipAddress.equals(ipAddress)) {
				i.addRequest();
				if (i.requests>MAX_REQUESTS) {
					parent.adminLog.addMessage("RequestTracker banned " + i.ipAddress + ":\t Too many requests!");
					parent.banKickIP(ipAddress,"Banned: Too many requests!");
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
