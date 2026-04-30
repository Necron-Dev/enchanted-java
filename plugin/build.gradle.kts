plugins {
  id("java-gradle-plugin")
  id("maven-publish")
  id("com.gradleup.shadow") version "9.2.2"
}

group = "net.yqloss"
version = "0.5.0"

repositories {
  mavenCentral()
}

val shade by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

configurations.compileOnly.get().extendsFrom(shade)

dependencies {
  shade("org.ow2.asm:asm:9.9")
  shade("org.ow2.asm:asm-tree:9.9")
  shade("org.ow2.asm:asm-analysis:9.9")
  shade("org.ow2.asm:asm-util:9.9")
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

val agentManifestAttributes = mapOf(
  "Premain-Class" to "net.yqloss.enchant.plugin.EnchantAgent",
  "Agent-Class" to "net.yqloss.enchant.plugin.EnchantAgent",
  "Can-Redefine-Classes" to "true",
  "Can-Retransform-Classes" to "true"
)

tasks.jar {
  manifest {
    attributes(agentManifestAttributes)
  }
}

tasks.shadowJar {
  archiveClassifier.set("")
  configurations = listOf(shade)
  relocate("org.objectweb.asm", "net.yqloss.enchant.asm")
  manifest {
    attributes(agentManifestAttributes)
  }
}

tasks.build {
  dependsOn(tasks.shadowJar)
}
