package ru.hivetech.kits;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = HiveTechKits.MODID,
    name = HiveTechKits.NAME,
    version = HiveTechKits.VERSION,
    acceptableRemoteVersions = "*"
)
public final class HiveTechKits
{
    public static final String MODID = "hivetechkits";
    public static final String NAME = "HiveTech Kits";
    public static final String VERSION = "1.0.0";

    public static final String[] RANKS = {
        "legend",
        "elite",
        "premium",
        "vip"
    };

    public static Logger LOG;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        LOG = event.getModLog();

        KitStorage.init(
            event.getModConfigurationDirectory()
        );

        LOG.info(
            "HiveTech Kits storage initialized"
        );
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        for(String rank : RANKS)
        {
            PermissionAPI.registerNode(
                "hivetech.kit." + rank,
                DefaultPermissionLevel.NONE,
                "Allows HiveTech " + rank + " kit"
            );
        }

        LOG.info(
            "HiveTech Kits permissions registered"
        );
    }

    @Mod.EventHandler
    public void serverStarting(
        FMLServerStartingEvent event
    )
    {
        event.registerServerCommand(
            new KitCommand()
        );

        event.registerServerCommand(
            new AdminKitCommand()
        );

        LOG.info(
            "HiveTech Kits commands registered: /kit, /hkit"
        );
    }
}
