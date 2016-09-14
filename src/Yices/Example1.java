package Yices;
import yices.YicesLite;

//Yices syntax example
public class Example1 {

	public void go(){
		int ctx;
		YicesLite yL = new YicesLite();
		ctx = yL.yicesl_mk_context();
		yL.yicesl_read(ctx, "(define x::int)");
		yL.yicesl_read(ctx, "(define y::int)");
		yL.yicesl_read(ctx, "(assert (= (+ x y) 0))");
		yL.yicesl_read(ctx, "(assert (>= y 0))");
		display(yL,ctx);
		yL.yicesl_read(ctx, "(push)");
		yL.yicesl_read(ctx, "(assert (>= x 1))");
		display(yL,ctx);
		yL.yicesl_read(ctx, "(pop)");
		yL.yicesl_read(ctx, "(assert (>= x 0))");
		display(yL,ctx);
		yL.yicesl_read(ctx, "(set-evidence! true)");
		yL.yicesl_read(ctx, "(check)");
		yL.yicesl_del_context(ctx);
	}
	
	public void go2(){
		int ctx;
		YicesLite yL = new YicesLite();
		ctx = yL.yicesl_mk_context();
		yL.yicesl_read(ctx, "(define x::int)");
		yL.yicesl_read(ctx, "(define y::int)");
		yL.yicesl_read(ctx, "(define z::int)");
		yL.yicesl_read(ctx, "(define a::(subtype (n::int) (= n 1)))");
		yL.yicesl_read(ctx, "(define b::(subtype (n::int) (= n 3)))");
		yL.yicesl_read(ctx, "(define c::(subtype (n::int) (= n 0)))");
		yL.yicesl_read(ctx, "(assert (= (+ x y) 0))");
		yL.yicesl_read(ctx, "(assert (>= y 0))");
		yL.yicesl_read(ctx, "(assert (>= x 0))");
		yL.yicesl_read(ctx, "(check)");
		/*
		yL.yicesl_read(ctx, "(assert (= z 0))");
		yL.yicesl_read(ctx, "(assert (= z 1))");
		yL.yicesl_read(ctx, "(check)");
		*/
		yL.yicesl_read(ctx, "(assert (> a 0))");
		yL.yicesl_read(ctx, "(check)");
		yL.yicesl_read(ctx, "(assert (> b 0))");
		yL.yicesl_read(ctx, "(check)");
		yL.yicesl_read(ctx, "(assert (and a a a))");
		yL.yicesl_read(ctx, "(check)");
		yL.yicesl_del_context(ctx);
	}
	
	public static void display(YicesLite yL, int ctx){
		
		if( yL.yicesl_inconsistent(ctx) == 0 )
			System.out.println("Unsatisfiable");
		else
			System.out.println("Satisfiable");
		
	}
}
