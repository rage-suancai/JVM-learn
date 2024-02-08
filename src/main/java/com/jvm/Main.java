package com.jvm;

public class Main {

    private static Test a;

    public static void main(String[] args) throws InterruptedException {

        a = new Test(); a = null;

        System.gc(); Thread.sleep(1000);
        System.out.println(a);

        a = null;

        System.gc(); Thread.sleep(1000);
        System.out.println(a);

    }

    private static class Test {

        @Override
        protected void finalize() {

            System.out.println(this + " 开启了它的救赎之路"); a = this;

            // System.out.println(Thread.currentThread()); a = this;

        }

    }

}