package dev.ohate.board;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;

public interface BoardProvider {

    Component getTitle(Player player);

    List<Component> getLines(Player player);

}
