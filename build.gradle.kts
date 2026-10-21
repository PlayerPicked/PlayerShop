plugins {
    id("java")
    id("com.gradleup.shadow") version("8.3.2")
    id("io.github.revxrsal.zapper") version("1.0.2")
    id("io.freefair.lombok") version("8.11")
}

group = "com.playerpicked"
version = "1.6.1"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.nightexpressdev.com/releases")
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://repo.bg-software.com/repository/api/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.36")

    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.17") {
        exclude(module = "lamp.common")
        exclude(module = "lamp.brigadier")
    }

    zap("org.bstats:bstats-bukkit:3.0.2")
    zap("mysql:mysql-connector-java:8.0.33")
    zap("com.h2database:h2:2.3.232")
    zap("com.zaxxer:HikariCP:6.2.1")
    zap("io.github.revxrsal:lamp.common:4.0.0-rc.17")
    zap("io.github.revxrsal:lamp.brigadier:4.0.0-rc.17")
    zap("com.github.Anon8281:UniversalScheduler:0.1.6")
    implementation("dev.dejvokep:boosted-yaml:1.3.6")

    compileOnly("com.infernalsuite.asp:api:4.0.0-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(module = "bukkit")
    }
    compileOnly("org.black_ixx:playerpoints:3.3.2")
    compileOnly("su.nightexpress.coinsengine:CoinsEngine:2.6.0")
    compileOnly("com.bgsoftware:SuperiorSkyblockAPI:2026.1")

    implementation("com.playerpicked:pickly-paper:1.0.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

zapper {
    libsFolder = "libs"
    relocationPrefix = "com.playerpicked.playershop"

    repositories {
        includeProjectRepositories()
    }

    relocate("org.bstats", "bstats")
    relocate("com.github.Anon8281.universalScheduler", "universalScheduler")
    relocate("com.playerpicked.pickly", "pickly")
}