package com.tencent.plugin;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.regex.Pattern;

/**
 * @author gavin
 * @date 2019/2/18
 * lifecycle class visitor
 */
public class LifecycleClassVisitor extends ClassVisitor implements Opcodes {
    private static final String CLASS_ANNOTATION_DESC = "Lcom/tencent/tiw/asm/TiwClassAnnotation;";
    private String mClassName;
    private boolean isInjected;
    private MethodHookConfig config;

    public LifecycleClassVisitor(ClassVisitor cv, MethodHookConfig config) {
        super(Opcodes.ASM6, cv);
        this.config = config;
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        //System.out.println("LifecycleClassVisitor : visit -----> started ：" + name);
        this.mClassName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        System.out.println("LifecycleClassVisitor : visitAnnotation : " + descriptor);
        if (CLASS_ANNOTATION_DESC.equals(descriptor)) {
            isInjected = true;
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access);
        System.out.println("LifecycleClassVisitor : visitInnerClass : " + "name:" + name + " outerName:" + outerName + " innerName:" + innerName);
      /*  HashSet<String> set = new HashSet<>();
        set.add("com/tencent/tiw/asm/TestCallback");
        try {
            ClassReader reader = new ClassReader(name);
            if (SpecifiedInterfaceImplementionChecked.hasImplSpecifiedInterfaces(reader, set)) {
                System.out.println("LifecycleClassVisitor : visitInnerClass TestCallback true");
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                ClassVisitor visitor = new InterfaceVisitor(writer);
                reader.accept(visitor, ClassReader.SKIP_CODE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        System.out.println("LifecycleClassVisitor : visitMethod : " + name + " className:" + this.mClassName);
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        //匹配FragmentActivity
        if (isInject(mClassName)) {
            return new TestAdapterVisitor(Opcodes.ASM6, mv, access, mClassName, name, desc, isInjected, config);
        }
        return mv;
    }

    private boolean isInject(String mClassName) {


        for (String value : config.getClassRegexs()) {
            if (Pattern.matches(value, mClassName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void visitEnd() {
        //System.out.println("LifecycleClassVisitor : visit -----> end");
        super.visitEnd();
    }
}
