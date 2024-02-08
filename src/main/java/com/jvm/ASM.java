package com.jvm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.FileOutputStream;
import java.io.IOException;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class ASM {

    public static void main(String[] args) {

        //asm1();

        asm2();

    }

    final static ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

    private static void asm1() {

        writer.visit(V17, ACC_PUBLIC,"com/test/Main",null, "java/lang/Object",null);

        MethodVisitor visitor = writer.visitMethod(ACC_PUBLIC, "<init>","()V", null,null);

        visitor.visitCode();
        Label l1 = new Label(); visitor.visitLabel(l1); visitor.visitLineNumber(11, l1);
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object","<init>","()V", false);
        visitor.visitInsn(RETURN);

        Label l2 = new Label(); visitor.visitLabel(l2);
        visitor.visitLocalVariable("this","Lcom/test/Main;",null, l1,l2,0);
        visitor.visitMaxs(1, 1); writer.visitEnd();

        try (FileOutputStream stream = new FileOutputStream("./Main.class")) {
            stream.write(writer.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void asm2() {

        writer.visit(V17, ACC_PUBLIC,"com/test/Main",null, "java/lang/Object",null);

        MethodVisitor visitor = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);

        visitor.visitCode();
        Label l1 = new Label(); visitor.visitLabel(l1); visitor.visitLineNumber(13, l1);
        visitor.visitIntInsn(BIPUSH, 10); visitor.visitIntInsn(ISTORE, 1);
        Label l2 = new Label(); visitor.visitLabel(l2); visitor.visitLineNumber(14, l2);

        visitor.visitFieldInsn(GETSTATIC, "java/lang/System","out","Ljava/io/PrintStream;");
        visitor.visitVarInsn(ILOAD, 1);
        visitor.visitMethodInsn(INVOKESPECIAL, "java/io/PrintStream","println","(I)V", false);
        Label l3 = new Label(); visitor.visitLabel(l3); visitor.visitLineNumber(15, l3);
        visitor.visitInsn(RETURN);

        Label l4 = new Label(); visitor.visitLabel(l4);
        visitor.visitLocalVariable("args","[Ljava/lang/String;", null, l1,l4,0);
        visitor.visitLocalVariable("a", "I", null, l2,l4,1);
        visitor.visitMaxs(1, 2); visitor.visitEnd();

        try (FileOutputStream stream = new FileOutputStream("./Main.class")) {
            stream.write(writer.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
