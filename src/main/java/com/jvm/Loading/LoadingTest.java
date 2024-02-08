package com.jvm.Loading;

import java.io.FileInputStream;
import java.io.IOException;

public class LoadingTest {

    public static void main(String[] args) throws ReflectiveOperationException {

        Class<?> testClass1 = LoadingTest.class.getClassLoader().loadClass("com.jvm.Loading.Clazz");
        CustomClassLoader customClassLoader = new CustomClassLoader();
        Class<?> testClass2 = customClassLoader.loadClass("com.jvm.Loading.Clazz");

        //System.out.println(testClass1.getClassLoader()); System.out.println(testClass2.getClassLoader());

        //System.out.println(testClass1); System.out.println(testClass2);

        //System.out.println(testClass1 == testClass2);

        Clazz clazz = (Clazz) testClass2.getDeclaredConstructor().newInstance();

    }

    private static class CustomClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {

            try (FileInputStream stream = new FileInputStream("./target/classes/" + name.replace(".", "/") + ".class")) {
                byte[] data = new byte[stream.available()]; stream.read(data);
                if (data.length == 0) return super.loadClass(name);
                return defineClass(name, data, 0, data.length);
            } catch (IOException e) {
                return super.loadClass(name);
            }

        }

    }

}
