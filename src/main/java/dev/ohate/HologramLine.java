package dev.ohate;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public class HologramLine extends Entity {

    public static final double OFFSET = 0.3;

    private final Function<Player, Component> componentFunction;
    private long currentTick = 0;

    public HologramLine(Function<Player, Component> componentFunction) {
        super(EntityType.TEXT_DISPLAY);

        this.componentFunction = componentFunction;

        TextDisplayMeta meta = new TextDisplayMeta(this, metadata);

        meta.setNotifyAboutChanges(false);

        meta.setHasNoGravity(true);
        meta.setText(Component.space());

        meta.setNotifyAboutChanges(true);

    }

    public void updateLine(Player player) {
        player.sendPacket(new EntityMetaDataPacket(getEntityId(), Map.of(
                (int) AbstractDisplayMeta.MAX_OFFSET,
                Metadata.Chat(componentFunction.apply(player))
        )));
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);
        updateLine(player);
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (currentTick % 60 == 0) {
            viewers.forEach(player -> updateLine(player));
        }

        currentTick++;
    }

}
