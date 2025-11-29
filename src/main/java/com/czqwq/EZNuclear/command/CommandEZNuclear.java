package com.czqwq.EZNuclear.command;

import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;

import com.czqwq.EZNuclear.Config;
import com.czqwq.EZNuclear.TaskBus;
import com.czqwq.EZNuclear.data.PendingMeltdown;

public class CommandEZNuclear extends CommandBase {

    @Override
    public String getCommandName() {
        return "eznuclear";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/eznuclear <debug|reload|status>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText("Usage: /eznuclear <debug|reload|status>"));
            return;
        }

        switch (args[0]) {
            case "debug":
                sender.addChatMessage(new ChatComponentText("§6[PendingMeltdown]"));
                for (Map.Entry<ChunkCoordinates, Integer> e : PendingMeltdown.getPending()
                    .entrySet()) {
                    sender.addChatMessage(new ChatComponentText("  §7" + e.getKey() + " @ dim " + e.getValue()));
                }

                sender.addChatMessage(new ChatComponentText("§6[TaskBus]"));
                int queued = TaskBus.getPendingTaskCount();
                sender.addChatMessage(new ChatComponentText("  §7Queued tasks: " + queued));
                break;

            case "reload":
                try {
                    Config.load();
                    sender.addChatMessage(new ChatComponentText("§aEZNuclear config reloaded."));
                } catch (Throwable t) {
                    sender.addChatMessage(new ChatComponentText("§cFailed to reload config: " + t.getMessage()));
                }
                break;

            case "status":
                sender.addChatMessage(new ChatComponentText("§6[EZNuclear Status]"));
                sender.addChatMessage(new ChatComponentText("§7IC2Explosion: " + Config.IC2Explosion));
                sender.addChatMessage(new ChatComponentText("§7DEExplosion: " + Config.DEExplosion));
                sender.addChatMessage(new ChatComponentText("§7RequireChatTrigger: " + Config.RequireChatTrigger));
                sender.addChatMessage(new ChatComponentText("§7ExplosionDelayMs: " + Config.ExplosionDelayMs + " ms"));
                sender.addChatMessage(
                    new ChatComponentText("§7PendingMeltdown tasks: " + PendingMeltdown.getScheduledCount()));
                sender.addChatMessage(new ChatComponentText("§7TaskBus queue size: " + TaskBus.getPendingTaskCount()));
                break;

            default:
                sender.addChatMessage(new ChatComponentText("§cUnknown subcommand: " + args[0]));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP only
    }
}
