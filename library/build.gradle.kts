plugins {
  id("java")
  id("maven-publish")
}

group = "net.yqloss"
version = "0.0.1"

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
