plugins {
    id("java")
    id("java-library")
    id("maven-publish")
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "org.inksnow.cputil"
    version = System.getenv("BUILD_NUMBER")
        ?.let { "1.$it" }
        ?: "1.0-SNAPSHOT"

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        withSourcesJar()
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                if (rootProject == project) {
                    artifactId = "core"
                }
                from(components["java"])
            }
        }

        repositories {
            if (System.getenv("CI").toBoolean()) {
                maven("https://s0.blobs.inksnow.org/maven/") {
                    credentials {
                        username = System.getenv("IREPO_USERNAME")
                        password = System.getenv("IREPO_PASSWORD")
                    }
                }
            } else {
                maven(rootProject.buildDir.resolve("repo"))
            }

        }
    }

    tasks.test {
        useJUnitPlatform()
    }
}

dependencies {
    api("org.slf4j:slf4j-api:1.7.36")
}