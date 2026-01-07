plugins {
    kotlin("jvm") version "2.3.0"
    application
}

group = "at.cnoize"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.24")

    testImplementation(platform("org.junit:junit-bom:6.0.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("at.cnoize.wingetmonitor.WingetMonitorKt")
}

//tasks.jar {
//    manifest {
//        attributes["Main-Class"] = "at.cnoize.wingetmonitor.WingetMonitorKt"
//    }
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
//}
