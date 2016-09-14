package Yices;

import yices.YicesLite;

public class Bench1 {

	YicesLite yices;
	
	public Bench1(){
		yices = new YicesLite();
	}
	
	/*
	 * Runs all of the benchmark test and returns runtime
	 */
	public void runAll(int iter){
		double begin, end, runtime; 
		//Run bench1
		begin = System.currentTimeMillis();
		benchmark1(iter);
		end = System.currentTimeMillis();
		runtime = (end-begin)/1000;
		System.out.println("Benchmark1: "+runtime+" sec");
		//Run bench2
		begin = System.currentTimeMillis();
		benchmark2(iter);
		end = System.currentTimeMillis();
		runtime = (end-begin)/1000;
		System.out.println("Benchmark2: "+runtime+" sec");
		//Run bench3
		begin = System.currentTimeMillis();
		benchmark3(iter);
		end = System.currentTimeMillis();
		runtime = (end-begin)/1000;
		System.out.println("Benchmark3: "+runtime+" sec");
		//Run bench4
		begin = System.currentTimeMillis();
		benchmark4(iter);
		end = System.currentTimeMillis();
		runtime = (end-begin)/1000;
		System.out.println("Benchmark4: "+runtime+" sec");
	}
	
	/*
	 * Makes new context each time
	 */
	public void benchmark1(int iter){
		int ctx;
		for(int i=0;i<iter;i++){
			ctx = yices.yicesl_mk_context();
			yices.yicesl_read(ctx, "(define x::int)");
			yices.yicesl_read(ctx, "(define y::int)");
			yices.yicesl_read(ctx, "(assert (= (+ x y) 0))");
			yices.yicesl_read(ctx, "(assert (>= y 0))");
			yices.yicesl_read(ctx, "(assert (>= x 0))");
			//yices.yicesl_read(ctx, "(check)");
			yices.yicesl_del_context(ctx);
		}
	}
	
	/*
	 * Resets context each time
	 */
	public void benchmark2(int iter){
		int ctx;
		ctx = yices.yicesl_mk_context();
		for(int i=0;i<iter;i++){
			yices.yicesl_read(ctx, "(define x::int)");
			yices.yicesl_read(ctx, "(define y::int)");
			yices.yicesl_read(ctx, "(assert (= (+ x y) 0))");
			yices.yicesl_read(ctx, "(assert (>= y 0))");
			yices.yicesl_read(ctx, "(assert (>= x 0))");
			//yices.yicesl_read(ctx, "(check)");
			yices.yicesl_read(ctx, "(reset)");
		}
		yices.yicesl_del_context(ctx);
	}
	
	/*
	 * Makes new context and creates new variables
	 */
	public void benchmark3(int iter){
		int ctx;
		for(int i=0;i<iter;i++){
			ctx = yices.yicesl_mk_context();
			yices.yicesl_read(ctx, "(define x"+i+"::int)");
			yices.yicesl_read(ctx, "(define y"+i+"::int)");
			yices.yicesl_read(ctx, "(assert (= (+ x"+i+" y"+i+") 0))");
			yices.yicesl_read(ctx, "(assert (>= y"+i+" 0))");
			yices.yicesl_read(ctx, "(assert (>= x"+i+" 0))");
			//yices.yicesl_read(ctx, "(check)");
			yices.yicesl_del_context(ctx);
		}
	}
	
	/*
	 * Resets context and creates new variables
	 */
	public void benchmark4(int iter){
		int ctx;
		ctx = yices.yicesl_mk_context();
		for(int i=0;i<iter;i++){
		yices.yicesl_read(ctx, "(define x"+i+"::int)");
			yices.yicesl_read(ctx, "(define y"+i+"::int)");
			yices.yicesl_read(ctx, "(assert (= (+ x"+i+" y"+i+") 0))");
			yices.yicesl_read(ctx, "(assert (>= y"+i+" 0))");
			yices.yicesl_read(ctx, "(assert (>= x"+i+" 0))");
			//yices.yicesl_read(ctx, "(check)");
			yices.yicesl_read(ctx, "(reset)");
		}
		yices.yicesl_del_context(ctx);
	}
}
