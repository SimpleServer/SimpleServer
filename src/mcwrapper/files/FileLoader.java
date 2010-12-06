package mcwrapper.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public abstract class FileLoader {
	protected String filename = "file.txt";
	protected String header ="";
	protected File obtainFile() {
		File check;
		check = new File(filename);
		if (check.exists()) {
			return check;
		}
		else {
			try {
				check.createNewFile();
				return check;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
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
