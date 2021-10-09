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
    testImplementation("junit:junit:4.+")
}