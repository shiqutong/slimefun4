package io.github.thebusybiscuit.slimefun4.core.attributes;

import io.github.thebusybiscuit.cscorelib2.chat.ChatColors;
import io.github.thebusybiscuit.slimefun4.core.services.holograms.HologramsService;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.HologramProjector;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;

/**
 * This {@link ItemAttribute} manages holograms.
 *
 * @author TheBusyBiscuit
 * @see HologramProjector
 * @see HologramsService
 */
public interface HologramOwner extends ItemAttribute {

    /**
     * This will update the holograms text for the given {@link Block}.
     *
     * @param b    The {@link Block} to which the holograms belongs
     * @param text The nametag for the holograms
     */
    default void updateHologram(@Nonnull Block b, @Nonnull String text) {
        Location loc = b.getLocation().add(getHologramOffset(b));
        SlimefunPlugin.getHologramsService().setHologramLabel(loc, ChatColors.color(text));
    }

    /**
     * This will remove the holograms for the given {@link Block}.
     *
     * @param b The {@link Block} to which the holograms blocks
     */
    default void removeHologram(@Nonnull Block b) {
        Location loc = b.getLocation().add(getHologramOffset(b));
        SlimefunPlugin.getHologramsService().removeHologram(loc);
    }

    /**
     * This returns the offset of the holograms as a {@link Vector}.
     * This offset is applied to {@link Block#getLocation()} when spawning
     * the holograms.
     *
     * @param block The {@link Block} which serves as the origin point
     * @return The holograms offset
     */
    @Nonnull
    default Vector getHologramOffset(@Nonnull Block block) {
        return SlimefunPlugin.getHologramsService().getDefaultOffset();
    }

}