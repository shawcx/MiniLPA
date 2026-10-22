
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.ArchUtils
import org.apache.commons.lang3.SystemUtils
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.kohsuke.github.GHRelease
import org.kohsuke.github.GitHub
import org.panteleyev.jpackage.ImageType

plugins {
    application
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("com.gradleup.shadow") version "8.3.0"
    id("io.ktor.plugin") version "3.0.0-beta-2"
    id("org.panteleyev.jpackageplugin") version "1.6.0"
    id("com.github.gmazzo.buildconfig") version "5.4.0"
    id("com.github.ben-manes.versions") version "0.51.0"
}

group = "moe.sekiu"
version = "1.1.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.formdev:flatlaf:3.5.1")
    implementation("com.formdev:flatlaf-extras:3.5.1")
    implementation("com.formdev:flatlaf-intellij-themes:3.5.1")
    implementation("com.miglayout:miglayout-swing:11.4.2")
    implementation("com.charleskorn.kaml:kaml:0.61.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0-RC.2")
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-websockets")
    implementation("app.softwork:kotlinx-uuid-core:0.1.2")
    implementation("org.boofcv:boofcv-core:1.1.5")
    implementation("org.boofcv:boofcv-kotlin:1.1.5")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("org.slf4j:slf4j-api:2.1.0-alpha1")
    implementation("ch.qos.logback:logback-classic:1.5.7")
    implementation("com.github.Dansoftowner:jSystemThemeDetector:3.9.1")
    implementation("org.apache.xmlgraphics:batik-all:1.17")
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-tiff:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-bmp:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-batik:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-psd:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-icns:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-pnm:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-iff:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-tga:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-sgi:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-pict:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-hdr:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-pcx:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-pdf:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-clippath:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-xwd:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-tiff-jdk-interop:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-tiff-jai-interop:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-jpeg-jep262-interop:3.11.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.kohsuke:github-api:1.324")
        classpath("org.apache.commons:commons-lang3:3.17.0")
        classpath("io.ktor:ktor-client-core:3.0.0-beta-2")
        classpath("io.ktor:ktor-client-cio:3.0.0-beta-2")
    }
}

val commitId = git("rev-parse", "HEAD")
val isDirty = git("diff", "--no-ext-diff", "--quiet", "--exit-code") == null
val shortCommitId = git("rev-parse", "--short", "HEAD")?.let { if (isDirty) "$it-dirty" else it }

buildConfig {
    packageName("moe.sekiu.minilpa")
    buildConfigField("IS_PACKAGED",
        (gradle.startParameter.taskNames.firstOrNull() == "jpackage" || project.switchProperty("is-package"))
                && project.findProperty("type") != "app-image"
    )
    buildConfigField("VERSION", "$version")
    buildConfigField("SHORT_COMMIT_ID", project.findProperty("short-commit-id") as String? ?: shortCommitId ?: "internal")
}

application {
    mainClass.set("moe.sekiu.minilpa.MainKt")
}

tasks.named<KotlinCompilationTask<*>>("compileKotlin").configure {
    compilerOptions.freeCompilerArgs.addAll(
        "-opt-in=kotlin.io.encoding.ExperimentalEncodingApi",
        "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
        "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        "-opt-in=kotlin.uuid.ExperimentalUuidApi"
    )
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

val lpacPlatformToArtifact = mapOf(
    "macos_universal" to "lpac-darwin-universal",
    "windows_x86" to "lpac-windows-x86_64-mingw",
    "windows_aarch64" to "lpac-windows-arm64-mingw",
    "linux_x86" to "lpac-linux-x86_64"
)

val euiccManifests = mapOf(
    "eum-accredited.json" to "https://euicc-manual.osmocom.org/docs/pki/eum/accredited.json",
    "ci-manifest.json" to "https://euicc-manual.osmocom.org/docs/pki/ci/manifest.json"
)

val buildPath = layout.buildDirectory.asFile.get().toPath()
val jpackagePath = buildPath.resolve("jpackage")
val distPath = buildPath.resolve("dist")
val appImagePath = distPath.resolve(if (SystemUtils.IS_OS_MAC_OSX) "${project.name}.app" else project.name)
val resourcesPath = projectDir.toPath().resolve("src/main/resources")
val eUICCInfoUpdateTimePath = buildPath.resolve("euicc_info_update_time")
val archiveFileName = tasks.shadowJar.get().archiveFileName.get()
val target = project.findProperty("target")?.toString()?.lowercase() ?: getPlatformInfo()
val packageType = project.findProperty("type")?.toString()
val githubToken = project.findProperty("github-token")?.toString()

val distName by lazy {
    val os = if (SystemUtils.IS_OS_WINDOWS) "Windows"
    else if (SystemUtils.IS_OS_LINUX) "Linux"
    else if (SystemUtils.IS_OS_MAC_OSX) "macOS"
    else throw UnsupportedOperationException("Unsupported os ${SystemUtils.OS_NAME}")

    val processor = ArchUtils.getProcessor()
    val arch = if (processor.isX86 && processor.is64Bit) "x86_64"
    else if (processor.isAarch64) "aarch64"
    else throw UnsupportedOperationException("Unsupported arch ${processor.type.label}")
    if (project.hasProperty("native-wayland")) "${project.name}-$os-$arch-Wayland" else "${project.name}-$os-$arch"
}

val latestRelease : GHRelease by lazy {
    val gitHub = if (githubToken != null) GitHub.connectUsingOAuth(githubToken) else GitHub.connectAnonymously()
    val repository = gitHub.getRepository("estkme-group/lpac")
    repository.latestRelease
}

val listAssets by lazy { latestRelease.listAssets().toList() }

tasks.generateBuildConfig { dependsOn("setupResources") }

task("setupResources")
{
    group = "Package"
    doLast {
        val skip = project.switchProperty("skip-setup-resources")
        val lpacBuildTimePath = buildPath.resolve("lpac_build_time")
        var lpacBuildTime = if (lpacBuildTimePath.notExists()) 0
        else Files.readString(lpacBuildTimePath).toLong()
        val languagePackPath = buildPath.resolve("languages.zip")

        if (!skip)
        {
            if (euiccManifests.keys.any { resourcesPath.resolve(it).notExists() } || eUICCInfoUpdateTimePath.notExists())
            {
                runBlocking { downloadEuiccInfo() }
            }

            val lpacLatestBuildTime = latestRelease.createdAt.time
            if (target == "all" ||
                buildPath.resolve("lpac").notExists() ||
                buildPath.resolve("lpac").resolve(target).notExists() ||
                lpacLatestBuildTime > lpacBuildTime)
            {
                if (target == "all")
                {
                    runBlocking { lpacPlatformToArtifact.keys.map { async { downloadlpac(it) } }.awaitAll() }
                }
                else
                {
                    lpacPlatformToArtifact[target] ?: throw IllegalArgumentException("Unknown target $target\nValid values are: [${lpacPlatformToArtifact.keys.joinToString(", ")}]")
                    runBlocking { downloadlpac(target) }
                }
                lpacBuildTimePath.writeText("$lpacLatestBuildTime")
                lpacBuildTime = lpacLatestBuildTime
            }

            FileSystems.newFileSystem(languagePackPath, mapOf("create" to true)).use { fs ->
                val languages = projectDir.toPath().resolve("src/main/languages/")
                for (path in Files.list(languages)) Files.copy(path, fs.getPath(path.name), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
            }
        }

        buildConfig {
            buildConfigField("LPAC_BUILD_TIME", lpacBuildTime)
            buildConfigField("LANGUAGE_PACK_UPDATE_TIME", languagePackPath.readAttributes<BasicFileAttributes>().lastModifiedTime().toMillis())
            buildConfigField("EUICC_INFO_UPDATE_TIME", eUICCInfoUpdateTimePath.readText().toLong())
        }
    }
}

task("updateEuiccInfo")
{
    group = "Package"
    doLast { runBlocking { downloadEuiccInfo() } }
}

task("setupjpackage")
{
    group = "Package"
    dependsOn("shadowJar")
    doLast {
        Files.createDirectories(jpackagePath)
        Files.copy(
            buildPath.resolve("libs/${archiveFileName}"),
            jpackagePath.resolve(archiveFileName),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        )
    }
}

tasks.processResources {
    dependsOn("setupResources")
    if (target == "all")
    {
        from(*lpacPlatformToArtifact.keys.map { buildPath.resolve("lpac").resolve("$it.zip") }.toTypedArray())
    }
    else
    {
        lpacPlatformToArtifact[target] ?: throw IllegalArgumentException("Unknown target $target\nValid values are: [${lpacPlatformToArtifact.keys.joinToString(", ")}]")
        from("${buildPath.resolve("lpac").resolve("$target.zip")}")
    }
    from(buildPath.resolve("languages.zip"))
}

tasks.shadowJar {
    minimize {
        exclude(dependency("ch.qos.logback:logback-classic:.*"))
        exclude(dependency("net.java.dev.jna:jna:.*"))
        exclude(dependency("com.formdev:flatlaf-intellij-themes:.*"))
        exclude(dependency("com.twelvemonkeys.imageio:.*:.*"))
        exclude(dependency("io.ktor:ktor-client-cio:.*"))
        exclude(dependency("com.github.jai-imageio:jai-imageio-core:.*"))
    }
}

tasks.jpackage {
    group = "Package"
    dependsOn("setupjpackage")
    input = jpackagePath.pathString
    destination = distPath.pathString
    appName = project.name
    vendor = "esim.moe"
    mainJar = archiveFileName
    mainClass = application.mainClass.get()
    javaOptions = mutableListOf("-Dfile.encoding=UTF-8")
    addModules = listOf("java.base", "java.desktop", "java.naming", "jdk.unsupported")

    if (project.hasProperty("type"))
    {
        val value = "${project.property("type")}"
        type = ImageType.values().find { it.value == value } ?: throw IllegalArgumentException("Type $value not found")
    }

    if (project.hasProperty("native-wayland")) javaOptions.add("-Dawt.toolkit.name=WLToolkit")
    windows {
        icon = projectDir.toPath().resolve("src/main/icons/window.ico").pathString
        if (type != ImageType.APP_IMAGE)
        {
            winDirChooser = true
            winMenu = true
            winMenuGroup = "MiniLPA"
            winShortcut = true
            winShortcutPrompt = true
        }
    }

    linux {
        icon = projectDir.toPath().resolve("src/main/icons/window.png").pathString
        if (type != ImageType.APP_IMAGE)
        {
            linuxShortcut = true
            linuxMenuGroup = "MiniLPA"
            installDir = "/usr/"
        }
    }

    mac {
        icon = projectDir.toPath().resolve("src/main/icons/window.icns").pathString
        if (type != ImageType.APP_IMAGE)
        {
            macPackageIdentifier = "moe.sekiu.MiniLPA"
            macAppCategory = "MiniLPA"
        }
    }

    if (packageType == "app-image") finalizedBy("zipAppImage")
    else doLast { renameDistFiles() }
}

task<Zip>("zipAppImage")
{
    dependsOn("jpackage")
    destinationDirectory = distPath.toFile()
    archiveFileName = provider { "$distName.zip" }
    // A macOS app-image is a .app bundle, so the bundle itself is the archive root; elsewhere it's the folder's contents
    if (SystemUtils.IS_OS_MAC_OSX) from(distPath) { include("${appImagePath.name}/**") }
    else from(appImagePath)
    doLast { appImagePath.toFile().deleteRecursively() }
}

fun getPlatformInfo() : String
{
    val os = if (SystemUtils.IS_OS_MAC_OSX) "macos"
    else if (SystemUtils.IS_OS_WINDOWS) "windows"
    else if (SystemUtils.IS_OS_LINUX) "linux"
    else throw UnsupportedOperationException("Unsupported os ${SystemUtils.OS_NAME}")

    val processor = ArchUtils.getProcessor()
    val arch = if (os == "macos") "universal"
    else if (processor.isX86) "x86"
    else if (processor.isAarch64) "aarch64"
    else throw UnsupportedOperationException("Unsupported arch ${processor.type.label}")
    return "${os}_${arch}"
}

val client = HttpClient()
suspend fun downloadlpac(platform : String)
{
    val asset = listAssets.first { it.name == "${lpacPlatformToArtifact[platform]}.zip" }
    val response = client.get(asset.browserDownloadUrl)
    if (!response.status.isSuccess()) throw IllegalStateException("Network error")
    withContext(Dispatchers.IO) {
        val lpacFolderPath = buildPath.resolve("lpac")
        Files.createDirectories(lpacFolderPath)
        Files.newOutputStream(lpacFolderPath.resolve("$platform.zip")).buffered()
            .use { out -> response.body<InputStream>().buffered().use { `in` -> `in`.copyTo(out) } }
    }
}

suspend fun downloadEuiccInfo()
{
    for ((name, url) in euiccManifests)
    {
        val response = client.get(url)
        if (!response.status.isSuccess()) throw IllegalStateException("Network error")
        withContext(Dispatchers.IO) {
            Files.newOutputStream(resourcesPath.resolve(name)).buffered()
                .use { out -> response.body<InputStream>().buffered().use { `in` -> `in`.copyTo(out) } }
        }
    }
    Files.createDirectories(buildPath)
    eUICCInfoUpdateTimePath.writeText("${System.currentTimeMillis()}")
}

fun renameDistFiles()
{
    // jpackage names installers like MiniLPA-1.1.1.dmg or minilpa_1.1.1_amd64.deb
    val jpackageOutput = Regex("${project.name}[-_]\\d.*", RegexOption.IGNORE_CASE)
    Files.list(distPath).use { it.toList() }
        .filter { it.isRegularFile() && jpackageOutput.matches(it.name) && (packageType == null || it.extension == packageType) }
        .forEach { path ->
            val distFilePath = distPath.resolve("$distName.${path.extension}")
            Files.move(path, distFilePath, StandardCopyOption.REPLACE_EXISTING)
            if (distFilePath.extension == "msi") updateMsiProductCode(distFilePath)
        }
}

fun updateMsiProductCode(msiPath : Path)
{
    // One ProductCode per commit lets an msi install over another build of the same version
    val productCode = commitId?.takeUnless { isDirty }?.let { UUID.nameUUIDFromBytes(it.toByteArray()) } ?: UUID.randomUUID()
    providers.exec {
        commandLine(
            "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
            "-File", projectDir.resolve("scripts/Update-MSI-ProductCode.ps1").path,
            "-Path", msiPath.pathString,
            "-ProductCode", "{${productCode.toString().uppercase()}}"
        )
    }.result.get()
}

fun git(vararg args : String) : String? = runCatching {
    val output = providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }
    if (output.result.get().exitValue == 0) output.standardOutput.asText.get().trim() else null
}.getOrNull()

fun Project.switchProperty(propertyName : String) : Boolean
{
    val value = "${findProperty(propertyName)}"
    if (value.isBlank()) return true
    return value.toBoolean()
}
