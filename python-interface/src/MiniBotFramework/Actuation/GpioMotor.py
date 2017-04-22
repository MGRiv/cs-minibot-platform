from MiniBotFramework.Actuation.Actuator import Actuator

class GpioMotor(Actuator):

    PWM_FREQUENCY=60

    def __init__(self, bot, name, pin_in, pin_out, GPIO, pwm):
        Actuator.__init__(self, bot, name)
        self.pin_in = pin_in
        self.pin_out = pin_out
        GPIO.setup(self.pin_in, GPIO.IN)
        GPIO.setup(self.pin_out, GPIO.OUT)
        self.in_pwm = GPIO.PWM(self.pin_in, PWM_FREQUENCY)
        self.out_pwm = GPIO.PWM(self.pin_out, PWM_FREQUENCY)
        # Stop the bot
        self.in_pwm.ChangeDutyCycle(0)
        self.in_pwm.ChangeDutyCycle(100)
        self.value_in = 0
        self.value_out = 0
        self.state_stop = False
        self.state_forward = True

    def read(self):
        return self.value

    def rotate_forward(self,power):
        self.set(False,True)
        self.in_pwm.ChangeDutyCycle(power)
        self.out_pwm.ChangeDutyCycle(100 - power)

    def rotate_backward(self,power):
        self.set(False,False)
        self.in_pwm.ChangeDutyCycle(100 - power)
        self.out_pwm.ChangeDutyCycle(100 - power)

    def stop(self):
        self.set(True,True)

    def set(self, state_stop, state_forward):
        self.state_stop = state_stop
        self.state_forward = state_forward
