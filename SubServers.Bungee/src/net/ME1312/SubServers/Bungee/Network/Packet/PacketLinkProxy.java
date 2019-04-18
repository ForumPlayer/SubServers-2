package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Event.SubAddProxyEvent;
import net.ME1312.SubServers.Bungee.Host.Proxy;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.Map;

/**
 * Link Proxy Packet
 */
public class PacketLinkProxy implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private int response;
    private String name;

    /**
     * New PacketLinkProxy (In)
     *
     * @param plugin SubPlugin
     */
    public PacketLinkProxy(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketLinkProxy (Out)
     *
     * @param name The name that was generated
     * @param response Response ID
     */
    public PacketLinkProxy(String name, int response) {
        this.name = name;
        this.response = response;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        json.set(0x0000, name);
        json.set(0x0001, response);
        return json;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        try {
            Map<String, Proxy> proxies = plugin.api.getProxies();
            String name = ((data.contains(0x0000))?data.getRawString(0x0000):null);
            Proxy proxy;
            if (name != null && proxies.keySet().contains(name.toLowerCase()) && proxies.get(name.toLowerCase()).getSubData() == null) {
                proxy = proxies.get(name.toLowerCase());
            } else {
                proxy = new Proxy((name != null && !proxies.keySet().contains(name.toLowerCase()))?name:null);
                plugin.getPluginManager().callEvent(new SubAddProxyEvent(proxy));
                plugin.proxies.put(proxy.getName().toLowerCase(), proxy);
            }

            client.setHandler(proxy);
            System.out.println("SubData > " + client.getAddress().toString() + " has been defined as Proxy: " + proxy.getName());
            client.sendPacket(new PacketLinkProxy(proxy.getName(), 0));
        } catch (Exception e) {
            client.sendPacket(new PacketLinkProxy(null, 1));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
