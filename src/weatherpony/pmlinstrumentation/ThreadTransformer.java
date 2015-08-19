package weatherpony.pmlinstrumentation;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import weatherpony.pml.launch.PMLLoadFocuser;

public class ThreadTransformer implements ClassFileTransformer, Opcodes{
	public static boolean saveThreadsForDebug = true;
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protection, byte[] data)throws IllegalClassFormatException {
		Class decider = classBeingRedefined;
		if(classBeingRedefined == null){
			ClassReader cr = new ClassReader(data);
			ClassNode tree = new ClassNode();
			cr.accept(tree, ClassReader.SKIP_DEBUG + ClassReader.SKIP_CODE);
			try{
				decider = loader.loadClass(tree.superName.replace('/', '.'));
			}catch(ClassNotFoundException e){
				System.err.println("PML-ClassLoaderTransformer: unable to figure out super information for "+className+". This is a serious error, and cannot be recovered from.");
				System.exit(3000);
			}
		}
		if(Thread.class.isAssignableFrom(decider)){
			//System.out.println("ThreadTransformer - found Thread: "+className);
			try{
				ClassReader cr = new ClassReader(data);
				ClassNode tree = new ClassNode();
				cr.accept(tree, ClassReader.EXPAND_FRAMES);
				boolean changed = false;
				if(classBeingRedefined != null && classBeingRedefined.equals(Thread.class)){
					for(MethodNode eachMethod : tree.methods){
						if(eachMethod.name.equals("setContextClassLoader") && eachMethod.desc.equals("(Ljava/lang/ClassLoader;)V")){
							MethodNode replacement = new MethodNode(eachMethod.access, eachMethod.name, eachMethod.desc, eachMethod.signature, eachMethod.exceptions.toArray(new String[eachMethod.exceptions.size()]));
							ThreadSetContextClassLoaderTransformer builder = new ThreadSetContextClassLoaderTransformer(replacement, eachMethod.access, eachMethod.name, eachMethod.desc);
							
							eachMethod.accept(builder);
							
							tree.methods.remove(eachMethod);
							tree.methods.add(replacement);
							
							changed = true;
							break;
						}else
							continue;
						
					}
				}
				if(changed){
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);
					tree.accept(cw);
					System.err.println("altered bytecode of "+tree.name+" for modified setContextClassLoader method");
					byte[] ret = cw.toByteArray();
					if(saveThreadsForDebug){
						try {
							File save = new File("PML/savedAlteredClasses", className+".class");
							if(save.exists())
								save.delete();
							save.getParentFile().mkdirs();
							save.createNewFile();
							Files.write(save.toPath(), ret);
							System.out.println("saving generated replacement class bytes for "+className);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					return ret;
				}
			}catch(Throwable e){
				System.out.println("thread transformer error");
				e.printStackTrace();
			}
		}//else
			throw new RuntimeException();//optimization, so that Java doesn't reparse the class
	}
	public static class ThreadSetContextClassLoaderTransformer extends AdviceAdapter implements Opcodes{
		
		protected ThreadSetContextClassLoaderTransformer(MethodVisitor mv, int access, String name, String desc) {
			super(ASM4, mv, access, name, desc);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onMethodEnter(){
			mv.visitTryCatchBlock(l0, l1, l2, "java/lang/ClassNotFoundException");
			mv.visitTryCatchBlock(l0, l1, l3, "java/lang/Exception");
		}
		Label l0 = new Label();
		Label l1 = new Label();
		Label l2 = new Label();
		Label l3 = new Label();
		
		@Override
		protected void onMethodExit(int opcode){
			if(opcode == ATHROW){
				super.onMethodExit(opcode);
				return;
			}
			int v3 = this.newLocal(Type.getType(Class.class));
			int v4 = this.newLocal(Type.getType(Method.class));
			mv.visitLabel(l0);
			mv.visitLineNumber(20, l0);
			mv.visitVarInsn(ALOAD, 1);
			Label l8 = new Label();
			mv.visitJumpInsn(IFNONNULL, l8);
			Label l9 = new Label();
			mv.visitLabel(l9);
			mv.visitLineNumber(21, l9);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
			mv.visitVarInsn(ASTORE, 1);
			mv.visitLabel(l8);
			mv.visitLineNumber(22, l8);
			//mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(PMLLoadFocuser.LoadFocuserClassName);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
			mv.visitVarInsn(ASTORE, v3);
			Label l10 = new Label();
			mv.visitLabel(l10);
			mv.visitLineNumber(23, l10);
			mv.visitVarInsn(ALOAD, v3);
			mv.visitLdcInsn(PMLLoadFocuser.pmlClassLoaderContextClassLoaderChangeMethodName);
			mv.visitInsn(ICONST_2);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitLdcInsn(Type.getType("Ljava/lang/Thread;"));
			mv.visitInsn(AASTORE);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
			mv.visitVarInsn(ASTORE, v4);
			Label l11 = new Label();
			mv.visitLabel(l11);
			mv.visitLineNumber(24, l11);
			mv.visitVarInsn(ALOAD, v4);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "setAccessible", "(Z)V");
			Label l12 = new Label();
			mv.visitLabel(l12);
			mv.visitLineNumber(25, l12);
			mv.visitVarInsn(ALOAD, 4);
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
			mv.visitInsn(POP);
			mv.visitLabel(l1);
			mv.visitLineNumber(26, l1);
			Label l13 = new Label();
			mv.visitJumpInsn(GOTO, l13);
			mv.visitLabel(l2);
			//mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/ClassNotFoundException"});
			mv.visitVarInsn(ASTORE, 3);
			mv.visitJumpInsn(GOTO, l13);
			mv.visitLabel(l3);
			mv.visitLineNumber(28, l3);
			//mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Exception"});
			mv.visitVarInsn(ASTORE, 3);
			Label l14 = new Label();
			mv.visitLabel(l14);
			mv.visitLineNumber(29, l14);
			mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
			mv.visitLdcInsn("PML-Thread_edit: critical error in changing the Context ClassLoader");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
			Label l15 = new Label();
			mv.visitLabel(l15);
			mv.visitLineNumber(30, l15);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "(Ljava/io/PrintStream;)V");
			Label l16 = new Label();
			mv.visitLabel(l16);
			mv.visitLineNumber(31, l16);
			mv.visitIntInsn(SIPUSH, 1000);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "exit", "(I)V");
			mv.visitLabel(l13);
			mv.visitLineNumber(33, l13);
			//mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		}
	}
}
