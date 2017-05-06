from MiniBotFramework.Sensing.Sensor import Sensor
from MiniBotFramework.Lib.TCS34725 import TCS34725 as CSensor
import smbus, math, time

def distance(p1, p2):
    """ Returns distance between two 3-tuples. 
    Used for evaluating color """
    return math.sqrt((p1[0]-p2[0])**2 + (p1[1]-p2[1])**2 + (p1[2]-p2[2])**2)

def normalize(vector):
    """ Returns a 3-element vector as a unit vector. """
    sum = vector[0] + vector[1] + vector[2]
    return (vector[0]/(sum+0.0), vector[1]/(sum+0.0), vector[2]/(sum+0.0))

class ColorSensor(Sensor):
    """ Pre-determined set of colors and corresponding RGB 3-tuples
    that are the basis of color inputs. Sensor inputs will be compared
    with values in this dict to see what "color" it is closest to. """

    def __init__(self, bot, name, pin_number):
        Sensor.__init__(self, bot, name)
        self.pin_number = pin_number
        self.color_sensor = CSensor()
        self.bus = smbus.SMBus(1)
        self.bus.write_byte(0x29,0x80|0x12)
        ver = self.bus.read_byte(0x29)
        self.bus.write_byte(0x29, 0x80|0x00) # 0x00 = ENABLE register
        self.bus.write_byte(0x29, 0x01|0x02) # 0x01 = Power on, 0x02 RGB sensors enabled
        self.bus.write_byte(0x29, 0x80|0x14) # Reading results start register 14, LSB then MSB

        self.colors = {
            # TRIAL 1
            # "RED":(7885,2631,3034),
            # "GREEN":(4794,10432,8395),
            # "BLUE":(14162,7582,4268),
            # "ORANGE":(14162,7582,4268),
            # "VIOLET":(8263,7538,9303),
            # "YELLOW":(13772,12879,5783),
            # "PINK":(11483,7839,8267)

            # TRIAL 2 - rugged side of color mat, stationary bot, 100 avg
            # "RED":(151,49,58),
            # "GREEN":(160,357,286),
            # "BLUE":(76,158,198),
            # "ORANGE":(250,134,76),
            # "VIOLET":(137,128,156),
            # "YELLOW":(245,229,103),
            # "PINK":(236,165,172)

            # TRIAL 3 - flat side of color mat, moving bot, 100 avg
            "RED":(155,68,70),
            "GREEN":(81,170,139),
            "BLUE":(73,139,167),
            #"ORANGE":(231,138,83),
            "VIOLET":(140,138,158) #,
            #"YELLOW":(241,234,113),
            #"PINK":(187,150,150)
        }

        self.colors_normalized = {}
        
        # for color in self.colors:
        #     self.colors_normalized[color] = normalize(self.colors[color])

    def calibrate(self):
        print "================== COLOR CALIBRATION =================="
        print """Before color-sensing, we must calibrate the colors. Please place
the corresponding color under the color sensor at recommended distance
(with wheels touching the ground before pressing enter."""

        for color in self.colors:
            if len(raw_input("\nPlease place the " +color.lower() + " mat under the sensor and press enter."))>-1:
                self.colors_normalized[color] = normalize(self.super_read(100))
                time.sleep(0.01)
                print "Calibrated!"

        print "Thank you! All of the colors have been calibrated."

    def super_read(self,n):
        color_data = {"R":0,"G":0,"B":0}
        for i in range(n):
            read = self.read()
            color_data["R"] += read[0]
            color_data["G"] += read[1]
            color_data["B"] += read[2]
        color_data["R"] = color_data["R"]/20.0
        color_data["G"] = color_data["G"]/20.0
        color_data["B"] = color_data["B"]/20.0
        return (color_data["R"],color_data["G"],color_data["B"])

    def read(self):
        """ Returns a NON-NORMALIZED 3-tuple of RGB value """
        data = self.bus.read_i2c_block_data(0x29, 0)
        # clear = clear = data[1] << 8 | data[0]
        red = data[3] << 8 | data[2]
        green = data[5] << 8 | data[4]
        blue = data[7] << 8 | data[6]

        rgb = (red, green, blue)
        return rgb

    def read_color(self):
        """ Returns string of color """
        color_guess = ("", 99999999999999999999999999) #tuple of (color, distance from color to input)
        color_actual = self.read()
        for c in self.colors_normalized:
            dist = distance(self.colors_normalized[c],color_actual)
            print "    " + c+ " dist: " + str(dist)
            if(dist < color_guess[1]):
                color_guess = (c, dist)
                print "    new guess is " + color_guess[0]
        return color_guess[0]

