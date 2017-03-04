package TestGenerator.Utility;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;

/**
 * Created by NatchaS on 2/18/17.
 */
public class RandomizedQueue<E> implements Queue<E>{
    private E[] elements;
    private int nextIdx;
    private Class<E> clazz;

    public RandomizedQueue(int initialSize){
        @SuppressWarnings("unchecked")
        final E[] e = (E[]) new Object[initialSize];
        this.elements = e;
        this.nextIdx = 0;
        this.clazz= clazz;
    }

    public RandomizedQueue(Collection<E> fromA){
        this(fromA.size());
        for(E e : fromA){
            this.add(e);
        }
    }

    public int size(){
        return this.nextIdx;
    }

    public boolean offer(E e){
        if (e == null) throw new NullPointerException();
        if (this.nextIdx >= this.elements.length){
            @SuppressWarnings("unchecked")
            final E[] newElms = (E[]) new Object[nextIdx*2];
            for(int i = 0; i < nextIdx; i++) newElms[i] = this.elements[i];
            this.elements = newElms;
        }
        this.elements[nextIdx] = e;
        this.nextIdx++;
        return true;
    }

    public boolean add(E e){
        return this.offer(e);
    }

    public E peek() {
        if (this.isEmpty()) return null;
        return this.elements[new Random().nextInt(nextIdx)];
    }

    public E element(){
        if (this.isEmpty()) throw new NoSuchElementException("Queue is empty.");
        return peek();
    }

    public E poll(){
        if (this.isEmpty()) return null;
        int randIdx = new Random().nextInt(this.nextIdx);
        E randElm = this.elements[randIdx];
        this.elements[randIdx] = this.elements[nextIdx-1];
        this.elements[nextIdx-1] = null;
        this.nextIdx--;
        return randElm;
    }

    public E remove(){
        if (this.isEmpty()) throw new NoSuchElementException("Qeuue is empty.");
        return poll();
    }

    public boolean contains(Object e){
        if (this.clazz.isAssignableFrom(e.getClass())) {
            for(int i = 0; i < nextIdx; i++){
                if (this.elements[i].equals(e)){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEmpty(){
        return this.nextIdx == 0;
    }

    public void clear(){
        throw new NotImplementedException();
    }

    public <T> T[] toArray(T[] e){
        if (e.length!=this.nextIdx){
            throw new IllegalArgumentException("Array of different length");
        }
        for(int i = 0; i < this.nextIdx; i++){
            e[i] = (T)this.elements[i];
        }
        return e;
    }

    public E[] toArray(){
        return (E[])Arrays.copyOfRange(this.elements, 0, nextIdx);
    }

    @Override
    public String toString(){
        return Arrays.toString(toArray());
    }

    public boolean removeAll(Collection<?> collection){
        throw new NotImplementedException();
    }

    public boolean retainAll(Collection<?> collection){
        throw new NotImplementedException();
    }

    public boolean containsAll(Collection<?> collection){
        throw new NotImplementedException();
    }

    public boolean addAll(Collection<? extends E> collection){
        throw new NotImplementedException();
    }

    public boolean remove(Object o){
        throw new NotImplementedException();
    }

    public Iterator<E> iterator(){
        LinkedList<E> l = new LinkedList<>();
        while(!this.isEmpty()){
            l.add(this.poll());
        }
        return l.iterator();
    }


}
