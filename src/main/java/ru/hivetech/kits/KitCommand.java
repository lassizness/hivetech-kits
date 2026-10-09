package ru.hivetech.kits;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import net.minecraftforge.server.permission.PermissionAPI;

import java.io.IOException;
import java.util.List;

public final class KitCommand extends CommandBase
{
    @Override
    public String getName()
    {
        return "kit";
    }

    @Override
    public String getUsage(
        ICommandSender sender
    )
    {
        return "/kit";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public void execute(
        MinecraftServer server,
        ICommandSender sender,
        String[] args
    )
    {
        EntityPlayerMP player;

        try
        {
            player =
                getCommandSenderAsPlayer(
                    sender
                );
        }
        catch(Exception e)
        {
            send(
                sender,
                TextFormatting.RED,
                "Команда доступна только игроку."
            );

            return;
        }

        String rank =
            detectRank(player);

        if(rank == null)
        {
            send(
                player,
                TextFormatting.RED,
                "У вас нет статуса с доступным старт-китом."
            );

            return;
        }

        List<ItemStack> kit;

        try
        {
            kit =
                KitStorage.loadKit(rank);
        }
        catch(IOException e)
        {
            HiveTechKits.LOG.error(
                "Unable to load {} kit",
                rank,
                e
            );

            send(
                player,
                TextFormatting.RED,
                "Не удалось загрузить набор."
            );

            return;
        }

        if(kit.isEmpty())
        {
            send(
                player,
                TextFormatting.YELLOW,
                "Набор "
                    + displayRank(rank)
                    + " пока не настроен."
            );

            return;
        }

        long now =
            System.currentTimeMillis();

        long last =
            KitStorage.getLastClaim(
                player.getUniqueID()
            );

        long cooldown =
            KitStorage.getCooldownMillis();

        long remaining =
            cooldown - (now - last);

        if(
            last > 0L
            && remaining > 0L
        )
        {
            send(
                player,
                TextFormatting.YELLOW,
                "Следующий набор будет доступен через "
                    + formatDuration(remaining)
                    + "."
            );

            return;
        }

        int emptySlots = 0;

        for(ItemStack stack :
            player.inventory.mainInventory)
        {
            if(stack.isEmpty())
            {
                emptySlots++;
            }
        }

        /*
         * Намеренно консервативная проверка:
         * на каждый сохранённый стек требуется
         * одно свободное место.
         *
         * Так мы никогда не выдадим половину кита.
         */
        if(emptySlots < kit.size())
        {
            send(
                player,
                TextFormatting.RED,
                "Для получения набора освободите минимум "
                    + kit.size()
                    + " ячеек инвентаря."
            );

            return;
        }

        for(ItemStack template : kit)
        {
            ItemStack stack =
                template.copy();

            boolean added =
                player.inventory
                    .addItemStackToInventory(
                        stack
                    );

            if(!added)
            {
                HiveTechKits.LOG.error(
                    "Unexpected inventory error while giving {} kit to {}",
                    rank,
                    player.getName()
                );

                send(
                    player,
                    TextFormatting.RED,
                    "Не удалось полностью выдать набор. Сообщите администрации."
                );

                return;
            }
        }

        player.inventory.markDirty();

        player.inventoryContainer
            .detectAndSendChanges();

        try
        {
            KitStorage.setLastClaim(
                player.getUniqueID(),
                now
            );
        }
        catch(IOException e)
        {
            HiveTechKits.LOG.error(
                "Unable to save kit cooldown for {}",
                player.getName(),
                e
            );

            send(
                player,
                TextFormatting.RED,
                "Набор выдан, но не удалось сохранить cooldown. Сообщите администрации."
            );

            return;
        }

        send(
            player,
            TextFormatting.GREEN,
            "Старт-кит "
                + displayRank(rank)
                + " успешно получен."
        );
    }

    static String detectRank(
        EntityPlayerMP player
    )
    {
        for(String rank :
            HiveTechKits.RANKS)
        {
            if(
                PermissionAPI.hasPermission(
                    player,
                    "hivetech.kit." + rank
                )
            )
            {
                return rank;
            }
        }

        return null;
    }

    static String displayRank(
        String rank
    )
    {
        if("vip".equals(rank))
        {
            return "VIP";
        }

        if("premium".equals(rank))
        {
            return "Premium";
        }

        if("elite".equals(rank))
        {
            return "Elite";
        }

        if("legend".equals(rank))
        {
            return "Legend";
        }

        return rank;
    }

    static String formatDuration(
        long milliseconds
    )
    {
        long totalMinutes =
            Math.max(
                1L,
                milliseconds / 60000L
            );

        long days =
            totalMinutes / 1440L;

        long hours =
            (
                totalMinutes % 1440L
            ) / 60L;

        long minutes =
            totalMinutes % 60L;

        StringBuilder result =
            new StringBuilder();

        if(days > 0L)
        {
            result
                .append(days)
                .append(" дн. ");
        }

        if(hours > 0L)
        {
            result
                .append(hours)
                .append(" ч. ");
        }

        if(
            days == 0L
            && minutes > 0L
        )
        {
            result
                .append(minutes)
                .append(" мин.");
        }

        return result
            .toString()
            .trim();
    }

    static void send(
        ICommandSender sender,
        TextFormatting color,
        String message
    )
    {
        sender.sendMessage(
            new TextComponentString(
                color + "[HiveTech] "
                    + TextFormatting.RESET
                    + message
            )
        );
    }
}
