import json
import os


class JsonLoader(object):
    def __init__(self, path):
        self.path = path
        self.json_data = {}
        self.load()

    def load(self):
        if os.path.isdir(self.path):
            for current_directory, child_directories, files in os.walk(self.path):
                for name in files:
                    self.load_file(os.path.join(current_directory, name))
        else:
            self.load_file(self.path)

    def load_file(self, path):
        try:
            with open(path) as json_file:
                self.json_data[path] = json.load(json_file)
        except IOError:
            print("There is no such file")
