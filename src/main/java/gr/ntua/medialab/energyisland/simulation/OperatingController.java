package gr.ntua.medialab.energyisland.simulation;

import gr.ntua.medialab.energyisland.model.DieselPlant;

import java.util.List;

/**
 * The fixed operating controller of the brief (A.1) with the v2 clarification for rules 2-3.
 * With net load {@code N = L - (P_PV + P_W)} and {@code D_max} the energy all batteries can
 * give this hour:
 * <ol>
 *   <li>{@code N <= 0}: batteries charge with the surplus; what does not fit is curtailed.
 *       Diesel off.</li>
 *   <li>{@code 0 < N <= D_max}: batteries discharge to cover N. Diesel off.</li>
 *   <li>{@code N > D_max}: diesel runs at {@code P_D = min(max(N - D_max, pmin), pmax)}.
 *       Batteries cover only {@code N - P_D}: they discharge if it is positive, and charge if
 *       it is negative; what cannot be stored is curtailed.</li>
 *   <li>Whatever is still missing is unserved energy (blackout).</li>
 * </ol>
 * Batteries are used one after another in the order of their sites in {@code sites}
 * (guide §6): the first charges or discharges as much as it can, the rest passes on.
 */
public final class OperatingController {

    private final DieselPlant diesel;

    /**
     * Creates a controller for the given diesel plant.
     *
     * @param diesel the island's diesel plant
     */
    public OperatingController(DieselPlant diesel) {
        this.diesel = diesel;
    }

    /**
     * Decides one hour and updates the battery levels.
     *
     * @param netLoad   {@code N = L - (P_PV + P_W)}, kWh; negative means surplus
     * @param batteries the installed batteries in site order; their levels are changed
     * @return the decision for the hour
     */
    public Dispatch dispatch(double netLoad, List<BatteryState> batteries) {
        if (netLoad <= 0) {
            double surplus = -netLoad;
            double charged = charge(batteries, surplus);
            return new Dispatch(ControlRule.SURPLUS, 0, 0, charged, 0, surplus - charged);
        }
        double dMax = 0;
        for (BatteryState b : batteries) {
            dMax += b.maxDischarge();
        }
        if (netLoad <= dMax) {
            double given = discharge(batteries, netLoad);
            return new Dispatch(ControlRule.BATTERIES, 0, given, 0, Math.max(0, netLoad - given), 0);
        }
        double pd = Math.min(Math.max(netLoad - dMax, diesel.pmin()), diesel.pmax());
        double rest = netLoad - pd;
        if (rest > 0) {
            double given = discharge(batteries, rest);
            return new Dispatch(ControlRule.DIESEL, pd, given, 0, Math.max(0, rest - given), 0);
        }
        double charged = charge(batteries, -rest);
        return new Dispatch(ControlRule.DIESEL, pd, 0, charged, 0, -rest - charged);
    }

    /** Discharges the batteries in order until {@code amount} is covered; returns what was given. */
    private static double discharge(List<BatteryState> batteries, double amount) {
        double given = 0;
        for (BatteryState b : batteries) {
            if (given >= amount) {
                break;
            }
            given += b.discharge(amount - given);
        }
        return given;
    }

    /** Charges the batteries in order with up to {@code amount}; returns what was taken. */
    private static double charge(List<BatteryState> batteries, double amount) {
        double taken = 0;
        for (BatteryState b : batteries) {
            if (taken >= amount) {
                break;
            }
            taken += b.charge(amount - taken);
        }
        return taken;
    }
}
