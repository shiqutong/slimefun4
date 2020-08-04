package io.github.thebusybiscuit.slimefun4.core.networks.energy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import io.github.thebusybiscuit.cscorelib2.math.DoubleHandler;
import io.github.thebusybiscuit.slimefun4.api.ErrorReport;
import io.github.thebusybiscuit.slimefun4.api.network.Network;
import io.github.thebusybiscuit.slimefun4.api.network.NetworkComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.utils.holograms.SimpleHologram;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.Slimefun;

/**
 * The {@link EnergyNet} is an implementation of {@link Network} that deals with
 * electrical energy being send from and to nodes.
 * 
 * @author meiamsome
 * @author TheBusyBiscuit
 * 
 * @see Network
 * @see EnergyNetComponent
 * @see EnergyNetProvider
 * @see EnergyNetComponentType
 *
 */
public class EnergyNet extends Network {

    private static final int RANGE = 6;

    private static EnergyNetComponent getComponent(Location l) {
        String id = BlockStorage.checkID(l);

        if (id == null) {
            return null;
        }

        SlimefunItem item = SlimefunItem.getByID(id);

        if (item instanceof EnergyNetComponent) {
            return ((EnergyNetComponent) item);
        }

        return null;
    }

    public static EnergyNet getNetworkFromLocationOrCreate(Location l) {
        Optional<EnergyNet> cargoNetwork = SlimefunPlugin.getNetworkManager().getNetworkFromLocation(l, EnergyNet.class);

        if (cargoNetwork.isPresent()) {
            return cargoNetwork.get();
        }
        else {
            EnergyNet network = new EnergyNet(l);
            SlimefunPlugin.getNetworkManager().registerNetwork(network);
            return network;
        }
    }

    private final Map<Location, EnergyNetComponent> generators = new HashMap<>();
    private final Map<Location, EnergyNetComponent> capacitors = new HashMap<>();
    private final Map<Location, EnergyNetComponent> consumers = new HashMap<>();

    protected EnergyNet(Location l) {
        super(SlimefunPlugin.getNetworkManager(), l);
    }

    @Override
    public int getRange() {
        return RANGE;
    }

    @Override
    public NetworkComponent classifyLocation(Location l) {
        if (regulator.equals(l)) {
            return NetworkComponent.REGULATOR;
        }

        EnergyNetComponent component = getComponent(l);

        if (component == null) {
            return null;
        }
        else {
            switch (component.getEnergyComponentType()) {
            case CAPACITOR:
                return NetworkComponent.CONNECTOR;
            case CONSUMER:
            case GENERATOR:
                return NetworkComponent.TERMINUS;
            default:
                return null;
            }
        }
    }

    @Override
    public void onClassificationChange(Location l, NetworkComponent from, NetworkComponent to) {
        if (from == NetworkComponent.TERMINUS) {
            generators.remove(l);
            consumers.remove(l);
        }

        EnergyNetComponent component = getComponent(l);

        if (component != null) {
            switch (component.getEnergyComponentType()) {
            case CAPACITOR:
                capacitors.put(l, component);
                break;
            case CONSUMER:
                consumers.put(l, component);
                break;
            case GENERATOR:
                generators.put(l, component);
                break;
            default:
                break;
            }
        }
    }

    public void tick(Block b) {
        AtomicLong timestamp = new AtomicLong(SlimefunPlugin.getProfiler().newEntry());

        if (!regulator.equals(b.getLocation())) {
            SimpleHologram.update(b, "&4Multiple Energy Regulators connected");
            return;
        }

        super.tick();

        if (connectorNodes.isEmpty() && terminusNodes.isEmpty()) {
            SimpleHologram.update(b, "&4No Energy Network found");
        }
        else {
            int supply = tickAllGenerators(timestamp::getAndAdd) + tickAllCapacitors();
            int remainingEnergy = supply;
            int demand = 0;

            for (Map.Entry<Location, EnergyNetComponent> entry : consumers.entrySet()) {
                Location l = entry.getKey();
                EnergyNetComponent component = entry.getValue();
                int capacity = component.getCapacity();
                int charge = component.getCharge(l);

                if (charge < capacity) {
                    int availableSpace = capacity - charge;
                    demand += availableSpace;

                    if (remainingEnergy > 0) {
                        if (remainingEnergy > availableSpace) {
                            component.setCharge(l, capacity);
                            remainingEnergy -= availableSpace;
                        }
                        else {
                            component.setCharge(l, charge + remainingEnergy);
                            remainingEnergy = 0;
                        }
                    }
                }
            }

            storeRemainingEnergy(remainingEnergy);
            updateHologram(b, supply, demand);
        }

        // We have subtracted the timings from Generators, so they do not show up twice.
        SlimefunPlugin.getProfiler().closeEntry(b.getLocation(), SlimefunItems.ENERGY_REGULATOR.getItem(), timestamp.get());
    }

    private void storeRemainingEnergy(int remainingEnergy) {
        for (Map.Entry<Location, EnergyNetComponent> entry : capacitors.entrySet()) {
            Location l = entry.getKey();
            EnergyNetComponent component = entry.getValue();

            if (remainingEnergy > 0) {
                int capacity = component.getCapacity();

                if (remainingEnergy > capacity) {
                    component.setCharge(l, capacity);
                    remainingEnergy -= capacity;
                }
                else {
                    component.setCharge(l, remainingEnergy);
                    remainingEnergy = 0;
                }
            }
            else {
                component.setCharge(l, 0);
            }
        }

        for (Map.Entry<Location, EnergyNetComponent> entry : generators.entrySet()) {
            Location l = entry.getKey();
            EnergyNetComponent component = entry.getValue();
            int capacity = component.getCapacity();

            if (remainingEnergy > 0) {
                if (remainingEnergy > capacity) {
                    component.setCharge(l, capacity);
                    remainingEnergy -= capacity;
                }
                else {
                    component.setCharge(l, remainingEnergy);
                    remainingEnergy = 0;
                }
            }
            else {
                component.setCharge(l, 0);
            }
        }
    }

    private int tickAllGenerators(LongConsumer timings) {
        Set<Location> exploded = new HashSet<>();
        int supply = 0;

        for (Map.Entry<Location, EnergyNetComponent> entry : generators.entrySet()) {
            long timestamp = SlimefunPlugin.getProfiler().newEntry();
            Location l = entry.getKey();
            EnergyNetComponent component = entry.getValue();

            if (component instanceof EnergyNetProvider) {
                SlimefunItem item = (SlimefunItem) component;
                try {
                    EnergyNetProvider provider = (EnergyNetProvider) component;
                    Config config = BlockStorage.getLocationInfo(l);
                    int energy = provider.getGeneratedOutput(l, config);

                    if (provider.isChargeable()) {
                        energy += provider.getCharge(l);
                    }

                    if (provider.willExplode(l, config)) {
                        exploded.add(l);
                        BlockStorage.clearBlockInfo(l);

                        Slimefun.runSync(() -> {
                            l.getBlock().setType(Material.LAVA);
                            l.getWorld().createExplosion(l, 0F, false);
                        });
                    }
                    else {
                        supply += energy;
                    }
                }
                catch (Exception | LinkageError t) {
                    exploded.add(l);
                    new ErrorReport(t, l, item);
                }

                long time = SlimefunPlugin.getProfiler().closeEntry(l, item, timestamp);
                timings.accept(time);
            }
            else {
                // This block seems to be gone now, better remove it to be extra safe
                exploded.add(l);
            }
        }

        // Remove all generators which have exploded
        generators.keySet().removeAll(exploded);
        return supply;
    }

    private int tickAllCapacitors() {
        int supply = 0;

        for (Map.Entry<Location, EnergyNetComponent> entry : capacitors.entrySet()) {
            supply += entry.getValue().getCharge(entry.getKey());
        }

        return supply;
    }

    private void updateHologram(Block b, double supply, double demand) {
        if (demand > supply) {
            String netLoss = DoubleHandler.getFancyDouble(Math.abs(supply - demand));
            SimpleHologram.update(b, "&4&l- &c" + netLoss + " &7J &e\u26A1");
        }
        else {
            String netGain = DoubleHandler.getFancyDouble(supply - demand);
            SimpleHologram.update(b, "&2&l+ &a" + netGain + " &7J &e\u26A1");
        }
    }
}
