import numpy as np
import skfuzzy as fuzz
from skfuzzy import control as ctrl

# New Antecedent/Consequent objects hold universe variables and membership
# functions
number_beacons = ctrl.Antecedent(np.arange(0, 6, 1), 'number_beacons')
matching_aps = ctrl.Antecedent(np.arange(0, 31, 1), 'matching_aps')
matching_beacons = ctrl.Antecedent(np.arange(0, 4, 1), 'matching_beacons')
technique = ctrl.Consequent(np.arange(0, 11, 1), 'technique')

# Auto-membership function population is possible with .automf(3, 5, or 7)
number_beacons['None'] = fuzz.trimf(number_beacons.universe, [0, 0, 1])
number_beacons['Medium'] = fuzz.trimf(number_beacons.universe, [1,2,3])
number_beacons['Good'] = fuzz.trimf(number_beacons.universe, [3,3,10])
matching_aps['Not Enough'] = fuzz.trapmf(matching_aps.universe, [0,0,10,15])
matching_aps['Enough'] = fuzz.trapmf(matching_aps.universe, [10,16,30,30])
matching_beacons['Not Enough'] = fuzz.trapmf(matching_beacons.universe, [0,0,0,3])
matching_beacons['Enough'] = fuzz.trapmf(matching_beacons.universe, [2,3,3,3])

# Custom membership functions can be built interactively with a familiar,
# Pythonic API
technique['Fingerprinting'] = fuzz.gaussmf(technique.universe,2.5,1)
technique['Proximity'] = fuzz.gaussmf(technique.universe, 0.5,1)
technique['Trilateration'] = fuzz.gaussmf(technique.universe, 1.5,1)

# You can see how these look with .view()
number_beacons['Medium'].view()
matching_aps.view()
matching_beacons.view()
technique.view()