package parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import DataTypes.MTS;
import DataTypes.MTS_state;
import DataTypes.MTS_transition;
import MTSGenerator.output.outputResults;

public class GraphMLParser {

	public static void main(String args[]) throws IOException
	{
		System.out.println("Welcome to GraphML Parser!");
		
		MTS mts = null;
		
		String filepath = "evaluation/groundtruth/";
		String packagename = "java.security";
		String classname = "Signature";
		
		try {
			mts = parse(filepath + packagename + "." + classname + ".xml", classname);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		
		ArrayList<MTS> list = new ArrayList<MTS>();
		list.add(mts);
		
		(new outputResults()).outputToMTSA(list, filepath + classname + ".lts");
		
		System.out.println("Created MTS!");
	}
	
	public static DataTypes.MTS parse(String filename, String graphName) throws IOException, ParserConfigurationException, SAXException
	{
		File f = new File(filename);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
		//doc.getDocumentElement().normalize();
		
		Node graph = doc.getElementsByTagName("graph").item(0);
		
		DataTypes.MTS our_mts = new DataTypes.MTS(graphName, null, null);
		HashMap<String, Integer> nameMap = new HashMap<String, Integer>();
		
		/* Build states */
		NodeList nodes = ((Element)graph).getElementsByTagName("node");
		for (int i = 0; i < nodes.getLength(); i++)
		{
			Node node = nodes.item(i);
			String nodeName = getAttributeValue(node, "id");
			nameMap.put(nodeName, i);
			our_mts.addMTSState(new MTS_state(i, null));
		}
		
		NodeList edges = ((Element)graph).getElementsByTagName("edge");
		for (int i = 0; i < edges.getLength(); i++)
		{
			Node edge = edges.item(i);
			String sourceName = getAttributeValue(edge, "source");
			String targetName = getAttributeValue(edge, "target");
			Integer source = nameMap.get(sourceName);
			Integer target = nameMap.get(targetName);
			
			String edgeName = ((Element)edge).getElementsByTagName("data").item(0).getTextContent();
			String[] methodNames = edgeName.split("\\|");
			
			for (String method : methodNames)
			{
				String[] signature = method.split("[\\.\\(\\)]");
				
				String transitionName = signature[1];
				if (transitionName.equals("[init]"))
					transitionName = signature[0].substring(signature[0].lastIndexOf("/") + 1).toLowerCase();
				transitionName += appendSignature(signature[2]);
				
				System.out.println(transitionName + ": " + sourceName + " -> " + targetName);
				
				our_mts.addMTSTransition(new MTS_transition(transitionName, source, target, "true"));
			}
		}
		
		return our_mts;
	}
	
	public static String getAttributeValue(Node node, String attrname)
	{
		return node.getAttributes().getNamedItem(attrname).getTextContent();
	}
	
	public static String appendSignature(String signature)
	{
		signature = signature.trim();
		if (signature.equals(""))
			return "";
		
		String str = "";
		int arrayCount = 0;
		
		for (int i = 0; i < signature.length(); i++)
		{
			if (signature.charAt(i) == '[')
			{
				arrayCount++;
				continue;
			}
			
			switch (signature.charAt(i))
			{
			case 'Z': str += "_boolean"; break;
			case 'B': str += "_byte"; break;
			case 'C': str += "_char"; break;
			case 'D': str += "_double"; break;
			case 'F': str += "_float"; break;
			case 'I': str += "_int"; break;
			case 'J': str += "_long"; break;
			case 'S': str += "_short"; break;
			case 'L': 
				int lastIndex = signature.indexOf(';', i);
				int firstIndex = signature.lastIndexOf('/', lastIndex) + 1;
				str += "_" + signature.substring(firstIndex, lastIndex);
				i = lastIndex;
				break;
			default: 
				System.out.println("Invalid signature: " + signature.charAt(i));
				System.exit(1);
			}
			
			for (int j = 0; j < arrayCount; j++)
				str += "_array";
			arrayCount = 0;
		}
		
		//System.out.println(str);
		return str;
	}
}
