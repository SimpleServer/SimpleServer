package simpleserver.threads;

import simpleserver.Server;


public class ServerAutoSave implements Runnable {
	Server parent;
	public ServerAutoSave(Server parent) {
		this.parent=parent;
	}


	public void run() {
		// TODO Auto-generated method stub
		try {
			while (true) {
				while(parent.options.autoSave) {
					Thread.sleep(parent.options.autoSaveMins*1000*60);
					parent.saveLock.acquire();
					if (parent.requiresBackup()) {
						//parent.runCommand("say Saving Map...");
						parent.sendToAll(parent.l.get("SAVING_MAP"));
						parent.isSaving(true);
						parent.runCommand("save-all");
					
					}
					while (parent.isSaving()) {
						try {
							Thread.sleep(20);
						}catch(Exception e) {
							break;
						}
					}
					parent.saveLock.release();
				}
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			if (!parent.isRestarting()) {
				e.printStackTrace();
				System.out.println("[WARNING] Automated Server Save Failure! Please run save-all and restart server!");
		
			}
		}
	}

}
