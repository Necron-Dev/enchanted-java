plugins {
  id("java-gradle-plugin")
  id("maven-publish")
}

group = "net.yqloss"
version = "0.0.2"

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.ow2.asm:asm:9.9")
  implementation("org.ow2.asm:asm-tree:9.9")
  implementation("org.ow2.asm:asm-analysis:9.9")
  implementation("org.ow2.asm:asm-util:9.9")
}

gradlePlugin {
  plugins {
    create("enchanted-java-plugin") {
      id = "net.yqloss.enchanted-java-plugin"
      implementationClass = "net.yqloss.enchant.plugin.MyPlugin"
    }
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}
