package com.ytdd9527.networksexpansion.core.items.unusable;

import com.balugaq.netex.api.interfaces.CraftTyped;
import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.utils.TextUtil;
import com.ytdd9527.networksexpansion.utils.itemstacks.ItemStackUtil;
import io.github.sefiraat.networks.network.stackcaches.BlueprintInstance;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.Theme;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.networks.utils.datatypes.PersistentCraftingBlueprintType;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractBlueprint extends UnusableSlimefunItem implements DistinctiveItem, CraftTyped {
    public AbstractBlueprint(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    public static void setBlueprint(ItemStack blueprint, ItemStack[] recipe, ItemStack output) {
        final ItemMeta itemMeta = blueprint.getItemMeta();
        DataTypeMethods.setCustom(
            itemMeta,
            Keys.BLUEPRINT_INSTANCE,
            PersistentCraftingBlueprintType.TYPE,
            new BlueprintInstance(recipe, output));

        itemMeta.setLore(buildBlueprintLore(recipe, output));

        blueprint.setItemMeta(itemMeta);
    }

    @NotNull
    private static List<String> buildBlueprintLore(@Nullable ItemStack[] recipe, @NotNull ItemStack output) {
        List<String> lore = new ArrayList<>();

        lore.add(Lang.getString("messages.blueprint.title"));

        if (recipe != null) {
            for (ItemStack item : recipe) {
                if (item == null) {
                    lore.add(Theme.PASSIVE + "- " + Lang.getString("messages.blueprint.empty"));
                    continue;
                }
                lore.add(Theme.PASSIVE + "- " + ItemStackHelper.getDisplayName(item));
            }
        }

        lore.add("");
        lore.add(Lang.getString("messages.blueprint.output"));

        lore.add(Theme.PASSIVE + "- " + ItemStackHelper.getDisplayName(output));

        return lore;
    }

    /*
     * Fix https://github.com/Sefiraat/Networks/issues/201
     */
    @Override
    public boolean canStack(@NotNull ItemMeta meta1, @NotNull ItemMeta meta2) {
        return meta1.getPersistentDataContainer().equals(meta2.getPersistentDataContainer());
    }

    /**
     * Check whether the given blueprint is outdated: either one of its stored items no longer
     * matches the currently registered Slimefun items, or its lore is not in the same style as
     * the lore generated for blueprints in the current version.
     * <p>
     * If the blueprint is outdated, a fixed clone is returned with the stored items refreshed,
     * the instance re-serialized under the current key and the lore rebuilt. The original
     * blueprint is not modified.
     *
     * @param blueprint the blueprint to check
     * @return a fixed clone if the blueprint was outdated, otherwise null
     */
    @Nullable
    public static ItemStack refreshOutdatedBlueprint(@NotNull ItemStack blueprint) {
        final ItemMeta blueprintMeta = blueprint.getItemMeta();
        if (blueprintMeta == null) {
            return null;
        }

        final BlueprintInstance instance = Keys.getBlueprintInstance(blueprintMeta);
        if (instance == null || instance == BlueprintInstance.INVALID) {
            return null;
        }

        boolean outdated = false;
        final ItemStack[] recipeItems = instance.getRecipeItems();
        if (recipeItems != null) {
            for (int i = 0; i < recipeItems.length; i++) {
                final ItemStack recipeItem = recipeItems[i];
                if (recipeItem == null || recipeItem.getType() == Material.AIR) {
                    continue;
                }
                final ItemStack refreshed = ItemStackUtil.refreshOutdatedItem(recipeItem);
                if (refreshed != null) {
                    recipeItems[i] = refreshed;
                    outdated = true;
                }
            }
        }

        final ItemStack output = instance.getItemStack();
        final ItemStack refreshedOutput = ItemStackUtil.refreshOutdatedItem(output);
        if (refreshedOutput != null) {
            outdated = true;
        }

        final ItemStack finalOutput = refreshedOutput != null ? refreshedOutput : output;

        // If the lore is not in the same style as the current blueprint lore, the blueprint is
        // considered outdated and gets remade with fresh items.
        if (!Objects.equals(blueprintMeta.getLore(), buildBlueprintLore(recipeItems, finalOutput))) {
            outdated = true;
        }

        if (!outdated) {
            return null;
        }

        final ItemStack fixed = blueprint.clone();
        setBlueprint(fixed, recipeItems, finalOutput);
        return fixed;
    }
}
