package io.github.sefiraat.networks.utils.datatypes;

import com.jeff_media.morepersistentdatatypes.DataType;
import com.ytdd9527.networksexpansion.implementation.ExpansionItems;
import io.github.sefiraat.networks.network.stackcaches.BlueprintInstance;
import io.github.sefiraat.networks.network.stackcaches.CardInstance;
import io.github.sefiraat.networks.utils.Keys;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * A {@link PersistentDataType} for {@link CardInstance}
 * Creatively thieved from {@see <a href="https://github.com/baked-libs/dough/blob/main/dough-data/src/main/java/io/github/bakedlibs/dough/data/persistent/PersistentUUIDDataType.java">PersistentUUIDDataType}
 *
 * @author Sfiguz7
 * @author Walshy
 * @author balugaq
 */
@NullMarked
public class PersistentCraftingBlueprintType implements PersistentDataType<PersistentDataContainer, BlueprintInstance> {

    public static final PersistentDataType<PersistentDataContainer, BlueprintInstance> TYPE =
        new PersistentCraftingBlueprintType();

    @Override
    public Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @Override
    public Class<BlueprintInstance> getComplexType() {
        return BlueprintInstance.class;
    }

    public static void setItemStack(PersistentDataContainer container, String key, ItemStack itemStack) {
        setItemStack(container, Keys.newKey(key), itemStack);
    }
    
    public static void setItemStack(PersistentDataContainer container, NamespacedKey key, ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        if (itemStack.isSimilar(new ItemStack(itemStack.getType()))) {
            // pure vanilla item
            container.set(key, DataType.STRING, "mc;" + itemStack.getType().name() + ";" + itemStack.getAmount());
            return;
        } else {
            SlimefunItem sf = SlimefunItem.getByItem(itemStack);
            if (sf != null) {
                if (itemStack.isSimilar(sf.getItem())) {
                    // pure slimefun item
                    container.set(key, DataType.STRING, "sf;" + sf.getId() + ";" + itemStack.getAmount());
                    return;
                }
            }

            // complex item
            container.set(key, DataType.ITEM_STACK, itemStack);
        }
    }

    @Nullable
    public static ItemStack getItemStack(PersistentDataContainer primitive, NamespacedKey key) {
        try {
            var s = primitive.get(key, DataType.STRING);
            var s2 = s.split(";");
            if (s2[0].equals("mc")) {
                return new ItemStack(Material.valueOf(s2[1]), Integer.parseInt(s2[2]));
            } else if (s2[0].equals("sf")) {
                SlimefunItem sf = SlimefunItem.getById(s2[1]);
                if (sf == null) sf = ExpansionItems.PLACEHOLDER_ITEM;

                ItemStack item = sf.getItem().clone();
                item.setAmount(Integer.parseInt(s2[2]));
                return item;
            } else {
                // impossible
                return null;
            }
        } catch (Exception ignored) {
            try {
                return primitive.get(key, DataType.ITEM_STACK);
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    @Override
    public PersistentDataContainer toPrimitive(
        BlueprintInstance complex, PersistentDataAdapterContext context) {
        final PersistentDataContainer container = context.newPersistentDataContainer();

        for (int i = 0; i < complex.getRecipeItems().length; i++) {
            setItemStack(container, "recipe_" + i, complex.getRecipeItems()[i]);
        }

        // container.set(Keys.RECIPE, DataType.ITEM_STACK_ARRAY, complex.getRecipeItems());
        if (complex.getItemStack() != null) {
            setItemStack(container, Keys.OUTPUT, complex.getItemStack());
//            container.set(Keys.OUTPUT, DataType.ITEM_STACK, complex.getItemStack());
        }
        return container;
    }

    @Override
    public BlueprintInstance fromPrimitive(
        PersistentDataContainer primitive, PersistentDataAdapterContext context) {
        @Nullable ItemStack @Nullable[] recipe = Keys.getRecipe(primitive);
        if (recipe == null) {
            // new format
            List<NamespacedKey> recipeKeys = primitive.getKeys().stream().filter(k -> k.getKey().startsWith("recipe_")).toList();
            recipe = new ItemStack[Math.max(9, recipeKeys.size())];
            for (NamespacedKey key : recipeKeys) {
                int slot = Integer.parseInt(key.getKey().split("_")[1]);
                recipe[slot] = getItemStack(primitive, key);
            }
        }
        ItemStack output;
        try {
            output = Keys.getOutput(primitive);
        } catch (Exception ignored) {
            // UncheckedIOException occurs randomly, seems DataType.ITEM_STACK no longer works.
            output = getItemStack(primitive, Keys.OUTPUT);
        }

        if (recipe == null || output == null) {
            return BlueprintInstance.INVALID;
        }
        return new BlueprintInstance(recipe, output);
    }
}
