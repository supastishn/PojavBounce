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
 *
 *
 */
package net.ccbluex.liquidbounce.deeplearn.executorch

import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File
import java.io.InputStream

/**
 * Utility for extracting native libraries from JAR resources to the filesystem.
 * This is necessary on Android where System.loadLibrary() requires libraries to be
 * on the filesystem in a specific location.
 */
object NativeLibraryExtractor {

    /**
     * Extracts a native library from JAR resources to a target directory.
     *
     * @param libraryName The base name of the library (e.g., "executorch")
     * @param targetDir The directory where the library will be extracted
     * @param osArch The OS architecture (e.g., "aarch64", "x86_64")
     * @return The extracted library file, or null if extraction failed
     */
    fun extractLibrary(libraryName: String, targetDir: File, osArch: String): File? {
        // Determine library file name based on OS
        val libFileName = "lib$libraryName.so"
        
        // Possible resource paths where the library might be located (only look in native dir)
        val resourcePaths = listOf(
            "/native/$libFileName"
        )

        logger.info("[NativeLibraryExtractor] Attempting to extract $libFileName for $osArch")
        
        // Try each possible resource path
        for (resourcePath in resourcePaths) {
            try {
                val inputStream = getResourceAsStream(resourcePath)
                if (inputStream != null) {
                    logger.info("[NativeLibraryExtractor] Found library at $resourcePath")
                    return extractToFile(inputStream, targetDir, libFileName)
                }
            } catch (e: Exception) {
                // Continue to next path
                logger.debug("[NativeLibraryExtractor] Library not found at $resourcePath: ${e.message}")
            }
        }

        logger.warn("[NativeLibraryExtractor] Could not find $libFileName in any resource path")
        return null
    }

    /**
     * Extracts multiple native libraries with dependencies.
     * This ensures that dependent libraries are extracted before their dependents.
     *
     * @param libraryNames List of library names in dependency order (dependencies first)
     * @param targetDir The directory where libraries will be extracted
     * @param osArch The OS architecture
     * @return List of successfully extracted library files
     */
    fun extractLibraries(libraryNames: List<String>, targetDir: File, osArch: String): List<File> {
        val extractedLibs = mutableListOf<File>()
        
        for (libraryName in libraryNames) {
            val extracted = extractLibrary(libraryName, targetDir, osArch)
            if (extracted != null) {
                extractedLibs.add(extracted)
            } else {
                logger.warn("[NativeLibraryExtractor] Failed to extract $libraryName, continuing with remaining libraries")
            }
        }
        
        return extractedLibs
    }

    /**
     * Extracts an input stream to a file in the target directory.
     */
    private fun extractToFile(inputStream: InputStream, targetDir: File, fileName: String): File {
        try {
            targetDir.mkdirs()
            val targetFile = File(targetDir, fileName)

            inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Make the library executable (owner-only for security)
            targetFile.setExecutable(true, true)
            targetFile.setReadable(true, true)
            targetFile.setWritable(true, true)

            logger.info("[NativeLibraryExtractor] Extracted library to ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            return targetFile
        } catch (e: Exception) {
            logger.error("[NativeLibraryExtractor] Failed to extract library to $targetDir/$fileName: ${e.message}", e)
            throw e
        }
    }

    /**
     * Gets a resource as an input stream, trying multiple class loaders.
     */
    private fun getResourceAsStream(path: String): InputStream? {
        val normalizedPath = path.removePrefix("/")
        
        // Try context class loader first
        Thread.currentThread().contextClassLoader?.getResourceAsStream(normalizedPath)?.let { return it }
        
        // Try this class's class loader
        NativeLibraryExtractor::class.java.getResourceAsStream(path)?.let { return it }
        
        // Try system class loader
        ClassLoader.getSystemResourceAsStream(normalizedPath)?.let { return it }
        
        return null
    }

    /**
     * Detects the current OS architecture.
     */
    fun detectOsArch(): String {
        val osArch = System.getProperty("os.arch", "").lowercase()
        return when {
            osArch.contains("aarch64") || osArch.contains("arm64") -> "aarch64"
            osArch.contains("arm") -> "arm"
            osArch.contains("x86_64") || osArch.contains("amd64") -> "x86_64"
            osArch.contains("x86") || osArch.contains("i386") || osArch.contains("i686") -> "x86"
            else -> osArch
        }
    }
}
