# minecraft-thread-pool-agent

This is a Java agent that allows to change sizes of Minecraft thread pools, both on client and server, modified or vanilla.

Theoretically, it can work on vanilla, Spigot, Forge and Fabric (PaperSpigot already has an option for this: `Paper.WorkerThreadCount`).

Specifically, 1.14.4, 1.16.2, 1.16.3 and 1.16.2 OptiFine clients were tested.

1.13 and older version do not require this optimization.

## Why to use

- To fix 100% CPU load when client or server starts.
- To decrease client or sever startup time.

This is immensely useful when starting multiple clients and servers locally (for development purposes).

You may not see improvements if you have a powerful PC or running only single client instance.

This agent does not (hopefully) affect in-game performance and does not try to improve it.

## Usage

- Get a built agent JAR.
- Add JVM arguments into your launcher or server startup script: `-DminecraftThreadPoolSize=2 -DminecraftBootstrapThreadPoolSize=1 -DminecraftMainThreadPoolSize=2 -javaagent:minecraft-thread-pool-agent-1.0-SNAPSHOT-shaded.jar`
- Launch the client or server.

## Options

These are system properties passed as `-Doption=value` JVM arguments.

Default vanilla pool size is `clamp(processor_count - 1, 1, 7)`. `processor_count` means thread count, if you have hyper threading, or physical core count.

For vanilla 1.16.3 (on 4 cores/8 threads, Java 11) main pool size = 2 and bootstrap pool size = 3 works well (6 sec start vs 11 sec start 100% CPU load during 15 sec).

- `minecraftThreadPoolSize` (all versions): changes size of all pools. Parameters below will overwrite this value.
- `minecraftMainThreadPoolSize` (1.16+): changes main thread pool size (used for loading resources).
- `minecraftBootstrapThreadPoolSize` (1.16+): changed bootstrap thread pool size (used for rewriting and optimizing DataFixer types).

## How it works

It detects code that creates thread pools (located in `net.minecraft.Util`) and inserts a call to pool replacer method.

Additionally, on client it will print time taken to start client:

`[STDOUT]: Done initial reload of resource manager, 11.60 sec passed since start`