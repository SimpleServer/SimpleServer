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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;

public class AdminLog implements Runnable {
	File logFile;
	FileWriter writer;
	LinkedList<String> lines = new LinkedList<String>();
	Calendar date = Calendar.getInstance();
	boolean changed=false;
	public AdminLog(){ 
		date = Calendar.getInstance();
		logFile = new File("logs" + File.separator+ "adminlog_"+date.get(Calendar.YEAR) + "-" + (date.get(Calendar.MONTH)+1) + "-" + date.get(Calendar.DATE) + "-" + date.get(Calendar.HOUR_OF_DAY) + "_" + date.get(Calendar.MINUTE) + ".txt");
		File dir = new File("logs");
		if (!dir.exists())
			dir.mkdir();
	}
	public void addMessage(String msg) {
		synchronized(lines) {
			lines.add("[SimpleServer]\t" + date.get(Calendar.HOUR_OF_DAY) + ":" + date.get(Calendar.MINUTE) + "\t" + msg + "\r\n");
		}
	}
	public void run() {
		// TODO Auto-generated method stub
		boolean write=false;
		while(!Thread.interrupted()) {
			synchronized(lines) {
				while (lines.size()>0) {
					if (!changed) {
						try {
							if (!logFile.exists())
								logFile.createNewFile();
							writer = new FileWriter(logFile,true);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						changed=true;
					}
					
					try {
						writer.write(lines.remove());
						write=true;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (write) {
					try {
						writer.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					write=false;
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		}
		if (writer!=null) {
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
