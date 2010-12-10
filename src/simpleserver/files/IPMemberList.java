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

import simpleserver.Player;
import simpleserver.Server;


public class IPMemberList extends FileLoader {
	static class Member {
		String ip;
		int group;
		public Member(String ip, int group) {
			this.ip=ip;
			this.group=group;
		}
	}
	LinkedList<Member> members = new LinkedList<Member>();
	Server parent;
	public IPMemberList(Server parent) {
		this.filename="ip-member-list.txt";
		this.parent=parent;
	}
	public int checkPlayer(Player p) {
		if (p!=null) {
			String ip = p.extsocket.getInetAddress().getHostAddress();
			for (Member i: members) {
				if (ip.startsWith(i.ip)) {
					return i.group;
				}
			}
		}
		return parent.options.defaultGroup;
	}
	public void setRank(Player p, int group) {
		if (p!=null) {
			String ip = p.extsocket.getInetAddress().getHostAddress();
			for (Member i: members) {
				if (i.ip.compareTo(ip.trim())==0) {
					i.group=group;
					save();
					return;
				}
			}
			members.add(new Member(ip.trim(),group));
			
			save();
		}
	}
	
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		members.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		String[] tokens = line.split("=");
		if (tokens.length>=2) 
			members.add(new Member(tokens[0],Integer.valueOf(tokens[1])));
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (Member i: members) {
        	line+=i.ip + "=" + i.group + "\r\n";
        }
		return line;
	}
}
