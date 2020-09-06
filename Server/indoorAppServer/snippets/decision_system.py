import numpy as np
import skfuzzy as fuzz
from skfuzzy import control as ctrl

def create_fuzzy_system():
    # New Antecedent/Consequent objects hold universe variables and membership
    # functions
    number_beacons = ctrl.Antecedent(np.arange(0, 6, 1), 'number_beacons')
    matching_aps = ctrl.Antecedent(np.arange(0, 31, 1), 'matching_aps')
    matching_beacons = ctrl.Antecedent(np.arange(0, 4, 1), 'matching_beacons')
    technique = ctrl.Consequent(np.arange(0, 11, 1), 'technique')

    # Membership Functions Definition for Antecedents
    number_beacons['None'] = fuzz.trimf(number_beacons.universe, [0, 0, 1])
    number_beacons['Medium'] = fuzz.trapmf(number_beacons.universe, [0,1,2,3])
    number_beacons['Good'] = fuzz.trimf(number_beacons.universe, [3,3,10])
    matching_aps['Not Enough'] = fuzz.trapmf(matching_aps.universe, [0,0,0,16])
    matching_aps['Enough'] = fuzz.trapmf(matching_aps.universe, [15,16,30,30])
    matching_beacons['Not Enough'] = fuzz.trapmf(matching_beacons.universe, [0,0,0,3])
    matching_beacons['Enough'] = fuzz.trapmf(matching_beacons.universe, [2,3,3,3])

    # Membership Function Definition for Consequent
    technique['Proximity'] = fuzz.gaussmf(technique.universe,1,0.1)
    technique['Trilateration'] = fuzz.gaussmf(technique.universe,3,0.1)
    technique['Fingerprinting'] = fuzz.gaussmf(technique.universe, 5,0.1)

    # MatPlotLib visualization of the membership functions
    number_beacons['Medium'].view()
    matching_aps.view()
    matching_beacons.view()
    technique.view()

    #Rule Base Definition
    rule1 = ctrl.Rule(number_beacons['None'] & matching_aps['Enough'] & matching_beacons['Not Enough'], technique['Fingerprinting'])
    rule2 = ctrl.Rule(number_beacons['Good'] & matching_aps['Enough'] & matching_beacons['Enough'], technique['Fingerprinting'])
    rule3 = ctrl.Rule(number_beacons['Medium'] & matching_aps['Enough'] & matching_beacons['Not Enough'], technique['Fingerprinting'])
    rule4 = ctrl.Rule(number_beacons['Good'] & matching_aps['Not Enough'] & matching_beacons['Enough'], technique['Fingerprinting'])
    rule5 = ctrl.Rule(number_beacons['Good'] & matching_aps['Not Enough'] & matching_beacons['Not Enough'], technique['Trilateration'])
    rule6 = ctrl.Rule(number_beacons['Medium'] & matching_aps['Not Enough'] & matching_beacons['Not Enough'], technique['Proximity'])

    rule1.view()

    #Control System Creation
    position_technique_ctrl = ctrl.ControlSystem([rule1,rule2,rule3,
                                                  rule4,rule5,rule6])
    return {'System':position_technique_ctrl,'Technique MF':technique}

def compute_fuzzy_decision(position_technique_ctrl,technique,number_beacons,matching_aps,matching_beacons):
    #Control System Simulation
    simulation = ctrl.ControlSystemSimulation(position_technique_ctrl)

    simulation.input['number_beacons'] = number_beacons
    simulation.input['matching_aps'] = matching_aps
    simulation.input['matching_beacons'] = matching_beacons

    simulation.compute()

    output_value = simulation.output['technique']
    print(output_value)

    technique.view(sim=simulation)
    result = ''

    if output_value == 1.0:
        result = 'Proximity'
    elif output_value == 3.0:
        result = 'Trilateration'
    elif output_value == 5.0:
        result = 'Fingerprinting'

    return result

def test_phase(control_system,technique):

    #TESTING PHASE
    test_rule1 = compute_fuzzy_decision(control_system,technique,0,16,0)
    assert test_rule1 == 'Fingerprinting'
    test_rule2 = compute_fuzzy_decision(control_system,technique,3,16,3)
    assert test_rule2 == 'Fingerprinting'
    test_rule3 = compute_fuzzy_decision(control_system,technique,1,16,0)
    assert test_rule3 == 'Fingerprinting'
    test_rule4 = compute_fuzzy_decision(control_system,technique,3,13,3)
    assert test_rule4 == 'Fingerprinting'
    test_rule5 = compute_fuzzy_decision(control_system,technique,3,13,2)
    assert test_rule5 == 'Trilateration'
    test_rule6 = compute_fuzzy_decision(control_system,technique,2,13,2)
    assert test_rule6 == 'Proximity'