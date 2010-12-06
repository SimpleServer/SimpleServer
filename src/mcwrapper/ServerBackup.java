package mcwrapper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import mcwrapper.files.ErrorLog;

public class ServerBackup implements Runnable {
	Server parent;
	public ServerBackup(Server parent) {
		this.parent=parent;
		checkOldBackups(new File("backups"));
	}


	public void run() {
		// TODO Auto-generated method stub
		while (!Thread.interrupted()) {
			while(parent.options.autoBackup) {
				try {
					Thread.sleep(parent.options.autoBackupMins*1000*60);
				} catch (InterruptedException e) {}
				try {
					parent.saveLock.acquire();
				} catch (InterruptedException e1) {
					return;
				}
				parent.runCommand("save-all");
				while (parent.isSaving) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {return;}
				}
				if (parent.isRestarting) {
					parent.saveLock.release();
					break;
				}
					
				try {
					backup();
				} catch (IOException e) {
					new Thread(new ErrorLog(e,"Server Backup Failure")).start();
					e.printStackTrace();
					System.out.println("[WARNING] Automated Server Backup Failure! Please run save-all and restart server!");
				}
				if (parent.numPlayers()==0) {
					parent.requireBackup=false;
				}
				parent.saveLock.release();
				
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return;
			}
		}
		
		
	}
	public void backup() throws IOException {
		if (parent.requireBackup || parent.numPlayers()>0) {
			parent.runCommand("save-off");
			System.out.println("[SimpleServer] Backing up server...");
			File backup = backupServer();
			zipDirectory(backup);
			parent.runCommand("save-on");
			deleteDirectory(backup);
			checkOldBackups(new File("backups"));
		}
	}
	public void zipDirectory(File directory) throws IOException {
		File f = new File(directory.getPath()+".zip");
		if (!f.exists())
			f.createNewFile();
		FileOutputStream dest = new FileOutputStream(f);
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
		zipDir(directory.getPath(),out);
		out.close();
		dest.close();
		System.out.println("[SimpleServer] Backup saved: " + directory.getPath() +".zip");		
	}
	public void zipDir(String dir2zip, ZipOutputStream zos) 
	{ 
	    try 
	   { 
	        //create a new File object based on the directory we have to zip 
	    	File zipDir = new File(dir2zip); 
	        //get a listing of the directory content 
	        String[] dirList = zipDir.list(); 
	        byte[] readBuffer = new byte[2156]; 
	        int bytesIn = 0; 
	        //loop through dirList, and zip the files 
	        for(int i=0; i<dirList.length; i++) 
	        { 
	            File f = new File(zipDir, dirList[i]); 
		        if(f.isDirectory()) 
		        { 
		                //if the File object is a directory, call this 
		                //function again to add its content recursively 
		            String filePath = f.getPath(); 
		            zipDir(filePath, zos); 
		                //loop again 
		            continue; 
		        } 
	            //if we reached here, the File object f was not a directory 
	            //create a FileInputStream on top of f 
	            FileInputStream fis = new FileInputStream(f); 
	            //create a new zip entry 
	            ZipEntry anEntry = new ZipEntry(f.getPath()); 
	            //place the zip entry in the ZipOutputStream object 
	            zos.putNextEntry(anEntry); 
	            //now write the content of the file to the ZipOutputStream 
	            while((bytesIn = fis.read(readBuffer,0,readBuffer.length)) != -1) 
	            { 
	                zos.write(readBuffer, 0, bytesIn); 
	            } 
	           //close the Stream 
	           fis.close(); 
	           
	        }
	   	}
	    catch (Exception e) {
	    	e.printStackTrace();
	    }
	    
	}
	public File backupServer() throws IOException {
		File backupDir = new File("backups");
		if (!backupDir.exists()) {
			backupDir.mkdir();
        }
		Calendar date = Calendar.getInstance();
		File backup = new File("backups"+File.separator+date.get(Calendar.YEAR) + "-" + (date.get(Calendar.MONTH)+1) + "-" + date.get(Calendar.DATE) + "-" + date.get(Calendar.HOUR_OF_DAY) + "_" + date.get(Calendar.MINUTE));
		copyDirectory(new File(parent.options.levelName),backup);
		return backup;
	}
	// If targetLocation does not exist, it will be created.
    public void copyDirectory(File sourceLocation , File targetLocation)
    throws IOException {
        
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            
            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {
            
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);
            
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }
    private void checkOldBackups(File backupDir) {
    	if (backupDir.isDirectory()) {
    		String[] dirs = backupDir.list();
    		for (int i=0;i<dirs.length;i++) {
    			File current = new File("backups" + File.separator + dirs[i]);
				if (System.currentTimeMillis()-current.lastModified()>1000*60*60*parent.options.keepBackupHours) {
					if (current.isDirectory()) {
						deleteDirectory(current);
					}
					else {
						current.delete();
					}
				}
    		}
    	}
    }
    private boolean deleteDirectory(File path) {
        if( path.exists() ) {
          File[] files = path.listFiles();
          for(int i=0; i<files.length; i++) {
             if(files[i].isDirectory()) {
               deleteDirectory(files[i]);
             }
             else {
               files[i].delete();
             }
          }
        }
        return( path.delete() );
      }

}
