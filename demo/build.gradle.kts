plugins {
    `java-library`
}

val autocoder by configurations.creating

dependencies {
    implementation(project(":refcount-core"))
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("commons-io:commons-io:2.13.0")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    autocoder(project(":refcount-autocoder"))
    autocoder("ch.qos.logback:logback-classic:1.4.11")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("ch.qos.logback:logback-classic:1.4.11")
}

val instrumentedSrcDir = layout.buildDirectory.dir("instrumented-src/main/java")

val copyDemoSources by tasks.registering(Sync::class) {
    from("src/main/java")
    into(instrumentedSrcDir)
}

val instrumentDemo by tasks.registering(JavaExec::class) {
    dependsOn(copyDemoSources, configurations.compileClasspath, autocoder)
    inputs.dir("src/main/java")
    outputs.dir(instrumentedSrcDir)
    classpath = autocoder
    mainClass.set("com.simiacryptus.ref.Insert")
    doFirst {
        val outDir = instrumentedSrcDir.get().asFile
        val cp = configurations.compileClasspath.get().asPath
        args = listOf(outDir.absolutePath, outDir.absolutePath, cp, "true", "true")
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf(instrumentedSrcDir))
    }
}

tasks.compileJava {
    dependsOn(instrumentDemo)
}
