class IndoorLocation(object):
    def __init__(self, place, floor, x, y):
        self.place = str(place)
        self.floor = int(floor)
        # Local Coordinates (e.g., using a reference grid over a building's floor plan)
        self.x = float(x)
        self.y = float(y)

    def __str__(self):
        return "Building: "+ building + " Floor: "+str(self.floor) + " X: " + str(self.x) + " Y: "+str(self.y)
