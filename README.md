# minecraft-thread-pool-agent

This is a Java agent that allows to change sizes of Minecraft thread pools, both on client and server, modified or vanilla.

Theoretically, it can work on vanilla, Spigot, Forge and Fabric (PaperSpigot already has an option for this: `Paper.WorkerThreadCount`).

Specifically, 1.14.4, 1.16.2, 1.16.3 and 1.16.2 OptiFine clients were tested. However, only 1.16 support is guaranteed because it's the latest version.

1.13 and older versions do not require this optimization -- they are already launching fast and do not consume much CPU.

## Why to use

- To fix 100% CPU load when client or server starts.
- To decrease client or sever startup time.

Reducing CPU load at start time is immensely useful when starting multiple clients and servers locally (for development purposes).

You may not see improvements if you have a powerful PC or running only single client instance.

This agent does not (hopefully) affect in-game performance and does not try to improve it.

### Potential issues

It is currently not known to me what are impilcations of delaying or completely disabling DataFixers Type rewrite. However, I did not notice any problems in multiplayer. Singleplayer was not tested at all, expecially opening and converting old worlds (the ultimate goal of creating DataFixers library).

I didn't check all usages of Main executor, and potentially reducing its thread count may be bad for ingame performance.

## Usage

- Get a built agent JAR (see [releases](https://github.com/saharNooby/minecraft-thread-pool-agent/releases)).
- Add JVM arguments into your launcher or server startup script: `-DminecraftThreadPoolSize=2 -DminecraftBootstrapThreadPoolSize=1 -DminecraftMainThreadPoolSize=2 -javaagent:minecraft-thread-pool-agent-1.0.0-shaded.jar`
- Launch the client or server.

## Options

These are system properties passed as `-Doption=value` JVM arguments.

Default vanilla pool size is `clamp(processor_count - 1, 1, 7)`. `processor_count` means thread count, if you have hyper threading, or physical core count.

In my case (vanilla 1.16.3, i7 7700HQ, 4 cores, 8 threads, OpenJDK 11) main pool size = 2 and bootstrap pool size = 1 works well (6 sec start with agent, 11 sec start 100% CPU load during 15 sec without).

- `minecraftThreadPoolSize` (any version): changes size of all pools. Parameters below will overwrite this value.
- `minecraftMainThreadPoolSize` (1.16+): changes main thread pool size (used for loading resources).
- `minecraftBootstrapThreadPoolSize` (1.16+): changed bootstrap thread pool size (used for rewriting and optimizing DataFixer types).

## How it works

### How Minecraft works

Information below is for 1.16 and was extracted while analyzing decompiled client code.

At the start, client or server creates two thread pools: Main and Bootstrap. There are other pools, but they are not important here.

Main thread pool is used to reload resource managers. During reload, a resource manager scans assets and resourcepacks, loads models ans textures, creates textures atlases and does many other related things.

Bootstrap thread pool is used to rewrite DataFixer types. Minecraft declares a set of Schemas, each Schema containing Types. I do not know exactly how it is intergrated into Minecraft, since their DataFixers library is overcomplicated and uses abstractions that are impossible to understand without mathematical background.

Anyway, there is a type rewrite stage, in which each declared type is rewritten according to "optimization" rule. I do not know whether it means "performance optimization" or some other kind of optimization, what matters is that this process creates a lot of garbage objects and consumes a lot of CPU (the code must be compiled by JVM and this garbage must be collected).

Most importand thing in these pools are their sizes. By default, these pools have size that is calculated using `clamp(processor_count - 1, 1, 7)` formula. `processor_count` is the result of calling `Runtime.getRuntime().availableProcessors()`. This method returns not physical processor count and not physical core count, but "logical" processor count, meaning if you have 4 cores with hyperthreading, it will return 8. Also, OS can limit code count that is available for process, and result of this method will reflect it, but this is not important here.

For example, on 4 cores/8 threads machine these pools will have size `clamp(8 - 1, 1, 7) = 7`, meaning there will be 14 threads total for these two executors.

### Performance issues

This is very bad for performance: type rewriting and GC are CPU-bound tasks, meaning while they working, they will fully consume a CPU core. For contrast, IO-bound tasks do not consume that much CPU because they are frequently waiting for IO resource (file, socket, etc.) and CPU can do other tasks while IO-bound task is waiting.

If you have more CPU-bound tasks than physical cores, tasks will compete and work slower. Even your PC may start lagging, because there is less CPU time for OS-related tasks and other applications.

The answer to the question _"Why since 1.14 Minecraft consumes too much CPU on start?"_, therefore, is: Minecraft creates to many threads to rewrite DataFixer types and load resources and does not consider physical core count, its other executors and the time needed for GC to do its work.

The solution is to reduce thread pool sizes to adequate numbers, 1-3 threads for each executor. I don't know whether type rewriting is important or not, but since Mojang decided not to wait for rewriting completion at startup and allowing player to load world or connect to a server immediately after resources are loaded, this might not be a issue.

Specifically, 1 thread for Bootstrap executor and 2 for Main work especially well on my setup. Resource loading theoretically may benefit from more threads, but you can test it yourself.

After pool sizes were reduced, startup time significantly increase and CPU load is heaviliy reduced.

### Agent and bytecode

To make my solution as universal as possible, I use a Java instrumentation framework and ASM library to manipulate bytecode of loaded classes at runtime. This allows me to not bother with creating and distributing patches (mods).

There is `net.minecraft.Util` class containing fields storing executors (thread pools). Executors are created using a private method that is called when this class is being initialized.

I add call to `AgentRuntime.replacePool` method before these private methods return a value, allowing me to replace these pools if needed. In this method I do trivial things like checking system properties and creating new instance of `ForkJoinPool`.

To detect `Util` class, I use some heuristics like "a class must contain at least X fields ans Y methods, all fields and methods must be static, there must be presents logger and ExecutorService fields". This allows me to not depend on concrete names and obfuscation maps.

To measure start time I apply another patch to `net.minecraft.Minecraft` class. This is needed because Minecraft client does not print to the logs start time (like server does).

I add a call to `AgentRuntime.startProfiling` into the constructor and a call to `AgentRuntime.endProfiling` to any private synthetic `()V` methods that contain conditional jump opcode. There is a callback lambda that is called when resource manager finishes reloading, and that is the actual moment when Minecraft client becomes responsive. So, placing `endProfiling` call here seem reasonable.

`endProfiling` prints measured time into stdout:

`[STDOUT]: Done initial reload of resource manager, 11.60 sec passed since start`

## Related

Minecraft bug report, where Mojang said "it's your problem": [MC-154946](https://bugs.mojang.com/browse/MC-154946)