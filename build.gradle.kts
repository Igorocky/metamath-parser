plugins {
    kotlin("jvm") version "1.5.30"
}

group = "org.igye"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
//    implementation("com.google.code.gson:gson:2.8.5")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.9")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.9")
    testImplementation("junit:junit:4.+")
}