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
