plugins {
    id("java")
    id("application")
}

group = "com.github.sibmaks"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.poi:poi:5.4.1")
    implementation("org.apache.poi:poi-ooxml:5.4.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass = "com.github.sibmaks.Application"
}

tasks.test {
    useJUnitPlatform()
}