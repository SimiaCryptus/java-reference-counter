plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "java-reference-counter"

include(":refcount-core")
project(":refcount-core").projectDir = file("core")

include(":autocoder-core")
project(":autocoder-core").projectDir = file("autocoder")

include(":refcount-autocoder")

include(":autocoder-test")

include(":refcount-demo")
project(":refcount-demo").projectDir = file("demo")
