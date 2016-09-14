package Yices;

import java.util.ArrayList;

import YicesHelpers.Converter;
import YicesHelpers.Generator;

import yices.YicesLite;

public class YicesRunner {

	private int ctx;
	private YicesLite yL;
	private ArrayList<String> definedVars;
	private ArrayList<String> expressions;
	private Generator gen;
	private Converter conv;
	
	public YicesRunner(){
		yL = new YicesLite();
		yL.yicesl_set_output_file("yices.out");
		ctx = yL.yicesl_mk_context();
		definedVars = new ArrayList<String>();
		expressions = new ArrayList<String>();
		gen = new Generator();
		conv = new Converter();
	}
	
	public void defineVars(ArrayList<String> vars){
		for(String var : vars){
			if(var.contains("array_")){
				var = var.substring(0,var.indexOf('['));
				String array = gen.defineArray(var, "int", "int");
				String arrayPost = gen.defineArray(var+"_post","int","int");
				//Check if already in there
				definedVars.add(array);
				definedVars.add(arrayPost);
			}
			else{
				definedVars.add(gen.define(var, "int"));
				definedVars.add(gen.define(var+"_post", "int"));
			}
		}
	}
	
	/*
	 * Add unformated yices infix expressions
	 */
	public void addExpr(ArrayList<String> exprs){
		for(String expr : exprs){
			expressions.add(gen.expr(conv.infixToPrefix(expr)));
		}
	}
	
	/*
	 * Add unformated yices infix expressions
	 */
	public void addExpr(String expr){
		expressions.add(gen.expr(conv.infixToPrefix(expr)));
	}
	
	/*
	 * Add formated yices prefix expressions
	 */
	public void addYicesExpr(ArrayList<String> exprs){
		for(String expr : exprs){
			expressions.add(expr);
		}
	}
	
	/*
	 * Add formated yices prefix expressions
	 */
	public void addYicesExpr(String expr){
		expressions.add(expr);
	}
	
	public int runYices(){
		int result=0;
		for(String var : definedVars ){
			yL.yicesl_read(ctx, var);
		}
		for(String var : expressions ){
			yL.yicesl_read(ctx, var);
		}
		result = yL.yicesl_inconsistent(ctx);
		return result;
	}
	
	public int runYices(ArrayList<String> defs, ArrayList<String> exprs){
		defineVars(defs);
		addExpr(exprs);
		return runYices();
	}
	
	public void reset(){
		yL.yicesl_read(ctx, gen.reset());
		expressions.clear();
	}
	
	public void dumpContext(){
		System.out.println("== Yices Context ==");
		for(String v : definedVars){
			System.out.println(v);
		}
		for(String e : expressions){
			System.out.println(e);
		}
		System.out.println("===================");
	}
}
