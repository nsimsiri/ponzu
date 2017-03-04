package TestGenerator.UnitTests;

import TestGenerator.Utility.RandomizedQueue;
import org.apache.commons.lang3.Range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by NatchaS on 3/3/17.
 */
public class RandomizedQueueTest {
    public static int size=10;
    public static Queue<Integer> build(){
        Queue<Integer> q = new RandomizedQueue<Integer>(size);
        for(int i = 0; i < size; i++){
            q.add(new Integer(i));
        }
        return q;
    }
    public static void test1(){
        Queue<Integer> q = build();
        assert(!q.isEmpty());
        assert(q.size()==size);
        System.out.println("completed test 1");
    }

    public static void test2(){
        Queue<Integer> q = build();
        Integer[] is = q.toArray(new Integer[size]);
        assert(is.length!=0);
        assert(is.length==size);
        assert(is[0] instanceof Integer);
        assert(is[size-1] instanceof Integer);
        Range<Integer> r = Range.between(0,size);
        for (int i =0 ; i < size;i++){
            assert(r.contains(is[i]));
        }
        System.out.println("completed test 2");
    }

    public static void test3(){
        Queue<Integer> q = build();
        int c = size;
        while(!q.isEmpty()){
            assert(q.size()==c--);
            q.poll();
        }
        assert(q.isEmpty());
        assert(q.size()==0);
        System.out.println("completed test 3");
    }

    public static void test4(){
        // LOW PROBABILITY of failed assertion
        // naive test of randomness
        Queue<Integer> q = build();
        Queue<Integer> q2 = build();
        Queue<Integer> q3 = build();
        final Integer[] is = q.toArray(new Integer[size]);
        Boolean c = IntStream.range(0,size).mapToObj((x)->is[x].equals(q.poll())).reduce((a,b)->a&&b).get();
        Boolean c2 = IntStream.range(0,size).mapToObj((x)->is[x].equals(q2.poll())).reduce((a,b)->a&&b).get();
        Boolean c3 = IntStream.range(0,size).mapToObj((x)->is[x].equals(q3.poll())).reduce((a,b)->a&&b).get();
        assert(!(c&&c2&&c3));
        assert(q.isEmpty());
        assert(q.size()==0);
        assert(q2.isEmpty());
        assert(q2.size()==0);
        assert(q3.isEmpty());
        assert(q3.size()==0);
        System.out.println("completed test 4");
    }

    public static void test5(){
        Queue<Integer> q = build();
        List<Integer> is = IntStream.range(0,size).boxed().collect(Collectors.toList());
        Set<Integer> sset = new HashSet<>(is);
        while(!q.isEmpty()){
            Integer x = q.poll();
            sset.contains(x);
            sset.remove(x);
        }
        assert(sset.isEmpty());
        assert(q.isEmpty());
        System.out.println("completed test 5");
    }

    public static void test6(){
        Queue<Integer> q = build();
        List<Integer> is = new ArrayList(q);
        Integer[] a = q.toArray(new Integer[size]);
        Boolean truth = IntStream.range(0,size).mapToObj((i)->is.get(i).equals(a[i])).reduce((c,b)->c&&b).get();
        assert(truth);
        System.out.println("completed test 6");
    }

    public static void printcheck(){
        Queue<Integer> q = build();
        List<Integer> l = new ArrayList<>();
        while(!q.isEmpty()){
            l.add(q.poll());
        }
        System.out.println(l.toString());
    }

    public static void main(String[] args){
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
//        printcheck();
    }
}
