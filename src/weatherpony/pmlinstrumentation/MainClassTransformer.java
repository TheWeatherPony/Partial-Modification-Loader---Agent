package weatherpony.pmlinstrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

import weatherpony.pml.launch.PMLLoadFocuser;

public class MainClassTransformer implements ClassFileTransformer, Opcodes{
	public MainClassTransformer(){
		this.mainClass = tryToFindMainClass_early();
		System.out.println("PML: normal main class found - "+mainClass);
		if(System.getProperty("pml.proxyMain") != null){
			System.setProperty(PMLLoadFocuser.pmlMainClassTransformerSystemProperty, "t");
		}else{
			System.setProperty(PMLLoadFocuser.pmlMainClassTransformerSystemProperty, "f");
		}
	}
	public static String tryToFindMainClass_early(){
		String mainClass = null;
		try{
			String[] params = System.getProperty("sun.java.command").split(" ");
			if(params[0].toLowerCase().endsWith(".jar")){
				JarFile jar = new JarFile(params[0]);
				mainClass = (String) jar.getManifest().getMainAttributes().get("Main-Class");
				jar.close();
			}else{
				mainClass = params[0];
			}
			if(mainClass == null){
				System.err.println("PML MainClassTransformer: \"sun.java.command\" System property doesn't exist");
			}
		}catch(SecurityException e){
			System.err.println("PML MainClassTransformer: \"sun.java.command\" System property not accessable");
		}catch(IOException e){
			System.err.println("PML MainClassTransformer: \"sun.java.command\" System property was a dead-end, since the jar couldn't be opened");
		}
		
		if(mainClass == null){
			System.err.println("PML MainClassTransformer: unable to determine main class - more case-code needed. Critical Error - closing application...");
			System.exit(100);
		}
		return mainClass;
	}
	public String mainClass;
	public boolean alreadyTransformed = false;
	@Override
	public byte[] transform(ClassLoader loader, String name, Class<?> arg2, ProtectionDomain protectiondomain, byte[] data) throws IllegalClassFormatException{
		if(mainClass.equals(name)){
			if(System.getProperty(PMLLoadFocuser.pmlMainClassTransformerSystemProperty).equals("t")){
				System.out.println("PML-MainClassTransformer: detected that the main class was already loaded and altered, so is not altering it as it gets loaded this time");
				throw new RuntimeException();//don't even bother - no changes
			}
		}
		try{
			if(mainClass.equals(name)){
				System.out.println("PML: normal main class loading");
				//transform the class to the moved one
				ClassReader cr = new ClassReader(data);
				ClassWriter cw = new ClassWriter(0);
				
				//generate the replacement main class
				MethodVisitor mv;
	//old class data not flushed - get rid of it and start from scratch
				cw = new ClassWriter(0);
				cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, mainClass.replace('.', '/'), null, "java/lang/Object", null);
	
				{
				mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitLineNumber(3, l0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
				mv.visitInsn(RETURN);
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitLocalVariable("this", "L"+mainClass.replace('.', '/')+";", null, l0, l1, 0);
				mv.visitMaxs(1, 1);
				mv.visitEnd();
				}
				{
				mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitLineNumber(5, l0);
				mv.visitTypeInsn(NEW, "weatherpony/pml/premain/ReplacementMainThread");
				mv.visitInsn(DUP);
				mv.visitLdcInsn(this.mainClass);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESPECIAL, "weatherpony/pml/premain/ReplacementMainThread", "<init>", "(Ljava/lang/String;[Ljava/lang/String;)V");
				mv.visitMethodInsn(INVOKEVIRTUAL, "weatherpony/pml/premain/ReplacementMainThread", "start", "()V");
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitLineNumber(13, l1);
				mv.visitInsn(RETURN);
				Label l2 = new Label();
				mv.visitLabel(l2);
				mv.visitLocalVariable("args", "[Ljava/lang/String;", null, l0, l2, 0);
				mv.visitMaxs(4, 1);
				mv.visitEnd();
				cw.visitEnd();
				}
				System.out.println("PML: main class finished transformation");
				this.alreadyTransformed = true;
				return cw.toByteArray();
			}
		}catch(Throwable e){
			e.printStackTrace();
			System.exit(500);
			throw e;
		}
		throw new RuntimeException();
	}
//mv.visitVarInsn(ALOAD, 0);
//mv.visitMethodInsn(INVOKESTATIC, "weatherpony/pml/launch/PMLRoot", "preload", "([Ljava/lang/String;)V", false);

	
}
