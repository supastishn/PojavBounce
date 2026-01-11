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

        logger.info("[NativeExtractor] Detecting ABI: $abi")
        logger.info("[NativeExtractor] Loading native libraries directly from AAR dependencies")

        // NEW APPROACH: Find and load directly from AAR files in classpath
        for (libName in libraryNames) {
            try {
                // Try to find the AAR file containing the native library
                val aarPath = findAarWithNativeLibrary(libName, abi)
                if (aarPath != null) {
                    // Extract the specific library from the AAR
                    val extractedFile = extractFromAar(aarPath, libName, abi, targetFolder)
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

        return extractedFiles
    }

    /**
     * Find AAR file in classpath that contains the specified native library
     */
    private fun findAarWithNativeLibrary(libName: String, abi: String): String? {
        try {
            // Get all URLs in classpath
            val classLoader = NativeLibExtractor::class.java.classLoader
            val classpathUrls = (classLoader as? java.net.URLClassLoader)?.urLs ?: return null

            for (url in classpathUrls) {
                if (url.protocol == "file" && url.path.endsWith(".aar")) {
                    try {
                        val jarFile = java.util.jar.JarFile(java.io.File(url.path))
                        val entry = jarFile.getEntry("jni/$abi/$libName")
                        jarFile.close()
                        if (entry != null) {
                            logger.info("[NativeExtractor] Found $libName in AAR: ${url.path}")
                            return url.path
                        }
                    } catch (e: Exception) {
                        // Continue searching
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("[NativeExtractor] Error searching for AAR files", e)
        }
        return null
    }

    /**
     * Extract a specific native library from an AAR file
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
