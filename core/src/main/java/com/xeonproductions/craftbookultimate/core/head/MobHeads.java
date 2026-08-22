// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.head;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * Whose head a creature's head actually is.
 *
 * <p>The game has a head for seven things and no more: a player, a zombie, a creeper, a skeleton, a
 * wither skeleton, a dragon and a piglin. Everything else here is a <em>player</em> head wearing
 * somebody's skin, which is the only way the game has ever had of showing a cow's face on a block.
 * The accounts named below are the long-standing ones made for exactly this, most of them Mojang's
 * own {@code MHF_} heads, and the identifier rather than the name is what is pinned: an account
 * that is renamed keeps its head, and a name somebody else takes over never becomes a cow.
 *
 * <p>No texture is recorded, so a server resolves each of these against Mojang the first time it
 * hands one out and remembers it afterwards. That means an offline-mode server with no way out to
 * the internet gets a blank head rather than a cow, which is a limitation of the approach and not
 * something this can decide.
 */
@NullMarked
public final class MobHeads {

    /** A head somebody else's face is on. */
    public record Owner(UUID id, String name) {}

    private static final Map<Key, Owner> OWNERS = assemble();

    private MobHeads() {
    }

    /** Whose head a creature's is, or nothing where the creature has no head at all. */
    public static Optional<Owner> ownerOf(Key creature) {
        return Optional.ofNullable(OWNERS.get(creature));
    }

    /** Which creature an account's head belongs to, for saying what a placed head is. */
    public static Optional<Key> creatureOwning(UUID id) {
        for (Map.Entry<Key, Owner> entry : OWNERS.entrySet()) {
            if (entry.getValue().id().equals(id)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /** Every creature that has a head, for anything that needs the whole list. */
    public static Map<Key, Owner> all() {
        return OWNERS;
    }

    private static Map<Key, Owner> assemble() {
        Map<Key, Owner> owners = new LinkedHashMap<>();
        put(owners, "bat", "339d5256-1cd8-4b79-bbd9-59874d25ba8f", "bozzobrain");
        put(owners, "blaze", "4c38ed11-596a-4fd4-ab1d-26f386c1cbac", "MHF_Blaze");
        put(owners, "cave_spider", "cab28771-f0cd-4fe7-b129-02c69eba79a5", "MHF_CaveSpider");
        put(owners, "chicken", "92deafa9-4307-42d9-b003-88601598d6c0", "MHF_Chicken");
        put(owners, "cow", "f159b274-c22e-4340-b7c1-52abde147713", "MHF_Cow");
        put(owners, "donkey", "5464dd67-5a46-4239-862f-b27b4944442c", "Donkey");
        put(owners, "elder_guardian", "57ef77e6-d9ea-493e-a0c7-564a36d9d12a", "ElderGuardian");
        put(owners, "enderman", "40ffb372-12f6-4678-b3f2-2176bf56dd4b", "MHF_Enderman");
        put(owners, "endermite", "3df6a050-b93e-4d8b-8fa4-b5228a797b84", "MHF_Endermite");
        put(owners, "evoker", "8b7d6844-f679-4c85-ab4d-0f99301f1899", "MHF_Evoker");
        put(owners, "ghast", "063085a6-797f-4785-be1a-21cd7580f752", "MHF_Ghast");
        put(owners, "guardian", "4005cac1-a16a-45aa-9e72-7fb514335717", "MHF_Guardian");
        put(owners, "horse", "1b90edcf-393d-4e93-a0d6-cf737dc80999", "gavertoso");
        put(owners, "iron_golem", "757f90b2-2344-4b8d-8dac-824232e2cece", "MHF_Golem");
        put(owners, "magma_cube", "0972bdd1-4b86-49fb-9ecc-a353f8491a51", "MHF_LavaSlime");
        put(owners, "mooshroom", "a46817d6-73c5-4f3f-b712-af6b3ff47b96", "MHF_MushroomCow");
        put(owners, "ocelot", "1bee9df5-4f71-42a2-bf52-d97970d3fea3", "MHF_Ocelot");
        put(owners, "parrot", "3d88c411-c7e1-40f9-b1f7-fbe4b7aef4a2", "MHF_Parrot");
        put(owners, "pig", "8b57078b-f1bd-45df-83c4-d88d16768fbe", "MHF_Pig");
        put(owners, "polar_bear", "38029209-38b0-4311-97a9-60663fbf74b5", "Polar_Bear");
        put(owners, "rabbit", "fbec11d4-80a7-4c1c-9de3-4136a16f1de0", "MHF_Rabbit");
        put(owners, "sheep", "dfaad551-4e7e-45a1-a6f7-c6fc5ec823ac", "MHF_Sheep");
        put(owners, "shulker", "160f7d8a-c6b0-4fc8-8925-9e9d6c9c57d5", "MHF_Shulker");
        put(owners, "silverfish", "6a4c6f38-243f-4c40-8342-749fa1009351", "MHF_Silverfish");
        put(owners, "slime", "870aba93-40e8-48b3-89c5-32ece00d6630", "MHF_Slime");
        put(owners, "snow_golem", "217f9e5e-601f-4a3d-879b-e10d30e3e59b", "MHF_SnowGolem");
        put(owners, "spider", "5ad55f34-41b6-4bd2-9c32-18983c635936", "MHF_Spider");
        put(owners, "squid", "72e64683-e313-4c36-a408-c66b64e94af5", "MHF_Squid");
        put(owners, "stray", "ed33403b-be7f-4a38-915c-abea93ebc9bc", "MHF_Stray");
        put(owners, "vex", "f5f20997-217f-4426-8ab9-c6db6cce023f", "MHF_Vex");
        put(owners, "villager", "bd482739-767c-45dc-a1f8-c33c40530952", "MHF_Villager");
        put(owners, "vindicator", "1f32ef1f-9bf1-436e-98e3-cb48758fd268", "Vindicator");
        put(owners, "witch", "fef85c49-2fdf-47f8-9132-552046243223", "MHF_Witch");
        put(owners, "wither", "39af6844-6809-4d2f-8ba4-7e92d087be18", "MHF_Wither");
        put(owners, "wolf", "8d2d1d6d-8034-4c89-bd86-809a31fd5193", "MHF_Wolf");
        put(owners, "zombified_piglin", "18a2bb50-334a-4084-9184-2c380251a24b", "MHF_PigZombie");
        return Collections.unmodifiableMap(owners);
    }

    private static void put(Map<Key, Owner> owners, String creature, String id, String name) {
        owners.put(Key.key(Key.MINECRAFT_NAMESPACE, creature), new Owner(UUID.fromString(id), name));
    }
}
