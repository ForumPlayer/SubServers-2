package net.ME1312.SubServers.Bungee.Host.External;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Event.SubCreateEvent;
import net.ME1312.SubServers.Bungee.Event.SubCreatedEvent;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExConfigureHost;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExCreateServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExDownloadTemplates;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExUploadTemplates;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;

import com.google.common.collect.Range;
import net.md_5.bungee.api.ChatColor;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * External SubCreator Class
 */
@SuppressWarnings("unchecked")
public class ExternalSubCreator extends SubCreator {
    private HashMap<String, ServerTemplate> templates = new HashMap<String, ServerTemplate>();
    private HashMap<String, ServerTemplate> templatesR = new HashMap<String, ServerTemplate>();
    private Boolean enableRT = false;
    private ExternalHost host;
    private Range<Integer> ports;
    private Value<Boolean> log;
    private String gitBash;
    private TreeMap<String, Pair<Integer, ExternalSubLogger>> thread;

    /**
     * Creates an External SubCreator
     *
     * @param host Host
     * @param ports The range of ports to auto-select from
     * @param log Whether SubCreator should log to console
     * @param gitBash The Git Bash directory
     */
    public ExternalSubCreator(ExternalHost host, Range<Integer> ports, boolean log, String gitBash) {
        if (!ports.hasLowerBound() || !ports.hasUpperBound()) throw new IllegalArgumentException("Port range is not bound");
        if (Util.isNull(host, ports, log, gitBash)) throw new NullPointerException();
        this.host = host;
        this.ports = ports;
        this.log = new Container<Boolean>(log);
        this.gitBash = gitBash;
        this.thread = new TreeMap<String, Pair<Integer, ExternalSubLogger>>();
        reload();
    }

    @Override
    public void reload() {
        templatesR.clear();
        if (new UniversalFile(host.plugin.dir, "SubServers:Templates").exists()) for (File file : new UniversalFile(host.plugin.dir, "SubServers:Templates").listFiles()) {
            try {
                if (file.isDirectory() && !file.getName().endsWith(".x")) {
                    ObjectMap<String> config = (new UniversalFile(file, "template.yml").exists())?new YAMLConfig(new UniversalFile(file, "template.yml")).get().getMap("Template", new ObjectMap<String>()):new ObjectMap<String>();
                    ServerTemplate template = new ServerTemplate(file.getName(), config.getBoolean("Enabled", true), config.getRawString("Icon", "::NULL::"), file, config.getMap("Build", new ObjectMap<String>()), config.getMap("Settings", new ObjectMap<String>()));
                    templatesR.put(file.getName().toLowerCase(), template);
                    if (config.getKeys().contains("Display")) template.setDisplayName(config.getString("Display"));
                }
            } catch (Exception e) {
                System.out.println(host.getName() + "/Creator > Couldn't load template: " + file.getName());
                e.printStackTrace();
            }
        }

        if (host.available && !Util.getDespiteException(() -> Util.reflect(SubProxy.class.getDeclaredField("reloading"), host.plugin), false)) {
            host.queue(new PacketExConfigureHost(host.plugin, host));
            host.queue(new PacketExUploadTemplates(host.plugin));
            if (enableRT == null || enableRT) host.queue(new PacketExDownloadTemplates(host.plugin, host));
        }
    }

    @Override
    public boolean create(UUID player, String name, ServerTemplate template, Version version, Integer port, Callback<SubServer> callback) {
        if (Util.isNull(name, template)) throw new NullPointerException();
        if (host.isAvailable() && host.isEnabled() && template.isEnabled() && !SubAPI.getInstance().getSubServers().keySet().contains(name.toLowerCase()) && !SubCreator.isReserved(name) && (version != null || !template.requiresVersion())) {
            StackTraceElement[] origin = new Exception().getStackTrace();

            if (port == null) {
                Container<Integer> i = new Container<Integer>(ports.lowerEndpoint() - 1);
                port = Util.getNew(getAllReservedAddresses(), () -> {
                    do {
                        ++i.value;
                        if (i.value > ports.upperEndpoint()) throw new IllegalStateException("There are no more ports available in range: " + ports.toString());
                    } while (!ports.contains(i.value));
                    return new InetSocketAddress(host.getAddress(), i.value);
                }).getPort();
            }
            String prefix = name + File.separator + "Creator";
            ExternalSubLogger logger = new ExternalSubLogger(this, prefix, log, null);
            thread.put(name.toLowerCase(), new ContainedPair<>(port, logger));

            final int fport = port;
            final SubCreateEvent event = new SubCreateEvent(player, host, name, template, version, port);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                logger.start();
                host.queue(new PacketExCreateServer(player, name, template, version, port, logger.getExternalAddress(), data -> {
                    finish(player, null, name, template, version, fport, prefix, origin, data, callback);
                    logger.stop();
                    this.thread.remove(name.toLowerCase());
                }));
                return true;
            } else {
                thread.remove(name.toLowerCase());
                return false;
            }
        } else return false;
    } private <T> void callback(StackTraceElement[] origin, Callback<T> callback, T value) {
        if (callback != null) try {
            callback.run(value);
        } catch (Throwable e) {
            Throwable ew = new InvocationTargetException(e);
            ew.setStackTrace(origin);
            ew.printStackTrace();
        }
    }

    @Override
    public boolean update(UUID player, SubServer server, ServerTemplate template, Version version, Callback<Boolean> callback) {
        if (Util.isNull(server)) throw new NullPointerException();
        final ServerTemplate ft = (template == null)?server.getTemplate():template;
        if (host.isAvailable() && host.isEnabled() && host == server.getHost() && server.isAvailable() && !server.isRunning() && ft != null && ft.isEnabled() && ft.canUpdate() && (version != null || !ft.requiresVersion())) {
            StackTraceElement[] origin = new Exception().getStackTrace();

            String name = server.getName();
            String prefix = name + File.separator + "Updater";
            Util.isException(() -> Util.reflect(SubServerImpl.class.getDeclaredField("updating"), server, true));
            ExternalSubLogger logger = new ExternalSubLogger(this, prefix, log, null);
            thread.put(name.toLowerCase(), new ContainedPair<>(server.getAddress().getPort(), logger));

            final SubCreateEvent event = new SubCreateEvent(player, server, version);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                logger.start();
                host.queue(new PacketExCreateServer(player, server, ft, version, logger.getExternalAddress(), data -> {
                    finish(player, server, server.getName(), ft, version, server.getAddress().getPort(), prefix, origin, data, s -> {
                        Util.isException(() -> Util.reflect(SubServerImpl.class.getDeclaredField("updating"), server, false));
                        if (callback != null) callback.run(s != null);
                    });
                    logger.stop();
                    this.thread.remove(name.toLowerCase());
                }));
                return true;
            } else {
                thread.remove(name.toLowerCase());
                return false;
            }
        } else return false;
    }

    private void finish(UUID player, SubServer update, String name, ServerTemplate template, Version version, int port, String prefix, StackTraceElement[] origin, ObjectMap<Integer> data, Callback<SubServer> callback) {
        try {
            if (data.getInt(0x0001) == 0) {
                Logger.get(prefix).info("Saving...");
                SubServer subserver = update;
                if (update == null || update.getTemplate() != template || template.getBuildOptions().getBoolean("Update-Settings", false)) {
                    if (host.plugin.exServers.keySet().contains(name.toLowerCase()))
                        host.plugin.exServers.remove(name.toLowerCase());

                    ObjectMap<String> server = new ObjectMap<String>();
                    ObjectMap<String> config = new ObjectMap<String>((Map<String, ?>) data.getObject(0x0002));

                    if (update == null) {
                        server.set("Enabled", true);
                        server.set("Display", "");
                        server.set("Host", host.getName());
                        server.set("Template", template.getName());
                        server.set("Group", new ArrayList<String>());
                        server.set("Port", port);
                        server.set("Motd", "Some SubServer");
                        server.set("Log", true);
                        server.set("Directory", "./" + name);
                        server.set("Executable", "java -Xmx1024M -jar " + template.getType().toString() + ".jar");
                        server.set("Stop-Command", "stop");
                        server.set("Stop-Action", "NONE");
                        server.set("Run-On-Launch", false);
                        server.set("Restricted", false);
                        server.set("Incompatible", new ArrayList<String>());
                        server.set("Hidden", false);
                    } else {
                        server.setAll(host.plugin.servers.get().getMap("Servers").getMap(name, new HashMap<>()));
                        server.set("Template", template.getName());
                    }
                    server.setAll(config);

                    if (update != null) Util.isException(() -> update.getHost().forceRemoveSubServer(name));
                    subserver = host.addSubServer(player, name, server.getBoolean("Enabled"), port, ChatColor.translateAlternateColorCodes('&', server.getString("Motd")), server.getBoolean("Log"),
                            server.getRawString("Directory"), server.getRawString("Executable"), server.getRawString("Stop-Command"), server.getBoolean("Hidden"), server.getBoolean("Restricted"));

                    if (server.getString("Display").length() > 0) subserver.setDisplayName(server.getString("Display"));
                    subserver.setTemplate(server.getRawString("Template"));
                    for (String group : server.getStringList("Group")) subserver.addGroup(group);
                    SubServer.StopAction action = Util.getDespiteException(() -> SubServer.StopAction.valueOf(server.getRawString("Stop-Action").toUpperCase().replace('-', '_').replace(' ', '_')), null);
                    if (action != null) subserver.setStopAction(action);
                    if (server.contains("Extra")) for (String extra : server.getMap("Extra").getKeys())
                        subserver.addExtra(extra, server.getMap("Extra").getObject(extra));
                    host.plugin.servers.get().getMap("Servers").set(name, server);
                    host.plugin.servers.save();

                    if (update == null && template.getBuildOptions().getBoolean("Run-On-Finish", true))
                        subserver.start();
                }

                host.plugin.getPluginManager().callEvent(new SubCreatedEvent(player, host, name, template, version, port, subserver, update != null, true));
                callback(origin, callback, subserver);
            } else {
                Logger.get(prefix).info(data.getString(0x0003));
                host.plugin.getPluginManager().callEvent(new SubCreatedEvent(player, host, name, template, version, port, update, update != null, false));
                callback(origin, callback, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            callback(origin, callback, null);
        }
    }

    @Override
    public void terminate() {
        HashMap<String, Pair<Integer, ExternalSubLogger>> thread = new HashMap<String, Pair<Integer, ExternalSubLogger>>();
        thread.putAll(this.thread);
        for (String i : thread.keySet()) {
            terminate(i);
        }
    }

    @Override
    public void terminate(String name) {
        if (this.thread.keySet().contains(name.toLowerCase())) {
            ((SubDataClient) host.getSubData()[0]).sendPacket(new PacketExCreateServer(name.toLowerCase()));
            thread.remove(name.toLowerCase());
        }
    }

    @Override
    public void waitFor() throws InterruptedException {
        HashMap<String, Pair<Integer, ExternalSubLogger>> thread = new HashMap<String, Pair<Integer, ExternalSubLogger>>();
        thread.putAll(this.thread);
        for (String i : thread.keySet()) {
            waitFor(i);
        }
    }

    @Override
    public void waitFor(String name) throws InterruptedException {
        while (this.thread.keySet().contains(name.toLowerCase()) && host.getSubData()[0] != null) {
            Thread.sleep(250);
        }
    }

    @Override
    public Host getHost() {
        return host;
    }

    @Override
    public Range getPortRange() {
        return ports;
    }

    @Override
    public void setPortRange(Range<Integer> value) {
        if (!value.hasLowerBound() || !value.hasUpperBound()) throw new IllegalArgumentException("Port range is not bound");
        ports = value;
    }

    @Override
    public String getBashDirectory() {
        return gitBash;
    }

    @Override
    public List<SubLogger> getLoggers() {
        List<SubLogger> loggers = new ArrayList<SubLogger>();
        HashMap<String, Pair<Integer, ExternalSubLogger>> temp = new HashMap<String, Pair<Integer, ExternalSubLogger>>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            loggers.add(getLogger(i));
        }
        return loggers;
    }

    @Override
    public SubLogger getLogger(String name) {
        return this.thread.get(name.toLowerCase()).value();
    }

    @Override
    public boolean isLogging() {
        return log.value();
    }

    @Override
    public void setLogging(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        log.value(value);
    }

    @Override
    public List<String> getReservedNames() {
        return new ArrayList<String>(thread.keySet());
    }

    @Override
    public List<Integer> getReservedPorts() {
        List<Integer> ports = new ArrayList<Integer>();
        for (Pair<Integer, ExternalSubLogger> task : thread.values()) ports.add(task.key());
        return ports;
    }

    @Override
    public Map<String, ServerTemplate> getTemplates() {
        TreeMap<String, ServerTemplate> map = new TreeMap<String, ServerTemplate>();
        if (enableRT != null && enableRT) map.putAll(templatesR);
        map.putAll(templates);
        return map;
    }

    @Override
    public ServerTemplate getTemplate(String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        return getTemplates().get(name.toLowerCase());
    }
}
