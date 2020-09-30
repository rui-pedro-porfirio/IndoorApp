from ..snippets import decision_system, websockets, radiomap


def test_ws_communication():
    print('Testing ws communication with dummy data.')
    uuid = '000000'
    position_dict = {'Regression': (1.0, 1.0), 'Classification': 'Personal'}
    websockets.publish('INIT', uuid, position_dict)


def create_and_assert_fuzzy_system():
    fuzzy_dict = decision_system.create_fuzzy_system()
    fuzzy_system = fuzzy_dict['System']
    fuzzy_technique = fuzzy_dict['Technique MF']
    print('Fuzzy System created.')
    decision_system.test_phase(fuzzy_system, fuzzy_technique)
    print('Fuzzy System successfully tested.')
    return fuzzy_dict


def train_existent_radio_maps():
    print('Training radio maps...')
    trained_rm =  radiomap.train_each_radio_map()
    print('Radio maps successfully trained.')
    return trained_rm
