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
        
        // Possible resource paths where the library might be located
        val resourcePaths = listOf(
            "/native/android/$osArch/$libFileName",
            "/native/linux/$osArch/$libFileName",
            "/lib/$osArch/$libFileName",
            "/$osArch/$libFileName",
            "/native/$libFileName",
            "/$libFileName"
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
     * Extracts an input stream to a file in the target directory.
     */
    private fun extractToFile(inputStream: InputStream, targetDir: File, fileName: String): File {
        targetDir.mkdirs()
        val targetFile = File(targetDir, fileName)

        inputStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Make the library executable
        targetFile.setExecutable(true, false)
        targetFile.setReadable(true, false)

        logger.info("[NativeLibraryExtractor] Extracted library to ${targetFile.absolutePath} (${targetFile.length()} bytes)")
        return targetFile
    }

    /**
     * Gets a resource as an input stream, trying multiple class loaders.
     */
    private fun getResourceAsStream(path: String): InputStream? {
        // Try context class loader first
        Thread.currentThread().contextClassLoader?.getResourceAsStream(path.removePrefix("/"))?.let { return it }
        
        // Try this class's class loader
        NativeLibraryExtractor::class.java.getResourceAsStream(path)?.let { return it }
        
        // Try system class loader
        ClassLoader.getSystemResourceAsStream(path.removePrefix("/"))?.let { return it }
        
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
