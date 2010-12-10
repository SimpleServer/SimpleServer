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
