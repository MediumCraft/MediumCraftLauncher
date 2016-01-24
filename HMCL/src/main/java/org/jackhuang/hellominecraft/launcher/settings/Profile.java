/*
 * Hello Minecraft! Launcher.
 * Copyright (C) 2013  huangyuhui
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see {http://www.gnu.org/licenses/}.
 */
package org.jackhuang.hellominecraft.launcher.settings;

import java.io.File;
import java.io.IOException;
import org.jackhuang.hellominecraft.utils.C;
import org.jackhuang.hellominecraft.utils.logging.HMCLog;
import org.jackhuang.hellominecraft.launcher.Main;
import org.jackhuang.hellominecraft.launcher.api.PluginManager;
import org.jackhuang.hellominecraft.launcher.core.LauncherVisibility;
import org.jackhuang.hellominecraft.launcher.core.MCUtils;
import org.jackhuang.hellominecraft.launcher.core.launch.LaunchOptions;
import org.jackhuang.hellominecraft.launcher.core.service.IMinecraftService;
import org.jackhuang.hellominecraft.utils.system.IOUtils;
import org.jackhuang.hellominecraft.launcher.core.version.GameDirType;
import org.jackhuang.hellominecraft.utils.StrUtils;
import org.jackhuang.hellominecraft.utils.Utils;
import org.jackhuang.hellominecraft.utils.EventHandler;
import org.jackhuang.hellominecraft.utils.system.Java;
import org.jackhuang.hellominecraft.utils.system.JdkVersion;
import org.jackhuang.hellominecraft.utils.system.OS;

/**
 *
 * @author huangyuhui
 */
public final class Profile {

    private String name, selectedMinecraftVersion = "", javaArgs, minecraftArgs, maxMemory, permSize, width, height, userProperties;
    private String gameDir, javaDir, precalledCommand, serverIp, java;
    private boolean fullscreen, debug, noJVMArgs, canceledWrapper;

    /**
     * 0 - Close the launcher when the game starts.<br/>
     * 1 - Hide the launcher when the game starts.<br/>
     * 2 - Keep the launcher open.<br/>
     */
    private int launcherVisibility;

    /**
     * 0 - .minecraft<br/>
     * 1 - .minecraft/versions/&lt;version&gt;/<br/>
     */
    private int gameDirType;

    protected transient IMinecraftService service;
    public transient final EventHandler<String> propertyChanged = new EventHandler<>(this);

    public Profile() {
        this("Default");
    }

    public Profile(String name) {
        this.name = name;
        gameDir = MCUtils.getInitGameDir().getPath();
        debug = fullscreen = canceledWrapper = false;
        launcherVisibility = gameDirType = 0;
        PluginManager.NOW_PLUGIN.onInitializingProfile(this);
        javaDir = java = minecraftArgs = serverIp = precalledCommand = "";
    }

    public void initialize(int gameDirType) {
        this.gameDirType = gameDirType;
    }

    public Profile(Profile v) {
        this();
        if (v == null)
            return;
        name = v.name;
        gameDir = v.gameDir;
        maxMemory = v.maxMemory;
        width = v.width;
        height = v.height;
        java = v.java;
        fullscreen = v.fullscreen;
        javaArgs = v.javaArgs;
        javaDir = v.javaDir;
        debug = v.debug;
        minecraftArgs = v.minecraftArgs;
        permSize = v.permSize;
        gameDirType = v.gameDirType;
        canceledWrapper = v.canceledWrapper;
        noJVMArgs = v.noJVMArgs;
        launcherVisibility = v.launcherVisibility;
        precalledCommand = v.precalledCommand;
        serverIp = v.serverIp;
    }

    public IMinecraftService service() {
        if (service == null)
            service = PluginManager.NOW_PLUGIN.provideMinecraftService(this);
        return service;
    }

    public String getSettingsSelectedMinecraftVersion() {
        return selectedMinecraftVersion;
    }

    public String getSelectedVersion() {
        String v = selectedMinecraftVersion;
        if (StrUtils.isBlank(v) || service.version().getVersionById(v) == null) {
            if (service.version().getVersionCount() > 0)
                v = service.version().getOneVersion().id;
            if (StrUtils.isNotBlank(v))
                setSelectedMinecraftVersion(v);
        }
        return StrUtils.isBlank(v) ? null : v;
    }

    public transient final EventHandler<String> selectedVersionChangedEvent = new EventHandler<>(this);

    public void setSelectedMinecraftVersion(String selectedMinecraftVersion) {
        this.selectedMinecraftVersion = selectedMinecraftVersion;
        propertyChanged.execute("selectedMinecraftVersion");
        selectedVersionChangedEvent.execute(selectedMinecraftVersion);
    }

    public String getGameDir() {
        if (StrUtils.isBlank(gameDir))
            gameDir = MCUtils.getInitGameDir().getPath();
        return IOUtils.addSeparator(gameDir);
    }

    public String getCanonicalGameDir() {
        return IOUtils.tryGetCanonicalFolderPath(getGameDirFile());
    }

    public File getCanonicalGameDirFile() {
        return IOUtils.tryGetCanonicalFile(getGameDirFile());
    }

    public File getGameDirFile() {
        return new File(getGameDir());
    }

    public Profile setGameDir(String gameDir) {
        this.gameDir = gameDir;
        service().version().refreshVersions();
        propertyChanged.execute("gameDir");
        return this;
    }

    public String getJavaDir() {
        Java j = getJava();
        if (j.getHome() == null)
            return javaDir;
        else
            return j.getJava();
    }

    public String getSettingsJavaDir() {
        return javaDir;
    }

    public File getJavaDirFile() {
        return new File(getJavaDir());
    }

    public void setJavaDir(String javaDir) {
        this.javaDir = javaDir;
        propertyChanged.execute("javaDir");
    }

    public Java getJava() {
        return Java.JAVA.get(getJavaIndexInAllJavas());
    }

    public int getJavaIndexInAllJavas() {
        if (StrUtils.isBlank(java) && StrUtils.isNotBlank(javaDir))
            java = "Custom";
        int idx = Java.JAVA.indexOf(new Java(java, null));
        if (idx == -1) {
            java = "Default";
            idx = 0;
        }
        return idx;
    }

    public void setJava(Java java) {
        if (java == null)
            this.java = Java.JAVA.get(0).getName();
        else {
            int idx = Java.JAVA.indexOf(java);
            if (idx == -1)
                return;
            this.java = java.getName();
        }
        propertyChanged.execute("java");
    }

    public File getFolder(String folder) {
        if (getSelectedVersion() == null)
            return new File(getCanonicalGameDirFile(), folder);
        return service().version().getRunDirectory(getSelectedVersion(), folder);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        propertyChanged.execute("name");
    }

    public String getJavaArgs() {
        if (StrUtils.isBlank(javaArgs))
            return "";
        return javaArgs;
    }

    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
        propertyChanged.execute("javaArgs");
    }

    public boolean hasJavaArgs() {
        return StrUtils.isNotBlank(getJavaArgs().trim());
    }

    public String getMaxMemory() {
        if (StrUtils.isBlank(maxMemory))
            return String.valueOf(Utils.getSuggestedMemorySize());
        return maxMemory;
    }

    public void setMaxMemory(String maxMemory) {
        this.maxMemory = maxMemory;
        propertyChanged.execute("maxMemory");
    }

    public String getWidth() {
        if (StrUtils.isBlank(width))
            return "854";
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
        propertyChanged.execute("width");
    }

    public String getHeight() {
        if (StrUtils.isBlank(height))
            return "480";
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
        propertyChanged.execute("height");
    }

    public String getUserProperties() {
        if (userProperties == null)
            return "";
        return userProperties;
    }

    public void setUserProperties(String userProperties) {
        this.userProperties = userProperties;
        propertyChanged.execute("userProperties");
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        propertyChanged.execute("fullscreen");
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        propertyChanged.execute("debug");
    }

    public LauncherVisibility getLauncherVisibility() {
        return LauncherVisibility.values()[launcherVisibility];
    }

    public void setLauncherVisibility(LauncherVisibility launcherVisibility) {
        this.launcherVisibility = launcherVisibility.ordinal();
        propertyChanged.execute("launcherVisibility");
    }

    public GameDirType getGameDirType() {
        if (gameDirType < 0 || gameDirType > 1)
            setGameDirType(GameDirType.ROOT_FOLDER);
        return GameDirType.values()[gameDirType];
    }

    public void setGameDirType(GameDirType gameDirType) {
        this.gameDirType = gameDirType.ordinal();
        service().version().setGameDirType(getGameDirType());
        propertyChanged.execute("gameDirType");
    }

    public String getPermSize() {
        return permSize;
    }

    public void setPermSize(String permSize) {
        this.permSize = permSize;
        propertyChanged.execute("permSize");
    }

    public boolean isNoJVMArgs() {
        return noJVMArgs;
    }

    public void setNoJVMArgs(boolean noJVMArgs) {
        this.noJVMArgs = noJVMArgs;
        propertyChanged.execute("noJVMArgs");
    }

    public String getMinecraftArgs() {
        return minecraftArgs;
    }

    public void setMinecraftArgs(String minecraftArgs) {
        this.minecraftArgs = minecraftArgs;
        propertyChanged.execute("minecraftArgs");
    }

    public boolean isCanceledWrapper() {
        return canceledWrapper;
    }

    public void setCanceledWrapper(boolean canceledWrapper) {
        this.canceledWrapper = canceledWrapper;
        propertyChanged.execute("canceledWrapper");
    }

    public String getPrecalledCommand() {
        return precalledCommand;
    }

    public void setPrecalledCommand(String precalledCommand) {
        this.precalledCommand = precalledCommand;
        propertyChanged.execute("precalledCommand");
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
        propertyChanged.execute("serverIp");
    }

    public void checkFormat() {
        gameDir = gameDir.replace('/', OS.os().fileSeparator).replace('\\', OS.os().fileSeparator);
    }

    public LaunchOptions createLaunchOptions() {
        LaunchOptions x = new LaunchOptions();
        x.setCanceledWrapper(isCanceledWrapper());
        x.setDebug(isDebug());
        x.setFullscreen(isFullscreen());
        x.setGameDir(getCanonicalGameDirFile());
        x.setGameDirType(getGameDirType());
        x.setHeight(getHeight());
        x.setJavaArgs(getJavaArgs());
        x.setLaunchVersion(getSelectedVersion());
        x.setMaxMemory(getMaxMemory());
        x.setMinecraftArgs(getMinecraftArgs());
        x.setName(getName());
        x.setNoJVMArgs(isNoJVMArgs());
        x.setPermSize(getPermSize());
        x.setPrecalledCommand(getPrecalledCommand());
        x.setProxyHost(Settings.getInstance().getProxyHost());
        x.setProxyPort(Settings.getInstance().getProxyPort());
        x.setProxyUser(Settings.getInstance().getProxyUserName());
        x.setProxyPass(Settings.getInstance().getProxyPassword());
        x.setServerIp(getServerIp());
        x.setUserProperties(getUserProperties());
        x.setVersionName(Main.makeTitle());
        x.setWidth(getWidth());

        String str = getJavaDir();
        if (!getJavaDirFile().exists()) {
            HMCLog.err(C.i18n("launch.wrong_javadir"));
            setJava(null);
            str = getJavaDir();
        }
        JdkVersion jv = new JdkVersion(str);
        if (Settings.getInstance().getJava().contains(jv))
            jv = Settings.getInstance().getJava().get(Settings.getInstance().getJava().indexOf(jv));
        else
            try {
                jv = JdkVersion.getJavaVersionFromExecutable(str);
                Settings.getInstance().getJava().add(jv);
                Settings.save();
            } catch (IOException ex) {
                HMCLog.warn("Failed to get java version", ex);
                jv = null;
            }
        x.setJava(jv);
        x.setJavaDir(str);
        return x;
    }
}
