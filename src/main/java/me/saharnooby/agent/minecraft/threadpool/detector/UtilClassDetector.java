package me.saharnooby.agent.minecraft.threadpool.detector;

import org.objectweb.asm.ClassReader;
import lombok.NonNull;

/**
 * Detects net.minecraft.Util class.
 * @author saharNooby
 * @since 18:41 30.09.2020
 */
public final class UtilClassDetector extends ClassDetector {

	public static boolean isUtilClass(@NonNull ClassReader reader) {
		UtilClassDetector detector = new UtilClassDetector();
		reader.accept(detector, ClassReader.SKIP_CODE);
		return detector.isUtilClass();
	}

	public boolean isUtilClass() {
		return this.fields >= 4 &&
				this.methods >= 25 &&
				!this.hasNonStaticMethods &&
				!this.hasNonStaticFields &&
				this.staticFieldDescriptors.contains("Lorg/apache/logging/log4j/Logger;") &&
				this.staticFieldDescriptors.contains("Ljava/util/concurrent/ExecutorService;");
	}

}
