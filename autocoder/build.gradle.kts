plugins {
    `java-library`
}

dependencies {
    compileOnly(gradleApi())
    api("org.slf4j:slf4j-api:2.0.16")
    api("commons-io:commons-io:2.13.0")
    api("org.apache.commons:commons-lang3:3.13.0")
    api("com.google.code.findbugs:jsr305:3.0.2")
    api("org.eclipse.jdt:org.eclipse.jdt.core:3.41.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.4.11")
}
