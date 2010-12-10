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


public class RulesLoader extends FileLoader {
	String rules;
	public RulesLoader() {
		this.filename="rules.txt";
	}
	public String getRules() {
		return rules;
	}
	public void setRules(String msg) {
		rules=msg;
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		rules="";
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		rules+=line+"\r\n";
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		return rules;
	}
}
