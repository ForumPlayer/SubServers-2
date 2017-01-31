package net.ME1312.SubServers.Host.API.Event;

import net.ME1312.SubServers.Host.Library.Event.Event;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.SubServers;

/**
 * SubServers.Host Enable Event Class
 */
public class SubEnableEvent extends Event {

    /**
     * SubServers.Host Enable Event
     *
     * @param plugin SubServers.Host
     */
    public SubEnableEvent(SubServers plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
    }
}
