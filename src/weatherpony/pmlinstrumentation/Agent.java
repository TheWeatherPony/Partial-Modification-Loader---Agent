package weatherpony.pmlinstrumentation;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.util.ArrayList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public class Agent{
	private static Instrumentation instrumentation = null;
	private static ClassFileTransformer transformer;
	private static ClassFileTransformer transformer2;
	private static ClassFileTransformer transformer3;
	
	public static void agentmain(String s, Instrumentation i){
		System.out.println("PML Agent loaded!");
		boolean devPrerun = System.getProperty("pml.devPrerun") != null;
		if(devPrerun){
			System.out.println("PML Agent noticed devPrerun property. Preparing for Bootstrap class alteration...");
			baseSaveDir = "PML/alteredBootClasses";
		}
		
		wipe();
		
		// initialization code:
		transformer = new ClassLoaderTransformer();
		transformer2 = new ThreadTransformer();
		transformer3 = new MainClassTransformer();
		instrumentation = i;
		
		ArrayList<Class> change = new ArrayList();
		for(Class each : i.getAllLoadedClasses()){
			if(ClassLoader.class.isAssignableFrom(each) ||
					Thread.class.isAssignableFrom(each))
				change.add(each);
		}
		instrumentation.addTransformer(transformer, true);
		//instrumentation.removeTransformer(transformer);//kept, just in case it gets re-transformed
		instrumentation.addTransformer(transformer2, true);
		instrumentation.addTransformer(transformer3, false);
		
		try{
			instrumentation.retransformClasses(change.toArray(new Class[change.size()]));
		}catch(UnmodifiableClassException e){
			System.err.println("PML Agent was unable to retransform classes");
			System.exit(10);
		}
		if(devPrerun){
			System.out.println("PML Agent finished devPrerun operation. Closing.");
			System.exit(0);
		}
	}
	public static void handleError(String error){
		System.err.println(error);
		System.exit(50);
	}
	static String baseSaveDir = "PML/savedAlteredClasses";
	static void wipe(){
		File saveRoot = new File(baseSaveDir);
		if(saveRoot.exists()){
			deleteFileRecursive(saveRoot);
		}
	}
	static void deleteFileRecursive(File delete){
		if(!delete.exists())
			return;
		if(delete.isFile())
			delete.delete();
		else{
			for(File sub : delete.listFiles()){
				sub.delete();
			}
		}
	}
	static void saveClass(String name, byte[] data){
		try {
			File save = new File(baseSaveDir, name+".class");
			if(save.exists())
				save.delete();
			save.getParentFile().mkdirs();
			save.createNewFile();
			Files.write(save.toPath(), data);
			System.out.println("saving generated replacement class bytes for "+name);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}


