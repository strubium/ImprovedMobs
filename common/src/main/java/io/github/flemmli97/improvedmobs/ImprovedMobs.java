package io.github.flemmli97.improvedmobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ImprovedMobs {

    public static final String MODID = "improvedmobs";
    public static final Logger logger = LogManager.getLogger(ImprovedMobs.MODID);

    public static TagKey<EntityType<?>> ARMOR_EQUIPPABLE = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(MODID, "armor_equippable"));
}
