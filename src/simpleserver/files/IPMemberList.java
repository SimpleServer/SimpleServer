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
