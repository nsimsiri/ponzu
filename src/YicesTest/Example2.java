package YicesTest;
import YicesHelpers.Converter;
import YicesHelpers.Generator;

/*
 * Example taking an infix expression and turning it into a Yices command
 * 
 * I imagine we will be taking the defines and the expressions from the 
 * XML nodes and attribs and then we can use these functions.
 */

public class Example2 {

	public void go(){
		Converter c = new Converter();
		Generator g = new Generator();
		String expr = "(((P+X)=4)&((X*P)<=3))";
		System.out.println(g.define("P","int"));
		System.out.println(g.define("X","int"));
		System.out.println(g.expr(c.infixToPrefix(expr)));
		System.out.println(g.expr(c.infixToPrefix("((X | Y) < 3)")));
	}
	
}
