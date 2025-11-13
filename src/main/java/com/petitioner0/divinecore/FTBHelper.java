package com.petitioner0.divinecore;


import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.server.level.ServerPlayer;

public class FTBHelper {

    public static void completeTask(ServerPlayer player, String taskId) {
        if (player == null || taskId == null) return;

        player.server.execute(() -> {
            // 传 false 表示取服务端文件（true 为客户端）
            ServerQuestFile file = (ServerQuestFile) FTBQuestsAPI.api().getQuestFile(false);
            if (file == null) return;

            Long id = QuestObjectBase.parseHexId(taskId).orElseThrow();
            Task task = file.getTask(id);
            if (task == null) return;

            TeamData data = file.getOrCreateTeamData(player);
            if (data.isCompleted(task)) return;

            data.setProgress(task, task.getMaxProgress());

        });
    }
}