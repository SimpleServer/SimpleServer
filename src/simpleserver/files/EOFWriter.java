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
import java.util.Iterator;
import java.util.LinkedList;

public class EOFWriter implements Runnable {
	byte[] buffer;
	LinkedList<byte[]> buffer2;
	Exception exception;
	String comments;
	public EOFWriter(byte[] buf, LinkedList<byte[]> lastSent, Exception e, String msg) {
		buffer=buf;
		buffer2=lastSent;
		exception = e;
		comments=msg;
	}
	public void run() {
		// TODO Auto-generated method stub
		Calendar date = Calendar.getInstance();
		File dump = new File("dump_"+date.get(Calendar.YEAR) + "-" + (date.get(Calendar.MONTH)+1) + "-" + date.get(Calendar.DATE) + "-" + date.get(Calendar.HOUR_OF_DAY) + "_" + date.get(Calendar.MINUTE) + ".txt");
		try {
		if (!dump.exists()) 
			dump.createNewFile();
			FileOutputStream f = new FileOutputStream(dump);
			PrintStream p = new PrintStream(f);
			if (exception!=null)
				exception.printStackTrace(p);
			
			p.println(comments);
			printStream(buffer,p);
			synchronized (buffer2) {
				for(Iterator<byte[]> itr = buffer2.iterator(); itr.hasNext();) {
					byte[] i = itr.next();
					printStream(i,p);
				}
			}
			p.close();
			f.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	private void printStream(byte[] stream, PrintStream p) {
		for(int i=0;i<stream.length;i++) {
			p.print(Byte.toString(stream[i]) + " ");
		}
		p.println();
	}

}
