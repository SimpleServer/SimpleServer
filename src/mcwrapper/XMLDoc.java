package mcwrapper;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XMLDoc {
	public Document xml;
	static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	public XMLDoc() {
		DocumentBuilder docbuild;
		try {
			docbuild = factory.newDocumentBuilder();
			xml = docbuild.newDocument();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public XMLDoc(Document doc) {
		xml = doc;
	}
	public void addText(String tag, String value) {
		Element element = xml.createElement(tag);
		element.setTextContent(value);
		xml.appendChild(element);
	}
	public String getDocument() {
		return xml.toString();
	}
	public Node f() {
		return xml.getFirstChild();
	}
	public Node l() {
		return xml.getLastChild();
	}
	

}
