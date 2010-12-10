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

import simpleserver.Server;


public class MemberList extends FileLoader {
	static class Member {
		String name;
		int group;
		public Member(String name, int group) {
			this.name=name;
			this.group=group;
		}
	}
	LinkedList<Member> members = new LinkedList<Member>();
	Server parent;
	public MemberList(Server parent) {
		this.filename="member-list.txt";
		this.parent=parent;
	}
	public int checkName(String name) {
		if (name!=null) {
			for (Member i: members) {
				if (name.toLowerCase().trim().compareTo(i.name.toLowerCase().trim())==0) {
					return i.group;
				}
			}
		}
		return parent.options.defaultGroup;
	}
	public void setGroup(String name, int group) throws InterruptedException {
		if (group>0&&!parent.groups.groupExists(group))
			return;
		for (Member i: members) {
			if (name.toLowerCase().trim().compareTo(i.name.toLowerCase().trim())==0) {
				
				i.group=group;
				parent.updateGroup(name);
				save();
				return;
			}
		}
		members.add(new Member(name,group));
		parent.updateGroup(name);
		save();
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
        	line+=i.name + "=" + i.group + "\r\n";
        }
		return line;
	}
}
