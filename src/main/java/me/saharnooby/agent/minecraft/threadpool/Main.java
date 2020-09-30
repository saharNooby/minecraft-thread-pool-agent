package me.saharnooby.agent.minecraft.threadpool;

import lombok.NonNull;
import me.saharnooby.agent.minecraft.threadpool.detector.MinecraftClassDetector;
import me.saharnooby.agent.minecraft.threadpool.detector.UtilClassDetector;
import me.saharnooby.agent.minecraft.threadpool.patcher.MinecraftClassPatcher;
import me.saharnooby.agent.minecraft.threadpool.patcher.UtilClassPatcher;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author saharNooby
 * @since 18:18 30.09.2020
 */
public final class Main {

	private static final AtomicBoolean runtimeClassAdded = new AtomicBoolean();

	public static void premain(String args, Instrumentation inst) {
		inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, bytes) -> {
			try {
				byte[] result = transform(className, bytes);

				if (result != null) {
					if (runtimeClassAdded.compareAndSet(false, true)) {
						// We need to copy AgentRuntime class into Minecraft class loader because
						// classes from agent class loader are not available from launchwrapper classloader
						// which is used by OptiFine.
						defineAgentRuntimeClass(loader);
					}
				}

				return result;
			} catch (Exception e) {
				System.err.println("Failed to process class " + className);
				e.printStackTrace();
				return null;
			}
		}, true);
	}

	private static byte[] transform(String className, byte[] bytes) {
		if (!isGameClass(className)) {
			return null;
		}

		ClassReader reader = new ClassReader(bytes);

		if (UtilClassDetector.isUtilClass(reader)) {
			System.out.println("net.minecraft.Util class name is " + className);

			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			reader.accept(new UtilClassPatcher(writer), 0);
			return writer.toByteArray();
		} else if (MinecraftClassDetector.isMinecraftClass(reader)) {
			System.out.println("net.minecraft.Minecraft class name is " + className);

			ClassWriter writer = new ClassWriter(0);
			reader.accept(new MinecraftClassPatcher(writer), 0);
			return writer.toByteArray();
		}

		return null;
	}

	private static void defineAgentRuntimeClass(@NonNull ClassLoader loader) throws Exception {
		String className = "me.saharnooby.agent.minecraft.threadpool.AgentRuntime";

		InputStream in = Main.class.getClassLoader().getResourceAsStream(className.replace('.', '/') + ".class");

		if (in == null) {
			throw new IllegalStateException("Failed to find " + className + " class file in the agent class loader");
		}

		byte[] bytes = readBytes(in);

		Method m = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(loader, className, bytes, 0, bytes.length);

		System.out.println("Loaded " + className + " class into game class loader " + loader);
	}

	private static boolean isGameClass(String className) {
		// If the package is empty, this is likely an obfuscated class.
		return className != null && (className.indexOf('/') == -1 || className.startsWith("net/minecraft/"));
	}

	private static byte[] readBytes(@NonNull InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int read;

		while ((read = in.read(buf)) != -1) {
			out.write(buf, 0, read);
		}

		return out.toByteArray();
	}

}
