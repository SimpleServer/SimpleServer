package simpleserver.files;

import java.util.Vector;

import simpleserver.Player;

public class Group {
	private int id;
	private String groupName;
	private boolean showTitle;
	private boolean isAdmin;
	private String color;
	
	public Group(int id, String groupName, boolean showTitle, boolean isAdmin, String color) {
		this.groupName=groupName;
		this.id=id;
		this.showTitle=showTitle;
		this.color=color;
		this.isAdmin=isAdmin;
	}
	public int getID() {return id;}
	public String getName(){return groupName;}
	public boolean showTitle() {return showTitle;}
	public boolean isAdmin() {return isAdmin;}
	public String getColor() {return color;}
	
	public static boolean contains(int[] groups, Player p) {
		if (groups!=null) {
			if (groups[0]==-1)
				return true;
			if (p.getGroup()==-1)
				return false;
			if (groups[0]==0)
				return true;
			for (int i: groups) {
				if (p.getGroup()==i)
					return true;
			}
		}
		return false;
	}
	public static int[] parseGroups(String idString) {
		return parseGroups(idString, ",");
	}
	public static int[] parseGroups(String idString,String delimiter) {
		String[] tokens = idString.split(delimiter);
		Vector<Integer> ranks = new Vector<Integer>();
		for(String i: tokens) {
			if (!i.startsWith("-")) {
				String[] tokens2 = i.split("-");
				if (tokens2.length<2) {
					try {
						int tryInt = Integer.valueOf(i);
						ranks.add(tryInt);
					}
					catch(Exception e) {}
				}
				else if (tokens2.length==2) {
					try {
						int lowInt = Integer.valueOf(tokens2[0]);
						int highInt = Integer.valueOf(tokens2[1]);
						if (lowInt<=highInt) {
							for (int k=lowInt;k<=highInt;k++) {
								ranks.add(k);
							}
						}
						
					}
					catch(Exception e) {}
				}
			}
			else {
				try {
					int tryInt = Integer.valueOf(i);
					ranks.add(tryInt);
				}
				catch(Exception e) {}
			}
		}
		int[] ret = new int[ranks.size()];
		int j=0;
		for (Integer num: ranks)
			ret[j++]=num;
		return ret;
	}
}
