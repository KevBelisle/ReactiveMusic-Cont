package circuitlord.reactivemusic;


import circuitlord.reactivemusic.config.ModConfig;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import circuitlord.reactivemusic.mixin.BossBarHudAccessor;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.*;

public final class SongPicker {



    public static Map<SongpackEventType, Boolean> songpackEventMap = new EnumMap<>(SongpackEventType.class);

    public static Map<TagKey<Biome>, Boolean> biomeTagEventMap = new HashMap<>();

    public static Map<Entity, Long> recentEntityDamageSources = new HashMap<>();


    private static final Set<String> BLOCK_COUNTER_BLACKLIST = Set.of(); // = Set.of("ore", "debris");

    public static boolean queuedToPrintBlockCounter = false;
    public static BlockPos cachedBlockCounterOrigin;
    public static int currentBlockCounterX = 99999;
    public static int currentBlockCounterY = 99999;

    public static Map<String, Integer> blockCounterMap = new HashMap<>();
    public static Map<String, Integer> cachedBlockChecker = new HashMap<>();

    public static String currentBiomeName = "";
    public static String currentDimName = "";


    private static final Random rand = new Random();

    private static List<String> recentlyPickedSongs = new ArrayList<>();

    public static final Field[] BIOME_TAG_FIELDS = ConventionalBiomeTags.class.getDeclaredFields();
    public static final List<TagKey<Biome>> BIOME_TAGS = new ArrayList<>();

    public static Long TIME_FOR_FORGET_DAMAGE_SOURCE = 200L;

    public static boolean wasSleeping = false;

    static {

        for (Field field : BIOME_TAG_FIELDS) {
            TagKey<Biome> biomeTag = getBiomeTagFromField(field);

            BIOME_TAGS.add(biomeTag);
            biomeTagEventMap.put(biomeTag, false);
        }
    }

    public static TagKey<Biome> getBiomeTagFromField(Field field) {
        if (field.getType() == TagKey.class) {
            try {
                @SuppressWarnings("unchecked")
                TagKey<Biome> tag = (TagKey<Biome>) field.get(null);
                return tag;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }




    public static void tickEventMap() {

        currentBiomeName = "";
        currentDimName = "";

        Minecraft mc = Minecraft.getInstance();
        if (mc == null)
            return;

        LocalPlayer player = mc.player;
        Level world = mc.level;


        songpackEventMap.put(SongpackEventType.MAIN_MENU, player == null || world == null);
        songpackEventMap.put(SongpackEventType.CREDITS, mc.screen instanceof WinScreen);

        // Early out if not in-game
        if (player == null || world == null) return;

        // World processing
        BlockPos playerPos = player.blockPosition();
        var biome = world.getBiome(playerPos);

        // Copied logic out from getIdAsString
        currentBiomeName = (String)biome.unwrapKey().map((key) -> {
            return key.location().toString();
        }).orElse("[unregistered]");

        boolean underground = !world.canSeeSky(playerPos);
        var indimension = world.dimension();

        currentDimName = indimension.location().toString();

        Entity riding = VersionHelper.GetRidingEntity(player);

        long time = world.getOverworldClockTime() % 24000;
        boolean night = time >= 13000 && time < 23000;
        boolean sunset = time >= 12000 && time < 13000;
        boolean sunrise = time >= 23000;


        // TODO: someone help me I have no idea how to get the name of the world/server but if you know how then put it instead of "saved"
        if (!wasSleeping && player.isSleeping()) {
            ReactiveMusic.config.savedHomePositions.put("saved", player.position());

            ModConfig.saveConfig();
        }

        wasSleeping = player.isSleeping();


        // special

        if (ReactiveMusic.config.savedHomePositions.containsKey("saved")) {

            Vec3 dist = player.position().subtract(ReactiveMusic.config.savedHomePositions.get("saved"));

            songpackEventMap.put(SongpackEventType.HOME, dist.length() < 45.0f);
        }
        else {
            songpackEventMap.put(SongpackEventType.HOME, false);
        }



        // Time
        songpackEventMap.put(SongpackEventType.DAY, !night);
        songpackEventMap.put(SongpackEventType.NIGHT, night);
        songpackEventMap.put(SongpackEventType.SUNSET, sunset);
        songpackEventMap.put(SongpackEventType.SUNRISE, sunrise);


        // Actions

        songpackEventMap.put(SongpackEventType.DYING, player.getHealth() / player.getMaxHealth() < 0.35);
        songpackEventMap.put(SongpackEventType.FISHING, player.fishing != null);

        songpackEventMap.put(SongpackEventType.MINECART, riding instanceof Minecart);
        songpackEventMap.put(SongpackEventType.BOAT, riding instanceof AbstractBoat);
        songpackEventMap.put(SongpackEventType.HORSE, riding instanceof Horse);
        songpackEventMap.put(SongpackEventType.PIG, riding instanceof Pig);


        songpackEventMap.put(SongpackEventType.OVERWORLD, indimension == Level.OVERWORLD);
        songpackEventMap.put(SongpackEventType.NETHER, indimension == Level.NETHER);
        songpackEventMap.put(SongpackEventType.END, indimension == Level.END);


        songpackEventMap.put(SongpackEventType.UNDERGROUND, indimension == Level.OVERWORLD && underground && playerPos.getY() < 55);
        songpackEventMap.put(SongpackEventType.DEEP_UNDERGROUND, indimension == Level.OVERWORLD && underground && playerPos.getY() < 15);
        songpackEventMap.put(SongpackEventType.HIGH_UP, indimension == Level.OVERWORLD && !underground && playerPos.getY() > 128);

        songpackEventMap.put(SongpackEventType.UNDERWATER, player.isUnderWater());

        // Weather
        songpackEventMap.put(SongpackEventType.RAIN, world.isRaining() && biome.value().getPrecipitationAt(playerPos, world.getSeaLevel()) == Biome.Precipitation.RAIN);
        songpackEventMap.put(SongpackEventType.SNOW, world.isRaining() && biome.value().getPrecipitationAt(playerPos, world.getSeaLevel()) == Biome.Precipitation.SNOW);

        songpackEventMap.put(SongpackEventType.STORM, world.isThundering());


        var currentTags = biome.tags().toList();

        // Update all ConventionalBiomeTags
        for (TagKey<Biome> tag : BIOME_TAGS) {
            boolean found = false;

            // search by ID instead of comparing tagkey, doesn't work on non-fabric
            for (TagKey<Biome> curTag : currentTags) {
                if (curTag.location() == tag.location()) {
                    found = true;
                    break;
                }
            }

            biomeTagEventMap.put(tag, found);
        }


        // process recent damage sources

        // remove past sources
        recentEntityDamageSources.entrySet().removeIf(entry -> entry.getKey() == null || !entry.getKey().isAlive() || world.getGameTime() - entry.getValue() > TIME_FOR_FORGET_DAMAGE_SOURCE);

        // add new damage sources
        var recentDamage = player.getLastDamageSource();

        if (recentDamage != null && recentDamage.getDirectEntity() != null) {
            recentEntityDamageSources.put(recentDamage.getDirectEntity(), world.getGameTime());
        }




        // Search for nearby entities that could be relevant to music

        {
            int villagerCount = 0;

            double radiusXZ = 30.0;
            double radiusY = 15.0;

            AABB box = new AABB(player.getX() - radiusXZ, player.getY() - radiusY, player.getZ() - radiusXZ,
                    player.getX() + radiusXZ, player.getY() + radiusY, player.getZ() + radiusXZ);

            List<Villager> nearbyVillagerCheck = world.getEntitiesOfClass(Villager.class, box, entity -> entity != null);

            for (Villager villagerEntity : nearbyVillagerCheck) {
                villagerCount++;
            }

            songpackEventMap.put(SongpackEventType.VILLAGE, villagerCount > 0);

        }

        {
            List<Monster> nearbyHostile = world.getEntitiesOfClass(Monster.class,
                    GetBoxAroundPlayer(player, 12.f, 6.f),
                    entity -> entity != null);

            songpackEventMap.put(SongpackEventType.NEARBY_MOBS, nearbyHostile.size() >= 1);

        }


        // try to get boss bars
        boolean bossBarActive = false;

        if (mc.gui != null && mc.gui.getBossOverlay() != null) {
            try {

                var bossBars = ((BossBarHudAccessor) mc.gui.getBossOverlay()).getBossBars();

                if (!bossBars.isEmpty()) {
                    bossBarActive = true;
                }
            } catch (Exception e) {
            }
        }


        songpackEventMap.put(SongpackEventType.BOSS, bossBarActive);


        songpackEventMap.put(SongpackEventType.GENERIC, true);
    }



    public static void tickBlockCounterMap() {

        long startTime = System.currentTimeMillis();
        long startNano = System.nanoTime();

        int RADIUS = 25;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level world = mc.level;
        if (player == null || world == null)
            return;



        // just X
        currentBlockCounterX++;
        if (currentBlockCounterX > RADIUS) {
            currentBlockCounterX = -RADIUS;
        }

        // finished iterating, reset
        if (currentBlockCounterX == -RADIUS) {

            if (queuedToPrintBlockCounter) {

                player.sendSystemMessage(Component.literal("[ReactiveMusic]: Logging Block Counter map! Radius: " + RADIUS));

                blockCounterMap.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                        .forEach(entry -> player.sendSystemMessage(Component.literal(entry.getKey() + ": " + entry.getValue())));

                queuedToPrintBlockCounter = false;

            }

            // copy
            cachedBlockChecker.clear();
            cachedBlockChecker.putAll(blockCounterMap);

            // reset
            blockCounterMap.clear();
            cachedBlockCounterOrigin = player.blockPosition();

        }

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int y = -RADIUS; y <= RADIUS; y++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {

                // don't allocate new blockpos everytime
                mutablePos.set(
                        cachedBlockCounterOrigin.getX() + currentBlockCounterX,
                        cachedBlockCounterOrigin.getY() + y,
                        cachedBlockCounterOrigin.getZ() + z
                );


                var block = world.getBlockState(mutablePos).getBlock();
                String key = BuiltInRegistries.BLOCK.getKey(block).location().toString();

                boolean isBlacklisted = false;
                for (String black : BLOCK_COUNTER_BLACKLIST) {
                    if (key.contains(black)) {
                        isBlacklisted = true;
                        break;
                    }
                }
                if (isBlacklisted)
                    continue;

                blockCounterMap.merge(key, 1, Integer::sum);

            }
        }



        long endNano = System.nanoTime();
        long elapsedNano = endNano - startNano;
        double elapsedMs = elapsedNano / 1_000_000.0;


    }



    private static AABB GetBoxAroundPlayer(LocalPlayer player, float radiusXZ, float radiusY) {

        return new AABB(player.getX() - radiusXZ, player.getY() - radiusY, player.getZ() - radiusXZ,
                player.getX() + radiusXZ, player.getY() + radiusY, player.getZ() + radiusXZ);

    }


    public static void initialize() {

        songpackEventMap.clear();

        for (SongpackEventType eventType : SongpackEventType.values()) {
            songpackEventMap.put(eventType, false);
        }
    }




    private static final List<SongpackEntry> reusableValidEntries = new ArrayList<>();


    static boolean hasSongNotPlayedRecently(List<String> songs) {
        for (String song : songs) {
            if (!recentlyPickedSongs.contains(song)) {
                return true;
            }
        }
        return false;
    }


    static List<String> getNotRecentlyPlayedSongs(String[] songs) {
        List<String> notRecentlyPlayed = new ArrayList<>(Arrays.asList(songs));
        notRecentlyPlayed.removeAll(recentlyPickedSongs);
        return notRecentlyPlayed;
    }


    static String pickRandomSong(List<String> songs) {

        if (songs.isEmpty()) {
            return null;
        }

        List<String> cleanedSongs = new ArrayList<>(songs);

        cleanedSongs.removeAll(recentlyPickedSongs);


        String picked;

        // If there's remaining songs, pick one of those
        if (!cleanedSongs.isEmpty()) {
            int randomIndex = rand.nextInt(cleanedSongs.size());
            picked = cleanedSongs.get(randomIndex);
        }

        // Else we've played all these recently so just pick a new random one
        else {
            int randomIndex = rand.nextInt(songs.size());
            picked = songs.get(randomIndex);
        }


        // only track the past X songs
        if (recentlyPickedSongs.size() >= 8) {
            recentlyPickedSongs.remove(0);
        }

        recentlyPickedSongs.add(picked);


        return picked;
    }


    public static String getSongName(String song) {
        return song == null ? "" : song.replaceAll("([^A-Z])([A-Z])", "$1 $2");
    }


    public static boolean isEntryValid(RMRuntimeEntry entry) {

        for (var condition : entry.conditions) {

            // each condition functions as an OR, if at least one of them is true then the condition is true


            boolean songpackEventsValid = false;

            for (SongpackEventType songpackEvent : condition.songpackEvents) {
                if (songpackEventMap.containsKey(songpackEvent) && songpackEventMap.get(songpackEvent)) {
                    songpackEventsValid = true;
                    break;
                }
            }

            boolean blocksValid = false;
            for (var blockCond : condition.blocks) {
                for (var kvp : cachedBlockChecker.entrySet()) {
                    if (kvp.getKey().contains(blockCond.block) && kvp.getValue() >= blockCond.requiredCount) {
                        blocksValid = true;
                        break;
                    }
                }
            }

            boolean biomeTypesValid = false;
            for (var biome : condition.biomeTypes) {
                if (currentBiomeName.contains(biome)) {
                    biomeTypesValid = true;
                    break;
                }
            }

            boolean biomeTagsValid = false;
            for (var biomeTag : condition.biomeTags) {
                if (biomeTagEventMap.containsKey(biomeTag) && biomeTagEventMap.get(biomeTag)) {
                    biomeTagsValid = true;
                    break;
                }
            }

            boolean dimsValid = false;
            for (var dim : condition.dimTypes) {
                if (currentDimName.contains(dim)) {
                    dimsValid = true;
                    break;
                }
            }


            if (!songpackEventsValid && !biomeTypesValid && !biomeTagsValid && !dimsValid && !blocksValid) {
                // none of the OR conditions were valid on this condition, return false
                return false;
            }

        }

        // we passed without failing so it must be true
        return true;

    }


}
