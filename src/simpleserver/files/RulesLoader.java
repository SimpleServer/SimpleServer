package simpleserver.files;


public class RulesLoader extends FileLoader {
	String rules;
	public RulesLoader() {
		this.filename="rules.txt";
	}
	public String getRules() {
		return rules;
	}
	public void setRules(String msg) {
		rules=msg;
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		rules="";
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		rules+=line+"\r\n";
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		return rules;
	}
}
