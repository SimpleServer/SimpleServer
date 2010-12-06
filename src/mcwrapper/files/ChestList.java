package mcwrapper.files;

import java.util.Collection;
import java.util.LinkedList;
import java.util.HashMap;
import mcwrapper.Server;

public class ChestList extends FileLoader {
	Server parent;
	class CoordinateMap {
		private class X {
			HashMap<Integer,Y> x = new HashMap<Integer,Y>();
			public Y get(int x) {
				return this.x.get(x);
			}
			public boolean put(Chest c) {
				if (x.containsKey(c.x))
					return x.get(c.x).put(c);
				else {
 					Y newY = new Y();
 					x.put(c.x, newY);
 					return newY.put(c);
				}
			}
			public Chest remove(int xC, int yC, int zC) {
				if (!x.containsKey(xC))
					return null;
				Chest c = x.get(xC).remove(yC,zC);
				Y yObj = x.get(xC);
				if (yObj.y.keySet().size()==0)
					x.remove(xC);
				return c;
			}
			public boolean remove(Chest c) {
				if (!x.containsKey(c.x))
					return false;
				if (x.get(c.x).remove(c))
					x.remove(c.x);
				if (x.keySet().size()==0)
					return true;
				return false;
			}
		}
		private class Y {
			HashMap<Integer,Z> y = new HashMap<Integer,Z>();
			public Z get(int y) {
				return this.y.get(y);
			}
			public boolean put(Chest c) {
				if (y.containsKey(c.y))
					return y.get(c.y).put(c);
				else {
					Z newZ = new Z();
 					y.put(c.y, newZ);
 					return newZ.put(c);
				}
			}
			public Chest remove(int yC, int zC) {
				if (!y.containsKey(yC))
					return null;
				Chest c = y.get(yC).remove(zC);
				Z zObj = y.get(yC);
				if (zObj.z.keySet().size()==0)
					y.remove(yC);
				return c;
			}
			public boolean remove(Chest c) {
				if (!y.containsKey(c.y))
					return false;
				if (y.get(c.y).remove(c))
					y.remove(c.y);
				if (y.keySet().size()==0)
					return true;
				return false;
			}
		}
		private class Z {
			HashMap<Integer,Chest> z = new HashMap<Integer,Chest>();
			public Chest get(int z) {
				return this.z.get(z);
			}
			public boolean put(Chest c) {
				if (z.containsKey(c.z))
					return false;
				else {
 					z.put(c.z, c);
 					return true;
				}
			}
			public Chest remove(int zC) {
				if (!z.containsKey(zC))
					return null;
				Chest c = z.get(zC);
				z.remove(zC);
				return c;
			}
			public boolean remove(Chest c) {
				if (!z.containsKey(c.z))
					return false;
				z.remove(c.z);
				if (z.keySet().size()==0)
					return true;
				return false;
			}
		}
		X map = new X();
		HashMap<String, Chest> names = new HashMap<String, Chest>();
		public boolean findLock(int x, int y, int z) {
			try {
				map.get(x).get(y).get(z);
				return true;
			}
			catch(Exception e) {
				
			}
			return false;
		}
		public boolean findLock(String name) {
			if (names.containsKey(name.toLowerCase()))
				return true;
			return false;
		}
		public Chest getLock(int x, int y, int z) {
			try {
				return map.get(x).get(y).get(z);
			}
			catch(Exception e) {}
			return null;
		}
		public Chest getLock(String name) {
			try {
				return names.get(name.toLowerCase());
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		public void removeLock(String name) {
			Chest c;
			if (!names.containsKey(name))
				return;
			c=names.get(name);
			names.remove(name);
			map.remove(c);
		}
		public void removeLock(int x, int y, int z) {
			Chest c = map.remove(x, y, z);
			if (c!=null) {
				names.remove(c.name.toLowerCase());
			}
		}
		public boolean addLock(Chest c) {
			names.put(c.name, c);
			return map.put(c);
		}
		public LinkedList<Chest> flatArray2() {
			LinkedList<Chest> chestList= new LinkedList<Chest>();
			Collection<Chest> chests = names.values();
			chestList.addAll(chests);
			return chestList;
		}
		public LinkedList<Chest> flatArray() {
			LinkedList<Chest> chestList= new LinkedList<Chest>();
			Collection<Y> xVals = map.x.values();
			for (Y i:xVals) {
				Collection<Z> yVals = i.y.values();
				for (Z j:yVals) {
					Collection<Chest> chestVals = j.z.values();
					chestList.addAll(chestVals);
				}
			}
			return chestList;
		}
		public void clear() {
			map=new X();
			names=new HashMap<String,Chest>();
		}
	}
	class Chest {
		String name;
		boolean isGroup;
		int x;
		int y;
		int z;
		public Chest(String name, int x, int y, int z) {
			this.name=name;
			this.x=x;
			this.y=y;
			this.z=z;
			this.isGroup=false;
		}
		public Chest(String name, int x, int y, int z, boolean isGroup) {
			this.name=name;
			this.x=x;
			this.y=y;
			this.z=z;
			this.isGroup=isGroup;
		}
	}
	private CoordinateMap chests = new CoordinateMap();
	@SuppressWarnings("unused")
	private ChestList() {}
	public ChestList(Server parent) {
		this.parent=parent;
		this.filename="chest-list.txt";
	}
	public synchronized boolean giveLock(String name, int x, int y, int z, boolean isGroupLock) {
		if (hasLock(name))
			return false;
		if (hasLock(x,y,z))
			return false;
		name = name.toLowerCase().trim();
		chests.addLock(new Chest(name,x,y,z,isGroupLock));
		save();
		return true;
	}
	public boolean hasLock(int x, int y, int z) {
		return chests.findLock(x,y,z);
	}
	public boolean ownsLock(int x, int y, int z, String name) {
		if (!hasLock(name))
			return false;
		Chest c = chests.getLock(name);
		if (c==null)
			return false;
		if (c.x==x&&c.y==y&&c.z==z)
			return true;
		return false;
	}
	public boolean hasLock(String name) {
		return chests.findLock(name);
	}
	public synchronized void releaseLock(String name) {
		chests.removeLock(name.toLowerCase());
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		chests.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		String[] tokens = line.split(",");
        if (tokens.length>0) {
        	try {
        		chests.addLock(new Chest(tokens[0],Integer.valueOf(tokens[2]),Integer.valueOf(tokens[3]),Integer.valueOf(tokens[4]),Boolean.valueOf(tokens[1])));
        	}
        	catch (Exception e){ e.printStackTrace(); }
        }
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (Chest i: chests.flatArray2()) {
        	line+=i.name + "," + i.isGroup + "," + i.x + "," + i.y + "," + i.z ;
        	line+="\r\n";
        }
		return line;
	}
}
