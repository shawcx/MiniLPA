
import com.github.gmazzo.buildconfig.BuildConfigTask
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.UUID
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.apache.commons.lang3.ArchUtils
import org.apache.commons.lang3.SystemUtils
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.panteleyev.jpackage.ImageType

plugins {
    application
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
    id("org.panteleyev.jpackageplugin") version "2.1.0"
    id("com.github.gmazzo.buildconfig") version "6.1.2"
    id("io.github.ben-manes.versions") version "0.66.0"
}

group = "moe.sekiu"
version = "1.2.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.formdev:flatlaf:3.7.2")
    implementation("com.formdev:flatlaf-extras:3.7.2")
    implementation("com.formdev:flatlaf-intellij-themes:3.7.2")
    implementation("com.miglayout:miglayout-swing:11.5.2")
    implementation("com.charleskorn.kaml:kaml:0.104.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
    implementation("org.boofcv:boofcv-core:1.5.0")
    implementation("org.boofcv:boofcv-kotlin:1.5.0")
    implementation("com.google.zxing:core:3.5.4")
    implementation("com.google.zxing:javase:3.5.4")
    implementation("org.apache.commons:commons-lang3:3.21.0")
    implementation("org.slf4j:slf4j-api:2.0.20")
    implementation("ch.qos.logback:logback-classic:1.6.5")
    implementation("com.github.Dansoftowner:jSystemThemeDetector:3.9.1")
    implementation("org.apache.xmlgraphics:batik-all:1.20")
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-tiff:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-bmp:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-batik:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-psd:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-icns:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-pnm:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-iff:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-tga:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-sgi:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-pict:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-hdr:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-pcx:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-pdf:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-clippath:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-xwd:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-tiff-jdk-interop:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-tiff-jai-interop:3.15.3")
    implementation("com.twelvemonkeys.imageio:imageio-jpeg-jep262-interop:3.15.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.apache.commons:commons-lang3:3.21.0")
    }
}

val commitId = git("rev-parse", "HEAD")
val isDirty = git("diff", "--no-ext-diff", "--quiet", "--exit-code") == null
val shortCommitId = git("rev-parse", "--short", "HEAD")?.let { if (isDirty) "$it-dirty" else it }

buildConfig {
    packageName("moe.sekiu.minilpa")
    buildConfigField("VERSION", "$version")
    buildConfigField("SHORT_COMMIT_ID", project.findProperty("short-commit-id") as String? ?: shortCommitId ?: "internal")
}

application {
    mainClass.set("moe.sekiu.minilpa.MainKt")
}

// AwtInternals in MiniDropArea.kt needs java.awt opened to the app, so every way of launching it passes this
val openJavaAwt = "java.desktop/java.awt"

tasks.named<JavaExec>("run") {
    // Running from source keeps settings, logs and the extracted lpac in the (gitignored) project folder
    systemProperty("minilpa.app-data-folder", projectDir.path)
    jvmArgs("--add-opens=$openJavaAwt=ALL-UNNAMED")
}

tasks.named<KotlinCompilationTask<*>>("compileKotlin").configure {
    compilerOptions.freeCompilerArgs.addAll(
        "-opt-in=kotlin.io.encoding.ExperimentalEncodingApi",
        "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
        "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
    )
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
    jvmArgs("--add-opens=$openJavaAwt=ALL-UNNAMED")
}

kotlin {
    jvmToolchain(21)
}

data class LpacArtifact(val name : String, val sha256 : String)

// lpac is pinned to one release and each download is checked against its SHA-256; bump all of these together
val lpacVersion = "v2.3.0"
// The release's creation time; the app re-extracts its bundled lpac when this increases
val lpacReleaseTime = 1755274668000L
val lpacPlatformToArtifact = mapOf(
    "macos_universal" to LpacArtifact("lpac-darwin-universal", "13115f5a6ce632d7b4799d236de3b99175aa28eb4827b5301b2a9576b83747f1"),
    "windows_x86" to LpacArtifact("lpac-windows-x86_64-mingw", "4781e8673d19c08c41cd104e8bc59f901e352eb10e8c952bcf2aa199a915ea6f"),
    "windows_aarch64" to LpacArtifact("lpac-windows-arm64-mingw", "9b0ef205d60b4fbe459459123e38ff7cb8b29b7091b33a62d267947b10d501ad"),
    "linux_x86" to LpacArtifact("lpac-linux-x86_64", "a6ff503719f3d57d6af09d6dc9ffc1567ecabfae50693130edc74654b26c64cb"),
    "linux_aarch64" to LpacArtifact("lpac-linux-aarch64", "9ace028ca3139d37d474743e125e976eda54b5c1de53354c743f80223f1356f5")
)

val euiccManifests = mapOf(
    "eum-accredited.json" to "https://euicc-manual.osmocom.org/docs/pki/eum/accredited.json",
    "ci-manifest.json" to "https://euicc-manual.osmocom.org/docs/pki/ci/manifest.json"
)

val buildPath = layout.buildDirectory.asFile.get().toPath()
val jpackagePath = buildPath.resolve("jpackage")
val distPath = buildPath.resolve("dist")
// jpackage empties its destination on every run, so it writes here and finished packages are moved to distPath
val jpackageOutputPath = buildPath.resolve("jpackage-output")
val appImagePath = jpackageOutputPath.resolve(if (SystemUtils.IS_OS_MAC_OSX) "${project.name}.app" else project.name)
val resourcesPath = projectDir.toPath().resolve("src/main/resources")
val eUICCInfoUpdateTimePath = buildPath.resolve("euicc_info_update_time")
// CI uploads and the docs refer to the fat jar by this name
val shadowJarName = "${project.name}-all.jar"
val target = project.findProperty("target")?.toString()?.lowercase() ?: getPlatformInfo()
val packageType = project.findProperty("type")?.toString()

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

tasks.withType<BuildConfigTask>().configureEach { dependsOn("setupResources") }

tasks.register("setupResources")
{
    group = "Package"
    val skip = project.switchProperty("skip-setup-resources")
    doLast {
        val languagePackPath = buildPath.resolve("languages.zip")

        if (!skip)
        {
            if (euiccManifests.keys.any { resourcesPath.resolve(it).notExists() } || eUICCInfoUpdateTimePath.notExists())
            {
                downloadEuiccInfo()
            }

            val platforms = if (target == "all") lpacPlatformToArtifact.keys else listOf(target)
            val outdated = platforms.filter { platform ->
                val zipPath = buildPath.resolve("lpac").resolve("$platform.zip")
                zipPath.notExists() || Files.readAllBytes(zipPath).sha256() != lpacPlatformToArtifact.getValue(platform).sha256
            }
            outdated.forEach { downloadlpac(it) }

            FileSystems.newFileSystem(languagePackPath, mapOf("create" to true)).use { fs ->
                val languages = projectDir.toPath().resolve("src/main/languages/")
                for (path in Files.list(languages)) Files.copy(path, fs.getPath(path.name), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
            }
        }

        buildConfig {
            buildConfigField("LPAC_BUILD_TIME", lpacReleaseTime)
            buildConfigField("LANGUAGE_PACK_UPDATE_TIME", languagePackPath.readAttributes<BasicFileAttributes>().lastModifiedTime().toMillis())
            buildConfigField("EUICC_INFO_UPDATE_TIME", eUICCInfoUpdateTimePath.readText().toLong())
        }
    }
}

tasks.register("updateEuiccInfo")
{
    group = "Package"
    doLast { downloadEuiccInfo() }
}

tasks.register("setupjpackage")
{
    group = "Package"
    dependsOn("shadowJar")
    doLast {
        Files.createDirectories(jpackagePath)
        Files.copy(
            buildPath.resolve("libs/$shadowJarName"),
            jpackagePath.resolve(shadowJarName),
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
    archiveFileName = shadowJarName
    // java -jar honours this manifest attribute the way other launchers honour --add-opens
    manifest { attributes("Add-Opens" to openJavaAwt) }
    // Shadow merges these with a transformer, so let duplicates reach it rather than dropping them first
    filesMatching("META-INF/*.kotlin_module") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    minimize {
        exclude(dependency("ch.qos.logback:logback-classic:.*"))
        exclude(dependency("net.java.dev.jna:jna:.*"))
        exclude(dependency("com.formdev:flatlaf-intellij-themes:.*"))
        exclude(dependency("com.twelvemonkeys.imageio:.*:.*"))
        exclude(dependency("com.github.jai-imageio:jai-imageio-core:.*"))
    }
}

tasks.jpackage {
    group = "Package"
    dependsOn("setupjpackage")
    input = jpackagePath.toFile()
    destination = jpackageOutputPath.toFile()
    appName = project.name
    appVersion = "$version"
    vendor = "esim.moe"
    mainJar = shadowJarName
    mainClass = application.mainClass
    javaOptions = listOf("-Dfile.encoding=UTF-8", "--add-opens=$openJavaAwt=ALL-UNNAMED")
    addModules = listOf("java.base", "java.desktop", "java.naming", "jdk.unsupported")

    if (packageType != null)
    {
        type = ImageType.entries.find { it.value == packageType } ?: throw IllegalArgumentException("Type $packageType not found")
    }

    if (project.hasProperty("native-wayland")) javaOptions.add("-Dawt.toolkit.name=WLToolkit")
    windows {
        icon = file("src/main/icons/window.ico")
        if (packageType != "app-image")
        {
            winDirChooser = true
            winMenu = true
            winMenuGroup = "MiniLPA"
            winShortcut = true
            winShortcutPrompt = true
        }
    }

    linux {
        icon = file("src/main/icons/window.png")
        if (packageType != "app-image")
        {
            linuxShortcut = true
            linuxMenuGroup = "MiniLPA"
            installDir = "/usr/"
        }
    }

    mac {
        icon = file("src/main/icons/window.icns")
        if (packageType != "app-image")
        {
            macPackageIdentifier = "moe.sekiu.MiniLPA"
            macAppCategory = "MiniLPA"
        }
    }

    if (packageType == "app-image") finalizedBy("zipAppImage")
    else doLast { renameDistFiles() }
}

// A macOS app-image is a signed .app bundle with symlinks inside, which Gradle's Zip would replace with copies and so
// break the signature; ditto keeps them and puts the bundle itself at the archive root. --norsrc leaves out extended
// attributes, which plain unzip would otherwise extract as stray ._ files that also break the signature
if (SystemUtils.IS_OS_MAC_OSX) tasks.register("zipAppImage")
{
    dependsOn("jpackage")
    doLast {
        val zipPath = distPath.resolve("$distName.zip")
        Files.createDirectories(distPath)
        Files.deleteIfExists(zipPath)
        providers.exec { commandLine("ditto", "-c", "-k", "--norsrc", "--keepParent", appImagePath.pathString, zipPath.pathString) }.result.get()
        appImagePath.toFile().deleteRecursively()
    }
}
else tasks.register<Zip>("zipAppImage")
{
    dependsOn("jpackage")
    destinationDirectory = distPath.toFile()
    archiveFileName = provider { "$distName.zip" }
    // Gradle 9 normalizes archive permissions by default, which would drop the launcher's executable bit
    useFileSystemPermissions()
    from(appImagePath)
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

// GitHub release downloads redirect to another https host, which NORMAL follows
val httpClient : HttpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

fun download(url : String) : ByteArray
{
    val response = httpClient.send(HttpRequest.newBuilder(URI(url)).build(), HttpResponse.BodyHandlers.ofByteArray())
    if (response.statusCode() !in 200..299) throw IllegalStateException("Downloading $url failed with HTTP ${response.statusCode()}")
    return response.body()
}

fun downloadlpac(platform : String)
{
    val artifact = lpacPlatformToArtifact.getValue(platform)
    val zip = download("https://github.com/estkme-group/lpac/releases/download/$lpacVersion/${artifact.name}.zip")
    val sha256 = zip.sha256()
    if (sha256 != artifact.sha256) throw IllegalStateException("${artifact.name}.zip SHA-256 is $sha256, expected ${artifact.sha256}")
    val lpacFolderPath = buildPath.resolve("lpac")
    Files.createDirectories(lpacFolderPath)
    Files.write(lpacFolderPath.resolve("$platform.zip"), zip)
}

fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }

fun downloadEuiccInfo()
{
    for ((name, url) in euiccManifests) Files.write(resourcesPath.resolve(name), download(url))
    Files.createDirectories(buildPath)
    eUICCInfoUpdateTimePath.writeText("${System.currentTimeMillis()}")
}

fun renameDistFiles()
{
    Files.createDirectories(distPath)
    Files.list(jpackageOutputPath).use { it.toList() }
        .filter { it.isRegularFile() }
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
