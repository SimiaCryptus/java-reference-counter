plugins {
    `java-gradle-plugin`
}

dependencies {
    api(project(":refcount-core"))
    api("org.ow2.asm:asm:9.7")
    api("org.ow2.asm:asm-tree:9.7")
    api("org.ow2.asm:asm-commons:9.7")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.jetbrains:annotations:24.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.4.11")
}

gradlePlugin {
    plugins {
        create("refcountBytecode") {
            id = "com.simiacryptus.refcount-bytecode"
            implementationClass = "com.simiacryptus.ref.bytecode.RefCountBytecodePlugin"
            displayName = "Java Reference Counter Bytecode Analysis Plugin"
            description = "Gradle plugin for bytecode reference-counting analysis and verification of compiled and non-Java JVM modules"
        }
    }
}

val demoClasses by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    demoClasses(project(":refcount-demo"))
}

tasks.test {
    dependsOn(":refcount-demo:jar")
    systemProperty("demo.jar.files", demoClasses.asPath)
    systemProperty("demo.classes.dir", project(":refcount-demo").layout.buildDirectory.dir("classes/java/main").get().asFile.absolutePath)
}
