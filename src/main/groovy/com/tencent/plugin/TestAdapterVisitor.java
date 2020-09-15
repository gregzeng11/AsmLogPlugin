package com.tencent.plugin;


import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.regex.Pattern;

/**
 * created by zyh
 * on 2020/8/21
 */
class TestAdapterVisitor extends AdviceAdapter {
    private MethodHookConfig config;

    private boolean isInjected = false;

    private int startTimeId;

    private int methodId;

    private String className;

    private String methodName;

    private String desc;

    private String actionName = "";

    private int logLevel = 2;

    private boolean isStaticMethod;

    private Type[] argumentArrays;
    private Type returnType;

    TestAdapterVisitor(int api, MethodVisitor mv, int access, String className, String methodName, String desc, boolean isInjected, MethodHookConfig config) {
        super(api, mv, access, methodName, desc);
        System.out.println("TestAdapterVisitor : className : " + className + " methodName:" + methodName);
        this.className = className;
        this.methodName = methodName;
        this.desc = desc;
        argumentArrays = Type.getArgumentTypes(desc);
        returnType = Type.getReturnType(desc);
        this.isInjected = isInjected;
        isStaticMethod = ((access & Opcodes.ACC_STATIC) != 0);
        this.config = config;
    }


    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        System.out.println("TestAdapterVisitor : visitAnnotation : " + desc);

        isInjected = isInject(desc);
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        return new AnnotationVisitor(Opcodes.ASM6, av) {
            @Override
            public void visit(String name, Object value) {
                super.visit(name, value);
                if (name.equals("actionName")) {
                    actionName = (String) value;
                } else if (name.equals("logLevel")) {
                    logLevel = Integer.parseInt(value.toString());
                }
                System.out.println("AnnotationVisitor visit name:" + name + " value:" + value);
            }

            @Override
            public void visitEnum(String name, String descriptor, String value) {
                super.visitEnum(name, descriptor, value);
            }
        };
    }

    private boolean isInject(String descAnnotation) {

        for (String classAnnotation : config.getClassAnnotations()) {
            if (Pattern.matches(classAnnotation, descAnnotation)) {
                return true;
            }
        }

        for (String methodAnnotation : config.getMethodAnnotations()) {
            if (Pattern.matches(methodAnnotation, descAnnotation)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onMethodEnter() {
        if (isInjected) {
            startTimeId = newLocal(Type.LONG_TYPE);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            mv.visitIntInsn(LSTORE, startTimeId);
        }
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (isInjected) {
            if (opcode == RETURN) {
                visitInsn(ACONST_NULL);
            } else if (opcode == ARETURN || opcode == ATHROW) {
                dup();
            } else {
                if (opcode == LRETURN || opcode == DRETURN) {
                    dup2();
                } else {
                    dup();
                }
                box(Type.getReturnType(this.methodDesc));
            }

            mv.visitLdcInsn(returnType.getClassName());
            mv.visitLdcInsn(className);
            mv.visitLdcInsn(methodName);
            mv.visitLdcInsn(actionName);
            mv.visitLdcInsn(logLevel);
            mv.visitLdcInsn(desc);
            mv.visitLdcInsn(getArgsType());

            getICONST(argumentArrays == null ? 0 : argumentArrays.length);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            if (argumentArrays != null) {
                int valLen = 0;
                for (int i = 0; i < argumentArrays.length; i++) {
                    mv.visitInsn(DUP);
                    getICONST(i);
                    getOpCodeLoad(argumentArrays[i], isStaticMethod ? (valLen) : (valLen + 1));
                    mv.visitInsn(AASTORE);
                    valLen += getlenByType(argumentArrays[i]);
                }
            }
            mv.visitVarInsn(LLOAD, startTimeId);
            mv.visitMethodInsn(INVOKESTATIC, "com/tencent/teduboard/track/ASMLog", "trackMethod",
                    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;" +
                            "Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;[Ljava/lang/Object;J)V", false);
        }
    }

    String getArgsType() {
        if (argumentArrays == null)
            return "null";

        int iMax = argumentArrays.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(String.valueOf(argumentArrays[i].getClassName()));
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    public void getOpCodeLoad(Type type, int argIndex) {
        if (type.equals(Type.INT_TYPE)) {
            mv.visitVarInsn(ILOAD, argIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            return;
        }
        if (type.equals(Type.BOOLEAN_TYPE)) {
            mv.visitVarInsn(ILOAD, argIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            return;
        }
        if (type.equals(Type.CHAR_TYPE)) {
            mv.visitVarInsn(ILOAD, argIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            return;
        }
        if (type.equals(Type.SHORT_TYPE)) {
            mv.visitVarInsn(ILOAD, argIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            return;
        }
        if (type.equals(Type.BYTE_TYPE)) {
            mv.visitVarInsn(ILOAD, argIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            return;
        }

        if (type.equals(Type.LONG_TYPE)) {
            mv.visitVarInsn(LLOAD, argIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            return;
        }
        if (type.equals(Type.FLOAT_TYPE)) {
            mv.visitVarInsn(FLOAD, argIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            return;
        }
        if (type.equals(Type.DOUBLE_TYPE)) {
            mv.visitVarInsn(DLOAD, argIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            return;
        }
        mv.visitVarInsn(ALOAD, argIndex);
    }

    public void getICONST(int i) {
        if (i == 0) {
            mv.visitInsn(ICONST_0);
        } else if (i == 1) {
            mv.visitInsn(ICONST_1);
        } else if (i == 2) {
            mv.visitInsn(ICONST_2);
        } else if (i == 3) {
            mv.visitInsn(ICONST_3);
        } else if (i == 4) {
            mv.visitInsn(ICONST_4);
        } else if (i == 5) {
            mv.visitInsn(ICONST_5);
        } else {
            mv.visitIntInsn(BIPUSH, i);
        }
    }

    public int getlenByType(Type type) {
        if (type.equals(Type.DOUBLE_TYPE)
                || type.equals(Type.LONG_TYPE)) {
            return 2;
        }
        return 1;
    }
}
