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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Calendar;

public class ErrorLog implements Runnable {
	Exception e;
	String comments;
	public ErrorLog(Exception e, String comments){ 
		this.e=e;
		this.comments=comments;
	}
	public void run() {
		// TODO Auto-generated method stub
		Calendar date = Calendar.getInstance();
		File dump = new File("error_"+date.get(Calendar.YEAR) + "-" + (date.get(Calendar.MONTH)+1) + "-" + date.get(Calendar.DATE) + "-" + date.get(Calendar.HOUR_OF_DAY) + "_" + date.get(Calendar.MINUTE) + ".txt");
		try {
		if (!dump.exists()) 
			dump.createNewFile();
			FileOutputStream f = new FileOutputStream(dump);
			PrintStream p = new PrintStream(f);
			p.println(comments);
			if (e!=null)
				e.printStackTrace(p);
			p.close();
			f.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
