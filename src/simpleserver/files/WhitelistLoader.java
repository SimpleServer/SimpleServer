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

public class WhitelistLoader extends FileLoader {
	LinkedList<String> users = new LinkedList<String>();
	public WhitelistLoader() {
		this.filename="white-list.txt";
	}
	public boolean isWhitelisted(String name) {
		for (String i: users) {
			if (name.toLowerCase().trim().equals(i.toLowerCase().trim())) {
				return true;
			}
		}
		return false;
	}
	public void addName(String name) {
		users.add(name);
		save();
	}
	public boolean removeName(String name) {
		for (String i: users) {
			if (name.toLowerCase().trim().compareTo(i.toLowerCase().trim())==0) {
				users.remove(i);
				save();
				return true;
			}
		}
		return false;	
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		users.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		users.add(line);
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (String i: users) {
        	line+=i + "\r\n";
        }
		return line;
	}
}
