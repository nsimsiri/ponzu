package TestGenerator.UnitTests;

import TestGenerator.ArgumentCache.ClassRetrievalAdapterFactory;
import TestGenerator.ArgumentCache.GraphAdapterBuilder;
import TestGenerator.ArgumentCache.UniversalTypeAdapterFactory;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static TestGenerator.ArgumentCache.UniversalTypeAdapterFactory.buildGson;
import static TestGenerator.ArgumentCache.UniversalTypeAdapterFactory.deserialize;
import static TestGenerator.ArgumentCache.UniversalTypeAdapterFactory.deserializeObjectArray;

/**
 * Created by NatchaS on 9/24/16.
 */
public class UniversalTypeAdapterFactoryTest {
    static Tree l1 = new ScalaTree(null, new int[]{3,2,2}, null);
    static Tree l12 = new ScalaTree(null, new int[]{99, 199, 299}, null);
    static Tree l2 = new BinTree(null, 1337, null);
    static Tree l22 = new BinTree(null, 9, null);
    static Tree t = new BinTree(l2, 1, l22);
    static Tree t2 = l12;
    static {
        t2.L = t; t2.R = l1;
    }

    public static abstract class Tree implements Serializable {

        public Tree L; public Tree R;
        private static final long serialVersionUID = -6074996219705033171L;
        public Tree(Tree L, Tree R){ this.L = L; this.R = R; }
    }
    public static interface SomeTree {
        public void hi();
    }
    public static class BinTree extends Tree{
        public int value;
        public SomeTree st;
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

    }

    /**
     *  Duplicate Field Tests and classes
     */
    public static class ASuper {
        private Integer x = 1;
        private double y = 2.0;
        public Integer getX() {
            return x;
        }
        public void setX(Integer x) {
            this.x = x;
        }
        public double getY() {
            return y;
        }
        public void setY(double y) {
            this.y = y;
        }
    }
    public static class A extends ASuper{
        private Integer x = 100;
        private double y = 200.0;
        private String z = "ok";
        public Integer getX() {
            return x;
        }
        public void setX(Integer x) {
            this.x = x;
        }
        public double getY() {
            return y;
        }
        public void setY(double y) {
            this.y = y;
        }
        public String getZ() {
            return z;
        }
        public void setZ(String z) {
            this.z = z;
        }
        public Integer getSuperX(){ return super.getX();}
        public double getSuperY(){ return super.getY();}
        @Override
        public String toString(){
            return String.format("s.x=%s s.y=%s x=%s y=%s z=%s\n", super.getX(), super.getY(), x, y, z);
        }
        @Override
        public boolean equals(Object o){
            A ro = (A)o;
            return ro.getX().equals(x) && ro.getY() == y && ro.getZ().equals(z)
                    && ro.getSuperX().equals(super.x) && ro.getSuperY()==super.getY();
        }
    }
    public static void testDuplicateFields(){
        GsonBuilder gb = new GsonBuilder();
        FieldNamingStrategy nameStrat = new UniversalTypeAdapterFactory.DuplicateFieldNamingStrategy();
        gb.setFieldNamingStrategy(nameStrat);
        Gson gson = gb.serializeNulls().setPrettyPrinting().create();
        A a = new A();
        String json = gson.toJson(a);
        A _a = gson.fromJson(json, a.getClass());
        assert(_a.equals(a));
        System.out.println("duplicate field 1 passed");

    }

    public static abstract class Node {
        public DeqNode next;
        public DeqNode prev;
        abstract public void add(Object x);
    }

    public static class DeqNode extends Node{
        public Integer x;
        public DeqNode(){}
        public DeqNode(Integer x){this.x=x;}
        public void add(Object x){
            this.next = new DeqNode((Integer)x);
            this.next.prev = this;
        }

        @Override
        public String toString(){
            return String.format("[%s <- %s (%s) -> %s] <-> %s", System.identityHashCode(this.prev), System.identityHashCode(this),
                    this.x, System.identityHashCode(this.next), this.next == null ? null : this.next.toString());
        }

        @Override
        public boolean equals(Object o){
            DeqNode od = (DeqNode) o;
            if (od == null) return false;
            if (od.next == null && next ==null) return true;
            if (od.next != null && next != null){
                return next.equals(od.next);
            } else return false;
        }

    }

    public static void testCyclicDeq(){
        Node head = new DeqNode(1);
        head.add(2);
        Node headCheck = new DeqNode(1);
        headCheck.add(2);
        head.equals(headCheck);
        Gson gson = buildGson(head);
        String json = gson.toJson(head);
        Node head2 = gson.fromJson(json, head.getClass());
        assert(head.equals(head2));
        System.out.println("cyclic deq 1 passed");
    }

    public static class GraphNode {
        public List<GraphNode> e = new ArrayList<>();
        public GraphNode p;
        public int val;
        public GraphNode(int val){
            this.val = val;
        }
        public void addEdge(GraphNode node){
            this.e.add(node);
            node.p = this;
        }

        @Override
        public String toString(){
            Queue<GraphNode> q = new LinkedList<>();
            q.add(this);
            GraphNode l = this;
            String s = "";
            while(!q.isEmpty()){
                GraphNode c = q.poll();
                if (c.p != l){
                    s += "";
                    l = c.p;
                }
                s += String.format("(%s <-> %s) ", (c.p == null ) ? null : c.p.val, c.val);
                for(GraphNode newN : c.e){
                    q.add(newN);
                }

            }
            return s;
        }
    }
    public static void testCyclicGraph() {
        GraphNode n1 = new GraphNode(1);
        GraphNode n2 = new GraphNode(2);
        GraphNode n3 = new GraphNode(3);
        GraphNode n4 = new GraphNode(4);
        GraphNode n5 = new GraphNode(5);
        n3.addEdge(n4); n3.addEdge(n5);
        n1.addEdge(n3); n1.addEdge(n2);

        Gson gson = buildGson(n1);
        String json = gson.toJson(n1);
//        System.out.println(json);
        GraphNode gn = gson.fromJson(json, n1.getClass());
        assert(n1.toString().equals(gn.toString()));
        System.out.println("cyclic graph 1 passed.");
    }

    public static void testArbArray() throws ClassNotFoundException {
        Object[] Os = new Object[]{"x", 1, 'c'};
        Gson gson = buildGson(Os);
        String json = gson.toJson(Os);
        Object[] _Os = (Object[])deserialize(json, Os.getClass(), gson);
        assert(Arrays.asList(_Os).equals(Arrays.asList(Os)));
        System.out.println("Object[] =" + Arrays.asList(_Os) + " passed.");

        Object[][] Oss = new Object[][]{Os, Os};
        gson = UniversalTypeAdapterFactory.buildGson(Oss);
        json = gson.toJson(Oss);
        List OssCheck = Arrays.asList(Oss).stream().map((Object[] __o) -> Arrays.asList(__o)).collect(Collectors.toList());
        Object[][] _Oss = (Object[][]) deserialize(json, Oss.getClass(), gson);
        List _OssCheck = Arrays.asList(_Oss).stream().map((Object[] __o) -> Arrays.asList(__o)).collect(Collectors.toList());
        assert(Arrays.asList(OssCheck).equals(Arrays.asList(_OssCheck)));
        System.out.println("Object[][] passed.");

        List<Object> ol = Arrays.asList(new Object[]{new Integer(2), new Character ('p')});
        gson = UniversalTypeAdapterFactory.buildGson(ol);
        json = gson.toJson(ol);
        List<Object> _ol = deserialize(json, ol.getClass(), gson);
        System.out.println("Testing.." + _ol);
        assert(ol.equals(_ol));
    }

    public static void testCollections() {
        String json = "";
        Gson gson = null;

        List lt = new ArrayList(Arrays.asList(new Tree[]{t, t2}));
        gson = UniversalTypeAdapterFactory.buildGson(lt);
        json = gson.toJson(lt);
        System.out.println(json);
        List _lt = gson.fromJson(json, lt.getClass());
        System.out.println(_lt);
        System.out.println(lt);
//        assert(lt.toString().equals(_lt.toString()));
//        System.out.println("List<Tree> passed. (Tree is abstract)");

//        gson = UniversalTypeAdapterFactory.buildGson();
//        List l = new ArrayList(Arrays.asList(new Integer[]{1,2}));
//        json = gson.toJson(l);
//        List _l = gson.fromJson(json, l.getClass());
//        assert(l.equals(_l));
//        System.out.println("List<Integer> passed.");
//
//        Set s = new HashSet(Arrays.asList(new Integer[]{1,2}));
//        gson = UniversalTypeAdapterFactory.buildGson();
//        json = gson.toJson(s);
//        Set _s = gson.fromJson(json, s.getClass());
//        assert(_s.equals(s));
//        System.out.println("Set<Integer> passed.");
//
//        List<List> ll = new LinkedList(Arrays.asList(new Object[]{s, l}));
//        gson = UniversalTypeAdapterFactory.buildGson();
//        json = gson.toJson(ll);
//        List<List> _ll = gson.fromJson(json, ll.getClass());
//        assert(ll.equals(_ll));
//        System.out.println("List<? extends Collection> passed.");

    }

    public static void testMap(){
        String json = "";

        Map m = new HashMap<String, Integer>();
        m.put("a",1); m.put("b",2);
        Gson gson = UniversalTypeAdapterFactory.buildGson(m);
        json = gson.toJson(m);
        Map _m = gson.fromJson(json, m.getClass());
        assert(_m.equals(m));
        System.out.println("HashMap<String, Integer> passed.");

        List m2l = new ArrayList();
        m2l.add(1);
        Map m2 = new HashMap<String, Object>();
        m2.put("a",m); m2.put("b",m2l);
        gson = UniversalTypeAdapterFactory.buildGson(m2);
        json = gson.toJson(m2);

        Map _m2 = gson.fromJson(json, m2.getClass());
        assert(m2.equals(_m2));
        System.out.println("HashMap<String, Object> passed. (Object includes Map and Collection)");
    }

    public static void testTree(){
        Gson gson = buildGson(t);
        String json = gson.toJson(t);
        Tree _t = gson.fromJson(json, t.getClass());
        assert(_t.equals(t));
        gson = buildGson(t2);
        json = gson.toJson(t2);
        System.out.println(json);
        Tree _t2 = gson.fromJson(json, t2.getClass());
        assert(_t2.equals(t2));
        System.out.println("Abstract Tree (BinTree, ScalaTree) passed.");
    }

    public static void testSpecialDoubleValue() throws ClassNotFoundException{
        String json = "";
        List<Double> d = new ArrayList<>();
        d.add(new Double(1.0));
        d.add(Double.NaN);
        d.add(Double.POSITIVE_INFINITY);
        d.add(Double.NEGATIVE_INFINITY);

        Gson gson = UniversalTypeAdapterFactory.buildGson(d);
        json = gson.toJson(d);
        List<Double> _d = deserialize(json, d.getClass(), gson);
        assert(_d.equals(d));

        Object[] A = new Object[]{new Double(1.0), new Character('c'), Double.NaN,  Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
        gson = UniversalTypeAdapterFactory.buildGson(A);
        json = gson.toJson(A);
        Object[] _A = deserialize(json, A.getClass(), gson);
        assert(Arrays.asList(_A).equals(Arrays.asList(A)));

        Number[] B = new Number[]{new Double(1.0), new Integer(2), Double.NaN};
        gson = UniversalTypeAdapterFactory.buildGson(B);
        json = gson.toJson(B);
        Number[] _B = (Number[])deserialize(json, B.getClass(), gson);
        assert(Arrays.asList(_B).equals(Arrays.asList(B)));

        Double[] C = new Double[]{new Double(1.0), Double.NaN,  Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
        gson = UniversalTypeAdapterFactory.buildGson(C);
        json = gson.toJson(C);
        Object[] _C = deserialize(json, C.getClass(), gson);
        assert(Arrays.asList(_C).equals(Arrays.asList(C)));
        System.out.println("Special Double value [NaN, Infinity, -Infinity] passed");
    }

    public static void testDecimalFormat() {
        String json = "";
        DecimalFormat d = new DecimalFormat("[1\\s+]");
        Gson gson = buildGson(d);
        json = gson.toJson(d);
        DecimalFormat _df = gson.fromJson(json, d.getClass());
    }

    public static class DoubleNull {
        public Double d;
        public DoubleNull(Double d){ this.d = d;}
        @Override public String toString() { return (d==null) ? null : d.toString(); }
    }

    public static void testDouble() throws Exception{
        DoubleNull dn = new DoubleNull(new Double(1));
        DoubleNull dd2 = new DoubleNull(null);
        DoubleNull[] dd = new DoubleNull[]{dn, dd2};
        Gson gson = buildGson(dd);
        String json = gson.toJson(dd);
        DoubleNull[] _dd = UniversalTypeAdapterFactory.deserialize(json, dd.getClass(), gson);
    }

    public static enum SomeEnum {
        a,b,c;
    }

    public static class EnumWrapper {
        public EnumWrapper next;
        public SomeEnum v;
        public EnumWrapper(SomeEnum se, EnumWrapper next){
            this.v = se; this.next = next;
        }
        @Override
        public String toString(){
            Set<Integer> uniqueRef = new HashSet<>();
            EnumWrapper cur = this;
            String s = "";
            while(!uniqueRef.contains(System.identityHashCode(cur))){
                int cur_ref = System.identityHashCode(cur);
                s += String.format("[%s %s]", cur_ref, cur.v);
                uniqueRef.add(cur_ref);
                if (cur.next == null) break;
                cur = cur.next;
            }
            return s;
        }
    }

    public static void enumTest() throws ClassNotFoundException{
        System.out.printf("%s %s\n", System.identityHashCode(SomeEnum.a), System.identityHashCode(SomeEnum.a));
        EnumWrapper ew = new EnumWrapper(SomeEnum.a, null);
        EnumWrapper ew2 = new EnumWrapper(SomeEnum.a, ew);
        ew.next = ew2;
        Gson gson = buildGson(ew);
        String json = gson.toJson(ew);
        EnumWrapper _ew = UniversalTypeAdapterFactory.deserialize(json, EnumWrapper.class, gson);
        System.out.println("enum passed");

    }

    public static class TwoDWrap {
        public Object[][] o = new Object[][]{{1.0},{2}};
        @Override public String toString(){
            String s = "";
            for(int i = 0; i < o.length; i++){
                s += String.format("%s ", Arrays.asList(o[i]));
            }
            return "{ " + s + " }";
        }
    }

    public static class DecFormWrap {
        public DecimalFormat d = new DecimalFormat("[1\\s+]");
//        public ScalaTree d = (ScalaTree)l1;
        public DecFormWrap next;
        public String k = "hello";
        @Override public String toString() { return String.format("[(%s)next=%s]", k, System.identityHashCode(next));}
    }

    public static void NestedCollectionObjectArrayTest() throws ClassNotFoundException, InterruptedException{

        Gson gson = null;
        String json = "";
        Object[][] o = new Object[][]{new Double[]{1.1, 1.2}, new Double[]{}};
        gson = buildGson(o);
        json = gson.toJson(o);
        Object[][] _o = UniversalTypeAdapterFactory.deserialize(json, o.getClass(), gson);
        gson = buildGson(_o);
        String json2 = gson.toJson(_o);
        assert(json2.equals(json));


        TwoDWrap x = new TwoDWrap();
        gson = buildGson(x);
        json = gson.toJson(x);

        TwoDWrap _x = gson.fromJson(json, x.getClass());
        assert(_x.toString().equals(x.toString()));

        List<Object> l = new ArrayList();
        l.add(new double[][] {{}, {}});
        l.add(new double[][] {{1.1}, {2.2}});
        l.add(new Object[] {'c', 1});
        gson = buildGson(l);
        json = gson.toJson(l);

        List<Object> _l  = UniversalTypeAdapterFactory.deserialize(json, l.getClass(), gson);
        gson = buildGson(_l);
        String testJson = gson.toJson(_l);
        assert(testJson.equals(json));

        System.out.println("Nested Collection/obj with Array passed");
    }

    public static void StringBuilderTest() throws ClassNotFoundException, InterruptedException{
        StringBuilder sb = new StringBuilder();
        sb.append("hello world");
        sb.append("- kappa Pride");
        Gson gson = null;
        String json = "";
        gson = buildGson(sb);
        json = gson.toJson(sb);
        StringBuilder _sb = gson.fromJson(json, StringBuilder.class);
        _sb.append("what");
        System.out.println("StringBuilder test passed");
    }


    public static void GregorianCalendarTest() {
        Calendar cal = Calendar.getInstance();
        Gson gson = buildGson(cal);
        String json = gson.toJson(cal);
        Calendar _cal = gson.fromJson(json, cal.getClass());
        assert(cal.getTime().toString().equals(_cal.getTime().toString()));
    }

    public static void CycleDecimalFormat() {
        DecFormWrap d = new DecFormWrap();
        d.next = new DecFormWrap();
        d.next.next = d;
        Gson gson = buildGson(d);
        String json = gson.toJson(d);
//        System.out.println(json);
        DecFormWrap _d = gson.fromJson(json, d.getClass());
        assert(_d.d.equals(d.d));
        assert(d.next.next.equals(d));
        assert(d.next.d.equals(d.d));
        System.out.println("Cycle on decimal format passed");
    }

    public static void nullCollection() throws ClassNotFoundException{
        Collection<Number> A = new LinkedList<Number>(Arrays.asList(new Number[]{1.0, 2, Double.NaN, null}));
        Gson gson = buildGson(A);
        String json = gson.toJson(A);
        System.out.println(json);
        Collection<Number> B = UniversalTypeAdapterFactory.deserialize(json, A.getClass(), gson);
        System.out.println(B);
    }
    public static void main(String args[]){
        try {
            testSpecialDoubleValue();
            testDuplicateFields();
            testCyclicDeq();
            testCyclicGraph();
            testArbArray();
            testTree();
            testCollections();
            testMap();
            testDecimalFormat();
            enumTest();
            testDouble();
            NestedCollectionObjectArrayTest();
            StringBuilderTest();
            GregorianCalendarTest();
            CycleDecimalFormat();
            nullCollection();

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
