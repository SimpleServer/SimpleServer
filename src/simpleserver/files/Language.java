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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Scanner;

import simpleserver.files.CommandList;

public class Language {

	public static final String[] defaults = {
		"BAD_BLOCK", "%1$s has tried to place illegal block #%2$s",
		"SAVING_MAP", "Saving Map...",
		"SAVE_COMPLETE","Save Complete!",
		"BACKING_UP", "Backing up...",
		"BACKUP_COMPLETE","Backup Complete!",
		"SERVER_RESTART_60","Server is restarting in 60 seconds!",
		"SERVER_RESTART_30","Server is restarting in 30 seconds!",
		"SERVER_RESTART_3","Server is restarting in 3 seconds!"		
	};
	Properties optionsLoader;
	
	public Language() {
		optionsLoader=new Properties();
	}
	public void save() {
		save(false);
	}
	private void loadDefaults() {
		if (optionsLoader!=null) {
			for (int i=0;i<defaults.length;i+=2) {
				optionsLoader.setProperty(defaults[i], defaults[i+1]);
			}
		}
	}
	public String get(String key) {
		return optionsLoader.getProperty(key);
	}
	public void save(boolean createNew) {
		try {
			if (createNew) { 
				loadDefaults();
			}
			File file = new File("language.properties");
			if (!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream w = new FileOutputStream(file);
						
			optionsLoader.store(w, "");
			
			w.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not write the properties file!");
		}
		
	}
	
	public void load() {
		try {
			loadDefaults();
			File file = new File("language.properties");
			if (!file.exists()) {
				file.createNewFile();
				save(true);
				return;
			}
			FileInputStream r = new FileInputStream(file);
			if (r==null || r.available()==0) {
				file.createNewFile();
				save(true);
				return;
			}
			optionsLoader.load(r);
			r.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not read the properties file!");
		}
	}

}
