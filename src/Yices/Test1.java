package Yices;

import yices.YicesLite;
import YicesHelpers.Generator;

public class Test1 {
	
	State startState;
	State nextState;
	
	public Test1(){
		init();
	}

	public void init(){
		startState = moveAlways_postconditions(1);
		nextState = new State();
	}
	
	public void run(){
		
		boolean f[] = {false,false,false};
		int ctx;
		YicesLite yL = new YicesLite();
		
		for(int i=0;i<3;i++){ //current Destination
			for(int j=0;j<3;j++){ //current Browsing
				for(int k=0;k<8;k++){
					if(k%1==0){
						f[0] = !f[0];
					}
					if(k%2==0){
						f[1] = !f[1];
					}
					if(k%4==0){
						f[2] = !f[2];
					}	
					
					nextState.setState(f,i,j,true);
					//nextState.printState();
					
					//Start State and New State
					String def = startState.getDefine();
					//System.out.println(def);
					String expr = nextState.getExpression();
					//System.out.println(expr);
					//System.out.println("(check)");
					
					//Create Yices context
					ctx = yL.yicesl_mk_context();
					yL.yicesl_read(ctx, def);
					yL.yicesl_read(ctx, expr);
					yL.yicesl_read(ctx, "(check)");
					yL.yicesl_del_context(ctx);
					
				}
			}		
		}
		
	}
	
	private State moveAlways_postconditions(int dest){
		if(dest<1 || dest>3){
			dest=1;
		}
		boolean folderElements[] = {false,false,false};
		folderElements[dest-1] = true;
		State moveAlways = new State(folderElements,dest,dest,true);
		return moveAlways;
	}
	
	public class State {
		
		public boolean folderHasElements[];
		public int currentDestination;
		public int currentBrowsing;
		public boolean initialized;
		private Generator gen;
		
		public State(){
			folderHasElements = new boolean[3];
			for(int i=0;i<3;i++){
				folderHasElements[i]=false;
			}
			currentDestination = 0;
			currentBrowsing = 0;
			initialized = false;
			gen = new Generator();
		}
		
		public State(boolean folder[], int currDest, int currBrowse, boolean init ){
			folderHasElements = new boolean[3];
			for(int i=0;i<3;i++){
				folderHasElements[i] = folder[i];
			}
			currentDestination = currDest;
			currentBrowsing = currBrowse;
			initialized = init;
			gen = new Generator();
		}
		
		public void setState(boolean folder[], int currDest, int currBrowse, boolean init ){
			folderHasElements = new boolean[3];
			for(int i=0;i<3;i++){
				folderHasElements[i] = folder[i];
			}
			currentDestination = currDest;
			currentBrowsing = currBrowse;
			initialized = init;
		}
		
		public void printState(){
			System.out.print("[ "+currentDestination+" "+currentBrowsing+" ");
			for(int i=0;i<3;i++){
				System.out.print(folderHasElements[i]+" ");
			}
			System.out.println("]");
		}
		
		/*
		 * Get the defined variables from the state in Yices form
		 */
		public String getDefine(){
			String defines;
			defines = gen.defineValue("currentDestination", "int", currentDestination);
			defines += "\n"+gen.defineValue("currentBrowsing", "int", currentBrowsing);
			defines += "\n"+gen.defineValue("folderHasElements1", "int", folderHasElements[0] == true ? 1 : 0);
			defines += "\n"+gen.defineValue("folderHasElements2", "int", folderHasElements[1] == true ? 1 : 0);
			defines += "\n"+gen.defineValue("folderHasElements3", "int", folderHasElements[2] == true ? 1 : 0);
			return defines;
		}
		
		/*
		 * Get the expression that represent the state in Yices form
		 */
		public String getExpression(){
			String expr;
			expr = gen.expr("currentDestination = "+currentDestination);
			expr += "\n"+gen.expr("currentDestination = "+currentDestination);
			expr += "\n"+gen.expr("currentBrowsing = "+currentBrowsing);
			expr += "\n"+gen.expr("folderHasElements1 = "+(folderHasElements[0] == true ? 1 : 0));
			expr += "\n"+gen.expr("folderHasElements2 = "+(folderHasElements[1] == true ? 1 : 0));
			expr += "\n"+gen.expr("folderHasElements3 = "+(folderHasElements[2] == true ? 1 : 0));
			return expr;
		}
		
	}
}
