package io.github.thebusybiscuit.slimefun4.implementation.setup;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.GrindStone;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.MakeshiftSmeltery;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.OreCrusher;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.Smeltery;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PostSetup {

    private PostSetup() {
    }

    public static void setupWiki() {
        SlimefunPlugin.logger().log(Level.INFO, "Loading Wiki pages...");

        JsonParser parser = new JsonParser();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(SlimefunPlugin.class.getResourceAsStream("/wiki.json"), StandardCharsets.UTF_8))) {
            JsonElement element = parser.parse(reader.lines().collect(Collectors.joining("")));
            JsonObject json = element.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                SlimefunItem item = SlimefunItem.getByID(entry.getKey());

                if (item != null) {
                    item.addOficialWikipage(entry.getValue().getAsString());
                }
            }
        } catch (IOException e) {
            SlimefunPlugin.logger().log(Level.SEVERE, "Failed to load wiki.json file", e);
        }
    }

    public static void loadItems() {
        Iterator<SlimefunItem> iterator = SlimefunPlugin.getRegistry().getEnabledSlimefunItems().iterator();

        while (iterator.hasNext()) {
            SlimefunItem item = iterator.next();

            if (item == null) {
                SlimefunPlugin.logger().log(Level.WARNING, "Removed bugged Item ('NULL?')");
                iterator.remove();
            } else {
                try {
                    item.load();
                } catch (Exception | LinkageError x) {
                    item.error("Failed to properly load this Item", x);
                }
            }
        }

        loadOreGrinderRecipes();
        loadSmelteryRecipes();

        CommandSender sender = Bukkit.getConsoleSender();

        int total = SlimefunPlugin.getRegistry().getEnabledSlimefunItems().size();
        int slimefunOnly = countNonAddonItems();

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "######################### - Slimefun v" + SlimefunPlugin.getVersion() + " - #########################");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "??????????????? " + total + " ???????????? " + SlimefunPlugin.getRegistry().getResearches().size() + " ?????????");
        sender.sendMessage(ChatColor.GREEN + "( " + slimefunOnly + " ??????????????????, " + (total - slimefunOnly) + " ??????????????? " + SlimefunPlugin.getInstalledAddons().size() + " ?????? )");
        sender.sendMessage("");

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + " - ??????:      https://github.com/StarWishsama/Slimefun4");
        sender.sendMessage(ChatColor.GREEN + " - Bug ??????:  https://github.com/StarWishsama/Slimefun4/issues");

        sender.sendMessage("");

        SlimefunPlugin.getItemCfg().save();
        SlimefunPlugin.getResearchCfg().save();

        SlimefunPlugin.getRegistry().setAutoLoadingMode(true);
    }

    private static int countNonAddonItems() {
        return (int) SlimefunPlugin.getRegistry().getEnabledSlimefunItems().stream()
                .filter(item -> item.getAddon() instanceof SlimefunPlugin)
                .count();
    }

    private static void loadOreGrinderRecipes() {
        List<ItemStack[]> grinderRecipes = new ArrayList<>();

        GrindStone grinder = (GrindStone) SlimefunItems.GRIND_STONE.getItem();
        if (grinder != null) {
            ItemStack[] input = null;

            for (ItemStack[] recipe : grinder.getRecipes()) {
                if (input == null) {
                    input = recipe;
                } else {
                    if (input[0] != null && recipe[0] != null) {
                        grinderRecipes.add(new ItemStack[]{input[0], recipe[0]});
                    }

                    input = null;
                }
            }
        }

        OreCrusher crusher = (OreCrusher) SlimefunItems.ORE_CRUSHER.getItem();
        if (crusher != null) {
            ItemStack[] input = null;

            for (ItemStack[] recipe : crusher.getRecipes()) {
                if (input == null) {
                    input = recipe;
                } else {
                    if (input[0] != null && recipe[0] != null) {
                        grinderRecipes.add(new ItemStack[]{input[0], recipe[0]});
                    }
                    input = null;
                }
            }
        }

        // Favour 8 Cobblestone -> 1 Sand Recipe over 1 Cobblestone -> 1 Gravel Recipe
        Stream<ItemStack[]> stream = grinderRecipes.stream();

        if (!SlimefunPlugin.getCfg().getBoolean("options.legacy-ore-grinder")) {
            stream = stream.sorted((a, b) -> Integer.compare(b[0].getAmount(), a[0].getAmount()));
        }

        stream.forEach(recipe -> registerMachineRecipe("ELECTRIC_ORE_GRINDER", 4, new ItemStack[]{recipe[0]}, new ItemStack[]{recipe[1]}));
    }

    private static void loadSmelteryRecipes() {
        Smeltery smeltery = (Smeltery) SlimefunItems.SMELTERY.getItem();

        if (smeltery != null && !smeltery.isDisabled()) {
            MakeshiftSmeltery makeshiftSmeltery = ((MakeshiftSmeltery) SlimefunItems.MAKESHIFT_SMELTERY.getItem());
            ItemStack[] input = null;

            for (ItemStack[] output : smeltery.getRecipes()) {
                if (input == null) {
                    input = output;
                } else {
                    if (input[0] != null && output[0] != null) {
                        addSmelteryRecipe(input, output, makeshiftSmeltery);
                    }

                    input = null;
                }
            }

            for (SlimefunItem item : SlimefunPlugin.getRegistry().getEnabledSlimefunItems()) {
                if (item instanceof AContainer) {
                    AContainer machine = (AContainer) item;

                    if (machine.getMachineIdentifier().equals("ELECTRIC_SMELTERY")) {
                        List<MachineRecipe> recipes = machine.getMachineRecipes();
                        Collections.sort(recipes, Comparator.comparingInt(recipe -> recipe == null ? 0 : -recipe.getInput().length));
                    }
                }
            }
        }
    }

    private static void addSmelteryRecipe(ItemStack[] input, ItemStack[] output, MakeshiftSmeltery makeshiftSmeltery) {
        List<ItemStack> ingredients = new ArrayList<>();

        // Filter out 'null' items
        for (ItemStack item : input) {
            if (item != null) {
                ingredients.add(item);
            }
        }

        // We want to redirect Dust to Ingot Recipes
        if (ingredients.size() == 1 && isDust(ingredients.get(0))) {
            makeshiftSmeltery.addRecipe(new ItemStack[]{ingredients.get(0)}, output[0]);

            registerMachineRecipe("ELECTRIC_INGOT_FACTORY", 8, new ItemStack[]{ingredients.get(0)}, new ItemStack[]{output[0]});
            registerMachineRecipe("ELECTRIC_INGOT_PULVERIZER", 3, new ItemStack[]{output[0]}, new ItemStack[]{ingredients.get(0)});
        } else {
            registerMachineRecipe("ELECTRIC_SMELTERY", 12, ingredients.toArray(new ItemStack[0]), new ItemStack[]{output[0]});
        }
    }

    private static boolean isDust(ItemStack item) {
        SlimefunItem sfItem = SlimefunItem.getByItem(item);
        return sfItem != null && sfItem.getId().endsWith("_DUST");
    }

    private static void registerMachineRecipe(String machine, int seconds, ItemStack[] input, ItemStack[] output) {
        for (SlimefunItem item : SlimefunPlugin.getRegistry().getEnabledSlimefunItems()) {
            if (item instanceof AContainer) {
                AContainer container = (AContainer) item;

                if (container.getMachineIdentifier().equals(machine)) {
                    container.registerRecipe(seconds, input, output);
                }
            }
        }
    }
}