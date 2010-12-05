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
