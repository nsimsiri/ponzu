package MTSGenerator2;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import DataTypes.AnalysisInstance;
import DataTypes.Event2;
import DataTypes.Scenario2;
import daikon.FileIO;
import daikon.PptMap;
import daikon.PptRelation;
import daikon.PptTopLevel;
import daikon.VarInfo;
import daikon.VarInfoName;
import daikon.inv.Implication;
import daikon.inv.Invariant;
import daikon.inv.binary.twoScalar.IntEqual;
import daikon.inv.binary.twoScalar.TwoScalar;
import daikon.inv.filter.ParentFilter;
import daikon.inv.ternary.threeScalar.ThreeScalar;
import daikon.inv.unary.scalar.OneOfScalar;
import daikon.inv.unary.scalar.SingleScalar;

public class InvParser {
	
/*	public static void main(String[] args)
	{
/*		InvParser parser = new InvParser("StackArTester.inv.gz");
		AnalysisInstance x = parser.parse("DataStructures.StackAr");
		x.printInvariants("StackArInvariants.txt");
*/		
/*		InvParser parser = new InvParser("jEdit.inv.gz");
		AnalysisInstance x = parser.parse("org.gjt.sp.jedit.buffer.UndoManager");
		x.printInvariants("UndoManager.txt");
*/		
/*		InvParser parser = new InvParser("jEdit2.inv.gz");
		AnalysisInstance x = parser.parse("org.gjt.sp.jedit.browser.VFSBrowser");
		x.printInvariants("VFSBrowser.txt");
*/
/*		InvParser parser = new InvParser("StandalonePlayer.inv.gz");
		AnalysisInstance x = parser.parse("javazoom.jlgui.player.amp.playlist.BasePlaylist");
		x.printInvariants("StandalonePlayer_InvParser.txt");
*/	
//		InvParser parser = new InvParser("evaluation/jlgui/ArrayList.inv.gz");
//		AnalysisInstance x = parser.parse("javazoom.jlgui.player.amp.util.ArrayList");
//		x.printInvariants("inv.txt");
//	}
	
	private PptMap ppts;
	private AnalysisInstance instance;
	private List<VarInfo> vars;
	private List<String> varnames;
	
	private static Random randomNum = new Random();
	public static boolean randomized = false;
	public static int seed = 10;
	
	public static boolean filtering = false;
	
	public InvParser(String filename)
	{
		// Load program points map object from the .inv file
		try {
			ppts = FileIO.read_serialized_pptmap(new File(filename), true);
			
		} catch (Exception e) {
			System.out.println("Exception caught (" + e.getClass().getSimpleName() 
					+ ") : " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		
		// This is a check that Daikon wants you to do just for safety
		ppts.repCheck();

//		for (PptTopLevel i : ppts.ppt_all_iterable()){
//			System.out.format("-- %s \n", i.name());
//		}

		// Turn off parent filter to be able to print all invariants from a subexit ppt
		ParentFilter.dkconfig_enabled = false;
		
		//ObviousFilter.dkconfig_enabled = false;
	}
	
	/** This method processes all objects in the returned AnalysisInstance 
	 * EXCEPT instance.scenario  
	 */
	public AnalysisInstance parse(String compname)
	{
		instance = new AnalysisInstance(compname);
		vars = new ArrayList<VarInfo>();
		varnames = new ArrayList<String>();
		
		// PptTopLevel represents a program point in Daikon (see doc)
		Iterator<PptTopLevel> i = ppts.pptIterator();
		while ( i.hasNext() )
        {
			PptTopLevel ppt = i.next();

			if (isInComponent(ppt, compname))
			{

				switch (ppt.type)
				{
				// If this program point contains class or object invariants; add invariants to objInvariants list
				case CLASS:
				case OBJECT:
					Iterator<Invariant> iv = ppt.invariants_iterator();
					while ( iv.hasNext() )
					{
						Invariant inv = iv.next();

						if (isWorthPrinting(inv) && isUsable(inv))
						{
							if(randomized && randomNum.nextInt(seed) == 0) {
								continue;
							} else {
								addAllVarsAndCheckPrestate(inv);
								instance.object_invariants.add(inv);
							}							
						}
					}

					break;
					
				// If this program point contains pre-conditions; ignore for now
				case ENTER: 
				// If this program point contains post-conditions for all exit points; ignore for now
				case EXIT: 
					break;
					
				// If this program point contains post-conditions for one exit point
				case SUBEXIT:
					// If this ppt has no conditional exit point
					if (ppt.children.size() == 0)
					{
						// If this point is worth considering (it was sampled at least once).
						if (ppt.num_samples() > 0)
						{
							Event2 e = createEvent(ppt);
							instance.events.put(ppt.name, e);
							instance.eventList.add(e);
						}
					}
					else
						for (PptRelation condChild : ppt.children)
						{
							if(condChild.child.num_samples() > 0)
							{
								Event2 e = createEvent(condChild.child);
								instance.events.put(condChild.child.name, e);
								instance.eventList.add(e);
							}
						}
				break;
					
				default:
					System.out.println("Unsupported program point type: " + ppt.type.name());
				}
			}
			
			// The following are robustness checks
			
			//Natcha Simsiri - uncommented these two lines when running older version of Daikon (1/2016)
//			if (ppt.combined_ppt != null)
//				System.out.println(ppt.name + " is a combined program point.");
        }

		// Important code that check whether a variable is a parameter or not.
		// May need to be revisited if it stops working as expected.
		for (VarInfo var : vars)
			if (var.is_static_constant)
			{
				instance.constants.put(var.name(), Scenario2.convert_to_string(var.constantValue()));
				System.out.println(var.name());
				System.out.println("  " + var.var_kind);
				System.out.println("  " + var.isParam());
				System.out.println("  " + var.isDerivedParam());
			}
			else if (!var.isPrestateDerived() && !var.is_add())// && !(var.isParam() || var.isDerivedParam()))
				instance.variables.add(var);
		
		return instance;
	}
		
	private Event2 createEvent(PptTopLevel ppt)
	{
		Event2 e = new Event2(ppt.ppt_name);
		
		// IVO: This peculiar loop seems to mostly work fine. Further testing
		// or rewriting would be desirable.
		// Find entry point

		PptTopLevel entry = ppt;
		while (entry.type != PptTopLevel.PptType.ENTER)
		{
			for (PptRelation parent : entry.parents)
				if (parent.parent.type == PptTopLevel.PptType.EXIT
						|| parent.parent.type == PptTopLevel.PptType.SUBEXIT
						|| parent.parent.type == PptTopLevel.PptType.ENTER)
				{
					entry = parent.parent;
					break;
				}
		}

		Iterator<Invariant> iv = entry.invariants_iterator();
		while ( iv.hasNext() )
		{
			Invariant inv = iv.next();
			if (isWorthPrinting(inv)) {
				if (isUsable(inv) && !isObjectInvariant(inv)) {
					if(randomized && randomNum.nextInt(seed) == 0) {
						continue;
					} else  {
						addAllVarsAndCheckPrestate(inv);
						e.addPreCond(inv);
					}
				}
			}
		}

		iv = ppt.invariants_iterator();
		while ( iv.hasNext() )
		{
			Invariant inv = iv.next();
//			System.out.format("[%s]%s u=%s !o=%s p=%s\n", inv.getClass().getSimpleName(), inv, isUsable(inv), !isObjectInvariant(inv),isWorthPrinting(inv));
			if (isWorthPrinting(inv) && isUsable(inv) && !isObjectInvariant(inv))
			{
				if(randomized && randomNum.nextInt(seed) == 0) {
					continue;
				} else {
						if (addAllVarsAndCheckPrestate(inv))
						e.addPreCond(inv);
					else
						e.addPostCond(inv);
				}
			}
		}
		return e;
	}
	
	private boolean isObjectInvariant(Invariant inv)
	{
		for (Invariant objInv : instance.object_invariants)
			if (objInv.isSameInvariant(inv))
				return true;
		return false;
	}
	
	private boolean isWorthPrinting(Invariant inv)
	{
		if (inv.isWorthPrinting())
			return true;
		
		// Check for size(array) == orig(size(array))
		if (inv.getClass().getSimpleName().equals("IntEqual"))
		{
			IntEqual ie = (IntEqual) inv;
			VarInfo v1 = ie.var1();
			VarInfo v2 = ie.var2();
			
			if (//(v1.is_size() || v2.is_size()) 
					//&& 
					(v1.name().equals("orig(" + v2.name() + ")")
					|| v2.name().equals("orig(" + v1.name() + ")"))
				) 
				return true;
		}
		
		return false;
	}
	
	private boolean addAllVarsAndCheckPrestate(Invariant inv)
	{
		String class_str = inv.getClass().getCanonicalName();

		if (class_str.startsWith("daikon.inv.binary.twoScalar"))
		{
			TwoScalar ts = (TwoScalar) inv;
			boolean first = addAllVarsAndCheckPrestate(ts.var1());
			boolean second = addAllVarsAndCheckPrestate(ts.var2());
			return first && second;
		}
		if (class_str.startsWith("daikon.inv.ternary.threeScalar"))
		{
			ThreeScalar ts = (ThreeScalar) inv;
			boolean first = addAllVarsAndCheckPrestate(ts.var1());
			boolean second = addAllVarsAndCheckPrestate(ts.var2());
			boolean third = addAllVarsAndCheckPrestate(ts.var3());
			return first && second && third;
		}
		if (class_str.startsWith("daikon.inv.unary.scalar"))
		{
			SingleScalar ss = (SingleScalar) inv;
			return addAllVarsAndCheckPrestate(ss.var());
		}
		
		if (class_str.contains("Implication"))
		{
			Implication imp = (Implication) inv;
			boolean antecedent = addAllVarsAndCheckPrestate(imp.predicate());
			boolean consequent = addAllVarsAndCheckPrestate(imp.consequent());
			return antecedent && consequent;
		}
		
		return false;
	}
	
	private boolean addAllVarsAndCheckPrestate(VarInfo var)
	{
		if (!varnames.contains(var.name()))
		{
			varnames.add(var.name());
			vars.add(var);
		}
		return addAllVarsAndCheckPrestate(var.get_VarInfoName());
	}
	
	private boolean addAllVarsAndCheckPrestate(VarInfoName vn)
	{
		if (vn instanceof VarInfoName.Add)
		{
			VarInfoName.Add add = (VarInfoName.Add) vn;
			return addAllVarsAndCheckPrestate(add.term);
		}
		if (vn instanceof VarInfoName.Prestate)
		{
			VarInfoName.Prestate pre = (VarInfoName.Prestate) vn;
			addAllVarsAndCheckPrestate(pre.term);
			return true;
		}
		if (vn instanceof VarInfoName.Poststate)
		{
			VarInfoName.Poststate post = (VarInfoName.Poststate) vn;
			addAllVarsAndCheckPrestate(post.term);
			return false;
		}
		
		return false;
	}
	
	public static boolean isInComponent(PptTopLevel ppt, String compName)
	{
		if (filtering) {
			return ppt.ppt_name.getFullClassName().equals(compName);
		} else {
			return true;
		}
		//return ppt.name.startsWith(compName + ".") || ppt.name.startsWith(compName + ":");
	}

	private boolean isUsable(Invariant inv)
	{
		String class_str = inv.getClass().getCanonicalName();
		
		if (class_str.contains("Float"))
			return false;

		// Avoid invariants that have modulo arithmetics; this may need to
		// be removed for some systems.
		if (class_str.contains("Divides")) 
			return false;
		
		if (class_str.contains("OneOfScalar")) {
			OneOfScalar os = (OneOfScalar) inv;
			int num_elts = os.num_elts();
			
			// This is to avoid invariants that say "x is one of {1,2,3}"
			if (num_elts > 1) { 
				if (filtering) {			
					return false;
				} else {
					return isUsableVariable(os.var());
				}
			}
			
//			if (os.is_hashcode())
//				return false;
		}
			
		
		if (class_str.startsWith("daikon.inv.binary.twoScalar"))
		{
			TwoScalar ts = (TwoScalar) inv;
			return isUsableVariable(ts.var1()) && isUsableVariable(ts.var2());
		}
		if (class_str.startsWith("daikon.inv.ternary.threeScalar"))
		{
			ThreeScalar ts = (ThreeScalar) inv;
			return isUsableVariable(ts.var1()) && isUsableVariable(ts.var2()) && isUsableVariable(ts.var3());
		}
		if (class_str.startsWith("daikon.inv.unary.scalar"))
		{
			SingleScalar ss = (SingleScalar) inv;
			if(ss.toString().contains("== null") || ss.toString().contains("!= null")) {
				//return false;
				isUsableNullVariable(ss.var());
			}
			return isUsableVariable(ss.var());	
		}
		
		if (class_str.contains("Implication"))
		{
			Implication imp = (Implication) inv;
			if (isUsable(imp.predicate()) && isUsable(imp.consequent()))
				return true;
			else
				return false;
		}
		
		if(filtering) {
			return false;
		} else {
			return false;
		}
	}
	
	/*private static boolean isRelation(Invariant inv)
	{
		String class_str = inv.getClass().getCanonicalName();
		
		if (class_str.startsWith("daikon.inv.binary.twoScalar"))
		{
			if (class_str.contains("IntEqual") || class_str.contains("IntGreaterEqual") 
					|| class_str.contains("IntGreaterThan") || class_str.contains("IntLessEqual")
					|| class_str.contains("IntLessThan") || class_str.contains("IntNonEqual")
					|| class_str.contains("LinearBinary"))
				return true;
			else
				return false;
		}
		else if (class_str.startsWith("daikon.inv.ternary.threeScalar"))
		{
			if (class_str.contains("FunctionBinary") || class_str.contains("LinearTernary"))
				return true;
			else
				return false;
		}
		else if (class_str.startsWith("daikon.inv.unary.scalar"))
		{
			if (class_str.contains("NonZero") || class_str.contains("OneOfScalar")
					|| class_str.contains("Positive") || class_str.contains("UpperFloat"))
				return true;
			else
				return false;
		}
		else
			return false;
	}*/
	
	
	// IVO: Changed it so that return variable is allowed in conditions.
	private static boolean isUsableVariable(VarInfo var)
	{
		if (!var.type.baseIsIntegral() && !var.type.baseIsBoolean())
			return false;
		
		// It seems that having a static constant just makes things 
		// run less reliably (invariants with the specific constant appear
		// somewhat forcefully).
		if (var.isStaticConstant()/* && var.type.baseIsBoolean()*/)
			return true;
//		{
//			System.out.println("Ignored CONSTANT: " + var);
//			return false;
//		}
		
		switch (var.var_kind)
		{
		case VARIABLE:	return filtering ? false : var.type.baseIsBoolean() || var.type.baseIsIntegral();//false;
		case RETURN:	return var.type.baseIsBoolean() || filtering ? false : var.type.baseIsIntegral();
		case FIELD:		return isUsableField(var);
		case FUNCTION:	return isUsableFunction(var);
		case ARRAY:		//shouldn't happen
		default:
		}
		
		return true;
	}
	
	private static boolean isUsableNullVariable(VarInfo var)
	{
/*		if (!var.type.baseIsIntegral() && !var.type.baseIsBoolean())
			return false;
		
		// It seems that having a static constant just makes things 
		// run less reliably (invariants with the specific constant appear
		// somewhat forcefully).
		if (var.isStaticConstant() && var.type.baseIsBoolean())
			return true;
*///		{
//			System.out.println("Ignored CONSTANT: " + var);
//			return false;
//		}
		
		switch (var.var_kind)
		{
		case VARIABLE:	return filtering ? false : var.type.baseIsBoolean() || var.type.baseIsIntegral();
		case RETURN:	return var.type.baseIsBoolean() || filtering ? false : var.type.baseIsIntegral();
		case FIELD:		return isUsableField(var);
		case FUNCTION:	return isUsableFunction(var);
		case ARRAY:		return false;
		default:
			return false;
		}
	}
	
	private static boolean isUsableField(VarInfo var)
	{
		
		if (var.isPrestate())
			return isUsableField(var.postState);
		
		if(! filtering) return true;
		
		if (!var.name().startsWith("this."))
		{
			System.out.println("Ignored FIELD: " + var);
			return false;
		}
		
		/* Check for too many dots 'this.oldImpl.socket.connected' */
		int limit = 2;
		if (var.var_kind == VarInfo.VarKind.ARRAY)
			limit += 2;
			
		if (var.name().split("\\.").length <= limit)
			return true;
		else
		{
			System.out.println("Ignored FIELD: " + var);
			return false;
		}
	}
	
	private static boolean isUsableFunction(VarInfo var)
	{
		//System.out.println("Function: " + var.name());
		
		if (var.isPrestate())
			return isUsableFunction(var.postState);
		
		for (VarInfo constituent : var.get_all_constituent_vars())
		{
			//System.out.println("  " + constituent + "is " + constituent.var_kind + "  " + constituent.type);
			if (constituent.var_kind == VarInfo.VarKind.ARRAY)
				if (isUsableField(constituent))
					continue;
				else
					return false;
			else if (!isUsableVariable(constituent))
				return false;
		}
		
		//System.out.println("Accepted: " + var.name());
		return true;
	}
	
	public static void main(String[] args)
	{
//		String classname = "CustomerConsole.Cancelled";
//		String filepath = "armagan/atm/";
//		String packagename = "atm.physical";
		
//		String classname = "Vector";
//		String filepath = "evaluation/jlgui/";
//		String tracename = "StandalonePlayer";
//		String packagename = "javazoom.jlgui.player.amp.util.javautil";
		
/*		String classname = "ElemNumber$NumberFormatStringTokenizer";
		String filepath = "evaluation/dacapo-stringtokenizer/";
		String tracename = "StringTokenizerDaCapo";
		String packagename = "org.apache.xalan.templates";
*/		
		
		/* Use filepath + tracename + ".inv.gz" */	
/*		String classname = "StringTokenizer";
		String filepath = "evaluation/jedit-comprehensive/";
		String tracename = "jEdit";
		String packagename = "org.gjt.sp.javautils";
*/		
//		/* Use filepath + tracename + ".inv.gz" */		
//		String classname = "Vector$1";
//		String filepath = "evaluation/jedit/";
//		String tracename = "Vector";
//		String packagename = "java.util";
		
//		/* Use filepath + tracename + ".inv.gz" */
//		String classname = "HashMap";
//		String filepath = "evaluation/voldemort/";
//		String tracename = "JUnitTestRunner";
//		String packagename = "voldemort.javautils";
		
		/* Use filepath + tracename + ".inv.gz" */		
/*		String classname = "StringTokenizer";
		String filepath = "evaluation/stringtokenizer-all/";
		String tracename = "StringTokenizer";
		String packagename = "java.util";
*/		
//		String classname = "Socket";
//		String filepath = "evaluation/socket-all/";
//		String packagename = "java.net";
//		String tracename = "Socket";
	
//		String classname = "ZipOutputStream";
//		String filepath = "evaluation/jarinstaller/";
//		String tracename = "ZipOutputStream";
//		String packagename = "java.util.zip";

//		String classname = "JarOutputStream";
//		String filepath = "evaluation/jarinstaller/";
//		String tracename = "JAR2";
//		String packagename = "java.util.jar";
		
		String classname = "SMTPProtocol";
		String filepath = "evaluation/smtpprotocol-columba2/";
		String tracename = "SMTPProtocol2";
		String packagename = "org.columba.ristretto.smtp";
		
/*		String classname = "Signature";
		String filepath = "evaluation/signature-all/";
		String tracename = "Signature";
		String packagename = "java.security";
*/		
		
/*		// Remeber to allow variables and exclude integers.
		String classname = "Connector";
		String filepath = "evaluation/dacapo-connector/";
		String tracename = "Connector";
		String packagename = "org.apache.catalina.connector";
*/		
		
		// Remeber to allow variables and exclude integers.
/*		String classname = "Connector";
		String filepath = "evaluation/connector-all/";
		String tracename = "Connector";
		String packagename = "org.apache.catalina.connector";
*/
		
/*		String classname = "ToHTMLStream";
		String filepath = "evaluation/tohtmlstream/";
		String tracename = "ToHTMLStream";
		String packagename = "org.apache.xml.serializer";
*/	
/*		String classname = "SftpConnection";
		String filepath = "evaluation/jftp-sftpconnection/";
		String tracename = "SftpConnection";
		String packagename = "net.sf.jftp.net.wrappers";
*/
		InvParser parser = new InvParser(filepath + tracename + ".inv.gz");
		AnalysisInstance x = parser.parse(packagename + "." + classname);
		x.printInvariants(filepath + classname + "-INV.txt");

		System.out.println("Invariants Parsed...");
	}
}

