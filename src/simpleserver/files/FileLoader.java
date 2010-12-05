package simpleserver.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public abstract class FileLoader {
	protected String filename = "file.txt";
	protected String folder = "simpleserver";
	protected String header ="";
	protected File obtainFile() {
		File check;
		File dir = new File(folder);
		check = new File(filename);
		File f = new File(folder + File.separator + filename);
		if (!dir.exists()) {
			dir.mkdir();			
		}
		if (check.exists() && !f.exists()) {
			try {
				f.createNewFile();
				copyFile(check,f);
			}
			catch(Exception e) {
				e.printStackTrace();
				System.out.println("[SimpleServer] Could not load configuration for " + filename);
				System.exit(-1);
			}
			check.delete();
		}
		
		if (f.exists()) {
			return f;
		}
		else {
			try {
				f.createNewFile();
				return f;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
	}
	private static void copyFile(File in, File out) throws Exception {
	    FileInputStream fis  = new FileInputStream(in);
	    FileOutputStream fos = new FileOutputStream(out);
	    try {
	        byte[] buf = new byte[1024];
	        int i = 0;
	        while ((i = fis.read(buf)) != -1) {
	            fos.write(buf, 0, i);
	        }
	    } 
	    catch (Exception e) {
	        throw e;
	    }
	    finally {
	        if (fis != null) fis.close();
	        if (fos != null) fos.close();
	    }
	  }
	protected abstract String saveString();
	public void save() {
		File outFile = obtainFile();
		outFile.delete();
		outFile = obtainFile();
		if (outFile!=null) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
				writer.write(header);
		        writer.write(saveString());
		        writer.flush();
		        writer.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("Unable to save " + filename + "!");
		}
	}
	protected abstract void beforeLoad();
	protected abstract void loadLine(String line);
	public void load() {
		beforeLoad();
		header="";
		File inFile = obtainFile();
		boolean readingHeader=true;
		if (inFile!=null) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(inFile));
				String line = null;
		        while ((line=reader.readLine()) != null) {
		        	if (readingHeader && line.startsWith("#")) {
		        			header+=line+"\r\n";
		        	}
		        	else {
		        		readingHeader=false;
		        		loadLine(line);
		        	}
		        }
		        reader.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("Unable to load " + filename + "!");
		}
	}
}
