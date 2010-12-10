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

public class IPBanLoader extends FileLoader {
	LinkedList<String> bans = new LinkedList<String>();
	public IPBanLoader() {
		this.filename="ip-ban-list.txt";
	}
	public void addBan(String ipAddress) {
		bans.add(ipAddress);
		save();
	}
	public boolean removeBan(String ipAddress) {
		for (String i: bans) {
			if (i.compareTo(ipAddress)==0) {
				bans.remove(i);
				save();
				return true;
			}
		}
		return false;
	}
	public boolean isBanned(String ipAddress) {
		for (String i: bans) {
			if (ipAddress.startsWith(i) && i.length()!=0) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		bans.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		if (line!=null && line!="") 
			bans.add(line);
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (String i: bans) {
        	line+=i + "\r\n";
        }
		return line;
	}
}
