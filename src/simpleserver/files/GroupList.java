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
