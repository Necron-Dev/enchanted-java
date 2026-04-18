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
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
    }
  }
}
