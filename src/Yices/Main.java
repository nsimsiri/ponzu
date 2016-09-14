package Yices;

import YicesTest.Test2;

public class Main {

	public static void main(String[] args) {			
		
		StateGenerator test2 = new StateGenerator();
		test2.run();
		
		//Yices TEST
		//Example1 ex1 = new Example1();
		//Example2 ex2 = new Example2();
		//System.out.println("Example 1"); //Simple example of the language
		//ex1.go();
		//System.out.println("Example 2"); //Wrote a converter for infix to prefix expressions
		//ex2.go();
		//ex1.go2();
		
		//Expression Parsing Test
		//Test2 t2 = new Test2();
		//t2.run();
		
		//BenchMark Test
		/*
		Test1 test = new Test1();
		long begin = System.currentTimeMillis();
		for(int trials=0;trials<1000;trials++){
			test.run();
		}
		long end = System.currentTimeMillis();
		double runtime = (end-begin)/1000;
		System.out.println("Runtime: "+runtime+"sec");
		*/
		
		//BenchMark Test
		//Bench1 bench = new Bench1();
		//bench.runAll(5000);
	}
	

}
