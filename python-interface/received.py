import cozmo
import time
from cozmo.util import distance_mm, speed_mmps, degrees

#TODO: Refactor this to subclass the baseminibot abstract class
action = None

def move_forward(power, robot: cozmo.robot.Robot):
    global action
    if action is not None:
        action.abort()
        robot.stop_all_motors()
        action=None
    action = robot.drive_straight(distance=distance_mm(10000), speed=speed_mmps(power))

def move_backward(power, robot: cozmo.robot.Robot):
    global action
    if action is not None:
        action.abort()
    action =   robot.drive_straight(distance=distance_mm(-10000), speed=speed_mmps(power))

def turn_left(robot: cozmo.robot.Robot):
    global action
    if action is not None:
        action.abort()
    action =   robot.turn_in_place(angle=-10000)

def turn_right(robot: cozmo.robot.Robot):
    global action
    if action is not None:
        action.abort()
    action =  robot.turn_in_place(angle=10000)

def wait(t, robot: cozmo.robot.Robot):
    time.sleep(t)

def cozmo_program(robot: cozmo.robot.Robot):
    move_forward(100,robot )
    time.sleep(2)
    move_backward(100,robot )
    time.sleep(2)
    wait(1,robot )
    time.sleep(2)

cozmo.run_program(cozmo_program)