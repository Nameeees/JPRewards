package me.jp.jprewards.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cabezas de jugador: con textura personalizada (value base64) o con la
 * cabeza de un jugador concreto, en cualquier version.
 *
 *   1.13.2+ : SkullMeta#setOwnerProfile(GameProfile)
 *   1.12.2- : campo privado 'profile' de CraftMetaSkull
 *   1.20.2+ : GameProfile exige nombre no nulo (se usa el del plugin)
 *
 * Si algo falla devuelve la cabeza SIN textura (nunca rompe el menu) y las
 * texturas ya creadas se cachean para no repetir el trabajo en cada apertura.
 */
public final class Cabezas {

    private static final String NOMBRE_PERFIL = "JPRewards";
    /** Maximo de texturas recordadas (los menus suelen usar unas pocas). */
    private static final int MAX_CACHE = 256;

    private static boolean perfilNoDisponible;

    private static final Map<String, ItemStack> CACHE = new LinkedHashMap<String, ItemStack>(64, 0.75f, true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ItemStack> mayor) {
            return size() > MAX_CACHE;
        }
    };

    private Cabezas() {
    }

    /** Cabeza de jugador con la textura indicada (value base64 de minecraft-heads). */
    public static ItemStack crear(String base64) {
        ItemStack base = Compat.material("PLAYER_HEAD", 1);
        if (base == null) {
            // Ultimo recurso: si PLAYER_HEAD/SKULL_ITEM no existe, un papel.
            base = Compat.material("PAPER", 1);
            return base == null ? new ItemStack(org.bukkit.Material.AIR) : base;
        }
        if (base64 == null || base64.trim().isEmpty()) {
            return base;
        }
        synchronized (CACHE) {
            ItemStack guardada = CACHE.get(base64);
            if (guardada != null) {
                return guardada.clone();
            }
        }
        ItemStack item = base;
        try {
            if (item.getItemMeta() instanceof SkullMeta) {
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                Object perfil = perfil(base64);
                if (!aplicarPerfil(meta, perfil)) {
                    Compat.aviso("cabezas-perfil",
                            "Could not apply the texture to a head on this version: a head without a texture will be used.");
                }
                item.setItemMeta(meta);
            }
        } catch (Throwable error) {
            Compat.aviso("cabezas-error", "Error al crear una cabeza con textura: " + error.getClass().getSimpleName());
        }
        synchronized (CACHE) {
            CACHE.put(base64, item.clone());
        }
        return item;
    }

    /** Cabeza del jugador indicado (por nombre). */
    @SuppressWarnings("deprecation")
    public static ItemStack deJugador(String nombre) {
        ItemStack item = Compat.material("PLAYER_HEAD", 1);
        if (item == null || nombre == null || nombre.trim().isEmpty()) {
            return item;
        }
        try {
            if (item.getItemMeta() instanceof SkullMeta) {
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                OfflinePlayer dueno = Bukkit.getOfflinePlayer(nombre.trim());
                if (!aplicarDueno(meta, dueno)) {
                    return item;
                }
                item.setItemMeta(meta);
            }
        } catch (Throwable error) {
            Compat.aviso("cabezas-jugador", "Could not create la cabeza de '" + nombre + "': "
                    + error.getClass().getSimpleName());
        }
        return item;
    }

    private static boolean aplicarDueno(SkullMeta meta, OfflinePlayer dueno) {
        try {
            Method metodo = meta.getClass().getMethod("setOwningPlayer", OfflinePlayer.class);
            metodo.invoke(meta, dueno);
            return true;
        } catch (Throwable sinOwningPlayer) {
            try {
                Method metodo = meta.getClass().getMethod("setOwner", String.class);
                metodo.invoke(meta, dueno.getName());
                return true;
            } catch (Throwable sinOwner) {
                return false;
            }
        }
    }

    /**
     * Valida una textura sin crear la cabeza.
     *
     * @return null si es correcta, o el motivo por el que no lo es.
     */
    public static String validarTextura(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }
        byte[] datos;
        try {
            datos = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException error) {
            return "is not valid base64 (copy the full value from minecraft-heads.com)";
        }
        String json = new String(datos, StandardCharsets.UTF_8);
        if (!json.contains("textures") || !json.contains("http")) {
            return "no parece una textura de Minecraft (debe contener la url de textures.minecraft.net)";
        }
        return null;
    }

    /** Vacía la cache (al recargar menus.yml). */
    public static void limpiarCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private static Object perfil(String base64) throws Exception {
        if (perfilNoDisponible) {
            throw new IllegalStateException("GameProfile no disponible");
        }
        Class<?> clasePerfil = Class.forName("com.mojang.authlib.GameProfile");
        Object uuid = UUID.randomUUID();
        Object perfil = null;

        // Constructor clasico (UUID, String). En 1.20.2+ el nombre no puede ser null.
        try {
            perfil = clasePerfil.getConstructor(UUID.class, String.class).newInstance(uuid, NOMBRE_PERFIL);
        } catch (Throwable ignorado) {
            for (Constructor<?> constructor : clasePerfil.getConstructors()) {
                if (constructor.getParameterCount() == 2) {
                    try {
                        perfil = constructor.newInstance(uuid, NOMBRE_PERFIL);
                        break;
                    } catch (Throwable ignorado2) {
                        // siguiente constructor
                    }
                }
            }
        }
        if (perfil == null) {
            perfilNoDisponible = true;
            throw new IllegalStateException("GameProfile sin constructor compatible");
        }

        Class<?> clasePropiedad = Class.forName("com.mojang.authlib.properties.Property");
        Object propiedad = clasePropiedad.getConstructor(String.class, String.class).newInstance("textures", base64);
        Object propiedades = clasePerfil.getMethod("getProperties").invoke(perfil);
        for (Method metodo : propiedades.getClass().getMethods()) {
            if ("put".equals(metodo.getName()) && metodo.getParameterCount() == 2) {
                metodo.invoke(propiedades, "textures", propiedad);
                break;
            }
        }
        return perfil;
    }

    private static boolean aplicarPerfil(SkullMeta meta, Object perfil) {
        // 1.13.2+ (Bukkit SkullMeta#setOwnerProfile y Paper 1.12.2+)
        try {
            Method metodo = meta.getClass().getMethod("setOwnerProfile", Class.forName("com.mojang.authlib.GameProfile"));
            metodo.invoke(meta, perfil);
            return true;
        } catch (Throwable ignorado) {
            // sigue abajo
        }
        // 1.8 - 1.12: campo privado 'profile' de CraftMetaSkull
        try {
            Field campo = meta.getClass().getDeclaredField("profile");
            campo.setAccessible(true);
            campo.set(meta, perfil);
            return true;
        } catch (Throwable ignorado) {
            return false;
        }
    }
}
