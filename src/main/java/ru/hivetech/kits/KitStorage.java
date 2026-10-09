package ru.hivetech.kits;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class KitStorage
{
    private static File baseDir;
    private static File kitsDir;
    private static File cooldownFile;
    private static File settingsFile;

    private static final Properties cooldowns =
        new Properties();

    private static final Properties settings =
        new Properties();

    private KitStorage()
    {
    }

    public static synchronized void init(
        File configDir
    )
    {
        baseDir = new File(
            configDir,
            "hivetech-kits"
        );

        kitsDir = new File(
            baseDir,
            "kits"
        );

        cooldownFile = new File(
            baseDir,
            "cooldowns.properties"
        );

        settingsFile = new File(
            baseDir,
            "settings.properties"
        );

        if(!kitsDir.exists() && !kitsDir.mkdirs())
        {
            throw new RuntimeException(
                "Unable to create " + kitsDir
            );
        }

        try
        {
            if(!settingsFile.isFile())
            {
                settings.clear();

                settings.setProperty(
                    "cooldown_days",
                    "30"
                );

                saveProperties(
                    settingsFile,
                    settings,
                    "HiveTech Kits settings"
                );
            }

            reload();
        }
        catch(IOException e)
        {
            throw new RuntimeException(
                "Unable to initialize HiveTech Kits",
                e
            );
        }
    }

    public static synchronized void reload()
        throws IOException
    {
        settings.clear();
        cooldowns.clear();

        loadProperties(
            settingsFile,
            settings
        );

        loadProperties(
            cooldownFile,
            cooldowns
        );
    }

    public static synchronized long getCooldownDays()
    {
        String value =
            settings.getProperty(
                "cooldown_days",
                "30"
            );

        try
        {
            return Math.max(
                0L,
                Long.parseLong(value)
            );
        }
        catch(NumberFormatException ignored)
        {
            return 30L;
        }
    }

    public static synchronized long getCooldownMillis()
    {
        return getCooldownDays()
            * 24L
            * 60L
            * 60L
            * 1000L;
    }

    public static synchronized int saveKit(
        String rank,
        EntityPlayerMP player
    ) throws IOException
    {
        NBTTagList items =
            new NBTTagList();

        int count = 0;

        for(ItemStack stack :
            player.inventory.mainInventory)
        {
            if(stack.isEmpty())
            {
                continue;
            }

            NBTTagCompound tag =
                new NBTTagCompound();

            stack.writeToNBT(tag);

            items.appendTag(tag);

            count++;
        }

        if(count == 0)
        {
            return 0;
        }

        NBTTagCompound root =
            new NBTTagCompound();

        root.setString(
            "rank",
            rank
        );

        root.setLong(
            "saved_at",
            System.currentTimeMillis()
        );

        root.setTag(
            "items",
            items
        );

        File file =
            getKitFile(rank);

        try(OutputStream out =
            new FileOutputStream(file))
        {
            CompressedStreamTools
                .writeCompressed(
                    root,
                    out
                );
        }

        return count;
    }

    public static synchronized List<ItemStack> loadKit(
        String rank
    ) throws IOException
    {
        File file =
            getKitFile(rank);

        if(!file.isFile())
        {
            return Collections.emptyList();
        }

        NBTTagCompound root;

        try(InputStream in =
            new FileInputStream(file))
        {
            root =
                CompressedStreamTools
                    .readCompressed(in);
        }

        NBTTagList items =
            root.getTagList(
                "items",
                10
            );

        List<ItemStack> result =
            new ArrayList<ItemStack>();

        for(int i = 0;
            i < items.tagCount();
            i++)
        {
            ItemStack stack =
                new ItemStack(
                    items.getCompoundTagAt(i)
                );

            if(!stack.isEmpty())
            {
                result.add(stack);
            }
        }

        return result;
    }

    public static synchronized long getLastClaim(
        UUID uuid
    )
    {
        String value =
            cooldowns.getProperty(
                uuid.toString()
            );

        if(value == null)
        {
            return 0L;
        }

        try
        {
            return Long.parseLong(value);
        }
        catch(NumberFormatException ignored)
        {
            return 0L;
        }
    }

    public static synchronized void setLastClaim(
        UUID uuid,
        long timestamp
    ) throws IOException
    {
        cooldowns.setProperty(
            uuid.toString(),
            Long.toString(timestamp)
        );

        saveCooldowns();
    }

    public static synchronized void resetCooldown(
        UUID uuid
    ) throws IOException
    {
        cooldowns.remove(
            uuid.toString()
        );

        saveCooldowns();
    }

    private static File getKitFile(
        String rank
    )
    {
        return new File(
            kitsDir,
            rank + ".dat"
        );
    }

    private static void loadProperties(
        File file,
        Properties properties
    ) throws IOException
    {
        if(!file.isFile())
        {
            return;
        }

        try(InputStream in =
            new FileInputStream(file))
        {
            properties.load(in);
        }
    }

    private static void saveCooldowns()
        throws IOException
    {
        saveProperties(
            cooldownFile,
            cooldowns,
            "HiveTech Kits cooldowns"
        );
    }

    private static void saveProperties(
        File file,
        Properties properties,
        String comment
    ) throws IOException
    {
        try(OutputStream out =
            new FileOutputStream(file))
        {
            properties.store(
                out,
                comment
            );
        }
    }
}
