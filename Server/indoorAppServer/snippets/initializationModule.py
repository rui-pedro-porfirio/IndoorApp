from ..snippets import decision_system, websockets


def test_ws_communication():
    uuid = '123456'
    position_dict = {'Regression': (0.0, 0.0), 'Classification': 'Personal'}
    websockets.publish('INIT', uuid, position_dict)


def create_and_assert_fuzzy_system():
    fuzzy_dict = decision_system.create_fuzzy_system()
    fuzzy_system = fuzzy_dict['System']
    fuzzy_technique = fuzzy_dict['Technique MF']
    decision_system.test_phase(fuzzy_system, fuzzy_technique)
    return fuzzy_dict
