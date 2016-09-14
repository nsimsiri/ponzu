package Yices;

import java.util.ArrayList;

import DataTypes.Event;
import YicesHelpers.Converter;

public class Test2 {

	Converter c;
	
	public Test2(){
		c = new Converter();
	}
	
	public void run(){
		/*
		ArrayList<String> exprs = new ArrayList<String>();
		exprs.add("not (x > y)");
		exprs.add("(x + y) = 0");
		exprs.add("x >= y");
		exprs.add("(x < y) or (z > y) or (z < y)");
		exprs.add("x => y");
		*/
		
		ArrayList<String> yicesExprs = new ArrayList<String>();
		
		yicesExprs.add("(define f::(-> int int))");
		yicesExprs.add("(define x::int)");
		//yicesExprs.add("(define f::(-> (subtype (n::int) (and (<= n 3) (>= n 0)) bool))"); 
		
		//yicesExprs.add("(define f1::(-> int int) (update f 0 2))");
		//yicesExprs.add("(define A2::(-> int int) (update A 1 2))"); 
	
		//yicesExprs.add("(assert (= A1 2))");
		
		//yicesExprs.add("(assert (= B 0))");
		//yicesExprs.add("(update (f 0) 1)");
		//yicesExprs.add("(assert( (update f (1) 0) )");
		//yicesExprs.add("(assert( (update f (2) 2) )");
		//yicesExprs.add("(assert (= (A 0) 0))");
		//yicesExprs.add("(assert (= (A B) 1))");
		yicesExprs.add("(assert(= x 0))");
		yicesExprs.add("(assert(= (f x) 1))");
		//yicesExprs.add("(assert(= (f 0) 1))");
		//yicesExprs.add("(assert(= (f 0) 2))");
		yicesExprs.add("(assert(= (f 0) 1))");
		//yicesExprs.add("(assert(= (f 1) 1))");
		
		yicesExprs.add("(assert(= (f 0) 0))");
		yicesExprs.add("(assert(= (f 2) 3))");
		yicesExprs.add("(assert(= 1 2))");
		
	
		
		//yicesExprs.add("(assert (and (= wp 9) (> rp 0)))");
		//yicesExprs.add("(assert (and (< wp 9) (< rp wp)))");
		
		//yicesExprs.add("(assert (not (< wp (- rp 1))))");
		//yicesExprs.add("(assert (< rp (- wp 1)))"); 
		//yicesExprs.add("(assert (< wp (- rp 1)))");
		//yicesExprs.add("(assert (and (= wp 9) (> rp 0)))");
		//yicesExprs.add("(assert (and (< wp 9) (< rp wp)))");
		/*
		yicesExprs.add("(assert (or (< wp (- rp 1)) (or (and (= wp 9) (> rp 0)) (and (< wp 9) (< rp wp)))))"); 
		yicesExprs.add("(assert (< wp_post (- rp_post 1)))"); 
		yicesExprs.add("(assert (not (= rp_post 9)))");
		yicesExprs.add("(assert (> rp_post 9))");
		yicesExprs.add("(assert (and (= rp_post rp) (and (=> (< wp 9) (= wp_post (+ wp 1))) (and (=> (= wp 9) (= wp_post 0)) (/= wp_post rp_post)))))");
		*/
		/*
		yicesExprs.add("(define rp::int)");
		yicesExprs.add("(define rp_post::int)");
		yicesExprs.add("(define wp::int)");
		yicesExprs.add("(define wp_post::int)");
		yicesExprs.add("(assert (< wp (- rp 1)))");
		yicesExprs.add("(assert (not (= wp 9)))");
		yicesExprs.add("(assert (> rp 0))");
		yicesExprs.add("(assert (< wp 9))");
		yicesExprs.add("(assert (not (< rp wp)))");
		yicesExprs.add("(assert (not (< rp (- wp 1))))");
		yicesExprs.add("(assert (= rp 9))");
		yicesExprs.add("(assert (not (> wp 0)))");
		yicesExprs.add("(assert (not (< rp 9)))");
		yicesExprs.add("(assert (< wp rp))");
		yicesExprs.add("(assert (or (< wp (- rp 1)) (or (and (= wp 9) (> rp 0)) (and (< wp 9) (< rp wp)))))");
		yicesExprs.add("(assert (< wp_post (- rp_post 1)))");
		yicesExprs.add("(assert (not (= wp_post 9)))");
		yicesExprs.add("(assert (> rp_post 0))");
		yicesExprs.add("(assert (< wp_post 9))");
		yicesExprs.add("(assert (not (< rp_post wp_post)))");
		yicesExprs.add("(assert (not (< rp_post (- wp_post 1))))");
		yicesExprs.add("(assert (= rp_post 9))");
		yicesExprs.add("(assert (not (> wp_post 0)))");
		yicesExprs.add("(assert (not (< rp_post 9)))");
		yicesExprs.add("(assert (< wp_post rp_post))");
		yicesExprs.add("(assert (and (= rp_post rp) (and (=> (< wp 9) (= wp_post (+ wp 1))) (and (=> (= wp 9) (= wp_post 0)) (/= wp_post rp_post)))))");
		
		*/
		/*
		for(String expr : exprs ){
			System.out.println("---");
			System.out.println(expr);
			System.out.println(c.infixToPrefix(expr));
		}
		*/
		
		YicesRunner runner = new YicesRunner();
		/*
		ArrayList<String> def = new ArrayList<String>();
		def.add("wp");
		def.add("rp");
		def.add("rp_post");
		def.add("rp_post");
		runner.defineVars(def);
		*/
		/*
		for(String expr: exprs){
			runner.addExpr(expr);
			System.out.println(expr+" : "+runner.runYices());
		}
		*/
		
		for(String expr: yicesExprs){
			runner.addYicesExpr(expr);
			System.out.println(expr+" : "+runner.runYices());
		}
		//runner.dumpContext();
		
		Event e = new Event("Hi","There");
		//e.expandFor("(x > y) and ( x < for(i=[0:4];(value[i])); ) and (y < z)",true);
		//e.expandFor("for(i=[0:1];(x > folder[i]);) asdf for(i=[0:1];(as+sd)); hghd",true);
		
	}
}
