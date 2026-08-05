pluginManagement {
    repositories {
        maven { url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
        gradlePluginPortal()
        mavenCentral()
    }
    // Convention-pluginene hentes fra build-logic-artefaktet direkte, ikke via plugin-markører.
    // Markørene ligger under en egen gruppe (`tiltakspenger.<navn>`) som ikke går gjennom Nav-speilet -
    // verifisert 404 der, mens `com.github.navikt.tiltakspenger-libs:build-logic` svarer 200.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("tiltakspenger.")) {
                useModule("com.github.navikt.tiltakspenger-libs:build-logic:${providers.gradleProperty("byggoppsettVersjon").get()}")
            }
        }
    }
}

dependencyResolutionManagement {
    // Repositories deklareres her, ikke i byggfila; en modul som legger til sitt eget feiler bygget.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        maven { url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
    }
    versionCatalogs {
        create("libs") {
            from("com.github.navikt.tiltakspenger-libs:versjonskatalog:${providers.gradleProperty("byggoppsettVersjon").get()}")
        }
    }
}

rootProject.name = "tiltakspenger-tiltak"
