package evaluation;

import java.util.ArrayList;

import DataTypes.AnalysisInstance;
import DataTypes.MTS;
import MTSGenerator.output.outputResults;
import MTSGenerator2.Initial_MTS_Generator;
import MTSGenerator2.InvParser;
import MTSGenerator2.TraceAnalyzer;
import MTSGenerator2.TraceParser;

public class GenerateRandom {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String[] classnames = {
//				"ElemNumber$NumberFormatStringTokenizer", 
/*				"Signature", 
				"StackAr",
*/				"SMTPProtocol", 
//				"Socket", 
//				"StringTokenizer", 
//				"ToHTMLStream", 
//				"ZipOutputStream"
		};
		String[] filepaths = {
//				"evaluation/randomized/numberstringtokenizer/", 
/*				"evaluation/randomized/signature/",
				"evaluation/randomized/stackar/",
*/				"evaluation/randomized/smtpprotocol/", 
//				"evaluation/randomized/socket/", 
//				"evaluation/randomized/stringtokenizer/", 
//				"evaluation/randomized/tohtmlstream/", 
//				"evaluation/randomized/zipoutputstream/"
		};
		String[] invnames = {
//				"StringTokenizerDaCapo",
/*				"Signature",
				"StackAr",
*/				"SMTPProtocol",
//				"Socket",
//				"StringTokenizer",
//				"ToHTMLStream",
//				"ZipOutputStream"
		};
		String[] packagenames = {
//				"org.apache.xalan.templates",
/*				"java.security",
				"DataStructures",
*/				"org.columba.ristretto.smtp",
//				"java.net",
//				"java.util",
//				"org.apache.xml.serializer",
//				"java.util.zip"
		};
		String[][] tracenames = {
//				{"StringTokenizerDaCapo"},
/*				{"Columba","Columba2","Columba3","Columba4","Columba5","Columba6","FTP","Signature"},
				{"StackAr"},
*/				{/*"Columba","Columba2","Columba3","Columba4","Columba5","Columba6",*/"SMTP","SMTP2"},
//				{"Columba","Columba2","Columba3","Columba4","Columba5","Columba6","Socket1","Socket2","Socket3","Socket4","Socket5","Socket6","Socket7","Socket8","Socket9","Socket10","Socket11"},
//				{"jEdit","jEdit2","StandalonePlayer"},
//				{"ToHTMLStream"},
//				{/*"JAR",*/"JAR2"/*,"jarinstaller"*/}
		};
		
/*		String classname = "SftpConnection";
		String filepath = "evaluation/jftp-sftpconnection/";
		String tracename = "SftpConnection";
		String packagename = "net.sf.jftp.net.wrappers";
*/

		InvParser.filtering = true;
		InvParser.randomized = true;
		InvParser.seed = 0;

		for (int run = 0; run < 2; run++) {
			InvParser.seed += 5;
			
			for (int i = 0; i < classnames.length; i++) {
				ArrayList<AnalysisInstance> instances = new ArrayList<AnalysisInstance>();
				boolean tracesProcessed = false;
			
				for (int randomCount = 0; randomCount < 10; randomCount++) {
					InvParser parser = new InvParser(filepaths[i] + invnames[i] + ".inv.gz");
					AnalysisInstance x = parser.parse(packagenames[i] + "." + classnames[i]);
					x.printInvariants(filepaths[i] + classnames[i] + "-" + run + "-" + randomCount + "-INV.txt");
					
					instances.add(x);
					System.out.println("Invariants Parsed...");
				
					if (true /*!tracesProcessed*/) {
						for (int j = 0; j < tracenames[i].length; j++) {
							System.out.println("Trace: " + tracenames[i][j]);
							new TraceParser(filepaths[i] + tracenames[i][j] + ".dtrace.gz", tracenames[i][j], x);
						}
						System.out.println("Traces Parsed...");
						tracesProcessed = true;
					} else {
						x.scenario = instances.get(0).scenario;
						x.eventList = instances.get(0).eventList;
					}
					
					x.eliminatePrivate();
//					x.printInvariants(filepaths[i] + classnames[i] + "-" + run + "-" + randomCount + "-INV.txt");
					
					Initial_MTS_Generator init_gen = new Initial_MTS_Generator();
					MTS invariant_based_MTS = init_gen.generateInitialMTS(x);
					
					if(invariant_based_MTS.getStateSize() == 1) {
						if (randomCount == 0) {
							tracesProcessed = false;
						}
						randomCount--;
						instances.remove(x);
						continue;
					}
					
					ArrayList<MTS> outputMTSs = new ArrayList<MTS>();
					outputMTSs.add(invariant_based_MTS);
					(new outputResults()).outputToMTSA(outputMTSs, filepaths[i] + classnames[i] + "_inv_based" + "-" + run + "-" + randomCount + ".lts");
				
					TraceAnalyzer trace_analysis = new TraceAnalyzer(x, invariant_based_MTS, init_gen.getYicesContext(), init_gen.getConverter());
					
					MTS refinedMTS = trace_analysis.refinementStrategy("Refinement");
					outputMTSs = new ArrayList<MTS>();
					outputMTSs.add(refinedMTS);
					(new outputResults()).outputToMTSA(outputMTSs, filepaths[i] + classnames[i] + refinedMTS.getName() + "-" + run + "-" + randomCount + ".lts");
					
					System.out.println("Created model: " + refinedMTS);
					outputMTSs = new ArrayList<MTS>();
					refinedMTS.removeNonRequired();
					outputMTSs.add(refinedMTS);
					(new outputResults()).outputToMTSA(outputMTSs, filepaths[i] + classnames[i] + refinedMTS.getName() + "-req" + "-" + run + "-" + randomCount + ".lts");
					
					System.out.println("Created model: " + refinedMTS);
					
					init_gen = null;
					trace_analysis.yicesRun = null;
				}
			}
		}
	}
}
