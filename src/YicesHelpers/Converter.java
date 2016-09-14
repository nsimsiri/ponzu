package YicesHelpers;

import java.util.ArrayList;
import java.util.Stack;
import java.util.StringTokenizer;

public class Converter {

	/*
	 * Takes an infix expression and input and outputs a prefix expression
	 * Right now the input must have explicit parentheses Ex. ((X+Y)>3)
	 * I can work on it later. If it seems like a problem I might just get an outside 
	 * parser and walk an AST to do this. 
	 */
	
	ArrayList<String> operators;
	
	public Converter(){
    	operators = new ArrayList<String>();
    	operators.add("+");
    	operators.add("-");
    	operators.add("*");
    	operators.add("/");
    	operators.add("=");
    	operators.add("/=");
    	operators.add("<");
    	operators.add(">");
    	operators.add("=>");
    	operators.add(">=");
    	operators.add("<=");
    	operators.add("and");
    	operators.add("or");
    	operators.add("not");
	}
	
    public String infixToPrefix(String infixExpr) {
    	
        Stack<String> operatorStack = new Stack<String>();
        Stack<String> operandStack = new Stack<String>();
        String symbol, operandA, operandB, operator, newExpr, prefixExpr;
        StringTokenizer tokenizer = new StringTokenizer(infixExpr, " () ", true);
        char e = infixExpr.charAt(0);
        
        //Loop through expression tokens
        while (tokenizer.hasMoreTokens()) {
           symbol = tokenizer.nextToken();
           
           e = symbol.charAt(0);
           
           if(e == ' '){   
           }
           else if( e == '('){
           }
           else if( isOperator(symbol) ){
        	   operatorStack.push(symbol);
           }
           else if( isOperand(e) ){
        	   operandStack.push(symbol);
           }
           else if( e == ')' ){
        	   if(operandStack.size()>1){
	        	   operator = operatorStack.pop();
	               operandA = operandStack.pop(); 
	               operandB = operandStack.pop();
	               newExpr = "("+operator + " " + operandB + " " + operandA+")";
	               operandStack.push(newExpr);
        	   }
        	   else if(operandStack.size()>0 && operatorStack.size()>0){
            	   operator = operatorStack.pop();
                   operandA = operandStack.pop(); 
                   newExpr = "("+operator + " " + operandA +")";
                   operandStack.push(newExpr);
        	   }
           }
        }    
            
        while(!operatorStack.empty()){
        	if(operandStack.size()>1){
	    	   operator = operatorStack.pop();
	           operandA = operandStack.pop(); 
	           operandB = operandStack.pop();
	           newExpr = "("+operator + " " + operandB + " " + operandA+")";
	           operandStack.push(newExpr);
        	}
        	else{
         	   operator = operatorStack.pop();
               operandA = operandStack.pop(); 
               newExpr = "("+operator + " " + operandA +")";
               operandStack.push(newExpr);
        	}
        }
        prefixExpr = (String)operandStack.pop();   
     
        return prefixExpr;
     }
     
    /*
     * Checks the character to see if it is an operand
     */
    public boolean isOperand(char c){
    	if(c >= 0x30 && c <= 0x39)
    		return true;
       	if(c >= 0x41 && c <= 0x5a)
    		return true;
       	if(c >= 0x61 && c <= 0x7a)
    		return true;
    	return false;
    }
    
    /*
     * Checks the character to see if it is an operator
     */
    public boolean isOperator(String s){
    	
    	for( String op : operators ){
    		if(s.equals(op))
    			return true;
    	}
    	return false;
    }
  

}


