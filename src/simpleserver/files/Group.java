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
