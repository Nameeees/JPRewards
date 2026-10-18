package me.jp.jprewards.util;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Compatibilidad 1.8 - ultima version.
 *
 * El plugin se compila contra la API de Spigot 1.8.8 (bytecode Java 8) y
 * resuelve en tiempo de ejecucion todo lo que cambia entre versiones:
 *
 *   - Materiales renombrados en 1.13 (SKULL_ITEM -> PLAYER_HEAD, etc.)
 *   - Sonidos renombrados en 1.9 (CLICK -> UI_BUTTON_CLICK, etc.)
 *   - Encantamientos renombrados en 1.13 (DAMAGE_ALL -> SHARPNESS)
 *   - Particulas (org.bukkit.Particle existe desde 1.9; en 1.8 se usa Effect)
 *   - Titulos de pantalla (metodo moderno o paquete NMS de 1.8)
 *   - Action bar (API de chat de Spigot por reflexion)
 *   - ItemMeta#setCustomModelData (1.14+) y setUnbreakable (1.11+)
 *
 * Ningun fallo de reflexion rompe el plugin: se avisa UNA vez por consola
 * y se degrada con elegancia.
 */
public final class Compat {

    /** Renombres de material: moderno -> [legacy, data]. */
    private static final Map<String, String[]> MATERIAL_LEGACY = new HashMap<String, String[]>();
    /** Sonidos: moderno -> legacy (1.8). */
    private static final Map<String, String> SONIDO_LEGACY = new HashMap<String, String>();
    /** Encantamientos: moderno -> legacy (1.8). */
    private static final Map<String, String> ENCANTAMIENTO_LEGACY = new HashMap<String, String>();
    /** Particulas: moderno (1.13+) -> nombre de 1.8. */
    private static final Map<String, String> PARTICULA_LEGACY = new HashMap<String, String>();
    /** Particulas: nombre de 1.9/1.13 -> nombre del enum Effect de 1.8. */
    private static final Map<String, String> EFECTO_18 = new HashMap<String, String>();
    /** Colores por nombre para fuegos artificiales y polvo. */
    private static final Map<String, int[]> COLORES = new HashMap<String, int[]>();
    /** Avisos ya mostrados (evita repetir el mismo warning en consola). */
    private static final Set<String> AVISOS = new HashSet<String>();

    private static Method metodoTituloModerno;
    private static boolean tituloModernoBuscado;
    private static boolean tituloPaqueteNoDisponible;
    private static Method metodoSonidoTexto;
    private static boolean sonidoTextoBuscado;

    private static Class<?> claseParticula;
    private static Method metodoParticula;
    private static boolean particulaBuscada;
    private static Class<?> claseEfecto;
    private static Method metodoEfecto;
    private static boolean efectoBuscado;

    static {
        MATERIAL_LEGACY.put("PLAYER_HEAD", new String[]{"SKULL_ITEM", "3"});
        MATERIAL_LEGACY.put("PLAYER_WALL_HEAD", new String[]{"SKULL_ITEM", "3"});
        MATERIAL_LEGACY.put("BLACK_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "15"});
        MATERIAL_LEGACY.put("GRAY_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "7"});
        MATERIAL_LEGACY.put("LIGHT_GRAY_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "8"});
        MATERIAL_LEGACY.put("WHITE_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "0"});
        MATERIAL_LEGACY.put("RED_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "14"});
        MATERIAL_LEGACY.put("GREEN_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "13"});
        MATERIAL_LEGACY.put("LIME_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "5"});
        MATERIAL_LEGACY.put("ORANGE_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "1"});
        MATERIAL_LEGACY.put("YELLOW_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "4"});
        MATERIAL_LEGACY.put("BLUE_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "11"});
        MATERIAL_LEGACY.put("LIGHT_BLUE_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "3"});
        MATERIAL_LEGACY.put("PURPLE_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "10"});
        MATERIAL_LEGACY.put("MAGENTA_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "2"});
        MATERIAL_LEGACY.put("PINK_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "6"});
        MATERIAL_LEGACY.put("CYAN_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "9"});
        MATERIAL_LEGACY.put("BROWN_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "12"});
        MATERIAL_LEGACY.put("GRAY_DYE", new String[]{"INK_SACK", "8"});
        MATERIAL_LEGACY.put("LIGHT_GRAY_DYE", new String[]{"INK_SACK", "7"});
        MATERIAL_LEGACY.put("LIME_DYE", new String[]{"INK_SACK", "10"});
        MATERIAL_LEGACY.put("RED_DYE", new String[]{"INK_SACK", "1"});
        MATERIAL_LEGACY.put("GREEN_DYE", new String[]{"INK_SACK", "2"});
        MATERIAL_LEGACY.put("BLUE_DYE", new String[]{"INK_SACK", "4"});
        MATERIAL_LEGACY.put("BLACK_DYE", new String[]{"INK_SACK", "0"});
        MATERIAL_LEGACY.put("COD", new String[]{"RAW_FISH", "0"});
        MATERIAL_LEGACY.put("SALMON", new String[]{"RAW_FISH", "1"});
        MATERIAL_LEGACY.put("TROPICAL_FISH", new String[]{"RAW_FISH", "2"});
        MATERIAL_LEGACY.put("PUFFERFISH", new String[]{"RAW_FISH", "3"});
        MATERIAL_LEGACY.put("COOKED_COD", new String[]{"COOKED_FISH", "0"});
        MATERIAL_LEGACY.put("COOKED_SALMON", new String[]{"COOKED_FISH", "1"});
        MATERIAL_LEGACY.put("WRITABLE_BOOK", new String[]{"BOOK_AND_QUILL", "0"});
        MATERIAL_LEGACY.put("WRITTEN_BOOK", new String[]{"WRITTEN_BOOK", "0"});
        MATERIAL_LEGACY.put("GRASS_BLOCK", new String[]{"GRASS", "0"});
        MATERIAL_LEGACY.put("OAK_PLANKS", new String[]{"WOOD", "0"});
        MATERIAL_LEGACY.put("SPRUCE_PLANKS", new String[]{"WOOD", "1"});
        MATERIAL_LEGACY.put("BIRCH_PLANKS", new String[]{"WOOD", "2"});
        MATERIAL_LEGACY.put("JUNGLE_PLANKS", new String[]{"WOOD", "3"});
        MATERIAL_LEGACY.put("ACACIA_PLANKS", new String[]{"WOOD", "4"});
        MATERIAL_LEGACY.put("DARK_OAK_PLANKS", new String[]{"WOOD", "5"});
        MATERIAL_LEGACY.put("OAK_LOG", new String[]{"LOG", "0"});
        MATERIAL_LEGACY.put("SPRUCE_LOG", new String[]{"LOG", "1"});
        MATERIAL_LEGACY.put("BIRCH_LOG", new String[]{"LOG", "2"});
        MATERIAL_LEGACY.put("JUNGLE_LOG", new String[]{"LOG", "3"});
        MATERIAL_LEGACY.put("ACACIA_LOG", new String[]{"LOG", "4"});
        MATERIAL_LEGACY.put("DARK_OAK_LOG", new String[]{"LOG", "5"});
        MATERIAL_LEGACY.put("SUNFLOWER", new String[]{"DOUBLE_PLANT", "0"});
        MATERIAL_LEGACY.put("TALL_GRASS", new String[]{"LONG_GRASS", "1"});
        MATERIAL_LEGACY.put("FERN", new String[]{"LONG_GRASS", "2"});
        MATERIAL_LEGACY.put("DEAD_BUSH", new String[]{"DEAD_BUSH", "0"});
        MATERIAL_LEGACY.put("NETHER_BRICK_FENCE", new String[]{"NETHER_FENCE", "0"});
        MATERIAL_LEGACY.put("NETHER_BRICKS", new String[]{"NETHER_BRICK", "0"});
        MATERIAL_LEGACY.put("NETHER_QUARTZ_ORE", new String[]{"QUARTZ_ORE", "0"});
        MATERIAL_LEGACY.put("COBBLESTONE_WALL", new String[]{"COBBLE_WALL", "0"});
        MATERIAL_LEGACY.put("TERRACOTTA", new String[]{"HARD_CLAY", "0"});
        MATERIAL_LEGACY.put("WHITE_TERRACOTTA", new String[]{"STAINED_CLAY", "0"});
        MATERIAL_LEGACY.put("ORANGE_TERRACOTTA", new String[]{"STAINED_CLAY", "1"});
        MATERIAL_LEGACY.put("RED_TERRACOTTA", new String[]{"STAINED_CLAY", "14"});
        MATERIAL_LEGACY.put("FIREWORK_ROCKET", new String[]{"FIREWORK", "0"});
        MATERIAL_LEGACY.put("FIREWORK_STAR", new String[]{"FIREWORK_CHARGE", "0"});
        MATERIAL_LEGACY.put("GOLDEN_APPLE", new String[]{"GOLDEN_APPLE", "0"});
        MATERIAL_LEGACY.put("ENCHANTED_GOLDEN_APPLE", new String[]{"GOLDEN_APPLE", "1"});
        MATERIAL_LEGACY.put("MUSIC_DISC_13", new String[]{"GOLD_RECORD", "0"});
        MATERIAL_LEGACY.put("MUSIC_DISC_CAT", new String[]{"GREEN_RECORD", "0"});
        MATERIAL_LEGACY.put("SNOWBALL", new String[]{"SNOW_BALL", "0"});
        MATERIAL_LEGACY.put("ARMOR_STAND", new String[]{"ARMOR_STAND", "0"});

        SONIDO_LEGACY.put("UI_BUTTON_CLICK", "CLICK");
        SONIDO_LEGACY.put("BLOCK_CHEST_OPEN", "CHEST_OPEN");
        SONIDO_LEGACY.put("BLOCK_CHEST_CLOSE", "CHEST_CLOSE");
        SONIDO_LEGACY.put("ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
        SONIDO_LEGACY.put("ENTITY_VILLAGER_NO", "VILLAGER_NO");
        SONIDO_LEGACY.put("ENTITY_VILLAGER_YES", "VILLAGER_YES");
        SONIDO_LEGACY.put("ENTITY_EXPERIENCE_ORB_PICKUP", "ORB_PICKUP");
        SONIDO_LEGACY.put("BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING");
        SONIDO_LEGACY.put("BLOCK_NOTE_BLOCK_HARP", "NOTE_PIANO");
        SONIDO_LEGACY.put("BLOCK_NOTE_BLOCK_BASS", "NOTE_BASS");
        SONIDO_LEGACY.put("ENTITY_CHICKEN_EGG", "CHICKEN_EGG_POP");
        SONIDO_LEGACY.put("BLOCK_ANVIL_LAND", "ANVIL_LAND");
        SONIDO_LEGACY.put("BLOCK_ANVIL_USE", "ANVIL_USE");
        SONIDO_LEGACY.put("ENTITY_ENDERMAN_TELEPORT", "ENDERMAN_TELEPORT");
        SONIDO_LEGACY.put("ENTITY_GENERIC_EXPLODE", "EXPLODE");
        SONIDO_LEGACY.put("ENTITY_FIREWORK_ROCKET_LAUNCH", "FIREWORK_LAUNCH");
        SONIDO_LEGACY.put("ENTITY_FIREWORK_ROCKET_BLAST", "FIREWORK_BLAST");
        SONIDO_LEGACY.put("ENTITY_FIREWORK_ROCKET_TWINKLE", "FIREWORK_TWINKLE");
        SONIDO_LEGACY.put("ENTITY_ITEM_PICKUP", "ITEM_PICKUP");
        SONIDO_LEGACY.put("BLOCK_LEVER_CLICK", "CLICK");
        SONIDO_LEGACY.put("ITEM_BOOK_PAGE_TURN", "ITEM_BREAK");
        SONIDO_LEGACY.put("ENTITY_ARROW_HIT_PLAYER", "SUCCESSFUL_HIT");
        SONIDO_LEGACY.put("ENTITY_PLAYER_BURP", "BURP");
        SONIDO_LEGACY.put("BLOCK_GRASS_BREAK", "DIG_GRASS");
        SONIDO_LEGACY.put("BLOCK_STONE_BREAK", "DIG_STONE");
        SONIDO_LEGACY.put("ENTITY_WITHER_SPAWN", "WITHER_SPAWN");
        SONIDO_LEGACY.put("ENTITY_ENDER_DRAGON_GROWL", "ENDERDRAGON_GROWL");

        ENCANTAMIENTO_LEGACY.put("SHARPNESS", "DAMAGE_ALL");
        ENCANTAMIENTO_LEGACY.put("PROTECTION", "PROTECTION_ENVIRONMENTAL");
        ENCANTAMIENTO_LEGACY.put("FIRE_PROTECTION", "PROTECTION_FIRE");
        ENCANTAMIENTO_LEGACY.put("FEATHER_FALLING", "PROTECTION_FALL");
        ENCANTAMIENTO_LEGACY.put("BLAST_PROTECTION", "PROTECTION_EXPLOSIONS");
        ENCANTAMIENTO_LEGACY.put("PROJECTILE_PROTECTION", "PROTECTION_PROJECTILE");
        ENCANTAMIENTO_LEGACY.put("RESPIRATION", "OXYGEN");
        ENCANTAMIENTO_LEGACY.put("AQUA_AFFINITY", "WATER_WORKER");
        ENCANTAMIENTO_LEGACY.put("EFFICIENCY", "DIG_SPEED");
        ENCANTAMIENTO_LEGACY.put("UNBREAKING", "DURABILITY");
        ENCANTAMIENTO_LEGACY.put("FORTUNE", "LOOT_BONUS_BLOCKS");
        ENCANTAMIENTO_LEGACY.put("LOOTING", "LOOT_BONUS_MOBS");
        ENCANTAMIENTO_LEGACY.put("POWER", "ARROW_DAMAGE");
        ENCANTAMIENTO_LEGACY.put("PUNCH", "ARROW_KNOCKBACK");
        ENCANTAMIENTO_LEGACY.put("FLAME", "ARROW_FIRE");
        ENCANTAMIENTO_LEGACY.put("INFINITY", "ARROW_INFINITE");
        ENCANTAMIENTO_LEGACY.put("LUCK_OF_THE_SEA", "LUCK");
        ENCANTAMIENTO_LEGACY.put("SWEEPING_EDGE", "SWEEPING_EDGE");
        ENCANTAMIENTO_LEGACY.put("DEPTH_STRIDER", "DEPTH_STRIDER");
        ENCANTAMIENTO_LEGACY.put("FROST_WALKER", "FROST_WALKER");
        ENCANTAMIENTO_LEGACY.put("MENDING", "MENDING");
        ENCANTAMIENTO_LEGACY.put("CURSE_OF_VANISHING", "VANISHING_CURSE");
        ENCANTAMIENTO_LEGACY.put("CURSE_OF_BINDING", "BINDING_CURSE");

        PARTICULA_LEGACY.put("HAPPY_VILLAGER", "VILLAGER_HAPPY");
        PARTICULA_LEGACY.put("ENCHANTED_HIT", "CRIT_MAGIC");
        PARTICULA_LEGACY.put("CRIT", "CRIT");
        PARTICULA_LEGACY.put("FLAME", "FLAME");
        PARTICULA_LEGACY.put("HEART", "HEART");
        PARTICULA_LEGACY.put("SMOKE", "SMOKE_NORMAL");
        PARTICULA_LEGACY.put("LARGE_SMOKE", "SMOKE_LARGE");
        PARTICULA_LEGACY.put("EXPLOSION", "EXPLOSION_NORMAL");
        PARTICULA_LEGACY.put("EXPLOSION_EMITTER", "EXPLOSION_HUGE");
        PARTICULA_LEGACY.put("POOF", "EXPLOSION_NORMAL");
        PARTICULA_LEGACY.put("FIREWORK", "FIREWORKS_SPARK");
        PARTICULA_LEGACY.put("PORTAL", "PORTAL");
        PARTICULA_LEGACY.put("SPELL_WITCH", "SPELL_WITCH");
        PARTICULA_LEGACY.put("SPELL_INSTANT", "SPELL_INSTANT");
        PARTICULA_LEGACY.put("TOTEM_OF_UNDYING", "VILLAGER_HAPPY");
        PARTICULA_LEGACY.put("ENCHANT", "ENCHANTMENT_TABLE");
        PARTICULA_LEGACY.put("CLOUD", "CLOUD");
        PARTICULA_LEGACY.put("VILLAGER_HAPPY", "VILLAGER_HAPPY");
        PARTICULA_LEGACY.put("TOTEM", "VILLAGER_HAPPY");
        PARTICULA_LEGACY.put("LAVA", "LAVA");
        PARTICULA_LEGACY.put("WATER_SPLASH", "WATER_SPLASH");
        PARTICULA_LEGACY.put("SNOWFLAKE", "SNOW_SHOVEL");
        PARTICULA_LEGACY.put("END_ROD", "END_ROD");
        PARTICULA_LEGACY.put("DRAGON_BREATH", "DRAGON_BREATH");
        PARTICULA_LEGACY.put("SLIME", "SLIME");
        PARTICULA_LEGACY.put("NOTE", "NOTE");
        PARTICULA_LEGACY.put("REDSTONE", "REDSTONE");
        PARTICULA_LEGACY.put("SOUL_FIRE_FLAME", "FLAME");
        PARTICULA_LEGACY.put("ELECTRIC_SPARK", "CRIT");
        PARTICULA_LEGACY.put("TOTEM_OF_UNDYING", "VILLAGER_HAPPY");

        // En 1.8 algunas particulas se llaman distinto (enum Effect).
        EFECTO_18.put("VILLAGER_HAPPY", "HAPPY_VILLAGER");
        EFECTO_18.put("HAPPY_VILLAGER", "HAPPY_VILLAGER");
        EFECTO_18.put("CRIT_MAGIC", "MAGIC_CRIT");
        EFECTO_18.put("ENCHANTED_HIT", "MAGIC_CRIT");
        EFECTO_18.put("SMOKE_NORMAL", "PARTICLE_SMOKE");
        EFECTO_18.put("SMOKE_LARGE", "LARGE_SMOKE");
        EFECTO_18.put("EXPLOSION_NORMAL", "EXPLOSION");
        EFECTO_18.put("EXPLOSION_LARGE", "EXPLOSION_LARGE");
        EFECTO_18.put("EXPLOSION_HUGE", "EXPLOSION_HUGE");
        EFECTO_18.put("WATER_SPLASH", "SPLASH");
        EFECTO_18.put("SNOWFLAKE", "SNOW_SHOVEL");
        EFECTO_18.put("ITEM_BREAK", "ITEM_BREAK");
        EFECTO_18.put("PORTAL", "PORTAL");
        EFECTO_18.put("FLAME", "FLAME");
        EFECTO_18.put("HEART", "HEART");
        EFECTO_18.put("CLOUD", "CLOUD");
        EFECTO_18.put("LAVA", "LAVA");
        EFECTO_18.put("CRIT", "CRIT");
        EFECTO_18.put("SLIME", "SLIME");
        EFECTO_18.put("NOTE", "NOTE");

        COLORES.put("AQUA", new int[]{0, 255, 255});
        COLORES.put("BLACK", new int[]{0, 0, 0});
        COLORES.put("BLUE", new int[]{0, 0, 255});
        COLORES.put("FUCHSIA", new int[]{255, 0, 255});
        COLORES.put("GRAY", new int[]{128, 128, 128});
        COLORES.put("GREEN", new int[]{0, 128, 0});
        COLORES.put("LIME", new int[]{0, 255, 0});
        COLORES.put("MAROON", new int[]{128, 0, 0});
        COLORES.put("NAVY", new int[]{0, 0, 128});
        COLORES.put("OLIVE", new int[]{128, 128, 0});
        COLORES.put("ORANGE", new int[]{255, 165, 0});
        COLORES.put("PINK", new int[]{255, 192, 203});
        COLORES.put("PURPLE", new int[]{128, 0, 128});
        COLORES.put("RED", new int[]{255, 0, 0});
        COLORES.put("SILVER", new int[]{192, 192, 192});
        COLORES.put("TEAL", new int[]{0, 128, 128});
        COLORES.put("WHITE", new int[]{255, 255, 255});
        COLORES.put("YELLOW", new int[]{255, 255, 0});
        COLORES.put("MAGENTA", new int[]{255, 0, 255});
        COLORES.put("LIGHT_BLUE", new int[]{85, 255, 255});
        COLORES.put("CYAN", new int[]{0, 255, 255});
        COLORES.put("BROWN", new int[]{139, 69, 19});
        COLORES.put("LIGHT_GRAY", new int[]{192, 192, 192});
    }

    private Compat() {
    }

    // ------------------------------------------------------------------
    // Materiales
    // ------------------------------------------------------------------

    /**
     * Crea un ItemStack probando primero el nombre moderno (1.13+) y despues
     * el legacy con su data value (1.8-1.12).
     *
     * @return el ItemStack, o null si el material no existe en esta version.
     */
    public static ItemStack material(String nombre, int cantidad) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String limpio = nombre.trim().toUpperCase(Locale.ROOT);
        Material moderno = Material.matchMaterial(limpio);
        if (moderno != null) {
            return new ItemStack(moderno, Math.max(1, cantidad));
        }
        String[] legacy = MATERIAL_LEGACY.get(limpio);
        if (legacy != null) {
            Material material = Material.matchMaterial(legacy[0]);
            if (material != null) {
                ItemStack item = new ItemStack(material, Math.max(1, cantidad));
                try {
                    item.setDurability(Short.parseShort(legacy[1]));
                } catch (NumberFormatException ignorado) {
                    // data value invalido: se queda el material sin data
                }
                return item;
            }
        }
        aviso("material-" + limpio, "El material '" + nombre + "' does not exist on this version de Minecraft: skipped.");
        return null;
    }

    /** true si el material existe en esta version (para el validador). */
    public static boolean materialExiste(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        String limpio = nombre.trim().toUpperCase(Locale.ROOT);
        if (Material.matchMaterial(limpio) != null) {
            return true;
        }
        String[] legacy = MATERIAL_LEGACY.get(limpio);
        return legacy != null && Material.matchMaterial(legacy[0]) != null;
    }

    // ------------------------------------------------------------------
    // Encantamientos
    // ------------------------------------------------------------------

    /** Devuelve el encantamiento por nombre moderno o legacy, o null. */
    @SuppressWarnings("deprecation")
    public static Enchantment encantamiento(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String limpio = nombre.trim().toUpperCase(Locale.ROOT);
        Enchantment encantamiento = Enchantment.getByName(limpio);
        if (encantamiento != null) {
            return encantamiento;
        }
        String legacy = ENCANTAMIENTO_LEGACY.get(limpio);
        if (legacy != null) {
            encantamiento = Enchantment.getByName(legacy);
            if (encantamiento != null) {
                return encantamiento;
            }
        }
        aviso("encantamiento-" + limpio, "El encantamiento '" + nombre + "' does not exist on this version: skipped.");
        return null;
    }

    /** true si el encantamiento existe (para el validador). */
    @SuppressWarnings("deprecation")
    public static boolean encantamientoExiste(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        String limpio = nombre.trim().toUpperCase(Locale.ROOT);
        if (Enchantment.getByName(limpio) != null) {
            return true;
        }
        String legacy = ENCANTAMIENTO_LEGACY.get(limpio);
        return legacy != null && Enchantment.getByName(legacy) != null;
    }

    // ------------------------------------------------------------------
    // Sonidos
    // ------------------------------------------------------------------

    /** Reproduce un sonido por nombre, con equivalencia automatica en 1.8. */
    public static void sonido(Player jugador, String nombre, float volumen, float tono) {
        if (jugador == null || nombre == null || nombre.trim().isEmpty()) {
            return;
        }
        Location ubicacion = jugador.getLocation();
        if (ubicacion == null || ubicacion.getWorld() == null) {
            return;
        }
        String limpio = nombre.trim().toUpperCase(Locale.ROOT);
        if (intentarSonidoEnum(jugador, ubicacion, limpio, volumen, tono)) {
            return;
        }
        String legacy = SONIDO_LEGACY.get(limpio);
        if (legacy != null && intentarSonidoEnum(jugador, ubicacion, legacy, volumen, tono)) {
            return;
        }
        if (intentarSonidoTexto(jugador, ubicacion, nombre.trim(), volumen, tono)) {
            return;
        }
        aviso("sonido-" + limpio, "El sonido '" + nombre + "' does not exist on this version: skipped.");
    }

    /**
     * true si el sonido se puede reproducir en esta version (para el validador).
     * Se comprueba con valueOf en vez de recorrer el enum: asi sigue
     * funcionando aunque una version futura cambie Sound por una interfaz.
     */
    @SuppressWarnings("deprecation")
    public static boolean sonidoExiste(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        String limpio = nombre.trim().toUpperCase(Locale.ROOT);
        try {
            Sound.valueOf(limpio);
            return true;
        } catch (Throwable noExiste) {
            // se prueba el equivalente de 1.8
        }
        String legacy = SONIDO_LEGACY.get(limpio);
        if (legacy != null) {
            try {
                Sound.valueOf(legacy);
                return true;
            } catch (Throwable noExiste) {
                // sigue abajo
            }
        }
        // 1.9+: los sonidos tambien pueden ser texto libre (resource packs).
        return limpio.indexOf(':') > 0 || limpio.indexOf('.') > 0;
    }

    private static boolean intentarSonidoEnum(Player jugador, Location ubicacion, String nombre, float volumen, float tono) {
        try {
            Sound sonido = Sound.valueOf(nombre);
            jugador.getWorld().playSound(ubicacion, sonido, volumen, tono);
            return true;
        } catch (Throwable ignorado) {
            return false;
        }
    }

    /** 1.9+: World#playSound(Location, String, float, float) por reflexion. */
    private static boolean intentarSonidoTexto(Player jugador, Location ubicacion, String nombre, float volumen, float tono) {
        try {
            if (!sonidoTextoBuscado) {
                sonidoTextoBuscado = true;
                try {
                    metodoSonidoTexto = org.bukkit.World.class.getMethod("playSound", Location.class, String.class, float.class, float.class);
                } catch (NoSuchMethodException ignorado) {
                    metodoSonidoTexto = null;
                }
            }
            if (metodoSonidoTexto == null) {
                return false;
            }
            metodoSonidoTexto.invoke(jugador.getWorld(), ubicacion, nombre, volumen, tono);
            return true;
        } catch (Throwable ignorado) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Particulas
    // ------------------------------------------------------------------

    /**
     * Lanza particulas en una ubicacion. Acepta nombres modernos
     * (HAPPY_VILLAGER) y antiguos (VILLAGER_HAPPY).
     *
     * @return true si la particula se pudo lanzar.
     */
    public static boolean particula(Location ubicacion, String nombre, int cantidad, double dispersion) {
        if (ubicacion == null || ubicacion.getWorld() == null || nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        String limpio = nombre.trim().toUpperCase(Locale.ROOT);
        if (lanzarParticula(ubicacion, limpio, cantidad, dispersion)) {
            return true;
        }
        String legacy = PARTICULA_LEGACY.get(limpio);
        if (legacy != null && lanzarParticula(ubicacion, legacy, cantidad, dispersion)) {
            return true;
        }
        String efecto18 = EFECTO_18.get(limpio);
        if (legacy != null) {
            String alternativo = EFECTO_18.get(legacy);
            if (alternativo != null) {
                efecto18 = alternativo;
            }
        }
        if (lanzarEfecto(ubicacion, limpio, cantidad)
                || (legacy != null && lanzarEfecto(ubicacion, legacy, cantidad))
                || (efecto18 != null && lanzarEfecto(ubicacion, efecto18, cantidad))) {
            return true;
        }
        aviso("particula-" + limpio, "La particula '" + nombre + "' does not exist on this version: skipped.");
        return false;
    }

    /** 1.9+: org.bukkit.Particle + World#spawnParticle (por reflexion). */
    private static boolean lanzarParticula(Location ubicacion, String nombre, int cantidad, double dispersion) {
        try {
            if (!particulaBuscada) {
                particulaBuscada = true;
                try {
                    claseParticula = Class.forName("org.bukkit.Particle");
                    metodoParticula = org.bukkit.World.class.getMethod("spawnParticle", claseParticula, Location.class,
                            int.class, double.class, double.class, double.class, double.class);
                } catch (Throwable sinParticulas) {
                    claseParticula = null;
                    metodoParticula = null;
                }
            }
            if (claseParticula == null || metodoParticula == null) {
                return false;
            }
            Object particula = valorEnum(claseParticula, nombre);
            if (particula == null) {
                return false;
            }
            metodoParticula.invoke(ubicacion.getWorld(), particula, ubicacion, Math.max(1, cantidad),
                    dispersion, dispersion, dispersion, 0.0D);
            return true;
        } catch (Throwable ignorado) {
            return false;
        }
    }

    /** 1.8: org.bukkit.Effect + World#playEffect. */
    private static boolean lanzarEfecto(Location ubicacion, String nombre, int cantidad) {
        try {
            if (!efectoBuscado) {
                efectoBuscado = true;
                try {
                    claseEfecto = Class.forName("org.bukkit.Effect");
                    metodoEfecto = org.bukkit.World.class.getMethod("playEffect", Location.class, claseEfecto, int.class);
                } catch (Throwable sinEfectos) {
                    claseEfecto = null;
                    metodoEfecto = null;
                }
            }
            if (claseEfecto == null || metodoEfecto == null) {
                return false;
            }
            Object efecto = valorEnum(claseEfecto, nombre);
            if (efecto == null) {
                return false;
            }
            metodoEfecto.invoke(ubicacion.getWorld(), ubicacion, efecto, 1);
            return true;
        } catch (Throwable ignorado) {
            return false;
        }
    }

    /** true si la particula se puede lanzar en esta version (para el validador). */
    public static boolean particulaExiste(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        String limpio = nombre.trim().toUpperCase(Locale.ROOT);
        String legacy = PARTICULA_LEGACY.get(limpio);
        try {
            Class<?> tipo = Class.forName("org.bukkit.Particle");
            return valorEnum(tipo, limpio) != null || (legacy != null && valorEnum(tipo, legacy) != null);
        } catch (Throwable sinParticulas) {
            try {
                Class<?> tipo = Class.forName("org.bukkit.Effect");
                if (valorEnum(tipo, limpio) != null || (legacy != null && valorEnum(tipo, legacy) != null)) {
                    return true;
                }
                String efecto18 = EFECTO_18.get(limpio);
                if (efecto18 == null && legacy != null) {
                    efecto18 = EFECTO_18.get(legacy);
                }
                return efecto18 != null && valorEnum(tipo, efecto18) != null;
            } catch (Throwable sinEfectos) {
                return false;
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object valorEnum(Class<?> clase, String nombre) {
        try {
            return Enum.valueOf((Class<Enum>) clase, nombre);
        } catch (Throwable noExiste) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Fuegos artificiales
    // ------------------------------------------------------------------

    /** Lanza un fuego artificial de colores (por nombre: RED, AQUA, ...). */
    public static void fuegoArtificial(Location ubicacion, List<String> colores, int potencia) {
        if (ubicacion == null || ubicacion.getWorld() == null) {
            return;
        }
        try {
            Firework fuego = ubicacion.getWorld().spawn(ubicacion, Firework.class);
            FireworkMeta meta = fuego.getFireworkMeta();
            FireworkEffect.Builder efecto = FireworkEffect.builder().with(FireworkEffect.Type.BALL).withTrail()
                    .withFlicker();
            boolean algunColor = false;
            if (colores != null) {
                for (String nombre : colores) {
                    Color color = color(nombre);
                    if (color != null) {
                        efecto.withColor(color);
                        algunColor = true;
                    }
                }
            }
            if (!algunColor) {
                efecto.withColor(Color.AQUA, Color.FUCHSIA, Color.YELLOW);
            }
            meta.addEffect(efecto.build());
            meta.setPower(Math.max(0, Math.min(4, potencia)));
            fuego.setFireworkMeta(meta);
        } catch (Throwable error) {
            aviso("fuegos", "No se pudo lanzar un fuego artificial: " + error.getClass().getSimpleName());
        }
    }

    /** Color por nombre (RED, LIGHT_BLUE, ...) o por codigo legacy (&c). */
    public static Color color(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String limpio = nombre.trim().toUpperCase(Locale.ROOT);
        int[] rgb = COLORES.get(limpio);
        if (rgb == null) {
            rgb = COLORES.get(porCodigo(limpio));
        }
        if (rgb == null) {
            return null;
        }
        return Color.fromRGB(rgb[0], rgb[1], rgb[2]);
    }

    /** Traduce un codigo legacy (&c) a un nombre de color. */
    private static String porCodigo(String codigo) {
        if (codigo.length() != 2) {
            return "";
        }
        char letra = codigo.charAt(1);
        switch (letra) {
            case '0': return "BLACK";
            case '1': return "NAVY";
            case '2': return "GREEN";
            case '3': return "TEAL";
            case '4': return "MAROON";
            case '5': return "PURPLE";
            case '6': return "ORANGE";
            case '7': return "SILVER";
            case '8': return "GRAY";
            case '9': return "BLUE";
            case 'a': return "LIME";
            case 'b': return "AQUA";
            case 'c': return "RED";
            case 'd': return "FUCHSIA";
            case 'e': return "YELLOW";
            case 'f': return "WHITE";
            default: return "";
        }
    }

    // ------------------------------------------------------------------
    // Titulos y action bar
    // ------------------------------------------------------------------

    /** Titulo de pantalla en cualquier version (con degradacion a chat). */
    public static void titulo(Player jugador, String arriba, String abajo) {
        String titulo = Texto.color(arriba == null ? "" : arriba);
        String subtitulo = Texto.color(abajo == null ? "" : abajo);
        if (probarTituloModerno(jugador, titulo, subtitulo)) {
            return;
        }
        if (probarTituloPaquete(jugador, titulo, subtitulo)) {
            return;
        }
        if (!titulo.isEmpty()) {
            jugador.sendMessage(titulo);
            if (!subtitulo.isEmpty()) {
                jugador.sendMessage(subtitulo);
            }
        }
    }

    /** true si el servidor soporta titulos (para el validador y /info). */
    public static boolean soportaTitulos() {
        try {
            org.bukkit.entity.Player.class.getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            return true;
        } catch (Throwable sinTitulos) {
            return comprobarPaqueteTitulos();
        }
    }

    private static boolean comprobarPaqueteTitulos() {
        try {
            String paquete = Bukkit.getServer().getClass().getPackage().getName();
            String[] partes = paquete.split("\\.");
            if (partes.length < 4) {
                return false;
            }
            Class.forName("net.minecraft.server." + partes[3] + ".PacketPlayOutTitle");
            return true;
        } catch (Throwable sinPaquete) {
            return false;
        }
    }

    /** 1.11+: Player#sendTitle(String, String, int, int, int). */
    private static boolean probarTituloModerno(Player jugador, String titulo, String subtitulo) {
        try {
            if (!tituloModernoBuscado) {
                tituloModernoBuscado = true;
                try {
                    metodoTituloModerno = jugador.getClass().getMethod("sendTitle", String.class, String.class,
                            int.class, int.class, int.class);
                } catch (NoSuchMethodException ignorado) {
                    metodoTituloModerno = null;
                }
            }
            if (metodoTituloModerno == null) {
                return false;
            }
            metodoTituloModerno.invoke(jugador, titulo, subtitulo, 10, 40, 10);
            return true;
        } catch (Throwable ignorado) {
            return false;
        }
    }

    /** 1.8 - 1.10: paquetes PacketPlayOutTitle por reflexion. */
    private static boolean probarTituloPaquete(Player jugador, String titulo, String subtitulo) {
        if (tituloPaqueteNoDisponible) {
            return false;
        }
        try {
            String paqueteServidor = Bukkit.getServer().getClass().getPackage().getName();
            String[] partes = paqueteServidor.split("\\.");
            if (partes.length < 4) {
                tituloPaqueteNoDisponible = true;
                return false;
            }
            String version = partes[3];
            Class<?> claseTitulo = Class.forName("net.minecraft.server." + version + ".PacketPlayOutTitle");
            Class<?> claseAccion = Class.forName("net.minecraft.server." + version + ".PacketPlayOutTitle$EnumTitleAction");
            Class<?> claseComponente = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> claseSerializador = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");

            Method serializar = claseSerializador.getMethod("a", String.class);
            Object componentTitulo = serializar.invoke(null, json(titulo));
            Object componentSubtitulo = serializar.invoke(null, json(subtitulo));

            Object tiempos = claseTitulo
                    .getConstructor(claseAccion, claseComponente, int.class, int.class, int.class)
                    .newInstance(valorEnum(claseAccion, "TIMES"), null, 10, 40, 10);
            Object paqueteTitulo = claseTitulo
                    .getConstructor(claseAccion, claseComponente)
                    .newInstance(valorEnum(claseAccion, "TITLE"), componentTitulo);
            Object paqueteSubtitulo = claseTitulo
                    .getConstructor(claseAccion, claseComponente)
                    .newInstance(valorEnum(claseAccion, "SUBTITLE"), componentSubtitulo);

            Object entidad = jugador.getClass().getMethod("getHandle").invoke(jugador);
            Field campoConexion = entidad.getClass().getField("playerConnection");
            Object conexion = campoConexion.get(entidad);
            Method enviar = conexion.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"));
            enviar.invoke(conexion, tiempos);
            enviar.invoke(conexion, paqueteTitulo);
            enviar.invoke(conexion, paqueteSubtitulo);
            return true;
        } catch (Throwable ignorado) {
            tituloPaqueteNoDisponible = true;
            return false;
        }
    }

    /** Action bar via API de chat de Spigot (existe desde 1.8.8). */
    public static boolean actionBar(Player jugador, String texto) {
        try {
            Class<?> claseTipo = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Object accion = valorEnum(claseTipo, "ACTION_BAR");
            Class<?> claseComponente = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Class<?> claseTexto = Class.forName("net.md_5.bungee.api.chat.TextComponent");

            Object componente = claseTexto.getConstructor(String.class).newInstance(Texto.color(texto));
            Object spigot = jugador.getClass().getMethod("spigot").invoke(jugador);
            Method enviar = spigot.getClass().getMethod("sendMessage", claseTipo, claseComponente);
            enviar.invoke(spigot, accion, componente);
            return true;
        } catch (Throwable ignorado) {
            return false;
        }
    }

    /** true si el servidor soporta action bar. */
    public static boolean soportaActionBar() {
        try {
            Class.forName("net.md_5.bungee.api.ChatMessageType");
            return true;
        } catch (Throwable sinActionBar) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // ItemMeta por version
    // ------------------------------------------------------------------

    /** ItemMeta#setCustomModelData existe desde 1.14. */
    public static boolean customModelData(ItemMeta meta, int dato) {
        if (meta == null || dato <= 0) {
            return false;
        }
        try {
            Method metodo = meta.getClass().getMethod("setCustomModelData", Integer.class);
            metodo.invoke(meta, Integer.valueOf(dato));
            return true;
        } catch (Throwable sinModelData) {
            aviso("custom-model-data", "Tu version de Minecraft no soporta 'custom-model-data' (1.14+): skipped.");
            return false;
        }
    }

    /** ItemMeta#setUnbreakable existe desde 1.11 (en 1.8 se simula con flag). */
    public static void irrompible(ItemMeta meta, boolean irrompible) {
        if (meta == null) {
            return;
        }
        try {
            Method metodo = meta.getClass().getMethod("setUnbreakable", boolean.class);
            metodo.invoke(meta, Boolean.valueOf(irrompible));
            return;
        } catch (Throwable sinUnbreakable) {
            // 1.8 - 1.10: encantamiento de durabilidad oculto.
            if (irrompible) {
                Enchantment durabilidad = encantamiento("DURABILITY");
                if (durabilidad != null) {
                    meta.addEnchant(durabilidad, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }
        }
    }

    /** ItemFlag por nombre, ignorando los que no existan en esta version. */
    public static ItemFlag flag(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        try {
            return ItemFlag.valueOf(nombre.trim().toUpperCase(Locale.ROOT));
        } catch (Throwable noExiste) {
            aviso("flag-" + nombre.toUpperCase(Locale.ROOT), "El item-flag '" + nombre + "' does not exist on this version: skipped.");
            return null;
        }
    }

    /** Nombres de item-flag mas usados (para el validador y la documentacion). */
    public static List<String> flagsConocidos() {
        List<String> nombres = new ArrayList<String>();
        for (ItemFlag flag : ItemFlag.values()) {
            nombres.add(flag.name());
        }
        return nombres;
    }

    // ------------------------------------------------------------------
    // Entorno
    // ------------------------------------------------------------------

    /** Version del servidor, p. ej. "1.8.8" o "1.21.4". */
    public static String versionServidor() {
        String bukkit = Bukkit.getBukkitVersion();
        int guion = bukkit.indexOf('-');
        return guion > 0 ? bukkit.substring(0, guion) : bukkit;
    }

    /** Version mayor del servidor: 8 para 1.8, 21 para 1.21 (0 si no se sabe). */
    public static int versionMayor() {
        return versionMayor(versionServidor());
    }

    /** Version mayor de un texto tipo "1.8.8" / "1.21": 8 / 21 (0 si no se sabe). */
    public static int versionMayor(String version) {
        if (version == null) {
            return 0;
        }
        String[] partes = version.split("\\.");
        if (partes.length < 2) {
            return 0;
        }
        try {
            int mayor = Integer.parseInt(partes[0].trim());
            int menor = Integer.parseInt(partes[1].replaceAll("[^0-9]", ""));
            if (mayor > 1) {
                // Servidores "26.3" (Paper) cuentan como muy modernos.
                return mayor;
            }
            return menor;
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    /** true si el servidor es 1.8.x (limitaciones de titulos, action bar...). */
    public static boolean esLegacy() {
        return versionMayor() == 8;
    }

    // ------------------------------------------------------------------
    // Utilidades internas
    // ------------------------------------------------------------------

    private static String json(String texto) {
        String escapado = texto.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"text\":\"" + escapado + "\"}";
    }

    /** Avisa una sola vez por clave (evita spamear la consola). */
    public static void aviso(String clave, String mensaje) {
        if (AVISOS.add(clave)) {
            Bukkit.getLogger().warning("[JPRewards] " + mensaje);
        }
    }

    /** Limpia los avisos ya mostrados (al recargar, para volver a avisar). */
    public static void limpiarAvisos() {
        AVISOS.clear();
    }

}
