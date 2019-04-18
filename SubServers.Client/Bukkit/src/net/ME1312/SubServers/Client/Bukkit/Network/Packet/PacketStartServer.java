package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;

import java.util.HashMap;
import java.util.UUID;

/**
 * Start Server Packet
 */
public class PacketStartServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<Integer>>[]>();
    private UUID player;
    private String server;
    private UUID tracker;

    /**
     * New PacketStartServer (In)
     */
    public PacketStartServer() {}

    /**
     * New PacketStartServer (Out)
     *
     * @param player Player Starting
     * @param server Server
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketStartServer(UUID player, String server, Callback<ObjectMap<Integer>>... callback) {
        if (Util.isNull(server, callback)) throw new NullPointerException();
        this.player = player;
        this.server = server;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, server);
        if (player != null) data.set(0x0002, player.toString());
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        for (Callback<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.run(data);
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
