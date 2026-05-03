package net.yqloss.enchant.plugin;

import org.gradle.api.provider.MapProperty;

public abstract class EnchantedJavaExtension {
  public abstract MapProperty<String, Object> getProperties();
}
