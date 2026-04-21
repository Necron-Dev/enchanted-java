plugins {
  id("java")
  id("maven-publish")
}

group = "net.yqloss"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8

  withSourcesJar()
  withJavadocJar()
}

tasks.withType<Javadoc> {
  (options as StandardJavadocDocletOptions).addStringOption(
    "Xdoclint:none",
    "-quiet"
  )
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
    }
  }
}
