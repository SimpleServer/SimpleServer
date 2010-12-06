package mcwrapper.files;

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
