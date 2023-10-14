package dev.ohate.board;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.time.Tick;

import java.util.*;

@Getter
public class BoardManager {

    private static final TaskSchedule SCHEDULE = TaskSchedule.duration(Tick.server(2));

    @Getter
    private static BoardManager instance;

    private final BoardProvider provider;
    private final Map<UUID, Sidebar> scoreboards;

    public BoardManager(BoardProvider provider) {
        if (instance != null) {
            throw new IllegalStateException("BoardManager has already been initialized.");
        }

        instance = this;

        this.provider = provider;
        this.scoreboards = new HashMap<>();

        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            for (UUID playerId : scoreboards.keySet()) {
                Player player = MinecraftServer.getConnectionManager().getPlayer(playerId);

                if (player == null) {
                    continue;
                }

                update(player);
            }
        }, TaskSchedule.nextTick(), TaskSchedule.tick(2));
    }

    public void setup(Player player) {
        List<Component> lines = provider.getLines(player);
        Sidebar sidebar = new Sidebar(provider.getTitle(player));

        for (int index = 0; index < lines.size(); index++) {
            sidebar.createLine(new Sidebar.ScoreboardLine(String.valueOf(index), Component.empty(), index));
        }

        sidebar.addViewer(player);

        scoreboards.put(player.getUuid(), sidebar);

        update(player);
    }

    public void update(Player player) {
        Sidebar sidebar = scoreboards.get(player.getUuid());
        Set<Sidebar.ScoreboardLine> scoreboardLines = sidebar.getLines();
        List<Component> lines = provider.getLines(player);

        int currentSize = scoreboardLines.size();
        int targetSize = lines.size();

        // Remove excess lines
        for (int i = currentSize - 1; i >= targetSize; i--) {
            sidebar.removeLine(String.valueOf(i));
        }

        // Add new lines
        for (int i = currentSize; i < targetSize; i++) {
            sidebar.createLine(new Sidebar.ScoreboardLine(String.valueOf(i), Component.empty(), i));
        }

        // Update line content
        for (int index = 0; index < lines.size(); index++) {
            sidebar.updateLineContent(String.valueOf(index), lines.get(index));
        }
    }

}
