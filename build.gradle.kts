plugins {
    base
    `jacoco-report-aggregation`
}

allprojects {
    group = "com.simiacryptus"
    version = "2.1.0"

    repositories {
        mavenCentral()
    }
}

dependencies {
    jacocoAggregation(project(":refcount-core"))
    jacocoAggregation(project(":autocoder-core"))
    jacocoAggregation(project(":refcount-autocoder"))
    jacocoAggregation(project(":autocoder-test"))
    jacocoAggregation(project(":refcount-demo"))
}

subprojects {
    plugins.withType<JavaPlugin> {
        apply(plugin = "jacoco")

        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        tasks.withType<JavaCompile> {
            options.encoding = "UTF-8"
        }

        tasks.withType<Test> {
            useJUnitPlatform()
            finalizedBy(tasks.named("jacocoTestReport"))
        }

        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.named("test"))
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }
    }
}

reporting {
    reports {
        val testCodeCoverageReport by creating(JacocoCoverageReport::class) {
            testType.set(TestSuiteType.UNIT_TEST)
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
