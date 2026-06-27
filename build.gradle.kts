import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val mockkVersion = "1.14.11"
val ktorVersion = "3.4.3"
val jacksonVersion = "3.2.0"
val jacksonAnnotationsVersion = "2.22"
val kotestVersion = "6.2.1"
val felleslibVersion = "0.0.842"

plugins {
    application
    kotlin("jvm") version "2.4.0"
    id("com.diffplug.spotless") version "8.7.0"
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))
    implementation("ch.qos.logback:logback-classic:1.5.34")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")
    implementation("org.jetbrains:annotations:26.1.0")

    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("com.github.navikt.tiltakspenger-libs:tiltak-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:logging:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:texas:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:json:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:periodisering:$felleslibVersion")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson3:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    // Autentisering og validering av tokens
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")

    implementation("tools.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonAnnotationsVersion")

    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions:$kotestVersion")

    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.4.0")
    testImplementation("com.github.navikt.tiltakspenger-libs:ktor-test-common:$felleslibVersion")
}

application {
    mainClass.set("no.nav.tiltakspenger.tiltak.ApplicationKt")
}

spotless {
    kotlin {
        ktlint()
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_max-line-length" to "off",
                    "ktlint_standard_function-signature" to "disabled",
                    "ktlint_standard_function-expression-body" to "disabled",
                ),
            )
    }
}

tasks {
    kotlin {
        jvmToolchain(25)
        compilerOptions {
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
        }
    }
    test {
        // JUnit 5 support
        useJUnitPlatform()
        // https://phauer.com/2018/best-practices-unit-testing-kotlin/
        systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
        testLogging {
            // We only want to log failed and skipped tests when running Gradle.
            events("skipped", "failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
    register<Copy>("gitHooks") {
        from(file(".scripts/pre-commit"))
        into(file(".git/hooks"))
    }
}
