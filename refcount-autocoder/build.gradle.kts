plugins {
    `java-gradle-plugin`
}

dependencies {
    api(project(":refcount-core"))
    api(project(":autocoder-core"))
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("commons-io:commons-io:2.13.0")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.jetbrains:annotations:24.1.0")

    testImplementation(project(":autocoder-test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.4.11")
}

gradlePlugin {
    plugins {
        create("refcountAutocoder") {
            id = "com.simiacryptus.refcount-autocoder"
            implementationClass = "com.simiacryptus.ref.RefCountAutocoderPlugin"
            displayName = "Java Reference Counter Autocoder Plugin"
            description = "Gradle plugin that automatically instruments and verifies Java reference counting code"
        }
    }
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
