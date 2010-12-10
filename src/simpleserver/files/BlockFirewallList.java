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


public class BlockFirewallList extends FileLoader {
	Server parent;
	static class BlockEntry {
		int id;
		int[] groups;
		public BlockEntry(int id, int[] groups) {
			this.id=id;
			this.groups=groups;
		}
	}
	LinkedList<BlockEntry> blocks = new LinkedList<BlockEntry>();
	@SuppressWarnings("unused")
	private BlockFirewallList(){}
	public BlockFirewallList(Server parent) {
		this.parent=parent;
		this.filename="block-list.txt";
	}
	
	
	public boolean checkCheck(int blockID) {
		for (BlockEntry i: blocks) {
			if (i.id==blockID) {
				return true;
			}
		}
		return false;
	}
	public boolean checkAllowed(Player p, int blockID) {
		for (BlockEntry i: blocks) {
			if (i.id==blockID) {
				return Group.contains(i.groups,p);
			}
		}
		return true;
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		blocks.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		String[] tokens = line.split(":");
		if (tokens.length>=2) 
			blocks.add(new BlockEntry(Integer.valueOf(tokens[0]),Group.parseGroups(tokens[1])));
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (BlockEntry i: blocks) {
        	line+=i.id + ":";
        	for(int group: i.groups) {
        		line+= group+",";
        	}
        	line+="\r\n";
        }
		return line;
	}
}
