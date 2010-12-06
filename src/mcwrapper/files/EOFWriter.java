package mcwrapper.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Calendar;

public class EOFWriter implements Runnable {
	byte[] buffer;
	byte[] buffer2;
	Exception exception;
	String comments;
	public EOFWriter(byte[] buf, byte[] lastSent, Exception e, String msg) {
		buffer=buf;
		buffer2=lastSent;
		exception = e;
		comments=msg;
	}
	public EOFWriter(byte[] buf, byte[] lastSent, String msg, boolean truevar) {
		buffer=buf;
		buffer2=lastSent;
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
			printStream(buffer2,p);
			p.println();
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
