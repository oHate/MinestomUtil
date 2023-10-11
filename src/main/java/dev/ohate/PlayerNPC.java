package dev.ohate;

import dev.ohate.commonlib.StringUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.PlayerMeta;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.network.packet.server.play.EntityHeadLookPacket;
import net.minestom.server.network.packet.server.play.EntityRotationPacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket.*;

@Getter
public class PlayerNPC extends LivingEntity {

    public static final PlayerSkin DEFAULT_SKIN = new PlayerSkin(
            "eyJ0aW1lc3RhbXAiOjE1ODc4MjU0NzgwNDcsInByb2ZpbGVJZCI6ImUzYjQ0NWM4NDdmNTQ4ZmI4YzhmYTNmMWY3ZWZiYThlIiwicHJvZmlsZU5hbWUiOiJNaW5pRGlnZ2VyVGVzdCIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2E1ODg4YWEyZDdlMTk5MTczYmEzN2NhNzVjNjhkZTdkN2Y4NjJiMzRhMTNiZTMyNDViZTQ0N2UyZjIyYjI3ZSJ9fX0=",
            "Yt6VmTAUTbpfGQoFneECtoYcbu7jcARAwZu2LYWv3Yf1MJGXv6Bi3i7Pl9P8y1ShB7V1Q2HyA1bce502x1naOKJPzzMJ0jKZfEAKXnzaFop9t9hXzgOq7PaIAM6fsapymYhkkulRIxnJdMrMb2PLRYfo9qiBJG+IEbdj8MTSvWJO10xm7GtpSMmA2Xd0vg5205hsj0OxSdgxf1uuWPyRaXpPZYDUU05/faRixDKti86hlkBs/v0rttU65r1UghkftfjK0sJoPpk9hABvkw4OjXVFb63wcb27KPhIiSHZzTooSxjGNDniauCsF8Je+fhhMebpXeba1R2lZPLhkHwazNgZmTCKbV1M/a8BDHN24HH9okJpQOR9SPCPOJrNbK+LTPsrR06agj+H/yvYq0ZMJTF6IE6C3KJqntPJF1NQvJM0/YegPPtzpbT/7O1cd4JBCVmguhadOFYvrxqCKHcmaYdkyMJtnGub/5sCjJAG7fZadACftwLnmdBZoQRcNKQMubpdUjuzF8g6C03MiZkeNBUgqkfVjXi7DqpmB0ZvTttp34vy7EIBCo3Hfj15779nGs8SoTw9V2zZc+LgiVPjWF6tffjWkgzLq8K2Cndu6RDlWGJWmrztN/X9lIiLdn8GEfSSGY983n0C91x8mkpOKSfAWPnSZd7NuHU5GaoMvyE="
    );

    public static final Team NPC_TEAM;

    static {
        NPC_TEAM = MinecraftServer.getTeamManager().createTeam(
                "NPC-TEAM",
                Component.text("[NPC] ", NamedTextColor.DARK_GRAY),
                NamedTextColor.DARK_GRAY,
                Component.empty()
        );

        NPC_TEAM.setNameTagVisibility(TeamsPacket.NameTagVisibility.NEVER);
    }

    private final String username;
    private final PlayerSkin skin;
    private final boolean facePlayer;
    private final Consumer<PlayerEntityInteractEvent> interaction;
    private final List<HologramLine> holograms = new ArrayList<>();

    private PlayerNPC(Builder builder) {
        super(EntityType.PLAYER);

        username = StringUtil.randomAlphanumericString(10);

        this.skin = builder.skin;
        this.facePlayer = builder.facePlayer;
        this.interaction = builder.interaction;

        holograms.addAll(builder.holograms);

        PlayerMeta meta = new PlayerMeta(this, metadata);

        meta.setNotifyAboutChanges(false);

        meta.setHasNoGravity(true);
        meta.setCapeEnabled(true);
        meta.setHatEnabled(true);
        meta.setJacketEnabled(true);
        meta.setLeftLegEnabled(true);
        meta.setLeftSleeveEnabled(true);
        meta.setRightLegEnabled(true);
        meta.setRightSleeveEnabled(true);

        meta.setNotifyAboutChanges(true);

        NPC_TEAM.addMember(username);
    }

    private PlayerInfoUpdatePacket getAddPlayerInfoPacket() {
        Property textureProperty = new Property(
                "textures",
                skin.textures(),
                skin.signature()
        );

        Entry playerEntry = new Entry(
                uuid,
                username,
                List.of(textureProperty),
                false,
                1,
                GameMode.CREATIVE,
                null,
                null
        );

        return new PlayerInfoUpdatePacket(Action.ADD_PLAYER, playerEntry);
    }

    @Override
    public void spawn() {
        super.spawn();

        updateHologramLocation();
    }

    public void updateHologramLocation() {
        for (int i = holograms.size() - 1; i >= 0; i--) {
            double height = 1 + HologramLine.OFFSET + (holograms.size() - i + 1) * HologramLine.OFFSET;

            holograms.get(i).setInstance(instance, position.add(0, height, 0));
        }
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        player.sendPacket(getAddPlayerInfoPacket());

        super.updateNewViewer(player);
    }

    @Override
    public void tick(long time) {
        if (facePlayer) {
            for (Player player : viewers) {
                Pos direction = position.withDirection(player.getPosition().sub(position));

                player.sendPacket(new EntityRotationPacket(getEntityId(), direction.yaw(), direction.pitch(), false));
                player.sendPacket(new EntityHeadLookPacket(getEntityId(), direction.yaw()));
            }
        }

        super.tick(time);
    }

    public static void addListener(GlobalEventHandler handler) {
        handler.addListener(PlayerEntityInteractEvent.class, e -> {
            Entity target = e.getTarget();

            if (!(target instanceof PlayerNPC npc)) {
                return;
            }

            Consumer<PlayerEntityInteractEvent> callback = npc.getInteraction();

            if (callback == null) {
                return;
            }

            callback.accept(e);
        });
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private PlayerSkin skin = DEFAULT_SKIN;
        private boolean facePlayer = false;
        private Consumer<PlayerEntityInteractEvent> interaction = e -> {};
        private List<HologramLine> holograms = new ArrayList<>();

        public Builder skin(PlayerSkin skin) {
            this.skin = skin;
            return this;
        }

        public Builder facePlayer() {
            facePlayer = true;
            return this;
        }

        public Builder handleInteraction(Consumer<PlayerEntityInteractEvent> interaction) {
            this.interaction = interaction;
            return this;
        }

        public Builder addHologram(HologramLine hologramLine) {
            holograms.add(hologramLine);
            return this;
        }

        public PlayerNPC build() {
            return new PlayerNPC(this);
        }

    }

}
