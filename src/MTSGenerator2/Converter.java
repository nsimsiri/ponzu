package MTSGenerator2;

import DataTypes.AnalysisInstance;
import DataTypes.Event2;
import daikon.VarInfo;
import daikon.VarInfoName;
import daikon.inv.Implication;
import daikon.inv.Invariant;
import daikon.inv.binary.twoScalar.LinearBinary;
import daikon.inv.binary.twoScalar.NumericInt;
import daikon.inv.binary.twoScalar.TwoScalar;
import daikon.inv.unary.scalar.LowerBound;
import daikon.inv.unary.scalar.Modulus;
import daikon.inv.unary.scalar.NonZero;
import daikon.inv.unary.scalar.OneOfScalar;
import daikon.inv.unary.scalar.Positive;
import daikon.inv.unary.scalar.UpperBound;

public class Converter {
	
	AnalysisInstance instance;
	
	public Converter(AnalysisInstance instance)
	{
		this.instance = instance;
		
		// Bug fix: This code should go through only once; we do not want to duplicate and write variables
		// or pre/postconditions twice.
		if (instance.var_names.size() == 0)
		{
			// Change variable names to Yices format
			for (VarInfo var : instance.variables)
			{
				String varname = var.name();
				if (var.is_size())
				{
					String arrayname = varname.substring(5, varname.length()-1);
					instance.var_names.add("size_" + toYicesExpr(arrayname, true));
				}
				else	
					instance.var_names.add(toYicesExpr(var.name(), true));
			}
		
			// Convert the predicate list
			for (Event2 e : instance.events.values()) {
				for (Invariant inv : e.getPreCond())
					e.addPreCond(toYicesExpr(inv, true));
				for (Invariant inv : e.getPostCond())
					e.addPostCond(toYicesExpr(inv, false));
			}
		}
	}
	
	public String toYicesExpr(Invariant inv, boolean isPre)
	{
		String class_str = inv.getClass().getCanonicalName();
		
		if (class_str.startsWith("daikon.inv.binary.twoScalar"))
		{
			if (class_str.contains("LinearBinary"))
			{
				LinearBinary lb = (LinearBinary) inv;
				return "(= (+ (* " + toYicesExpr(lb.core.a) + " " 
					+ toYicesExpr(lb.var1(), isPre) + ") (* " 
					+ toYicesExpr(lb.core.b) + " " 
					+ toYicesExpr(lb.var2(), isPre) + ") " 
					+ toYicesExpr(lb.core.c) + ") 0)" ;
			}
			else if (class_str.contains("Divides"))
			{
				NumericInt.Divides div = (NumericInt.Divides) inv;
				return "(= (mod " + toYicesExpr(div.var1(), isPre) + " "
					+ toYicesExpr(div.var2(), isPre) + ") 0)";
			}
			else
			{
				String sign = "";
				
				if (class_str.contains("Greater"))
					sign += ">";
				else if (class_str.contains("Less"))
					sign += "<";
				else if (class_str.contains("Non"))
					sign += "/";
				
				if (class_str.contains("Equal"))
					sign += "=";
				
				TwoScalar ts = (TwoScalar) inv;
				
				return "(" + sign + " " + toYicesExpr(ts.var1(), isPre) 
					+ " " + toYicesExpr(ts.var2(), isPre) + ")";
			}
		}
		/*if (class_str.startsWith("daikon.inv.ternary.threeScalar"))
		{
			System.out.println("Invalid invariant type: " + class_str);
			System.exit(1);
		}*/
		if (class_str.startsWith("daikon.inv.unary.scalar"))
		{
			if (class_str.contains("LowerBound"))
			{
				LowerBound lb = (LowerBound) inv;
				return "(>= " + toYicesExpr(lb.var(), isPre) + " " + toYicesExpr(lb.min()) + ")";
			}
			else if (class_str.contains("NonZero"))
			{
				NonZero nz = (NonZero) inv;
				return "(/= " + toYicesExpr(nz.var(), isPre) + " 0)";
			}
			else if (class_str.contains("OneOfScalar"))
			{
				OneOfScalar os = (OneOfScalar) inv;
				long[] elts = os.getElts();
				int num_elts = os.num_elts();
				if (num_elts == 1)
					return "(= " + toYicesExpr(os.var(), isPre) + " " + toYicesExpr(elts[0]) + ")";
				else
				{
					String text = "";
					for (int i = 0; i < num_elts; i++)
						text += " (= " + toYicesExpr(os.var(), isPre) + " " + toYicesExpr(elts[i]) + ")";
					return "(or" + text + ")";
				}
			}
			else if (class_str.contains("Positive"))
			{
				Positive p = (Positive) inv;
				return "(> " + toYicesExpr(p.var(), isPre) + " 0)";
			}
			else if (class_str.contains("UpperBound"))
			{
				UpperBound ub = (UpperBound) inv;
				return "(>= " + toYicesExpr(ub.var(), isPre) + " " + toYicesExpr(ub.max()) + ")";
			}
			
			return "Invalid invariant type: " + class_str;
		}
		
		if (class_str.contains("Implication"))
		{
			Implication imp = (Implication) inv;
			
			return "(=> " + toYicesExpr(imp.predicate(), isPre) + " "
				+ toYicesExpr(imp.consequent(), isPre);
		}
		
		System.out.println("Invalid invariant type: " + class_str);
		//System.exit(1);
		return null;
	}
	
	public String toYicesExpr(VarInfo var, boolean isPre)
	{
		return toYicesExpr(var.get_VarInfoName(), isPre);
	}
	
	public String toYicesExpr(VarInfoName vn, boolean isPre)
	{
		if (vn instanceof VarInfoName.Add)
		{
			VarInfoName.Add add = (VarInfoName.Add) vn;
			return "(+ " + toYicesExpr(add.term, isPre) + " " + toYicesExpr(add.amount) + ")";
		}
		if (vn instanceof VarInfoName.SizeOf)
		{
			VarInfoName.SizeOf size = (VarInfoName.SizeOf) vn;
			return "size_" + toYicesExpr(size.sequence.name(), isPre);
		}
		if (vn instanceof VarInfoName.Prestate)
		{
			VarInfoName.Prestate pre = (VarInfoName.Prestate) vn;
			return toYicesExpr(pre.term, true);
		}
		if (vn instanceof VarInfoName.Poststate)
		{
			VarInfoName.Poststate post = (VarInfoName.Poststate) vn;
			return toYicesExpr(post.term, false);
		}
		
		return toYicesExpr(vn.name(), isPre);
		
			
		/*String text = var.name() + "-(";
		for (VarInfo v : var.get_all_constituent_vars())
			text += v.name() + ",";
		text += ")";
		
		if (var.is_add())
			text += "isadd";
		if (var.is_size())
			text += "issize";
		if (var.isPrestate())
			text += "ispre";
		
		return text;*/
	}
	
	public String toYicesExpr(String str, boolean isPre)
	{
		if (str == null) return "null";
		if (instance.constants.containsKey(str)) {
			// Natcha Simsiri - value may be a long.
//			return toYicesExpr(Integer.parseInt(instance.constants.get(str)));
			return toYicesExpr(Long.parseLong(instance.constants.get(str)));
		}
		str = str.replaceAll("\\[\\.\\.\\]", "");
		str = str.replaceAll("\\.", "_");
		str = str.replaceAll("\\[\\]", "");
		if (!isPre)
			return str + "_post";
		else
			return str;
	}
	
	public String toYicesExpr(long x)
	{
		if (x < 0)
			return "(- 0 " + Math.abs(x) + ")";
		else
			return "" + x;
	}
	
	public String toYicesExpr(double x)
	{
		return toYicesExpr((long) x);
	}
	public static void main(String[] args){
		System.out.println(Long.MAX_VALUE);
		long l = -8168173757291644622L;
		String str = "-8168173757291644622L";

		if (!Character.isDigit(str.charAt(str.length() - 1))) {
			str = str.substring(0, str.length() - 1);
		}
		System.out.println(str);
		System.out.println(Long.parseLong(str));
	}
}
