package com.company;

import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by abelbezu on 6/2/17.
 *
 * This is a word for word copy of the implementation given in section 14.4 of
 * Herlihy, Shavit, "The Art of Multiprocessor Programming"
 */

public class LockFreeSkipList<T> {
    static final int MAX_LEVEL = 100;
    final Node<T> head = new Node<T>(Integer.MIN_VALUE);
    final Node<T> tail = new Node<T>(Integer.MAX_VALUE);

    public LockFreeSkipList() {
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = new AtomicMarkableReference<LockFreeSkipList.Node<T>>(tail, false);
        }
    }

    private int randomLevel(){
        return  (int)Math.random()*MAX_LEVEL;
    }

    boolean add(T x) {
        int topLevel = randomLevel();
        int bottomLevel = 0;
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        while (true) {
            boolean found = find(x, preds, succs);
            if (found) {
                return false;
            } else {
                Node<T> newNode = new Node(x, topLevel);
                for (int level = bottomLevel; level <= topLevel; level++) {
                    Node<T> succ = succs[level];
                    newNode.next[level].set(succ, false);
                }
                Node<T> pred = preds[bottomLevel];
                Node<T> succ = succs[bottomLevel];
                newNode.next[bottomLevel].set(succ, false);
                if (!pred.next[bottomLevel].compareAndSet(succ, newNode,
                        false, false)) {
                    continue;
                }
                for (int level = bottomLevel+1; level <= topLevel; level++) {
                    while (true) {
                        pred = preds[level];
                        succ = succs[level];
                        if (pred.next[level].compareAndSet(succ, newNode, false, false))
                            break;
                        find(x, preds, succs);
                    }
                }
                return true;
            }
        }
    }

    boolean remove(T x) {
        int bottomLevel = 0;
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T> succ;
        while (true) {
            boolean found = find(x, preds, succs);
            if (!found) {
                return false;
            } else {
                Node<T> nodeToRemove = succs[bottomLevel];
                for (int level = nodeToRemove.topLevel;
                     level >= bottomLevel+1; level--) {
                    boolean[] marked = {false};
                    succ = nodeToRemove.next[level].get(marked);
                    while (!marked[0]) {
                        nodeToRemove.next[level].attemptMark(succ, true);
                        succ = nodeToRemove.next[level].get(marked);
                    }
                }
                boolean[] marked = {false};
                succ = nodeToRemove.next[bottomLevel].get(marked);
                while (true) {
                    boolean iMarkedIt =
                            nodeToRemove.next[bottomLevel].compareAndSet(succ, succ,
                                    false, true);
                    succ = succs[bottomLevel].next[bottomLevel].get(marked);
                    if (iMarkedIt) {
                        find(x, preds, succs);
                        return true;
                    }
                    else if (marked[0]) return false;
                }
            }
        }
    }

    boolean find(T x, Node<T>[] preds, Node<T>[] succs) {
        int bottomLevel = 0;
        int key = x.hashCode();
        boolean[] marked = {false};
        boolean snip;
        Node<T> pred = null, curr = null, succ = null;
        retry:
        while (true) {
            pred = head;
            for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
                curr = pred.next[level].getReference();
                while (true) {
                    succ = curr.next[level].get(marked);
                    while (marked[0]) {
                        snip = pred.next[level].compareAndSet(curr, succ,
                                false, false);
                        if (!snip) continue retry;
                        curr = pred.next[level].getReference();
                        succ = curr.next[level].get(marked);
                    }
                    if (curr.key < key){
                        pred = curr; curr = succ;
                    } else {
                        break;
                    }
                }
                preds[level] = pred;
                succs[level] = curr;
            }
            return (curr.key == key);
        }
    }
    
    boolean contains(T x) {
        int bottomLevel = 0;
        int v = x.hashCode();
        boolean[] marked = {false};
        Node<T> pred = head, curr = null, succ = null;
        for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
            curr = pred.next[level].getReference();
            while (true) {
                succ = curr.next[level].get(marked);
                while (marked[0]) {
                    curr = pred.next[level].getReference();
                    succ = curr.next[level].get(marked);
                }
                if (curr.key < v){
                    pred = curr;
                    curr = succ;
                } else {
                    break;
                }
            }
        }
        return (curr.key == v);
    }
    private static final class Node<T> {
        final T value;
        final int key;

        final AtomicMarkableReference<Node<T>>[] next;
        private int topLevel;
        public Node(int key) { // sentinel node constructor
            this.value = null;
            this.key = key;
            this.next = (AtomicMarkableReference<Node<T>>[])new AtomicMarkableReference[MAX_LEVEL + 1];
            this.topLevel = MAX_LEVEL;
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node<T>>(null,false);
            }
        }
        public Node(T x, int height) {
            this.value = null;
            this.key = x.hashCode();
            this.next = (AtomicMarkableReference<Node<T>>[])new AtomicMarkableReference[MAX_LEVEL + 1];
            this.topLevel = MAX_LEVEL;
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node<T>>(null,false);
            }
        }

    }

}
