plugins {
    java
}

group = "dev.wndrxz"
version = "0.1.2"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    // Gradle 9 wants the platform launcher on the test runtime classpath.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


// tests compile against paper-api too, mirror the compileOnly deps over
configurations {
    testImplementation {
        extendsFrom(configurations.compileOnly.get())
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
