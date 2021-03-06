package io.github.thebusybiscuit.slimefun4.core.attributes;

import io.github.thebusybiscuit.cscorelib2.config.Config;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This interface, when attached to a {@link SlimefunItem}, provides an easy method for damaging
 * an {@link ItemStack}, see {@link #damageItem(Player, ItemStack)}.
 * <p>
 * It also provides a simple {@link #isDamageable()} method, in case you wanna add a config
 * option that decides whether or not this {@link SlimefunItem} shall be damageable.
 *
 * @author TheBusyBiscuit
 */
public interface DamageableItem extends ItemAttribute {

    /**
     * Implement this method to make the behaviour of this interface dependent
     * on the state of your object.
     * You could add a {@link Config} option to toggle the behaviour for example.
     *
     * @return Whether this {@link SlimefunItem} is damageable
     */
    boolean isDamageable();

    /**
     * This method will damage the given {@link ItemStack} once.
     * It also takes into account the {@link Enchantment} {@code Unbreaking}.
     *
     * It will only apply the damage if {@link #isDamageable()} returned true.
     *
     * @param p
     *            The {@link Player} to which the item belongs
     * @param item
     *            The {@link ItemStack} to damage
     */
    default void damageItem(@Nonnull Player p, @Nullable ItemStack item) {
        if (isDamageable() && item != null && !item.getType().isAir() && item.getAmount() > 0) {
            int unbreakingLevel = item.getEnchantmentLevel(Enchantment.DURABILITY);

            if (unbreakingLevel > 0 && Math.random() * 100 <= (60 + Math.floorDiv(40, (unbreakingLevel + 1)))) {
                return;
            }

            ItemMeta meta = item.getItemMeta();
            if (!meta.isUnbreakable()) {
                Damageable damageable = (Damageable) meta;

                if (damageable.getDamage() >= item.getType().getMaxDurability()) {
                    p.playSound(p.getEyeLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
                    item.setAmount(0);
                } else {
                    damageable.setDamage(damageable.getDamage() + 1);
                    item.setItemMeta(meta);
                }
            }
        }
    }

}