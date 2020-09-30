package me.saharnooby.agent.minecraft.threadpool.detector;

import org.objectweb.asm.ClassReader;
import lombok.NonNull;

/**
 * Detects net.minecraft.Minecraft class.
 * @author saharNooby
 * @since 18:41 30.09.2020
 */
public final class MinecraftClassDetector extends ClassDetector {

	public static boolean isMinecraftClass(@NonNull ClassReader reader) {
		MinecraftClassDetector detector = new MinecraftClassDetector(reader.getClassName());
		reader.accept(detector, ClassReader.SKIP_CODE);
		return detector.isMinecraftClass();
	}

	private final String className;

	public MinecraftClassDetector(@NonNull String className) {
		this.className = className;
	}

	public boolean isMinecraftClass() {
		// There is Entity class that is also big, but it does not have "instance" field.
		return this.fields >= 80 &&
				this.methods >= 100 &&
				this.staticFieldDescriptors.contains("Lorg/apache/logging/log4j/Logger;") &&
				this.staticFieldDescriptors.contains("L" + this.className + ";");
	}

}
