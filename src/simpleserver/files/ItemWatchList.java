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


public class ItemWatchList extends FileLoader {
	Server parent;
	static class ItemEntry {
		int id;
		int threshold;
		int[] groups;
		public ItemEntry(int id, int threshold, int[] groups) {
			this.id=id;
			this.threshold = threshold;
			this.groups=groups;
		}
	}
	LinkedList<ItemEntry> items = new LinkedList<ItemEntry>();
	
	@SuppressWarnings("unused")
	private ItemWatchList(){}
	
	public ItemWatchList(Server parent) {
		this.parent=parent;
		this.filename="item-watch-list.txt";
	}
	
	
	public boolean checkCheck(int blockID) {
		for (ItemEntry i: items) {
			if (i.id==blockID) {
				return true;
			}
		}
		return false;
	}
	public boolean checkAllowed(Player p, int blockID, int amt) {
		for (ItemEntry i: items) {
			if (i.id==blockID) {
				if (amt>=i.threshold)
					return Group.contains(i.groups,p);
			}
		}
		return true;
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		items.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		String[] tokens = line.split(":");
		if (tokens.length>=3) 
			items.add(new ItemEntry(Integer.valueOf(tokens[0]),Integer.valueOf(tokens[1]),Group.parseGroups(tokens[2])));
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (ItemEntry i: items) {
        	line+=i.id + ":" + i.threshold + ":";
        	for(int group: i.groups) {
        		line+= group+",";
        	}
        	line+="\r\n";
        }
		return line;
	}
}
