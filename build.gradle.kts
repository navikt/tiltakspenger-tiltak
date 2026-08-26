import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val ktorVersion = "3.4.3"
val jacksonVersion = "3.2.2"
val jacksonAnnotationsVersion = "2.22"
val kotestVersion = "6.2.4"
val felleslibVersion = "0.0.20260819100154"

plugins {
    application
    id("tiltakspenger.kotlin")
    id("tiltakspenger.githooks")
    id("org.jetbrains.kotlinx.kover") version "0.9.9"
}

dependencies {
    // Lås versjonene på alle Kotlin-komponenter til samme versjon
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))

    // Lås alle io.netty:* til samme versjon som forsikring mot fremtidig 4.1/4.2-drift.
    // ktor-server-netty drar inn netty 4.2.x; en BOM hindrer at en transitiv avhengighet
    // senere blander inn 4.1.x og legger duplikate baseklasser på classpath (jf. `-cp lib/*`).
    implementation(platform("io.netty:netty-bom:4.2.17.Final"))
    implementation("ch.qos.logback:logback-classic:1.5.38")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")
    implementation("org.jetbrains:annotations:26.1.0")

    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("com.github.navikt.tiltakspenger-libs:tiltak-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:ktor-common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:logging:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:texas:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:json:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:periodisering:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:httpklient-infrastruktur:$felleslibVersion")

    // Brukes direkte i klient- og service-koden (Either); gjøres eksplisitt i stedet for å arves transitivt fra libs.
    implementation("io.arrow-kt:arrow-core:2.2.3")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson3:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    // Autentisering og validering av tokens
    implementation("io.ktor:ktor-server-auth:$ktorVersion")

    implementation("io.ktor:ktor-serialization:$ktorVersion")

    implementation("tools.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonAnnotationsVersion")

    // Delte arkitekturregler; drar inn konsist transitivt (api-avhengighet). Egen versjon inntil felleslibVersion bumpes.
    testImplementation("com.github.navikt.tiltakspenger-libs:konsist-regler:$felleslibVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions:$kotestVersion")

    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.4.10")
    testImplementation("com.github.navikt.tiltakspenger-libs:ktor-test-common:$felleslibVersion")
    testImplementation("com.github.navikt.tiltakspenger-libs:test-common:$felleslibVersion")
    testImplementation(testFixtures("com.github.navikt.tiltakspenger-libs:httpklient-infrastruktur:$felleslibVersion"))
}

// --- Kover --------------------------------------------------------------------
// Klientene som er migrert til libs sin `httpklient` skal ha full linjedekning, jf. HTTP-klient-seksjonen i AGENTS-backend.md.
// Utvid lista når repoet får flere klienter.
val httpklientKlasserMedDekningskrav =
    listOf(
        "no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.TiltakshistorikkClient",
        "no.nav.tiltakspenger.tiltak.person.infra.http.pdl.PdlClient",
    )

kover {
    currentProject {
        instrumentation {
            // Instrumenter kun klassene dekningsgaten måler (`*`-suffikset tar med indre klasser og lambdaer).
            includedClasses.addAll(httpklientKlasserMedDekningskrav.map { "$it*" })
        }
    }
    reports {
        total {
            filters {
                includes {
                    classes(httpklientKlasserMedDekningskrav)
                }
            }
            verify {
                onCheck = true
                rule("migrerte httpklient-klienter har full linjedekning") {
                    bound {
                        minValue = 100
                        coverageUnits = CoverageUnit.LINE
                        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    }
                }
            }
        }
    }
}

// En includes-liste med feilstavet klassenavn ville gitt en tom rapport, og en tom rapport består terskelen på 100 % uten å måle noe.
tasks.named("koverXmlReport") {
    val xmlReport = layout.buildDirectory.file("reports/kover/report.xml")
    doLast {
        val xml = xmlReport.get().asFile
        val classCount = xml.readText().split("<class ").size - 1
        if (classCount == 0) throw GradleException("Kover-rapporten inneholder ingen klasser – includes-filteret treffer ingenting.")
    }
}

application {
    mainClass.set("no.nav.tiltakspenger.tiltak.ApplicationKt")
}


tasks {
    test {
        // JUnit 5-støtte
        useJUnitPlatform()
        // https://phauer.com/2018/best-practices-unit-testing-kotlin/
        systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
        // Testene kjører parallelt, både på tvers av klasser og mellom metodene i én klasse.
        // Det er en håndhevelse, ikke bare fart: ingen tester skal dele muterbar tilstand, og med `per_class`-livssyklusen
        // deles testinstansen mellom metodene — et muterbart instansfelt gir da flakiness her i stedet for å overleve til CI.
        // Deler to tester tilstand (køer i en fake, en global mock, et statisk register), skal det feile høyt og tidlig.
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
        testLogging {
            // Vi logger bare feilede og hoppede tester når Gradle kjører.
            events("skipped", "failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
