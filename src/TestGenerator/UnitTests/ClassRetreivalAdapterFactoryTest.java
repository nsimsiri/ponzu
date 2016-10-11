package TestGenerator.UnitTests;

import TestGenerator.ArgumentCache.ClassRetrievalAdapterFactory;
import TestGenerator.ArgumentCache.UniversalTypeAdapterFactory;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;

/**
 * Created by NatchaS on 10/10/16.
 */
public class ClassRetreivalAdapterFactoryTest {
    public static abstract class Tree implements Serializable, SomeTree {
        public SomeTree side;
        public Tree L; public Tree R;
        private static final long serialVersionUID = -6074996219705033171L;
        public Tree(Tree L, Tree R){ this.L = L; this.R = R; }
        public void setSide(SomeTree side){this.side =side;}
    }
    public static interface SomeTree {
        public int hi();
    }
    public static class BinTree extends Tree {
        public int value;
        public transient SomeTree st;
        public BinTree(Tree L, int value, Tree R){ super(L, R); this.value = value; }
        @Override public String toString() { return String.format("(%s L=%s R=%s)", value, L!=null ? L.toString() : null, R!= null ? R.toString() : null);}
        @Override public boolean equals(Object o){
            if (o instanceof BinTree){
                BinTree bt = (BinTree)o;
                return Arrays.asList(new Boolean[]{
                        (this.L == bt.L || this.L.equals(bt.L)),
                        (this.R == bt.R || R.equals(bt.R)),
                        this.value == bt.value})
                        .stream()
                        .reduce((a, b) -> a && b).get();
            }
            return false;
        }
        @Override public int hi(){ return 1; }

    }
    public static class ScalaTree extends Tree {
        public int[] values;
        public ScalaTree(Tree L, int[] values, Tree R){ super(L, R); this.values = values; }

        @Override public String toString() { return String.format("(%s L=%s R=%s)", Arrays.toString(values),  L!=null ? L.toString() : null, R!= null ? R.toString() : null);}
        @Override public boolean equals(Object o){
            if (o instanceof ScalaTree){
                ScalaTree bt = (ScalaTree)o;
                return Arrays.asList(new Boolean[]{
                        (this.L == bt.L || this.L.equals(bt.L)),
                        (this.R == bt.R || R.equals(bt.R)),
                        Arrays.equals(this.values, bt.values)})
                        .stream()
                        .reduce((a, b) -> a && b).get();
            }
            return false;
        }
        @Override public int hi(){ return 2; }
    }

    public static void test1(){
        BinTree b1 = new BinTree(null, 1, null);
        ScalaTree s1 = new ScalaTree(null, new int[]{1,2}, null);
        BinTree b2 = new BinTree(null, 2, null);
        ScalaTree s2 = new ScalaTree(null, new int[]{99}, null);
        b1.L = s1; b1.R = b2; ((Tree)b1).side = s2;

        Set<String> s = ClassRetrievalAdapterFactory.getUniqueClassname(b1);
        System.out.println(s);
        System.out.println("passed");
    }

    public static void test2(){
        Class<Integer> c1 = Integer.class;
        Class<SomeTree> c2 = SomeTree.class;

        Set<String> s = ClassRetrievalAdapterFactory.getUniqueClassname(c1);
    }
    public static void main(String[] args){
//        test1();
        test2();
    }
}
