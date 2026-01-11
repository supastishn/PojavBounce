/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.deeplearn.utils

import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File

/**
 * Utility for extracting native libraries from JAR resources to filesystem.
 * Required for ONNX Runtime on Android, where native libraries must be
 * extracted to app-private storage before System.load().
 */
object NativeLibExtractor {

    /**
     * Detect Android ABI from system properties.
     * Maps os.arch values to Android ABI strings.
     */
    private fun detectAndroidAbi(): String {
        val osArch = System.getProperty("os.arch", "").lowercase()
        return when {
            osArch.contains("aarch64") || osArch.contains("arm64") -> "arm64-v8a"
            osArch.contains("arm") -> "armeabi-v7a"
            osArch.contains("x86_64") || osArch.contains("amd64") -> "x86_64"
            osArch.contains("x86") || osArch.contains("i686") -> "x86"
            else -> {
                logger.warn("[NativeExtractor] Unknown ABI from os.arch='$osArch', defaulting to arm64-v8a")
                "arm64-v8a"
            }
        }
    }

    /**
     * Extract native libraries for ONNX Runtime from JAR resources.
     * NEW APPROACH: Load directly from AAR file at runtime instead of extracting.
     *
     * @param targetFolder Destination folder (typically enginesCacheFolder)
     * @param libraryNames List of library filenames to extract
     * @return List of extracted File objects
     */
    fun extractNativeLibraries(targetFolder: File, libraryNames: List<String> = listOf("libonnxruntime.so")): List<File> {
        val abi = detectAndroidAbi()
        val extractedFiles = mutableListOf<File>()

        logger.info("[NativeExtractor] ===== STARTING NATIVE LIBRARY EXTRACTION =====")
        logger.info("[NativeExtractor] Detected ABI: $abi")
        logger.info("[NativeExtractor] Target folder: ${targetFolder.absolutePath}")
        logger.info("[NativeExtractor] Library names: $libraryNames")

        // NEW APPROACH: Find and load directly from AAR files in classpath
        for (libName in libraryNames) {
            logger.info("[NativeExtractor] Processing library: $libName")
            try {
                // First, try to find the library already packaged in our resources tree
                val resourcePath = "/natives/android/$abi/$libName"
                val resourceStream = NativeLibExtractor::class.java.getResourceAsStream(resourcePath)
                if (resourceStream != null) {
                    logger.info("[NativeExtractor] Found $libName bundled as resource at $resourcePath, extracting to filesystem...")
                    targetFolder.mkdirs()
                    val targetFile = File(targetFolder, libName)
                    resourceStream.use { input -> targetFile.outputStream().use { output -> input.copyTo(output) } }
                    targetFile.setExecutable(true)
                    targetFile.setReadable(true)
                    logger.info("[NativeExtractor] Extracted ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                    extractedFiles.add(targetFile)
                    // proceed to next library
                    continue
                }

                // Try to find the AAR file containing the native library
                val aarPath = findAarWithNativeLibrary(libName, abi)
                logger.info("[NativeExtractor] AAR search result for $libName: $aarPath")
                if (aarPath != null) {
                    // Extract the specific library from the AAR
                    val extractedFile = extractFromAar(aarPath, libName, abi, targetFolder)
                    logger.info("[NativeExtractor] Extraction result for $libName: ${extractedFile?.absolutePath}")
                    if (extractedFile != null) {
                        extractedFiles.add(extractedFile)
                    }
                } else {
                    logger.warn("[NativeExtractor] Could not find AAR containing $libName for ABI $abi")
                }
            } catch (e: Exception) {
                logger.error("[NativeExtractor] Failed to load $libName from AAR", e)
            }
        }

        logger.info("[NativeExtractor] ===== EXTRACTION COMPLETE: ${extractedFiles.size} libraries extracted =====")
        return extractedFiles
    }

    /**
     * Find AAR file in classpath that contains the specified native library
     */
    private fun findAarWithNativeLibrary(libName: String, abi: String): String? {
        logger.info("[NativeExtractor] Searching for AAR containing $libName for ABI $abi")
        try {
            // Approach 1: Use java.class.path system property (works with Fabric)
            val classPath = System.getProperty("java.class.path", "")
            logger.info("[NativeExtractor] java.class.path: $classPath")

            if (classPath.isNotEmpty()) {
                val paths = classPath.split(File.pathSeparator)
                logger.info("[NativeExtractor] Found ${paths.size} paths in java.class.path")

                for (path in paths) {
                    if (path.endsWith(".aar") || path.contains("onnxruntime")) {
                        logger.info("[NativeExtractor] Checking potential AAR/jar: $path")
                        try {
                            val file = File(path)
                            if (file.exists()) {
                                val jarFile = java.util.jar.JarFile(file)
                                val entry = jarFile.getEntry("jni/$abi/$libName")
                                logger.info("[NativeExtractor] Entry 'jni/$abi/$libName' exists in $path: ${entry != null}")
                                jarFile.close()
                                if (entry != null) {
                                    logger.info("[NativeExtractor] Found $libName in: $path")
                                    return path
                                }
                            } else {
                                logger.warn("[NativeExtractor] Path does not exist: $path")
                            }
                        } catch (e: Exception) {
                            logger.warn("[NativeExtractor] Error checking $path: ${e.message}")
                        }
                    }
                }
            }

            // Approach 2: Search classloader resources for the native library
            val resourceName = "jni/$abi/$libName"
            val classLoader = Thread.currentThread().contextClassLoader ?: NativeLibExtractor::class.java.classLoader
            logger.info("[NativeExtractor] Searching classloader resources for $resourceName using $classLoader")
            try {
                val resources = classLoader.getResources(resourceName)
                while (resources.hasMoreElements()) {
                    val url = resources.nextElement()
                    logger.info("[NativeExtractor] Found resource for $resourceName at $url")
                    val urlStr = url.toString()
                    if (urlStr.startsWith("jar:file:")) {
                        logger.info("[NativeExtractor] Resource points to jar URL: $urlStr")
                        return urlStr
                    } else if (url.protocol == "file") {
                        try {
                            val file = java.io.File(url.toURI())
                            var current: java.io.File? = file.parentFile
                            while (current != null) {
                                current.listFiles()?.forEach { candidate ->
                                    if (candidate.isFile && (candidate.name.endsWith(".jar") || candidate.name.endsWith(".aar") || candidate.name.contains("onnxruntime"))) {
                                        logger.info("[NativeExtractor] Checking candidate jar: ${candidate.absolutePath}")
                                        try {
                                            val jarFile = java.util.jar.JarFile(candidate)
                                            val entry = jarFile.getEntry(resourceName)
                                            jarFile.close()
                                            if (entry != null) {
                                                logger.info("[NativeExtractor] Found $libName in: ${candidate.absolutePath}")
                                                return candidate.absolutePath
                                            }
                                        } catch (e: Exception) {
                                            logger.warn("[NativeExtractor] Error checking candidate jar ${candidate.absolutePath}: ${e.message}")
                                        }
                                    }
                                }
                                current = current.parentFile
                            }
                        } catch (e: Exception) {
                            logger.warn("[NativeExtractor] Error while processing file resource $url: ${e.message}")
                        }
                    } else {
                        logger.info("[NativeExtractor] Resource URL has unsupported protocol: ${url.protocol}")
                    }
                }
            } catch (e: Exception) {
                logger.warn("[NativeExtractor] Error searching resources for $resourceName: ${e.message}")
            }

            // Approach 3: Check for embedded JARs in current JAR
            val currentJarUrl = NativeLibExtractor::class.java.protectionDomain?.codeSource?.location
            if (currentJarUrl != null && currentJarUrl.protocol == "file") {
                logger.info("[NativeExtractor] Current JAR URL: $currentJarUrl")
                try {
                    val currentJarFile = File(currentJarUrl.toURI())
                    logger.info("[NativeExtractor] Checking for embedded ONNX Runtime in: ${currentJarFile.name}")

                    val jarFile = java.util.jar.JarFile(currentJarFile)
                    val entries = jarFile.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name.contains("onnxruntime") && entry.name.endsWith(".jar") && !entry.isDirectory) {
                            logger.info("[NativeExtractor] Found embedded ONNX Runtime JAR: ${entry.name}")

                            // Construct the embedded JAR path
                            val embeddedJarPath = "jar:file:${currentJarFile.absolutePath}!/${entry.name}"
                            logger.info("[NativeExtractor] Trying embedded path: $embeddedJarPath")

                            // Test if this embedded JAR contains our native library
                            try {
                                // For testing, we'll assume it contains the library and let extractFromAar handle it
                                logger.info("[NativeExtractor] Found embedded ONNX Runtime AAR: $embeddedJarPath")
                                jarFile.close()
                                return embeddedJarPath
                            } catch (e: Exception) {
                                logger.warn("[NativeExtractor] Error with embedded path $embeddedJarPath: ${e.message}")
                            }
                        }
                    }
                    jarFile.close()
                } catch (e: Exception) {
                    logger.warn("[NativeExtractor] Error checking current JAR: ${e.message}")
                }
            }

            logger.warn("[NativeExtractor] No AAR found containing $libName for ABI $abi")
        } catch (e: Exception) {
            logger.error("[NativeExtractor] Error searching for AAR files", e)
        }
        return null
    }

    /**
     * Extract a specific native library from an AAR file (or embedded JAR)
     */
    private fun extractFromAar(aarPath: String, libName: String, abi: String, targetFolder: File): File? {
        return try {
            targetFolder.mkdirs()
            val targetFile = File(targetFolder, libName)

            // Skip if already extracted
            if (targetFile.exists()) {
                logger.debug("[NativeExtractor] Library already extracted: ${targetFile.absolutePath}")
                return targetFile
            }

            // Handle jar:file: URLs (either embedded JARs or direct native entry inside a JAR)
            if (aarPath.startsWith("jar:file:")) {
                logger.info("[NativeExtractor] Handling jar-file URL: $aarPath")
                val mainJarPath = aarPath.substringAfter("jar:file:").substringBefore("!")
                val embeddedPath = aarPath.substringAfter("!").substring(1) // Remove leading /

                logger.info("[NativeExtractor] Main JAR: $mainJarPath")
                logger.info("[NativeExtractor] Embedded path: $embeddedPath")

                val mainJarFile = java.util.jar.JarFile(mainJarPath)
                try {
                    // If this points to an embedded JAR (e.g., META-INF/jars/onnxruntime-*.jar), extract the embedded JAR first
                    if (embeddedPath.contains("META-INF/jars/") && embeddedPath.endsWith(".jar")) {
                        val embeddedEntry = mainJarFile.getEntry(embeddedPath)

                        if (embeddedEntry != null) {
                            // Extract the embedded JAR to a temp location first
                            val tempJarFile = File.createTempFile("onnxruntime", ".jar")
                            mainJarFile.getInputStream(embeddedEntry).use { input ->
                                tempJarFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }

                            // Now extract the native library from the embedded JAR
                            val tempJar = java.util.jar.JarFile(tempJarFile)
                            val nativeEntry = tempJar.getEntry("jni/$abi/$libName")

                            if (nativeEntry != null) {
                                logger.info("[NativeExtractor] Extracting $libName from embedded JAR...")
                                tempJar.getInputStream(nativeEntry).use { input ->
                                    targetFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                tempJar.close()
                                tempJarFile.delete()

                                // Set permissions
                                targetFile.setExecutable(true)
                                targetFile.setReadable(true)

                                logger.info("[NativeExtractor] Extracted ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                                return targetFile
                            } else {
                                // Some ONNX distributions place their native libraries under ai/onnxruntime/native/<platform>/
                                val mappedPlatform = when (abi) {
                                    "arm64-v8a" -> "linux-aarch64"
                                    "armeabi-v7a" -> "linux-arm32"
                                    "x86_64" -> "linux-x64"
                                    "x86" -> "linux-x86"
                                    else -> abi
                                }

                                val altPath = "ai/onnxruntime/native/$mappedPlatform/$libName"
                                val altEntry = tempJar.getEntry(altPath)

                                if (altEntry != null) {
                                    logger.info("[NativeExtractor] Extracting $libName from embedded ONNX layout at $altPath...")
                                    tempJar.getInputStream(altEntry).use { input ->
                                        targetFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    tempJar.close()
                                    tempJarFile.delete()

                                    // Set permissions
                                    targetFile.setExecutable(true)
                                    targetFile.setReadable(true)

                                    logger.info("[NativeExtractor] Extracted ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                                    return targetFile
                                }

                                logger.warn("[NativeExtractor] Native library not found in embedded JAR at jni/$abi/$libName or $altPath")
                                tempJar.close()
                                tempJarFile.delete()
                                return null
                            }
                        } else {
                            logger.warn("[NativeExtractor] Embedded JAR not found at $embeddedPath")
                            return null
                        }
                    } else {
                        // The embeddedPath likely points directly to the native file inside the main JAR
                        val nativeEntry = mainJarFile.getEntry(embeddedPath)
                        if (nativeEntry != null) {
                            logger.info("[NativeExtractor] Extracting $libName from main JAR entry $embeddedPath...")
                            mainJarFile.getInputStream(nativeEntry).use { input ->
                                targetFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }

                            // Set permissions
                            targetFile.setExecutable(true)
                            targetFile.setReadable(true)

                            logger.info("[NativeExtractor] Extracted ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                            return targetFile
                        } else {
                            logger.warn("[NativeExtractor] Native library not found in main JAR at $embeddedPath")
                            return null
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("[NativeExtractor] Error handling jar-file URL $aarPath: ${e.message}")
                } finally {
                    try { mainJarFile.close() } catch (ignored: Exception) {}
                }
            }

            // Original direct file approach
            val jarFile = java.util.jar.JarFile(aarPath)
            val entry = jarFile.getEntry("jni/$abi/$libName")

            if (entry == null) {
                logger.warn("[NativeExtractor] Library $libName not found in AAR at jni/$abi/")
                jarFile.close()
                return null
            }

            logger.info("[NativeExtractor] Extracting $libName from AAR...")
            jarFile.getInputStream(entry).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            jarFile.close()

            // Set permissions
            targetFile.setExecutable(true)
            targetFile.setReadable(true)

            logger.info("[NativeExtractor] Extracted ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            targetFile
        } catch (e: Exception) {
            logger.error("[NativeExtractor] Failed to extract $libName from AAR", e)
            null
        }
    }

    /**
     * Load native libraries using System.load().
     *
     * @param libraries List of File objects representing extracted libraries
     * @return true if all libraries loaded successfully
     */
    fun loadNativeLibraries(libraries: List<File>): Boolean {
        var allLoaded = true

        for (libFile in libraries) {
            try {
                logger.info("[NativeExtractor] Loading ${libFile.absolutePath}...")
                System.load(libFile.absolutePath)
                logger.info("[NativeExtractor] Successfully loaded ${libFile.name}")
            } catch (e: UnsatisfiedLinkError) {
                logger.error("[NativeExtractor] Failed to load ${libFile.name}", e)
                logger.error("[NativeExtractor] Link error details: ${e.message}")
                allLoaded = false
            } catch (e: Exception) {
                logger.error("[NativeExtractor] Unexpected error loading ${libFile.name}", e)
                allLoaded = false
            }
        }

        return allLoaded
    }

    /**
     * Extract and load native libraries in one operation.
     *
     * @param targetFolder Destination folder for extracted libraries
     * @param libraryNames List of library filenames to extract and load
     * @return true if extraction and loading succeeded
     */
    fun extractAndLoad(targetFolder: File, libraryNames: List<String> = listOf("libonnxruntime.so")): Boolean {
        val extracted = extractNativeLibraries(targetFolder, libraryNames)

        if (extracted.isEmpty()) {
            logger.error("[NativeExtractor] No libraries were extracted")
            return false
        }

        return loadNativeLibraries(extracted)
    }

    /**
     * Collect diagnostic information for troubleshooting native library issues.
     *
     * @param targetFolder Folder where libraries should be extracted
     * @return Diagnostic information string
     */
    fun collectDiagnostics(targetFolder: File): String = buildString {
        appendLine("Native Library Diagnostics:")
        appendLine("  Detected ABI: ${detectAndroidAbi()}")
        appendLine("  os.arch: ${System.getProperty("os.arch")}")
        appendLine("  os.name: ${System.getProperty("os.name")}")
        appendLine("  java.library.path: ${System.getProperty("java.library.path")}")

        appendLine("\nClassLoader diagnostics:")
        try {
            val ctxCl = Thread.currentThread().contextClassLoader
            appendLine("  contextClassLoader: ${ctxCl?.javaClass?.name} (toString: $ctxCl)")

            var cl: ClassLoader? = ctxCl
            while (cl != null) {
                appendLine("  - ${cl.javaClass.name}")
                try {
                    // Try common methods to get URLs from classloader
                    val getUrlsMethod = cl.javaClass.methods.firstOrNull { it.name == "getUrls" || it.name == "getURLs" }
                    if (getUrlsMethod != null) {
                        val urls = getUrlsMethod.invoke(cl)
                        appendLine("    urls: ${urls?.toString()}")
                    }
                } catch (e: Exception) {
                    appendLine("    (failed to read URLs: ${e.message})")
                }
                cl = cl.parent
            }
        } catch (e: Exception) {
            appendLine("  Error collecting classloader diagnostics: ${e.message}")
        }

        appendLine("\nTarget Folder (${targetFolder.absolutePath}):")
        try {
            if (!targetFolder.exists()) {
                appendLine("  Folder does not exist")
            } else {
                val files = targetFolder.listFiles()
                if (files == null || files.isEmpty()) {
                    appendLine("  Empty or not accessible")
                } else {
                    files.forEach { file ->
                        val type = if (file.isDirectory) "dir" else "file"
                        val size = if (file.isFile) "${file.length()} bytes" else ""
                        val permissions = buildString {
                            append(if (file.canRead()) "r" else "-")
                            append(if (file.canWrite()) "w" else "-")
                            append(if (file.canExecute()) "x" else "-")
                        }
                        appendLine("  - ${file.name} ($type, $size, $permissions)")
                    }
                }
            }
        } catch (e: Exception) {
            appendLine("  Error listing folder: ${e.message}")
        }
    }
}
