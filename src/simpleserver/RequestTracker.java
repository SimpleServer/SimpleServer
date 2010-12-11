/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
