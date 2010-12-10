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
