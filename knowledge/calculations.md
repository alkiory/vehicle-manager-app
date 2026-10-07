# Calculations

Fuel consumption and cost calculations operate on integer minor units to eliminate floating-point rounding errors.

## Formulas

1. **Fuel Consumption (L/100 km):**
   $$\text{Consumption} = \left(\frac{\text{Total Liters (L)}}{\text{Total Distance (km)}}\right) \times 100$$
   - Uses weighted average over the last N (default 3) full-tank refuel intervals.

2. **Cost per Kilometer (€/km):**
   $$\text{Cost/Km} = \frac{\text{Total Cost (€)}}{\text{Total Distance (km)}}$$
   - Distance calculated as `maxOdometer - initialOdometer`.

3. **Remaining Range Estimation:**
   $$\text{Remaining Range (km)} = \left(\frac{\text{Tank Capacity (L)}}{\text{Average Consumption (L/100km)}}\right) \times 100$$

4. **Next Refuel Odometer:**
   $$\text{Next Refuel Odometer} = \text{Current Odometer} + (\text{Remaining Range} \times 0.9)$$
