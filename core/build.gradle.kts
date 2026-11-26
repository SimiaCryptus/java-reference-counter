plugins {
    `java-library`
}

dependencies {
    api("org.checkerframework:checker-qual:3.42.0")
    api("org.slf4j:slf4j-api:2.0.16")
    api("com.google.code.findbugs:jsr305:3.0.2")
    api("org.jetbrains:annotations:24.1.0")
    api("com.google.guava:guava:33.1.0-jre")
    api("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.4.11")
}
