package me.jp.jprewards.manager;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.model.BonusRacha;
import me.jp.jprewards.model.Hito;
import me.jp.jprewards.model.Recompensa;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee los archivos de recompensas (recompensas/*.yml) y los convierte en
 * objetos. Los avisos de configuracion son concretos a proposito: dicen el
 * archivo, la clave y que se esperaba, para no tener que adivinar.
 */
public final class Cargador {

    private final JPRewards plugin;

    public Cargador(JPRewards plugin) {
        this.plugin = plugin;
    }

    /** Lee un ciclo (dias o semanas): claves numericas 1..N. */
    public Map<Integer, Recompensa> cargarCiclo(String archivo, String seccion, String nombreEntrada) {
        Map<Integer, Recompensa> ciclo = new LinkedHashMap<Integer, Recompensa>();
        FileConfiguration config = plugin.archivos().cargar(archivo);
        if (config == null) {
            plugin.getLogger().severe("Could not read " + archivo + " (the cycle " + nombreEntrada + " stays empty).");
            return ciclo;
        }
        ConfigurationSection entradas = config.getConfigurationSection(seccion);
        if (entradas == null) {
            plugin.getLogger().warning(archivo + " has no section '" + seccion + "': the cycle "
                    + nombreEntrada + " queda vacio.");
            return ciclo;
        }
        for (String clave : entradas.getKeys(false)) {
            ConfigurationSection entrada = entradas.getConfigurationSection(clave);
            if (entrada == null) {
                plugin.getLogger().warning(archivo + " -> " + seccion + "." + clave
                        + ": se esperaba una seccion con 'lore', 'comandos' o 'items'; skipped.");
                continue;
            }
            int numero;
            try {
                numero = Integer.parseInt(clave.trim());
            } catch (NumberFormatException error) {
                plugin.getLogger().warning(archivo + " -> " + seccion + ": la clave '" + clave
                        + "' is not a number of " + nombreEntrada + "; skipped.");
                continue;
            }
            if (numero <= 0) {
                plugin.getLogger().warning(archivo + " -> " + seccion + "." + clave
                        + ": el numero debe ser 1 o mayor; skipped.");
                continue;
            }
            Recompensa recompensa = Recompensa.leer("dia-" + numero, entrada, nombrePorDefecto(nombreEntrada, numero));
            if (recompensa != null) {
                ciclo.put(Integer.valueOf(numero), recompensa);
            }
        }
        return ordenado(ciclo);
    }

    private Map<Integer, Recompensa> ordenado(Map<Integer, Recompensa> ciclo) {
        List<Integer> claves = new ArrayList<Integer>(ciclo.keySet());
        Collections.sort(claves);
        Map<Integer, Recompensa> resultado = new LinkedHashMap<Integer, Recompensa>();
        for (Integer clave : claves) {
            resultado.put(clave, ciclo.get(clave));
        }
        return resultado;
    }

    private String nombrePorDefecto(String tipo, int numero) {
        if ("semana".equals(tipo)) {
            return "Week " + numero;
        }
        if ("hito".equals(tipo)) {
            return "Milestone " + numero;
        }
        return "Day " + numero;
    }

    /** Lee los hitos mensuales (seccion 'hitos'). */
    public List<Hito> cargarHitos(String archivo) {
        List<Hito> hitos = new ArrayList<Hito>();
        FileConfiguration config = plugin.archivos().cargar(archivo);
        if (config == null) {
            plugin.getLogger().severe("Could not read " + archivo + " (there will be no monthly milestones).");
            return hitos;
        }
        ConfigurationSection seccion = config.getConfigurationSection("hitos");
        if (seccion == null) {
            plugin.getLogger().warning(archivo + " has no section 'hitos': no habra hitos mensuales.");
            return hitos;
        }
        for (String id : seccion.getKeys(false)) {
            ConfigurationSection entrada = seccion.getConfigurationSection(id);
            if (entrada == null) {
                plugin.getLogger().warning(archivo + " -> hitos." + id + ": se esperaba una seccion; skipped.");
                continue;
            }
            int dias = entrada.getInt("dias-requeridos", 0);
            if (dias <= 0) {
                plugin.getLogger().warning(archivo + " -> hitos." + id
                        + ": 'dias-requeridos' is missing (or it is 0); the milestone is skipped.");
                continue;
            }
            Recompensa recompensa = Recompensa.leer("hito-" + id, entrada, "Milestone of " + dias + " days");
            if (recompensa != null) {
                hitos.add(new Hito(id, dias, recompensa));
            }
        }
        Collections.sort(hitos, new Comparator<Hito>() {
            @Override
            public int compare(Hito uno, Hito otro) {
                return Integer.compare(uno.diasRequeridos(), otro.diasRequeridos());
            }
        });
        return hitos;
    }

    /** Lee los bonus de racha (seccion 'bonus'). */
    public List<BonusRacha> cargarBonus(String archivo) {
        List<BonusRacha> bonus = new ArrayList<BonusRacha>();
        FileConfiguration config = plugin.archivos().cargar(archivo);
        if (config == null) {
            plugin.getLogger().info("Could not read " + archivo + " (there will be no streak bonuses).");
            return bonus;
        }
        ConfigurationSection seccion = config.getConfigurationSection("bonus");
        if (seccion == null) {
            plugin.getLogger().warning(archivo + " has no section 'bonus': no habra bonus de racha.");
            return bonus;
        }
        for (String id : seccion.getKeys(false)) {
            ConfigurationSection entrada = seccion.getConfigurationSection(id);
            if (entrada == null) {
                plugin.getLogger().warning(archivo + " -> bonus." + id + ": se esperaba una seccion; skipped.");
                continue;
            }
            BonusRacha.Tipo tipo = BonusRacha.Tipo.desde(entrada.getString("tipo", "diaria"));
            if (tipo == null) {
                plugin.getLogger().warning(archivo + " -> bonus." + id + ".tipo: '"
                        + entrada.getString("tipo") + "' is not valid (diaria o semanal); using diaria.");
                tipo = BonusRacha.Tipo.DIARIA;
            }
            int racha = entrada.getInt("racha-minima", 0);
            if (racha <= 0) {
                plugin.getLogger().warning(archivo + " -> bonus." + id
                        + ": 'racha-minima' is missing (or it is 0); the bonus is skipped.");
                continue;
            }
            Recompensa recompensa = Recompensa.leer("racha-" + id, entrada,
                    tipo == BonusRacha.Tipo.SEMANAL ? racha + " week streak" : racha + " day streak");
            if (recompensa != null) {
                bonus.add(new BonusRacha(id, tipo, racha, recompensa));
            }
        }
        Collections.sort(bonus, new Comparator<BonusRacha>() {
            @Override
            public int compare(BonusRacha uno, BonusRacha otro) {
                int porTipo = uno.tipo().compareTo(otro.tipo());
                return porTipo != 0 ? porTipo : Integer.compare(uno.rachaMinima(), otro.rachaMinima());
            }
        });
        return bonus;
    }
}
