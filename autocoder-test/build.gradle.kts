plugins {
    `java-library`
}

dependencies {
    api(project(":autocoder-core"))
    api("org.slf4j:slf4j-api:2.0.16")
    api("commons-io:commons-io:2.13.0")
    api("com.google.code.findbugs:jsr305:3.0.2")
    api("org.jetbrains:annotations:24.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.4.11")
}

val demoClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    demoClasspath(project(":refcount-demo"))
}

tasks.test {
    systemProperty("demo.project.dir", project(":refcount-demo").projectDir.absolutePath)
    systemProperty("demo.classpath", demoClasspath.asPath)
}
