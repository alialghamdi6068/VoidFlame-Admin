plugins {
    java
    id("com.gradleup.shadow") version "9.0.0"
}
group = "net.voidflame"
version = "1.0.0"
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")
}
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}
tasks.shadowJar {
    archiveBaseName.set("VoidFlame-Admin")
    archiveClassifier.set("")
}
tasks.build { dependsOn(tasks.shadowJar) }
