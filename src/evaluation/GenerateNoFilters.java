package evaluation;

import DataTypes.AnalysisInstance;
import MTSGenerator2.InvParser;

public class GenerateNoFilters {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String[] classnames = {
				"ElemNumber$NumberFormatStringTokenizer", 
				"Signature", 
				"StackAr",
				"SMTPProtocol", 
				"Socket", 
				"StringTokenizer", 
				"ToHTMLStream", 
				"ZipOutputStream"
		};
		String[] filepaths = {
				"evaluation/dacapo-stringtokenizer/", 
				"evaluation/signature-all/",
				"evaluation/stackar-long/",
				"evaluation/smtpprotocol-columba2/", 
				"evaluation/socket-all/", 
				"evaluation/stringtokenizer-all/", 
				"evaluation/tohtmlstream/", 
				"evaluation/jarinstaller/"
		};
		String[] invnames = {
				"StringTokenizerDaCapo",
				"Signature",
				"StackAr",
				"SMTPProtocol",
				"Socket",
				"StringTokenizer",
				"ToHTMLStream",
				"ZipOutputStream"
		};
		String[] packagenames = {
				"org.apache.xalan.templates",
				"java.security",
				"DataStructures",
				"org.columba.ristretto.smtp",
				"java.net",
				"java.util",
				"org.apache.xml.serializer",
				"java.util.zip"
		};
/*		String classname = "SftpConnection";
		String filepath = "evaluation/jftp-sftpconnection/";
		String tracename = "SftpConnection";
		String packagename = "net.sf.jftp.net.wrappers";
*/

		String destFilepath = "evaluation/contractor/no-invariant-filtering/";
		InvParser.filtering = false;
		
		for (int i = 0; i < classnames.length ; i++) {
			InvParser parser = new InvParser(filepaths[i] + invnames[i] + ".inv.gz");
			AnalysisInstance x = parser.parse(packagenames[i] + "." + classnames[i]);
			x.printInvariants(destFilepath + classnames[i] + "-INV.txt");
	
			System.out.println("Invariants Parsed...");
		}
	}

}
