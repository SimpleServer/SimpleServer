package simpleserver.files;


public class MOTDLoader extends FileLoader {
	String motd;

	public MOTDLoader() {
		this.filename="motd.txt";
	}
	public String getMOTD() {
		return motd;
	}
	public void setMOTD(String msg) {
		motd=msg;
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		motd="";
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		motd+=line+"\r\n";
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		return motd;
	}
}
