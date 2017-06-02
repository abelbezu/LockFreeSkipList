package com.company;

class TestThread extends Thread{
    private LockFreeSkipList<Long> skipList;
    public TestThread(LockFreeSkipList skipList){
        this.skipList = skipList;
    }
    public void run(){
        long threadId = this.getId();
        System.out.println("Thread id: " + threadId);
        this.skipList.add(threadId);
        System.out.println("Skip List contains my Id: " + this.skipList.contains(threadId));
        System.out.println("Skip List contains randomNumber: " + this.skipList.contains(threadId  * 29));
    }
}
public class Main {



    public static void main(String[] args) {
	    LockFreeSkipList<Integer> l = new LockFreeSkipList<Integer>();
	    for(int i = 0; i < 5; i ++){
            (new TestThread(l)).start();
        }
    }
}
