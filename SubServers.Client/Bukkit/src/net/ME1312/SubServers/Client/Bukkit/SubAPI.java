package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataProtocol;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Bukkit.Graphic.UIHandler;
import net.ME1312.SubServers.Client.Common.ClientAPI;

import org.bukkit.Bukkit;

import java.util.*;

/**
 * SubAPI Class
 */
public final class SubAPI extends ClientAPI {
    LinkedList<Runnable> reloadListeners = new LinkedList<Runnable>();
    private final SubPlugin plugin;
    private static SubAPI api;
    String name;

    SubAPI(SubPlugin plugin) {
        this.plugin = plugin;
        GAME_VERSION = getGameVersion();
        api = this;
    }

    /**
     * Gets the SubAPI Methods
     *
     * @return SubAPI
     */
    public static SubAPI getInstance() {
        return api;
    }

    /**
     * Gets the SubServers Internals
     *
     * @deprecated Use SubAPI Methods when available
     * @return SubPlugin Internals
     */
    @Deprecated
    public SubPlugin getInternals() {
        return plugin;
    }

    /**
     * Adds a SubAPI Reload Listener
     *
     * @param reload An Event that will be called after SubAPI is soft-reloaded
     */
    public void addListener(Runnable reload) {
        if (reload != null) reloadListeners.add(reload);
    }

    /**
     * Get the Server Name
     *
     * @return Server Name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the SubData Network Connections
     *
     * @return SubData Network Connections
     */
    public DataClient[] getSubDataNetwork() {
        LinkedList<Integer> keys = new LinkedList<Integer>(plugin.subdata.keySet());
        LinkedList<SubDataClient> channels = new LinkedList<SubDataClient>();
        Collections.sort(keys);
        for (Integer channel : keys) channels.add(plugin.subdata.get(channel));
        return channels.toArray(new DataClient[0]);
    }

    /**
     * Gets the SubData Network Protocol
     *
     * @return SubData Network Protocol
     */
    public DataProtocol getSubDataProtocol() {
        return plugin.subprotocol;
    }

    /**
     * Gets the current SubServers Lang Channels
     *
     * @return SubServers Lang Channel list
     */
    public Collection<String> getLangChannels() {
        return plugin.lang.value().keySet();
    }

    /**
     * Gets values from the SubServers Lang
     *
     * @param channel Lang Channel
     * @return Lang Value
     */
    public Map<String, String> getLang(String channel) {
        if (Util.isNull(channel)) throw new NullPointerException();
        return new LinkedHashMap<>(plugin.lang.value().get(channel.toLowerCase()));
    }

    /**
     * Gets the Graphics Handler
     *
     * @return Graphics Handler
     */
    public UIHandler getGraphicHandler() {
        return plugin.gui;
    }

    /**
     * Sets the Graphics Handler for SubServers to use
     *
     * @param graphics Graphics Handler
     */
    public void setGraphicHandler(UIHandler graphics) {
        if (plugin.gui != null) plugin.gui.disable();
        plugin.gui = graphics;
    }

    /**
     * Gets the SubServers Version
     *
     * @return SubServers Version
     */
    public Version getPluginVersion() {
        return plugin.version;
    }

    /**
     * Gets the SubServers Build Signature
     *
     * @return SubServers Build Signature (or null if unsigned)
     */
    public Version getPluginBuild() {
        return (SubPlugin.class.getPackage().getSpecificationTitle() != null)?new Version(SubPlugin.class.getPackage().getSpecificationTitle()):null;
    }

    /**
     * Gets the Server Version
     *
     * @return Server Version
     */
    public Version getServerVersion() {
        return new Version(Bukkit.getServer().getVersion());
    }

    /**
     * Gets the Minecraft Version
     *
     * @return Minecraft Version
     */
    public Version getGameVersion() {
        if (GAME_VERSION == null) {
            if (System.getProperty("subservers.minecraft.version", "").length() > 0) {
                return new Version(System.getProperty("subservers.minecraft.version"));
            } else {
                try {
                    return new Version(Bukkit.getBukkitVersion().split("-")[0]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    plugin.getLogger().warning("Could not determine this server's game version; Now using 1.x.x as a placeholder.");
                    plugin.getLogger().warning("Use this launch argument to specify what version this server serves: -Dsubservers.minecraft.version=1.x.x");
                    return new Version("1.x.x");
                }
            }
        } else return GAME_VERSION;
    }
    private final Version GAME_VERSION;
}
