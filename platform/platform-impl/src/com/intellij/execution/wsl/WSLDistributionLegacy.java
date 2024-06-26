// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.execution.wsl;

import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.intellij.openapi.util.NullableLazyValue.atomicLazyNullable;

/**
 * Represents legacy bash.exe WSL, see RUBY-20359
 */
public final class WSLDistributionLegacy extends WSLDistribution {
  private static final WslDistributionDescriptor LEGACY_WSL =
    new WslDistributionDescriptor("UBUNTU_LEGACY", "ubuntu_bash", "bash.exe", "Ubuntu (Legacy)");

  private static final String WSL_ROOT_CHUNK = "\\lxss\\rootfs";

  private static final NullableLazyValue<String> WSL_ROOT_IN_WINDOWS_PROVIDER = atomicLazyNullable(() -> {
    String localAppDataPath = System.getenv().get("LOCALAPPDATA");
    return StringUtil.isEmpty(localAppDataPath) ? null : localAppDataPath + WSL_ROOT_CHUNK;
  });

  private static @Nullable Path getExecutableRootPath() {
    String windir = System.getenv().get("windir");
    return StringUtil.isEmpty(windir) ? null : Paths.get(windir, "System32");
  }

  /**
   * @return legacy WSL ("Bash-on-Windows") if it's available, {@code null} otherwise
   */
  public static @Nullable WSLDistributionLegacy getInstance() {
    final Path executableRoot = getExecutableRootPath();
    if (executableRoot == null) return null;

    final Path executablePath = executableRoot.resolve(LEGACY_WSL.getExecutablePath());
    if (Files.exists(executablePath, LinkOption.NOFOLLOW_LINKS)) {
      return new WSLDistributionLegacy(executablePath);
    }
    return null;
  }

  private WSLDistributionLegacy(@NotNull Path executablePath) {
    super(LEGACY_WSL, executablePath);
    setVersion(1);
  }

  @Override
  protected @NotNull String getRunCommandLineParameter() {
    return "-c";
  }

  @Override
  public @Nullable String getWslPath(@NotNull Path windowsPath) {
    String wslRootInHost = WSL_ROOT_IN_WINDOWS_PROVIDER.getValue();
    if (wslRootInHost == null) {
      return null;
    }

    if (FileUtil.isAncestor(wslRootInHost, windowsPath.toString(), true)) {  // this is some internal WSL file
      return FileUtil.toSystemIndependentName(windowsPath.toString().substring(wslRootInHost.length()));
    }

    return super.getWslPath(windowsPath);
  }
}
