plugins {
    `java-library`
}

dependencies {
    implementation(project(":refcount-core"))
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("commons-io:commons-io:2.13.0")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("ch.qos.logback:logback-classic:1.4.11")
}
