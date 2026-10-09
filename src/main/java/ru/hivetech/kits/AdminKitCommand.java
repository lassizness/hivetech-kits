package ru.hivetech.kits;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.util.List;

public final class AdminKitCommand
    extends CommandBase
{
    @Override
    public String getName()
    {
        return "hkit";
    }

    @Override
    public String getUsage(
        ICommandSender sender
    )
    {
        return "/hkit save <vip|premium|elite|legend>"
            + " | reload"
            + " | reset <player>"
            + " | status <player>"
            + " | list";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 4;
    }

    @Override
    public void execute(
        MinecraftServer server,
        ICommandSender sender,
        String[] args
    )
    {
        if(args.length < 1)
        {
            usage(sender);
            return;
        }

        String action =
            args[0].toLowerCase();

        if("save".equals(action))
        {
            save(sender, args);
            return;
        }

        if("reload".equals(action))
        {
            reload(sender);
            return;
        }

        if("reset".equals(action))
        {
            reset(
                server,
                sender,
                args
            );

            return;
        }

        if("status".equals(action))
        {
            status(
                server,
                sender,
                args
            );

            return;
        }

        if("list".equals(action))
        {
            list(sender);
            return;
        }

        usage(sender);
    }

    private void save(
        ICommandSender sender,
        String[] args
    )
    {
        if(
            args.length != 2
            || !isRank(args[1])
        )
        {
            KitCommand.send(
                sender,
                TextFormatting.YELLOW,
                "Использование: /hkit save <vip|premium|elite|legend>"
            );

            return;
        }

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
            KitCommand.send(
                sender,
                TextFormatting.RED,
                "Сохранение кита выполняется из игры."
            );

            return;
        }

        String rank =
            args[1].toLowerCase();

        try
        {
            int stacks =
                KitStorage.saveKit(
                    rank,
                    player
                );

            if(stacks == 0)
            {
                KitCommand.send(
                    sender,
                    TextFormatting.RED,
                    "Инвентарь пуст. Сначала положите туда предметы кита."
                );

                return;
            }

            KitCommand.send(
                sender,
                TextFormatting.GREEN,
                "Набор "
                    + KitCommand.displayRank(rank)
                    + " сохранён. Стеков: "
                    + stacks
                    + "."
            );
        }
        catch(IOException e)
        {
            HiveTechKits.LOG.error(
                "Unable to save {} kit",
                rank,
                e
            );

            KitCommand.send(
                sender,
                TextFormatting.RED,
                "Ошибка сохранения набора."
            );
        }
    }

    private void reload(
        ICommandSender sender
    )
    {
        try
        {
            KitStorage.reload();

            KitCommand.send(
                sender,
                TextFormatting.GREEN,
                "Настройки HiveTech Kits перезагружены. Cooldown: "
                    + KitStorage.getCooldownDays()
                    + " дней."
            );
        }
        catch(IOException e)
        {
            HiveTechKits.LOG.error(
                "Unable to reload HiveTech Kits",
                e
            );

            KitCommand.send(
                sender,
                TextFormatting.RED,
                "Ошибка перезагрузки настроек."
            );
        }
    }

    private void reset(
        MinecraftServer server,
        ICommandSender sender,
        String[] args
    )
    {
        if(args.length != 2)
        {
            KitCommand.send(
                sender,
                TextFormatting.YELLOW,
                "Использование: /hkit reset <player>"
            );

            return;
        }

        EntityPlayerMP target;

        try
        {
            target =
                getPlayer(
                    server,
                    sender,
                    args[1]
                );
        }
        catch(Exception e)
        {
            KitCommand.send(
                sender,
                TextFormatting.RED,
                "Игрок не найден."
            );

            return;
        }

        try
        {
            KitStorage.resetCooldown(
                target.getUniqueID()
            );

            KitCommand.send(
                sender,
                TextFormatting.GREEN,
                "Cooldown игрока "
                    + target.getName()
                    + " сброшен."
            );
        }
        catch(IOException e)
        {
            HiveTechKits.LOG.error(
                "Unable to reset cooldown for {}",
                target.getName(),
                e
            );

            KitCommand.send(
                sender,
                TextFormatting.RED,
                "Ошибка сохранения cooldown."
            );
        }
    }

    private void status(
        MinecraftServer server,
        ICommandSender sender,
        String[] args
    )
    {
        if(args.length != 2)
        {
            KitCommand.send(
                sender,
                TextFormatting.YELLOW,
                "Использование: /hkit status <player>"
            );

            return;
        }

        EntityPlayerMP target;

        try
        {
            target =
                getPlayer(
                    server,
                    sender,
                    args[1]
                );
        }
        catch(Exception e)
        {
            KitCommand.send(
                sender,
                TextFormatting.RED,
                "Игрок не найден."
            );

            return;
        }

        String rank =
            KitCommand.detectRank(target);

        long last =
            KitStorage.getLastClaim(
                target.getUniqueID()
            );

        long remaining =
            KitStorage.getCooldownMillis()
                - (
                    System.currentTimeMillis()
                    - last
                );

        String cooldown;

        if(
            last == 0L
            || remaining <= 0L
        )
        {
            cooldown = "готов";
        }
        else
        {
            cooldown =
                KitCommand.formatDuration(
                    remaining
                );
        }

        KitCommand.send(
            sender,
            TextFormatting.AQUA,
            target.getName()
                + ": статус="
                + (
                    rank == null
                        ? "нет"
                        : KitCommand.displayRank(rank)
                )
                + ", kit="
                + cooldown
        );
    }

    private void list(
        ICommandSender sender
    )
    {
        for(String rank :
            HiveTechKits.RANKS)
        {
            try
            {
                List<ItemStack> kit =
                    KitStorage.loadKit(rank);

                KitCommand.send(
                    sender,
                    TextFormatting.GRAY,
                    KitCommand.displayRank(rank)
                        + ": "
                        + kit.size()
                        + " стеков"
                );
            }
            catch(IOException e)
            {
                KitCommand.send(
                    sender,
                    TextFormatting.RED,
                    KitCommand.displayRank(rank)
                        + ": ошибка чтения"
                );
            }
        }
    }

    private static boolean isRank(
        String rank
    )
    {
        for(String value :
            HiveTechKits.RANKS)
        {
            if(
                value.equalsIgnoreCase(rank)
            )
            {
                return true;
            }
        }

        return false;
    }

    private void usage(
        ICommandSender sender
    )
    {
        KitCommand.send(
            sender,
            TextFormatting.YELLOW,
            getUsage(sender)
        );
    }
}
