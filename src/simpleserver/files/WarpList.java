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


public class WarpList extends FileLoader {
	Server parent;
	static class Warp {
		String name;
		double x,y,z,stance;
		public Warp(String name, double x, double y, double z, double stance) {
			this.name = name.toLowerCase().trim();
			this.x=x;
			this.y=y;
			this.z=z;
			this.stance=stance;
		}
	}
	LinkedList<Warp> warps = new LinkedList<Warp>();
	@SuppressWarnings("unused")
	private WarpList() {}
	public WarpList(Server parent) {
		this.parent=parent;
		this.filename="warp-list.txt";
	}
	public boolean warpExists(String name) {
		for(Warp i: warps) {
			if (i.name.compareTo(name.toLowerCase().trim())==0)
				return true;
		}
		return false;
	}
	public double[] getWarp(String name) {
		for(Warp i: warps) {
			if (i.name.compareTo(name.toLowerCase().trim())==0)
				return new double[]{i.x,i.y,i.z,i.stance};
		}
		return null;
	}
	public boolean makeWarp(String name, double x, double y, double z, double stance) {
		if (warpExists(name))
			return false;
		warps.add(new Warp(name,x,y,z,stance));
		return true;
	}
	public boolean removeWarp(String name) {
		for(Warp i: warps) {
			if (i.name.compareTo(name.toLowerCase().trim())==0)
				warps.remove(i);
				return true;
		}
		return false;
	}
	public void listWarps(Player p) {
		//int rank=parent.ranks.checkName(p.getName());
		String line = "Warps: ";
		for (Warp i: warps) {
				line+=i.name+", ";
		}
		p.addMessage(line);
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		warps.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		String[] tokens = line.split(",");
    	try {
    		warps.add(
    				new Warp(
    				tokens[0],
    				Double.valueOf(tokens[1]),
    				Double.valueOf(tokens[2]),
    				Double.valueOf(tokens[3]),
    				Double.valueOf(tokens[4])
    				)
    		);
    	}
    	catch (Exception e) {}
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (Warp i: warps) {
        	line+=i.name + "," + i.x + ","+ i.y + ","+ i.z + ","+ i.stance + ",";
        	line+="\r\n";
        }
		return line;
	}
}
