package me.saharnooby.agent.minecraft.threadpool.detector;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects possibly renamed or obfuscated classes by methods and fields.
 * @author saharNooby
 * @since 18:41 30.09.2020
 */
public abstract class ClassDetector extends ClassVisitor {

	protected int fields;
	protected int methods;
	protected boolean hasNonStaticFields;
	protected boolean hasNonStaticMethods;
	protected final List<String> staticFieldDescriptors = new ArrayList<>();

	public ClassDetector() {
		super(Opcodes.ASM9);
	}

	@Override
	public final FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (!Modifier.isStatic(access)) {
			this.hasNonStaticFields = true;
		} else {
			this.staticFieldDescriptors.add(descriptor);
		}

		this.fields++;

		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public final MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (!Modifier.isStatic(access) && !name.equals("<init>")) {
			this.hasNonStaticMethods = true;
		}

		this.methods++;

		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}

}
