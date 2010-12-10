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


public class GroupList extends FileLoader {
	
	LinkedList<Group> groups = new LinkedList<Group>();
	Server parent;
	public GroupList(Server parent) {
		this.filename="group-list.txt";
		this.parent=parent;
	}
	public int checkGroupName(String name) {
		if (name!=null) {
			for (Group i: groups) {
				if (name.toLowerCase().trim().compareTo(i.getName().toLowerCase().trim())==0) {
					return i.getID();
				}
			}
		}
		return parent.options.defaultGroup;
	}
	public boolean groupExists(int group) {
		for(Group i: groups) {
			if (i.getID()==group)
				return true;
		}
		return false;
	}
	public Group getGroup(int id) {
		for (Group i: groups) {
			if (i.getID()==id) {
				return i;
			}
		}
		return null;
	}
	
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		groups.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		String[] tokens = line.split("=");
		if (tokens.length>=2) {
			String[] tokens2 = tokens[1].split(",");
			if (tokens2.length>=3) {
				groups.add(new Group(Integer.valueOf(tokens[0]),tokens2[0],Boolean.valueOf(tokens2[1]),Boolean.valueOf(tokens2[2]),tokens2[3]));
			}
		}
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (Group i: groups) {
        	line+=i.getID() + "=" + i.getName() + "," + i.showTitle() + "," + i.isAdmin() + "," + i.getColor() + "\r\n";
        }
		return line;
	}
}
