package me.saharnooby.agent.minecraft.threadpool.patcher;

import lombok.NonNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

/**
 * Inserts calls to profiler.
 * @author saharNooby
 * @since 18:56 30.09.2020
 */
public final class MinecraftClassPatcher extends ClassVisitor {

	public MinecraftClassPatcher(@NonNull ClassVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);

		if (name.equals("<init>")) {
			return new MethodVisitor(Opcodes.ASM8, visitor) {

				@Override
				public void visitCode() {
					super.visitCode();
					super.visitMethodInsn(
							Opcodes.INVOKESTATIC,
							"me/saharnooby/agent/minecraft/threadpool/AgentRuntime",
							"startProfiling",
							"()V",
							false
					);

					System.out.println("Inserted call to startProfiling into " + name + " " + descriptor);
				}
			};
		}

		// This is the body of the lambda that is called when resource manager has been reloaded.
		// It must contain if* opcode because it checks whether the missing resource check is needed.
		if (!Modifier.isStatic(access) && Modifier.isPrivate(access) && isSynthetic(access) && descriptor.equals("()V")) {
			return new MethodVisitor(Opcodes.ASM8, visitor) {

				private boolean hasConditionalJump;

				@Override
				public void visitInsn(int opcode) {
					if (opcode == Opcodes.RETURN && this.hasConditionalJump) {
						super.visitMethodInsn(
								Opcodes.INVOKESTATIC,
								"me/saharnooby/agent/minecraft/threadpool/AgentRuntime",
								"endProfiling",
								"()V",
								false
						);

						System.out.println("Inserted call to endProfiling into " + name + " " + descriptor);
					}

					super.visitInsn(opcode);
				}

				@Override
				public void visitJumpInsn(int opcode, Label label) {
					if (opcode != Opcodes.GOTO) {
						this.hasConditionalJump = true;
					}

					super.visitJumpInsn(opcode, label);
				}

			};
		}

		return visitor;
	}

	private static boolean isSynthetic(int access) {
		return (access & 0x1000) != 0;
	}

}
