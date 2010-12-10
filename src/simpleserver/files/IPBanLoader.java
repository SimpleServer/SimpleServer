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
package simpleserver.files;

import java.util.LinkedList;

public class IPBanLoader extends FileLoader {
	LinkedList<String> bans = new LinkedList<String>();
	public IPBanLoader() {
		this.filename="ip-ban-list.txt";
	}
	public void addBan(String ipAddress) {
		bans.add(ipAddress);
		save();
	}
	public boolean removeBan(String ipAddress) {
		for (String i: bans) {
			if (i.compareTo(ipAddress)==0) {
				bans.remove(i);
				save();
				return true;
			}
		}
		return false;
	}
	public boolean isBanned(String ipAddress) {
		for (String i: bans) {
			if (ipAddress.startsWith(i) && i.length()!=0) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		bans.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		if (line!=null && line!="") 
			bans.add(line);
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (String i: bans) {
        	line+=i + "\r\n";
        }
		return line;
	}
}
