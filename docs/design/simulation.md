# Simulation design

Package `gr.ntua.medialab.energyisland.simulation`. Implements brief A.1 "Προσομοίωση λειτουργίας",
"Ελεγκτής λειτουργίας" and "Σχέδια και κόστος", together with guide §6, both in **v2**.

## Classes

| Class | Role |
|---|---|
| `Simulator` | Entry point. Created once per scenario. It snapshots the island and precomputes the hourly demand of each day. `simulate(Plan)` → `SimulationResult`. Stateless between calls, so it is safe to share between threads (for the optimiser). |
| `OperatingController` | The fixed controller: rules 1–4 for one hour. `dispatch(N, batteries)` → `Dispatch`. |
| `BatteryState` | Mutable level of one installed battery: `maxCharge`, `maxDischarge`, `charge`, `discharge`, `reset`. |
| `Dispatch` (record) | Hour decision: rule, `P_D`, discharge, charge, unserved `U`, curtailed `C`. |
| `ControlRule` (enum) | `SURPLUS` (rule 1), `BATTERIES` (rule 2), `DIESEL` (rule 3). A blackout (rule 4) shows up as `U > 0`. |
| `HourResult` (record) | Load, PV, wind, dispatch, diesel start, battery levels at the end of the hour (in site order). Used for GUI playback and charts. |
| `DayResult` | 24 hours + day totals, starts `S_d`, emissions, operating cost, `hasBlackout()`. |
| `SimulationResult` | All days + annual totals: `annualCost()` = K, `renewableShare()`, `annualEmissions()`, `withinBudget()`, `hasBlackout()`, `isAccepted()`. |
| `InvalidPlanException` | Thrown by `simulate` when `validation.PlanValidator` finds problems: unknown site, unknown item, or a technology not allowed at the site. |

## Hour step

1. `L` = Σ loads `scale × profile × 1000` (precomputed); `P_PV` = Σ `size × solar`; `P_W` = Σ wind curve
   (×1.15 on mountain).
2. `N = L − (P_PV + P_W)`.
3. Controller, batteries in **site order** (the first does as much as it can, the rest passes on):
   - `N ≤ 0` → charge with `−N`, curtail the rest, diesel off.
   - `0 < N ≤ D_max` → discharge `N`, diesel off. `D_max = Σ min(pmax, level − soc_min·size)`.
   - `N > D_max` → `P_D = min(max(N − D_max, pmin), pmax)`, then `r = N − P_D`: discharge `r` if positive
     (anything not covered is unserved), or charge `−r` if negative (anything not stored is curtailed).
4. Battery: charging with E from the grid adds `E·eff`; discharging E removes E; at most `pmax` per hour;
   the level is clamped to `[soc_min·size, soc_max·size]`. Each day starts at
   `min(max(0.5, soc_min), soc_max)·size`.
5. Diesel start: on this hour and off the previous one. Every day starts with the plant off.

## Day and year

- Day operating cost = `fuel·E_D + start·S + λ·co2·E_D`, with λ = 0.08 (`DieselPlant.dailyCost`).
- `K = Σ capex/life + Σ_d w_d · dayCost`.
- Accepted = investment ≤ budget **and** no day with unserved energy > `DayResult.BLACKOUT_TOLERANCE`
  (1e-6 kWh, which only absorbs floating-point rounding).
- RES share = `1 − Σ w·E_D / Σ w·L` (informational only).

## Decisions and notes

- Plans over budget can still be simulated (the GUI shows their cost); they are just not accepted.
- An empty plan is valid ("build nothing").
- The brief's RES share formula counts unserved demand as "not diesel". So the empty plan of scenario `b`,
  which has blackouts, shows RES ≈ 0.1 %. This follows the formula as given.
- Comparisons follow the rules exactly, with no tolerance (e.g. `N ≤ D_max`). The goal is to match other
  implementations to the €1 accuracy of the automatic check.
- Speed: 65,536 simulations of scenario `b` (6 days, 8 sites) take ≈ 1.8 s on one thread.

## Regression check

`tools/check-simulation.sh` (jshell, no JUnit) covers:
- hand-computed controller cases for rules 1–4, a full battery, no battery, and two batteries in order
- per-hour invariants on samples `a` and `b` under several plans:
  - energy balance
  - no simultaneous charge and discharge
  - `U` and `C` never both positive
  - diesel within `[pmin, pmax]`
  - battery levels within limits
  - start counting
- the empty-plan annual cost against a closed-form formula
- rejection of invalid plans

Expected output: **24 passed, 0 failed**.
