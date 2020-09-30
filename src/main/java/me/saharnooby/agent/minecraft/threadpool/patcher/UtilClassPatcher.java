package me.saharnooby.agent.minecraft.threadpool.patcher;

import lombok.NonNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

/**
 * Inserts call to pool replacing method.
 * @author saharNooby
 * @since 18:56 30.09.2020
 */
public final class UtilClassPatcher extends ClassVisitor {

	public UtilClassPatcher(@NonNull ClassVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);

		if (!Modifier.isPrivate(access)) {
			return visitor;
		}

		String withoutName = "()Ljava/util/concurrent/ExecutorService;";
		String withName = "(Ljava/lang/String;)Ljava/util/concurrent/ExecutorService;";

		if (!descriptor.equals(withoutName) && !descriptor.equals(withName)) {
			return visitor;
		}

		return new MethodVisitor(Opcodes.ASM8, visitor) {

			@Override
			public void visitCode() {
				super.visitCode();

				// Remember pool name for later use
				if (descriptor.equals(withName)) {
					super.visitVarInsn(Opcodes.ALOAD, 0);
				} else {
					super.visitInsn(Opcodes.ACONST_NULL);
				}

				super.visitFieldInsn(
						Opcodes.PUTSTATIC,
						"me/saharnooby/agent/minecraft/threadpool/AgentRuntime",
						"poolName",
						"Ljava/lang/String;"
				);
			}

			@Override
			public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
				super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
			}

			@Override
			public void visitInsn(int opcode) {
				if (opcode == Opcodes.ARETURN) {
					super.visitMethodInsn(
							Opcodes.INVOKESTATIC,
							"me/saharnooby/agent/minecraft/threadpool/AgentRuntime",
							"replacePool",
							"(Ljava/util/concurrent/ExecutorService;)Ljava/util/concurrent/ExecutorService;",
							false
					);

					System.out.println("Inserted call to replacePool into " + name + " " + descriptor);
				}

				super.visitInsn(opcode);
			}

		};
	}

}
