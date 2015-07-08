package weatherpony.pmlinstrumentation;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import weatherpony.pml.launch.PMLLoadFocuser;

public class ClassLoaderTransformer implements ClassFileTransformer, Opcodes{
	
	private static final String defineClassDesc = "(Ljava/lang/String;[BIILjava/security/ProtectionDomain;)Ljava/lang/Class;";
	private static final String findClassDesc = "(Ljava/lang/String;)Ljava/lang/Class;";
	private static final String loadClassDesc = "(Ljava/lang/String;Z)Ljava/lang/Class;";
	
	
	private static final boolean saveClassLoadersForDebug = true;
	private static boolean testingcode = false;
	
	
	//the following commented method is here to help with writing the ASM code 
	/*public Class defineClass(String name, byte[] dat, int off, int len, ProtectionDomain domain){
		if(testingCode){
			if(dat == null)
				throw new RuntimeException("PML: the class' bytecode was null");
			if(name == null)
				throw new RuntimeException("PML: the class' name was null");
		}
		try{
			Class pmlLoadFocuser = Class.forName(PMLLoadFocuser.LoadFocuserClassName);
			Method edit = pmlLoadFocuser.getDeclaredMethod(PMLLoadFocuser.pmlClassTransformationMethodName, ClassLoader.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
			Object ret = edit.invoke(this, name, dat, off, len, domain);
			if(ret == null && dat != null){
				throw new RuntimeException("PML: PML has royally messed up");
			}
			dat = (byte[])ret;
			off = 0;
			len = dat.length;
		}catch(ClassNotFoundException e){
		}catch(Throwable e){
			if(e instanceof RuntimeException)
				throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
			
		return Class.class;
	}*/
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		Class decider = classBeingRedefined;
		if(classBeingRedefined == null){
			ClassReader cr = new ClassReader(classfileBuffer);
			ClassNode tree = new ClassNode();
			cr.accept(tree, ClassReader.SKIP_DEBUG + ClassReader.SKIP_CODE);
			try{
				decider = loader.loadClass(tree.superName.replace('/', '.'));
			}catch(ClassNotFoundException e){
				System.err.println("PML-ClassLoaderTransformer: unable to figure out super information for "+className+". This is a serious error, and cannot be recovered from.");
				System.exit(3000);
			}
		}
		boolean altered = false;
		if(classBeingRedefined != null && className != null){
			try{
				if(className.equals("java/lang/ClassLoader")){
					ClassReader cr = new ClassReader(classfileBuffer);
					ClassNode tree = new ClassNode();
					cr.accept(tree, 0);
					boolean found = false;
					for(MethodNode method : tree.methods){//I is lazy right now
						if(!method.name.equals("defineClass"))
							continue;
						if(!method.desc.equals(defineClassDesc))
							continue;
						//String name, byte[] b, int off, int len,ProtectionDomain protectionDomain
						found = true;
						
						InsnList old = method.instructions;
						method.instructions = new InsnList();
						MethodVisitor mv = method;
						mv.visitCode();
						Label l0 = new Label();
						Label l1 = new Label();
						Label l2 = new Label();
						mv.visitTryCatchBlock(l0, l1, l2, "java/lang/ClassNotFoundException");
						Label l3 = new Label();
						mv.visitTryCatchBlock(l0, l1, l3, "java/lang/Throwable");
						Label l4 = new Label();
						mv.visitLabel(l4);
						mv.visitLineNumber(26, l4);
						mv.visitInsn(testingcode ? ICONST_1 : ICONST_0);
						mv.visitVarInsn(ISTORE, 6);
						Label l5 = new Label();
						mv.visitLabel(l5);
						mv.visitLineNumber(27, l5);
						mv.visitVarInsn(ILOAD, 6);
						mv.visitJumpInsn(IFEQ, l0);
						Label l6 = new Label();
						mv.visitLabel(l6);
						mv.visitLineNumber(28, l6);
						mv.visitVarInsn(ALOAD, 2);
						Label l7 = new Label();
						mv.visitJumpInsn(IFNONNULL, l7);
						Label l8 = new Label();
						mv.visitLabel(l8);
						mv.visitLineNumber(29, l8);
						mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
						mv.visitInsn(DUP);
						mv.visitLdcInsn("PML: the class' bytecode was null");
						mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V");
						mv.visitInsn(ATHROW);
						mv.visitLabel(l7);
						mv.visitLineNumber(30, l7);
						mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {Opcodes.INTEGER}, 0, null);
						mv.visitVarInsn(ALOAD, 1);
						mv.visitJumpInsn(IFNONNULL, l0);
						Label l9 = new Label();
						mv.visitLabel(l9);
						mv.visitLineNumber(31, l9);
						mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
						mv.visitInsn(DUP);
						mv.visitLdcInsn("PML: the class' name was null");
						mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V");
						mv.visitInsn(ATHROW);
						mv.visitLabel(l0);
						mv.visitLineNumber(34, l0);
						mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
						mv.visitLdcInsn(PMLLoadFocuser.LoadFocuserClassName);
						mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
						mv.visitVarInsn(ASTORE, 7);
						Label l10 = new Label();
						mv.visitLabel(l10);
						mv.visitLineNumber(35, l10);
						mv.visitVarInsn(ALOAD, 7);
						mv.visitLdcInsn(PMLLoadFocuser.pmlClassTransformationMethodName);
						mv.visitIntInsn(BIPUSH, 6);
						mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
						mv.visitInsn(DUP);
						mv.visitInsn(ICONST_0);
						mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
						mv.visitInsn(AASTORE);
						mv.visitInsn(DUP);
						mv.visitInsn(ICONST_1);
						mv.visitLdcInsn(Type.getType("Ljava/lang/String;"));
						mv.visitInsn(AASTORE);
						mv.visitInsn(DUP);
						mv.visitInsn(ICONST_2);
						mv.visitLdcInsn(Type.getType("[B"));
						mv.visitInsn(AASTORE);
						mv.visitInsn(DUP);
						mv.visitInsn(ICONST_3);
						mv.visitFieldInsn(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
						mv.visitInsn(AASTORE);
						mv.visitInsn(DUP);
						mv.visitInsn(ICONST_4);
						mv.visitFieldInsn(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
						mv.visitInsn(AASTORE);
						mv.visitInsn(DUP);
						mv.visitInsn(ICONST_5);
						mv.visitLdcInsn(Type.getType("Ljava/security/ProtectionDomain;"));
						mv.visitInsn(AASTORE);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
						mv.visitVarInsn(ASTORE, 8);
						Label l11 = new Label();
						mv.visitLabel(l11);
						mv.visitLineNumber(36, l11);
						mv.visitVarInsn(ALOAD, 8);
						mv.visitVarInsn(ALOAD, 0);
						mv.visitInsn(ICONST_5);
						mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
						mv.visitInsn(DUP);
						mv.visitInsn(ICONST_0);
						mv.visitVarInsn(ALOAD, 1);
						mv.visitInsn(AASTORE);
						mv.visitInsn(DUP);
						mv.visitInsn(ICONST_1);
						mv.visitVarInsn(ALOAD, 2);
						mv.visitInsn(AASTORE);
						mv.visitInsn(DUP);
						mv.visitInsn(ICONST_2);
						mv.visitVarInsn(ILOAD, 3);
						mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
						mv.visitInsn(AASTORE);
						mv.visitInsn(DUP);
						mv.visitInsn(ICONST_3);
						mv.visitVarInsn(ILOAD, 4);
						mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
						mv.visitInsn(AASTORE);
						mv.visitInsn(DUP);
						mv.visitInsn(ICONST_4);
						mv.visitVarInsn(ALOAD, 5);
						mv.visitInsn(AASTORE);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
						mv.visitVarInsn(ASTORE, 9);
						Label l12 = new Label();
						mv.visitLabel(l12);
						mv.visitLineNumber(37, l12);
						mv.visitVarInsn(ALOAD, 9);
						Label l13 = new Label();
						mv.visitJumpInsn(IFNONNULL, l13);
						mv.visitVarInsn(ALOAD, 2);
						mv.visitJumpInsn(IFNULL, l13);
						Label l14 = new Label();
						mv.visitLabel(l14);
						mv.visitLineNumber(38, l14);
						mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
						mv.visitInsn(DUP);
						mv.visitLdcInsn("PML: PML has royally messed up");
						mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V");
						mv.visitInsn(ATHROW);
						mv.visitLabel(l13);
						mv.visitLineNumber(40, l13);
						mv.visitFrame(Opcodes.F_APPEND,3, new Object[] {"java/lang/Class", "java/lang/reflect/Method", "java/lang/Object"}, 0, null);
						mv.visitVarInsn(ALOAD, 9);
						mv.visitTypeInsn(CHECKCAST, "[B");
						mv.visitVarInsn(ASTORE, 2);
						Label l15 = new Label();
						mv.visitLabel(l15);
						mv.visitLineNumber(41, l15);
						mv.visitInsn(ICONST_0);
						mv.visitVarInsn(ISTORE, 3);
						Label l16 = new Label();
						mv.visitLabel(l16);
						mv.visitLineNumber(42, l16);
						mv.visitVarInsn(ALOAD, 2);
						mv.visitInsn(ARRAYLENGTH);
						mv.visitVarInsn(ISTORE, 4);
						mv.visitLabel(l1);
						mv.visitLineNumber(43, l1);
						Label l17 = new Label();
						mv.visitJumpInsn(GOTO, l17);
						mv.visitLabel(l2);
						mv.visitFrame(Opcodes.F_FULL, 7, new Object[] {tree.name, "java/lang/String", "[B", Opcodes.INTEGER, Opcodes.INTEGER, "java/security/ProtectionDomain", Opcodes.INTEGER}, 1, new Object[] {"java/lang/ClassNotFoundException"});
						mv.visitVarInsn(ASTORE, 7);
						mv.visitJumpInsn(GOTO, l17);
						mv.visitLabel(l3);
						mv.visitLineNumber(44, l3);
						mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Throwable"});
						mv.visitVarInsn(ASTORE, 7);
						Label l18 = new Label();
						mv.visitLabel(l18);
						mv.visitLineNumber(45, l18);
						mv.visitVarInsn(ALOAD, 7);
						mv.visitTypeInsn(INSTANCEOF, "java/lang/RuntimeException");
						Label l19 = new Label();
						mv.visitJumpInsn(IFEQ, l19);
						Label l20 = new Label();
						mv.visitLabel(l20);
						mv.visitLineNumber(46, l20);
						mv.visitVarInsn(ALOAD, 7);
						mv.visitTypeInsn(CHECKCAST, "java/lang/RuntimeException");
						mv.visitInsn(ATHROW);
						mv.visitLabel(l19);
						mv.visitLineNumber(47, l19);
						mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {"java/lang/Throwable"}, 0, null);
						mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
						mv.visitInsn(DUP);
						mv.visitVarInsn(ALOAD, 7);
						mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
						mv.visitInsn(ATHROW);
						mv.visitLabel(l17);
						mv.visitLineNumber(50, l17);
						mv.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
						method.instructions.add(old);
					}
					if(!found){
						RuntimeException throwing = new RuntimeException();
						throwing.printStackTrace();
						System.exit(10000);
					}
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);
					tree.accept(cw);
					classfileBuffer = cw.toByteArray();
					altered = true;
				}
			}catch(Throwable e){
				e.printStackTrace();
				System.exit(2000);
			}
		}
		if(ClassLoader.class.isAssignableFrom(decider)){
			ClassReader cr = new ClassReader(classfileBuffer);
			ClassNode tree = new ClassNode();
			cr.accept(tree, ClassReader.EXPAND_FRAMES);
			boolean methodfound = false;
			for(MethodNode method : tree.methods){//I is lazy right now
				if(!method.name.equals("loadClass"))
					continue;
				if(!method.desc.equals(loadClassDesc))
					continue;
				try{
				MethodNode replacement = new MethodNode(method.access, method.name, method.desc, method.signature, method.exceptions.toArray(new String[method.exceptions.size()]));
				ClassLoaderLoadClassMethodVisitor changer = new ClassLoaderLoadClassMethodVisitor(replacement, tree.name);
				LocalVariablesSorter sorter = new LocalVariablesSorter(method.access, method.desc, changer);
				changer.sorter = sorter;
				method.accept(sorter);
				tree.methods.remove(method);
				tree.methods.add(replacement);
				methodfound = true;
				}catch(Throwable e){
					e.printStackTrace();
				}
				break;
			}
			for(MethodNode method : tree.methods){
				if(!method.name.equals("findClass"))
					continue;
				if(!method.desc.equals(findClassDesc))
					continue;
				try{
				MethodNode replacement = new MethodNode(method.access, method.name, method.desc, method.signature, method.exceptions.toArray(new String[method.exceptions.size()]));
				ClassLoaderFindClassMethodVisitor changer = new ClassLoaderFindClassMethodVisitor(replacement, tree.name);
				LocalVariablesSorter sorter = new LocalVariablesSorter(method.access, method.desc, changer);
				changer.sorter = sorter;
				method.accept(sorter);
				tree.methods.remove(method);
				tree.methods.add(replacement);
				methodfound = true;
				}catch(Throwable e){
					e.printStackTrace();
				}
				break;
			}
			List<MethodNode> add = new ArrayList();
			List<MethodNode> minus = new ArrayList();
			for(MethodNode method : tree.methods){
				try{
				if(!method.name.equals("<init>"))//initializer - constructor
					continue;
				MethodNode replacement = new MethodNode(method.access, method.name, method.desc, method.signature, method.exceptions.toArray(new String[method.exceptions.size()]));
				ClassLoaderConstructionMethodVisitor changer = new ClassLoaderConstructionMethodVisitor(replacement, method.access, method.name, method.desc, className.replace('/','.'));
				method.accept(changer);
				minus.add(method);
				add.add(replacement);
				methodfound = true;
				}catch(Throwable e){
					e.printStackTrace();	
				}
			}
			tree.methods.removeAll(minus);
			tree.methods.addAll(add);
			{
				try{
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);
				//ClassVisitor checker = new CheckClassAdapter(cw);
				tree.accept(cw);
				classfileBuffer = cw.toByteArray();
				altered = true;
				
				}catch(Throwable e){
					e.printStackTrace();
				}
			}
		}
		if(altered){
			if(saveClassLoadersForDebug){
				try {
					File save = new File("PML/savedAlteredClasses", className+".class");
					if(save.exists())
						save.delete();
					save.getParentFile().mkdirs();
					save.createNewFile();
					Files.write(save.toPath(), classfileBuffer);
					System.out.println("saving generated replacement class bytes for "+className);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return classfileBuffer;
		}
		throw new RuntimeException();
	}
	static class ClassLoaderFindClassMethodVisitor extends MethodVisitor implements Opcodes {
		public ClassLoaderFindClassMethodVisitor(MethodVisitor mv, String className){
			super(ASM4, mv);
			this.className = className;
		}
		String className;
		LocalVariablesSorter sorter;
		@Override
		public void visitCode(){
			mv.visitCode();
			
			int p2 = sorter.newLocal(Type.getType(Class.class));
			int p3 = sorter.newLocal(Type.BOOLEAN_TYPE);
			int p4 = sorter.newLocal(Type.getType(Method.class));
			int p5 = sorter.newLocal(Type.getType(InvocationTargetException.class));
			
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, "java/lang/reflect/InvocationTargetException");
			Label l3 = new Label();
			mv.visitTryCatchBlock(l0, l1, l3, "java/lang/Exception");
			Label l4 = new Label();
			Label l5 = new Label();
			Label l6 = new Label();
			mv.visitTryCatchBlock(l4, l5, l6, "java/lang/ClassNotFoundException");
			Label l7 = new Label();
			mv.visitTryCatchBlock(l4, l5, l7, "java/lang/reflect/InvocationTargetException");
			Label l8 = new Label();
			mv.visitTryCatchBlock(l4, l5, l8, "java/lang/RuntimeException");
			Label l9 = new Label();
			mv.visitTryCatchBlock(l4, l5, l9, "java/lang/Exception");
			Label l10 = new Label();
			mv.visitLabel(l10);
			mv.visitLineNumber(25, l10);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, this.className, "findLoadedClass", "(Ljava/lang/String;)Ljava/lang/Class;");
			mv.visitVarInsn(ASTORE, p2);
			Label l11 = new Label();
			mv.visitLabel(l11);
			mv.visitLineNumber(26, l11);
			mv.visitVarInsn(ALOAD, p2);
			Label l12 = new Label();
			mv.visitJumpInsn(IFNULL, l12);
			Label l13 = new Label();
			mv.visitLabel(l13);
			mv.visitLineNumber(27, l13);
			mv.visitVarInsn(ALOAD, p2);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l12);
			mv.visitLineNumber(28, l12);
			mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {"java/lang/Class"}, 0, null);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ISTORE, p3);
			Label l14 = new Label();
			mv.visitLabel(l14);
			mv.visitLineNumber(29, l14);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(PMLLoadFocuser.LoadFocuserClassName);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
			mv.visitJumpInsn(IFEQ, l4);
			Label l15 = new Label();
			mv.visitLabel(l15);
			mv.visitLineNumber(30, l15);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, this.className, "getParent", "()Ljava/lang/ClassLoader;");
			Label l16 = new Label();
			mv.visitJumpInsn(IFNULL, l16);
			mv.visitLabel(l0);
			mv.visitLineNumber(32, l0);
			mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
			mv.visitLdcInsn("findClass");
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitLdcInsn(Type.getType("Ljava/lang/String;"));
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
			mv.visitVarInsn(ASTORE, p4);
			Label l17 = new Label();
			mv.visitLabel(l17);
			mv.visitLineNumber(33, l17);
			mv.visitVarInsn(ALOAD, p4);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "setAccessible", "(Z)V");
			Label l18 = new Label();
			mv.visitLabel(l18);
			mv.visitLineNumber(34, l18);
			mv.visitVarInsn(ALOAD, p4);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, this.className, "getParent", "()Ljava/lang/ClassLoader;");
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
			mv.visitTypeInsn(CHECKCAST, "java/lang/Class");
			mv.visitLabel(l1);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l2);
			mv.visitLineNumber(35, l2);
			mv.visitFrame(Opcodes.F_FULL, 4, new Object[] {this.className, "java/lang/String", "java/lang/Class", Opcodes.INTEGER}, 1, new Object[] {"java/lang/reflect/InvocationTargetException"});
			mv.visitVarInsn(ASTORE, p4);
			Label l19 = new Label();
			mv.visitLabel(l19);
			mv.visitLineNumber(36, l19);
			mv.visitVarInsn(ALOAD, p4);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getCause", "()Ljava/lang/Throwable;");
			mv.visitVarInsn(ASTORE, p5);
			Label l20 = new Label();
			mv.visitLabel(l20);
			mv.visitLineNumber(37, l20);
			mv.visitVarInsn(ALOAD, 5);
			mv.visitTypeInsn(INSTANCEOF, "java/lang/ClassNotFoundException");
			Label l21 = new Label();
			mv.visitJumpInsn(IFEQ, l21);
			Label l22 = new Label();
			mv.visitLabel(l22);
			mv.visitLineNumber(38, l22);
			mv.visitVarInsn(ALOAD, p5);
			mv.visitTypeInsn(CHECKCAST, "java/lang/ClassNotFoundException");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l21);
			mv.visitLineNumber(40, l21);
			mv.visitFrame(Opcodes.F_APPEND,2, new Object[] {"java/lang/reflect/InvocationTargetException", "java/lang/Throwable"}, 0, null);
			mv.visitVarInsn(ALOAD, p5);
			mv.visitTypeInsn(INSTANCEOF, "java/lang/RuntimeException");
			Label l23 = new Label();
			mv.visitJumpInsn(IFEQ, l23);
			Label l24 = new Label();
			mv.visitLabel(l24);
			mv.visitLineNumber(41, l24);
			mv.visitVarInsn(ALOAD, p5);
			mv.visitTypeInsn(CHECKCAST, "java/lang/RuntimeException");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l23);
			mv.visitLineNumber(42, l23);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, p5);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l3);
			mv.visitLineNumber(43, l3);
			mv.visitFrame(Opcodes.F_FULL, 4, new Object[] {this.className, "java/lang/String", "java/lang/Class", Opcodes.INTEGER}, 1, new Object[] {"java/lang/Exception"});
			mv.visitVarInsn(ASTORE, p4);
			Label l25 = new Label();
			mv.visitLabel(l25);
			mv.visitLineNumber(44, l25);
			mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, p4);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l16);
			mv.visitLineNumber(47, l16);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitTypeInsn(NEW, "java/lang/ClassNotFoundException");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassNotFoundException", "<init>", "(Ljava/lang/String;)V");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l4);
			mv.visitLineNumber(50, l4);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitLdcInsn(PMLLoadFocuser.LoadFocuserClassName);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
			mv.visitVarInsn(ASTORE, p4);
			Label l26 = new Label();
			mv.visitLabel(l26);
			mv.visitLineNumber(51, l26);
			mv.visitVarInsn(ALOAD, p4);
			mv.visitLdcInsn(PMLLoadFocuser.pmlClassSearchOverrideMethodName);
			mv.visitInsn(ICONST_2);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
			mv.visitInsn(AASTORE);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitLdcInsn(Type.getType("Ljava/lang/String;"));
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
			mv.visitVarInsn(ASTORE, p5);
			Label l27 = new Label();
			mv.visitLabel(l27);
			mv.visitLineNumber(52, l27);
			mv.visitVarInsn(ALOAD, p5);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ICONST_2);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(AASTORE);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
			mv.visitTypeInsn(CHECKCAST, "java/lang/Class");
			mv.visitVarInsn(ASTORE, p2);
			Label l28 = new Label();
			mv.visitLabel(l28);
			mv.visitLineNumber(53, l28);
			mv.visitVarInsn(ALOAD, p2);
			Label l29 = new Label();
			mv.visitJumpInsn(IFNULL, l29);
			Label l30 = new Label();
			mv.visitLabel(l30);
			mv.visitLineNumber(54, l30);
			mv.visitVarInsn(ALOAD, p2);
			mv.visitLabel(l5);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l6);
			mv.visitLineNumber(55, l6);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/ClassNotFoundException"});
			mv.visitVarInsn(ASTORE, p4);
			mv.visitJumpInsn(GOTO, l29);
			mv.visitLabel(l7);
			mv.visitLineNumber(56, l7);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/reflect/InvocationTargetException"});
			mv.visitVarInsn(ASTORE, p4);
			Label l31 = new Label();
			mv.visitLabel(l31);
			mv.visitLineNumber(57, l31);
			mv.visitVarInsn(ALOAD, p4);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getCause", "()Ljava/lang/Throwable;");
			mv.visitVarInsn(ASTORE, p5);
			Label l32 = new Label();
			mv.visitLabel(l32);
			mv.visitLineNumber(58, l32);
			mv.visitVarInsn(ALOAD, p5);
			mv.visitTypeInsn(INSTANCEOF, "java/lang/RuntimeException");
			Label l33 = new Label();
			mv.visitJumpInsn(IFEQ, l33);
			Label l34 = new Label();
			mv.visitLabel(l34);
			mv.visitLineNumber(59, l34);
			mv.visitVarInsn(ALOAD, p5);
			mv.visitTypeInsn(CHECKCAST, "java/lang/RuntimeException");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l33);
			mv.visitLineNumber(60, l33);
			mv.visitFrame(Opcodes.F_APPEND,2, new Object[] {"java/lang/reflect/InvocationTargetException", "java/lang/Throwable"}, 0, null);
			mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, p5);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l8);
			mv.visitLineNumber(61, l8);
			mv.visitFrame(Opcodes.F_FULL, 4, new Object[] {this.className, "java/lang/String", "java/lang/Class", Opcodes.INTEGER}, 1, new Object[] {"java/lang/RuntimeException"});
			mv.visitVarInsn(ASTORE, p4);
			Label l35 = new Label();
			mv.visitLabel(l35);
			mv.visitLineNumber(62, l35);
			mv.visitVarInsn(ALOAD, p4);
			mv.visitInsn(ATHROW);
			mv.visitLabel(l9);
			mv.visitLineNumber(63, l9);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Exception"});
			mv.visitVarInsn(ASTORE, p4);
			Label l36 = new Label();
			mv.visitLabel(l36);
			mv.visitLineNumber(64, l36);
			mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, p4);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l29);
			mv.visitFrame(Opcodes.F_CHOP,2, null, 0, null);
		}
	};
	static class ClassLoaderLoadClassMethodVisitor extends MethodVisitor implements Opcodes {
		public ClassLoaderLoadClassMethodVisitor(MethodVisitor mv, String className){
			super(ASM4, mv);
			this.className = className;
		}
		String className;
		LocalVariablesSorter sorter;
		@Override
		public void visitCode(){
			
			mv.visitCode();

			int p3 = sorter.newLocal(Type.getType(Class.class));
			int p4 = sorter.newLocal(Type.BOOLEAN_TYPE);
			int p5 = sorter.newLocal(Type.getType(Method.class));
			int p6 = sorter.newLocal(Type.getType(InvocationTargetException.class));

			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, "java/lang/ClassNotFoundException");
			Label l3 = new Label();
			mv.visitTryCatchBlock(l0, l1, l3, "java/lang/reflect/InvocationTargetException");
			Label l4 = new Label();
			mv.visitTryCatchBlock(l0, l1, l4, "java/lang/RuntimeException");
			Label l5 = new Label();
			mv.visitTryCatchBlock(l0, l1, l5, "java/lang/Exception");
			Label l6 = new Label();
			mv.visitLabel(l6);
			mv.visitLineNumber(43, l6);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, this.className, "findLoadedClass", "(Ljava/lang/String;)Ljava/lang/Class;");
			mv.visitVarInsn(ASTORE, p3);
			Label l7 = new Label();
			mv.visitLabel(l7);
			mv.visitLineNumber(44, l7);
			mv.visitVarInsn(ALOAD, p3);
			Label l8 = new Label();
			mv.visitJumpInsn(IFNULL, l8);
			Label l9 = new Label();
			mv.visitLabel(l9);
			mv.visitLineNumber(45, l9);
			mv.visitVarInsn(ALOAD, p3);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l8);
			mv.visitLineNumber(46, l8);
			mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {"java/lang/Class"}, 0, null);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ISTORE, p4);
			Label l10 = new Label();
			mv.visitLabel(l10);
			mv.visitLineNumber(47, l10);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(PMLLoadFocuser.LoadFocuserClassName);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
			mv.visitJumpInsn(IFEQ, l0);
			Label l11 = new Label();
			mv.visitLabel(l11);
			mv.visitLineNumber(48, l11);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, this.className, "getParent", "()Ljava/lang/ClassLoader;");
			Label l12 = new Label();
			mv.visitJumpInsn(IFNULL, l12);
			Label l13 = new Label();
			mv.visitLabel(l13);
			mv.visitLineNumber(49, l13);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, this.className, "getParent", "()Ljava/lang/ClassLoader;");
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
			mv.visitInsn(ARETURN);
			mv.visitLabel(l12);
			mv.visitLineNumber(51, l12);
			mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {Opcodes.INTEGER}, 0, null);
			mv.visitTypeInsn(NEW, "java/lang/ClassNotFoundException");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassNotFoundException", "<init>", "(Ljava/lang/String;)V");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l0);
			mv.visitLineNumber(54, l0);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitLdcInsn(PMLLoadFocuser.LoadFocuserClassName);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
			mv.visitVarInsn(ASTORE, 5);
			Label l14 = new Label();
			mv.visitLabel(l14);
			mv.visitLineNumber(55, l14);
			mv.visitVarInsn(ALOAD, 5);
			mv.visitLdcInsn("huntForClass");
			mv.visitInsn(ICONST_2);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
			mv.visitInsn(AASTORE);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitLdcInsn(Type.getType("Ljava/lang/String;"));
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
			mv.visitVarInsn(ASTORE, p6);
			Label l15 = new Label();
			mv.visitLabel(l15);
			mv.visitLineNumber(56, l15);
			mv.visitVarInsn(ALOAD, p6);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ICONST_2);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(AASTORE);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
			mv.visitTypeInsn(CHECKCAST, "java/lang/Class");
			mv.visitVarInsn(ASTORE, p3);
			Label l16 = new Label();
			mv.visitLabel(l16);
			mv.visitLineNumber(57, l16);
			mv.visitVarInsn(ALOAD, p3);
			Label l17 = new Label();
			mv.visitJumpInsn(IFNULL, l17);
			Label l18 = new Label();
			mv.visitLabel(l18);
			mv.visitLineNumber(58, l18);
			mv.visitVarInsn(ALOAD, p3);
			mv.visitLabel(l1);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l2);
			mv.visitLineNumber(59, l2);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/ClassNotFoundException"});
			mv.visitVarInsn(ASTORE, p5);
			mv.visitJumpInsn(GOTO, l17);
			mv.visitLabel(l3);
			mv.visitLineNumber(60, l3);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/reflect/InvocationTargetException"});
			mv.visitVarInsn(ASTORE, p5);
			Label l19 = new Label();
			mv.visitLabel(l19);
			mv.visitLineNumber(61, l19);
			mv.visitVarInsn(ALOAD, p5);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getCause", "()Ljava/lang/Throwable;");
			mv.visitVarInsn(ASTORE, p6);
			Label l20 = new Label();
			mv.visitLabel(l20);
			mv.visitLineNumber(62, l20);
			mv.visitVarInsn(ALOAD, p6);
			mv.visitTypeInsn(INSTANCEOF, "java/lang/RuntimeException");
			Label l21 = new Label();
			mv.visitJumpInsn(IFEQ, l21);
			Label l22 = new Label();
			mv.visitLabel(l22);
			mv.visitLineNumber(63, l22);
			mv.visitVarInsn(ALOAD, p6);
			mv.visitTypeInsn(CHECKCAST, "java/lang/RuntimeException");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l21);
			mv.visitLineNumber(64, l21);
			mv.visitFrame(Opcodes.F_APPEND,2, new Object[] {"java/lang/reflect/InvocationTargetException", "java/lang/Throwable"}, 0, null);
			mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, p6);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l4);
			mv.visitLineNumber(65, l4);
			mv.visitFrame(Opcodes.F_FULL, 5, new Object[] {this.className, "java/lang/String", Opcodes.INTEGER, "java/lang/Class", Opcodes.INTEGER}, 1, new Object[] {"java/lang/RuntimeException"});
			mv.visitVarInsn(ASTORE, p5);
			Label l23 = new Label();
			mv.visitLabel(l23);
			mv.visitLineNumber(66, l23);
			mv.visitVarInsn(ALOAD, p5);
			mv.visitInsn(ATHROW);
			mv.visitLabel(l5);
			mv.visitLineNumber(67, l5);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Exception"});
			mv.visitVarInsn(ASTORE, p5);
			Label l24 = new Label();
			mv.visitLabel(l24);
			mv.visitLineNumber(68, l24);
			mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, p5);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l17);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		}
	};
	static class ClassLoaderConstructionMethodVisitor extends AdviceAdapter implements Opcodes {
		public ClassLoaderConstructionMethodVisitor(MethodVisitor mv, int access, String name, String desc, String className){
			super(ASM4, mv, access, name, desc);
			this.className = className;
		}//sun.reflect.DelegatingClassLoader
		String className;
		@Override 
		public void visitCode(){
			super.visitCode();
			mv.visitTryCatchBlock(l0, l1, l2, "java/lang/ClassNotFoundException");
			
			mv.visitTryCatchBlock(l0, l1, l3, "java/lang/reflect/InvocationTargetException");
			
			mv.visitTryCatchBlock(l0, l1, l4, "java/lang/Throwable");
			v1 = this.newLocal(Type.getType(Method.class));
		}
		Label l0 = new Label();
		Label l1 = new Label();
		Label l2 = new Label();
		Label l3 = new Label();
		Label l4 = new Label();
		
		int v1;
		@Override
		protected void onMethodExit(int opcode){
			if(opcode == ATHROW){
				super.onMethodExit(opcode);
				return;
			}
			Label l62 = new Label();
			mv.visitLabel(l62);
			mv.visitLineNumber(23, l62);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;");
			mv.visitLdcInsn("sun.reflect.");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z");
			mv.visitJumpInsn(IFEQ, l0);
			Label l72 = new Label();
			mv.visitLabel(l72);
			mv.visitLineNumber(24, l72);
			mv.visitInsn(RETURN);
			
			mv.visitLabel(l0);
			//mv.visitLineNumber(44, l0);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getContextClassLoader", "()Ljava/lang/ClassLoader;");
			mv.visitLdcInsn(PMLLoadFocuser.LoadFocuserClassName);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
			Label l5 = new Label();
			mv.visitLabel(l5);
			//mv.visitLineNumber(45, l5);
			mv.visitLdcInsn(PMLLoadFocuser.pmlClassLoaderRegistrationMethodName);
			mv.visitInsn(ICONST_3);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
			mv.visitInsn(AASTORE);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitLdcInsn(Type.getType("Ljava/lang/Class;"));
			mv.visitInsn(AASTORE);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_2);
			mv.visitLdcInsn(Type.getType("Ljava/lang/String;"));
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
			Label l6 = new Label();
			mv.visitLabel(l6);
			//mv.visitLineNumber(44, l6);
			mv.visitVarInsn(ASTORE, v1);
			Label l7 = new Label();
			mv.visitLabel(l7);
			//mv.visitLineNumber(46, l7);
			mv.visitVarInsn(ALOAD, v1);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "setAccessible", "(Z)V");
			Label l8 = new Label();
			mv.visitLabel(l8);
			//mv.visitLineNumber(47, l8);
			mv.visitVarInsn(ALOAD, v1);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ICONST_3);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(AASTORE);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitLdcInsn(Type.getType("L"+this.className.replace('.', '/')+";"));
			mv.visitInsn(AASTORE);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_2);
			mv.visitLdcInsn(this.methodDesc);
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
			mv.visitInsn(POP);
			mv.visitLabel(l1);
			//mv.visitLineNumber(48, l1);
			Label l9 = new Label();
			mv.visitJumpInsn(GOTO, l9);
			mv.visitLabel(l2);
			//mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/ClassNotFoundException"});
			mv.visitVarInsn(ASTORE, 1);
			mv.visitJumpInsn(GOTO, l9);
			mv.visitLabel(l3);
			//mv.visitLineNumber(50, l3);
			//mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/reflect/InvocationTargetException"});
			mv.visitVarInsn(ASTORE, 1);
			Label l10 = new Label();
			mv.visitLabel(l10);
			//mv.visitLineNumber(51, l10);
			mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getCause", "()Ljava/lang/Throwable;");
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l4);
			//mv.visitLineNumber(52, l4);
			//mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Throwable"});
			mv.visitVarInsn(ASTORE, 1);
			Label l11 = new Label();
			mv.visitLabel(l11);
			//mv.visitLineNumber(53, l11);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V");
			Label l12 = new Label();
			mv.visitLabel(l12);
			//mv.visitLineNumber(54, l12);
			mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l9);
			//mv.visitLineNumber(56, l9);
			//mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			//mv.visitInsn(RETURN);
			super.onMethodExit(opcode);
			/*Label l11 = new Label();
			mv.visitLabel(l11);
			mv.visitLocalVariable("this", "Lweatherpony/pml/premain/PMLBootClassLoader;", null, l0, l11, 0);
			mv.visitLocalVariable("e", "Ljava/lang/reflect/InvocationTargetException;", null, l8, l4, 1);
			mv.visitLocalVariable("e", "Ljava/lang/Throwable;", null, l9, l7, 1);
			mv.visitMaxs(6, 2);*/
		}
		
	}
}
