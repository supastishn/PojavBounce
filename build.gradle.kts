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

import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.task.NodeTask
import groovy.json.JsonOutput
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.gradle.kotlin.dsl.support.listFilesOrdered
import org.gradle.api.file.RelativePath
import java.util.jar.JarFile
import org.gradle.api.tasks.JavaExec

plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradleGitProperties)
    alias(libs.plugins.detekt)
    alias(libs.plugins.nodeGradle)
    alias(libs.plugins.dokka)
}

base {
    archivesName = project.property("archives_base_name") as String
    version = project.property("mod_version") as String
    group = project.property("maven_group") as String
}

/** Includes non-mod dependency recursively in the JAR file */
val includeDependency: Configuration by configurations.creating

/** Includes native-only dependency in the JAR file */
val includeNative: Configuration by configurations.creating

includeDependency.excludeProvidedLibs()

configurations {
    include.configure {
        extendsFrom(includeNative)
    }
    runtimeOnly.configure {
        extendsFrom(includeNative)
    }
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            name = "CCBlueX"
            url = uri("https://maven.ccbluex.net/releases")
        }
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Jitpack"
            url = uri("https://jitpack.io")
        }
        maven {
            name = "TerraformersMC"
            url = uri("https://maven.terraformersmc.com/")
        }
        maven {
            name = "ViaVersion"
            url = uri("https://repo.viaversion.com/")
        }
        maven {
            name = "modrinth"
            url = uri("https://api.modrinth.com/maven")
        }
        maven {
            name = "OpenCollab Snapshots"
            url = uri("https://repo.opencollab.dev/maven-snapshots/")
        }
        maven {
            name = "Lenni0451"
            url = uri("https://maven.lenni0451.net/everything")
        }
        maven {
            name = "NikOverflow"
            url = uri("https://reposilite.nikoverflow.com/releases")
        }
        maven {
            name = "ParchmentMC"
            url = uri("https://maven.parchmentmc.org")
        }
    }
}

loom {
    accessWidenerPath = file("src/main/resources/liquidbounce.accesswidener")
}

dependencies {
    // Minecraft
    minecraft(libs.minecraft)
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${libs.versions.minecraft.get()}:2025.12.20@zip")
    })

    // Fabric
    modApi(libs.fabric.loader)
    modApi(libs.fabric.api)
    modApi(libs.fabric.kotlin)

    // Mod menu
    modApi(libs.modmenu)

    // Recommended mods (on IDE)
    modApi(libs.sodium)
    modApi(libs.lithium)
    modRuntimeOnly(libs.immediatelyFast)
    modRuntimeOnly(libs.iris)

    // ViaFabricPlus
    modApi(libs.vfp.api)
    modRuntimeOnly(libs.vfp)

    // Exploit Preventer
    modApi(libs.exploitPreventer.api)
    modRuntimeOnly(libs.exploitPreventer)

    // Minecraft Authlib
    includeDependency(libs.mcAuthlib)

    includeDependency("net.ccbluex:netty-httpserver:2.4.2")
    // MacOS native (Linux native is included in game)
    includeDependency("io.netty:netty-transport-classes-kqueue:${project.property("netty_version")}")
    includeNative("io.netty:netty-transport-native-kqueue:${project.property("netty_version")}:osx-aarch_64")
    includeNative("io.netty:netty-transport-native-kqueue:${project.property("netty_version")}:osx-x86_64")


    // Discord RPC Support
    includeDependency(libs.discordIpc)

    // ScriptAPI
    includeDependency("net.fabricmc:tiny-mappings-parser:0.3.0+build.17")
    includeDependency(libs.polyglot)
    includeDependency(libs.polyglot.js)
    includeDependency(libs.polyglot.tools)

    // Machine Learning
    includeDependency(libs.djl.api)
    includeDependency(libs.djl.pytorch)
    includeDependency(libs.djl.mxnet)

    // ONNX Runtime (mobile) - used on Android for inference
    includeDependency(libs.onnxruntime.mobile)

    // HTTP library
    includeDependency(libs.bundles.okhttp)

    // SOCKS5 & HTTP Proxy Support
    includeDependency(libs.netty.handler.proxy)

    // Update Checker
    includeDependency(libs.semver4j)

    // Name Protect
    includeDependency(libs.ahocorasick)

    // External utils
    compileOnlyApi(libs.fastutil4k.extensionsOnly)
    includeDependency(libs.fastutil4k.moreCollections)

    // Test libraries
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
//    testImplementation(libs.fabric.loader.junit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    afterEvaluate {
        includeDependency.incoming.resolutionResult.allDependencies.forEach {
            val apiDependency = dependencies.api(it.requested.toString()) {
                isTransitive = false
            }

            dependencies.include(apiDependency)
        }
    }
}

// Task to extract ONNX Runtime natives from included AAR/jar dependencies into build folder
tasks.register<Copy>("extractOnnxRuntimeNatives") {
    description = "Extract Android JNI libraries from ONNX Runtime AARs into build/onnx-natives"
    group = "build"

    val destDir = layout.buildDirectory.dir("onnx-natives")

    // Resolve the Android AAR directly (Loom include does not reliably handle .aar artifacts)
    val onnxAndroidAar = configurations.detachedConfiguration(
        dependencies.create("com.microsoft.onnxruntime:onnxruntime-android:${libs.versions.onnxruntime.get()}@aar")
    ).apply {
        isTransitive = false
    }

    from({
        onnxAndroidAar.files.map { file -> zipTree(file) }
    })

    // Only extract JNI native libraries
    include("jni/**")
    includeEmptyDirs = false


    into(destDir)

    doFirst {
        logger.info("[build] Extracting ONNX Runtime natives from AAR/JAR dependencies into ${destDir.get().asFile.absolutePath}")
    }

    doLast {
        val extractedCount = fileTree(destDir.get().asFile).matching { include("**/*.so") }.files.size
        logger.info("[build] ONNX Runtime native extraction complete. Extracted $extractedCount files to ${destDir.get().asFile.absolutePath}")
    }
}

tasks.processResources {
    dependsOn("bundleTheme", "extractOnnxRuntimeNatives")

    val modVersion = providers.gradleProperty("mod_version")
    val minecraftVersion = libs.versions.minecraft
    val fabricVersion = libs.versions.fabric.api
    val loaderVersion = libs.versions.fabric.loader
    val minLoaderVersion = libs.versions.fabric.loaderMin
    val fabricKotlinVersion = libs.versions.fabric.kotlin
    val viafabricplusVersion = libs.versions.viafabricplus

    val contributors = provider {
        JsonOutput.prettyPrint(
            JsonOutput.toJson(getContributors("CCBlueX", "LiquidBounce"))
        )
    }

    inputs.property("version", modVersion)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("fabric_version", fabricVersion)
    inputs.property("loader_version", loaderVersion)
    inputs.property("min_loader_version", minLoaderVersion)
    inputs.property("fabric_kotlin_version", fabricKotlinVersion)
    inputs.property("viafabricplus_version", viafabricplusVersion)
    inputs.property("contributors", contributors)

    // Copy extracted ONNX natives into the resource tree (natives/android/<abi>/lib.so)
    from(layout.buildDirectory.dir("onnx-natives")) {
        include("jni/**/*.so")
        includeEmptyDirs = false
        eachFile {
            val segments = relativePath.segments
            if (segments.isNotEmpty() && segments[0] == "jni" && segments.size >= 3) {
                val abi = segments[1]
                relativePath = RelativePath(true, "natives", "android", abi, name)
            }
        }
    }

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to modVersion.get(),
                "minecraft_version" to minecraftVersion.get(),
                "fabric_version" to fabricVersion.get(),
                "loader_version" to loaderVersion.get(),
                "min_loader_version" to minLoaderVersion.get(),
                "contributors" to contributors.get(),
                "fabric_kotlin_version" to fabricKotlinVersion.get(),
                "viafabricplus_version" to viafabricplusVersion.get()
            )
        )
    }
}

// The following code will include the theme into the build

tasks.register<NpmTask>("npmInstallTheme") {
    workingDir = file("src-theme")
    args.set(listOf("i"))
    doLast {
        logger.info("Successfully installed dependencies for theme")
    }
    inputs.files("src-theme/package.json", "src-theme/package-lock.json")
    outputs.dir("src-theme/node_modules")
}

tasks.register<NpmTask>("buildTheme") {
    dependsOn("npmInstallTheme")
    workingDir = file("src-theme")
    args.set(listOf("run", "build"))
    doLast {
        logger.info("Successfully build theme")
    }

    inputs.files(
        "src-theme/package.json",
        "src-theme/package-lock.json",
        "src-theme/bundle.cjs",
        "src-theme/rollup.config.js"
    )
    inputs.dir("src-theme/src")
    outputs.dir("src-theme/dist")
}

tasks.register<NodeTask>("bundleTheme") {
    dependsOn("buildTheme")
    workingDir = file("src-theme")
    script = file("src-theme/bundle.cjs")
    doLast {
        logger.info("Successfully attached theme to build")
    }

    // Incremental stuff
    inputs.files(
        "src-theme/package.json",
        "src-theme/package-lock.json",
        "src-theme/bundle.cjs",
        "src-theme/rollup.config.js"
    )
    inputs.dir("src-theme/src")
    inputs.dir("src-theme/public")
    inputs.dir("src-theme/dist")
    outputs.files("src-theme/resources/assets/liquidbounce/themes/liquidbounce.zip")
}

sourceSets {
    main {
        resources {
            srcDirs("src-theme/resources")
        }
    }
}

// ensure that the encoding is set to UTF-8, no matter what the system default is
// this fixes some edge cases with special characters not displaying correctly
// see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"

    options.release = libs.versions.jdk.get().toInt()
}

tasks.test {
    useJUnitPlatform()
}

// Detekt check

detekt {
    config.setFrom(file("${rootProject.projectDir}/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("${rootProject.projectDir}/config/detekt/baseline.xml")
}

tasks.register<DetektCreateBaselineTask>("detektProjectBaseline") {
    description = "Overrides current baseline."
    ignoreFailures.set(true)
    parallel.set(true)
    buildUponDefaultConfig.set(true)
    setSource(files(rootDir))
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline.set(file("$rootDir/config/detekt/baseline.xml"))
    include("**/*.kt")
    include("**/*.kts")
    exclude("**/resources/**")
    exclude("**/build/**")
}

// i18n check

tasks.register<CompareJsonKeysTask>("verifyI18nJsonKeys") {
    val baselineFileName = "en_us.json"

    group = "verification"
    description = "Compare i18n JSON files with $baselineFileName as the baseline and report missing keys."

    val languageFolder = file("src/main/resources/resources/liquidbounce/lang")
    baselineFile.set(languageFolder.resolve(baselineFileName))
    files.from(languageFolder.listFilesOrdered { it.extension.equals("json", ignoreCase = true) })
    consoleOutputCount.set(5)
}

tasks.register<JavaExec>("liquidInstruction") {
    group = "other"
    description = "Run LiquidInstruction class."

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.ccbluex.liquidbounce.LiquidInstruction")
}

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }
}

kotlin {
    compilerOptions {
        suppressWarnings = true
        jvmToolchain(libs.versions.jdk.get().toInt())
        freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks.jar {
    val archivesBaseName = providers.gradleProperty("archives_base_name")
    val modVersion = providers.gradleProperty("mod_version")
    val mavenGroup = providers.gradleProperty("maven_group")
    val mappingFiles = provider {
        rootProject.configurations.mappings.get().map(::zipTree)
    }

    inputs.property("archives_base_name", archivesBaseName)
    inputs.property("mod_version", modVersion)
    inputs.property("maven_group", mavenGroup)
    inputs.files(mappingFiles).withPropertyName("mappingFiles")

    manifest {
        attributes["Main-Class"] = "net.ccbluex.liquidbounce.LiquidInstruction"
        attributes["Implementation-Title"] = archivesBaseName.get()
        attributes["Implementation-Version"] = modVersion.get()
        attributes["Implementation-Vendor"] = mavenGroup.get()
    }

    // Rename the project's license file to LICENSE_<project_name> to avoid conflicts
    from("LICENSE") {
        rename {
            "${it}_${archivesBaseName.get()}"
        }
    }

    from(files(mappingFiles.get())) {
        include("mappings/mappings.tiny")
    }
}

tasks.register<Copy>("copyZipInclude") {
    from("zip_include/")
    into("build/libs/zip")
}

tasks.named("sourcesJar") {
    dependsOn("bundleTheme", "extractOnnxRuntimeNatives")
}

tasks.named("build") {
    dependsOn("copyZipInclude")
}

// Verify that our built JAR includes Android native libraries under natives/android/*
tasks.register("verifyOnnxNativesInJar") {
    group = "verification"
    description = "Verify that ONNX Android native libraries are present in the JAR under natives/android"

    dependsOn("remapJar")
    doLast {
        val libsDir = layout.buildDirectory.dir("libs").get().asFile
        val jarFile = fileTree(libsDir).matching { include("liquidbounce-*.jar") }.files.firstOrNull()
            ?: throw GradleException("Built jar not found in ${libsDir.absolutePath}")
        JarFile(jarFile).use { jf ->
            val entries = jf.entries()
            var found = false
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith("natives/android/") && entry.name.endsWith(".so")) {
                    found = true
                    break
                }
            }

            if (!found) {
                throw GradleException("No Android native libraries found in ${jarFile.name} under natives/android/*")
            } else {
                logger.lifecycle("[build] Verified Android native libraries present in ${jarFile.name}")
            }
        }
    }
}

tasks.register<JavaExec>("exportDjlSavedModels") {
    group = "deeplearn"
    description = "Export DJL resource models to TensorFlow SavedModel directories"
    // Ensure classes are compiled before running the JavaExec task
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.ccbluex.liquidbounce.deeplearn.tools.ExportDjlToSavedModel")
}

tasks.named("check") {
    dependsOn("verifyOnnxNativesInJar")
}
