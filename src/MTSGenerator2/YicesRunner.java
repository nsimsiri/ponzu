package MTSGenerator2;

import java.util.ArrayList;
import java.util.List;

import yices.YicesLite;
import YicesHelpers.Generator;

public class YicesRunner {

	private int ctx;
	private YicesLite yL;
	private ArrayList<String> definedVars;
	private Generator gen;
	
	private boolean verbose = false;
	
	public YicesRunner()
	{
		yL = new YicesLite();
		//yL.yicesl_set_output_file("yices.out");
		yL.yicesl_set_verbosity((short) 2);
		ctx = yL.yicesl_mk_context();
		definedVars = new ArrayList<String>();
		gen = new Generator();
	}
	
	public void defineVars(List<String> varnames)
	{
		for (String var : varnames) {
			definedVars.add(gen.define(var, "int"));
			definedVars.add(gen.define(var+"_post", "int"));
		}
		defineVars();
	}
	
	private void defineVars()
	{
		for (String var : definedVars) {
			System.out.println(var);
			yL.yicesl_read(ctx, var);
		}
	}
	
	public void reset()
	{
		yL.yicesl_read(ctx, "(reset)");
		defineVars();
	}
	
	public void push()
	{
		if (verbose)
			System.out.println("Push");
		yL.yicesl_read(ctx, "(push)");
	}
	
	public void pop()
	{
		if (verbose)
			System.out.println("Pop");
		yL.yicesl_read(ctx, "(pop)");
	}
	
	public void assertExpr(String expr)
	{
		if (verbose)
			System.out.println("Assert: " + expr);
		
		yL.yicesl_read(ctx, gen.expr(expr));
	}
	
	public void assertExpr(ArrayList<String> exprs)
	{
		for (String expr : exprs)
			assertExpr(expr);
	}
	
	public boolean isInconsistent()
	{
		int inconsistent = yL.yicesl_inconsistent(ctx);
		if (inconsistent != 0)
		{
			if (verbose)
				System.out.print("Not ");
		}
		//else
			//dumpContext();
		if (verbose)
			System.out.println("Consistent");
		return inconsistent != 0;
	}
	
	public void dumpContext()
	{
		yL.yicesl_read(ctx, "(dump-context)");
	}
}
