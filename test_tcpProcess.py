#!/usr/bin/python

from socket import *
import multiprocessing, time, signal, os, sys, threading, socket

#From https//github.com/zephod/legopi
# from lib.legopi.lib import xbox_read


from multiprocessing import Process
from threading import Thread
from time import sleep

p = multiprocessing.Process(target=time.sleep, args=(1000,))

# Initialize our process
def run_script():
    from received import received

# https://docs.python.org/3/library/multiprocessing.html#multiprocessing-programming
def spawn_script_process(p):
    if (p.is_alive()):
        p.terminate()
    time.sleep(0.1)
    p = Process(target=run_script)
    p.start()
    # Return control to main after .1 seconds
    p.join(0.1)
    return p

# Prints and flushes so we see it right away
def print_flush(msg):
    print(msg)
    sys.stdout.flush()

# UDP code taken from < https://pymotw.com/2/socket/udp.html >
def udpBeacon():
	# Create a UDP socket
	sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

        my_ip = str(socket.gethostbyname(socket.gethostname()))
        spliced_subnet = my_ip[:my_ip.rfind('.')] + ".255"

	# Define broadcasting address and message
	server_address = (spliced_subnet, 5001)
	message = 'Hello, I am a minibot!'

	# Send message and resend every 9 seconds
	while True:
		try:
		    # Send data
                    print('sending broadcast: "%s"' % message)
		    sent = sock.sendto(message, server_address)
		except Exception as err:
                    print(err)
		time.sleep(9)

# Parses cmd and creates a received.py consisting of the semantics of cmd with custom modules prepended
def runScript(cmd,coz):
    global p
    script = ""
    if cmd.find("SCRIPT") > 0:
        begin = cmd.find(",") + 1
        end = cmd.find(">")
        script = cmd[begin:end]
    elif cmd.find("WHEELS") > 0:
        cmd = cmd[cmd.find(",")+1:cmd.find(">")]
        wheels = []
        for i in range(4):
            index = cmd.find(",")
            if index == -1:
                val = str(0)
            else:
                val = cmd[:cmd.find(",")]
                cmd = cmd[cmd.find(",")+1:]
            wheels.append(val)
        script = "bot.set_wheel_power(" + wheels[0] + ","+wheels[1]+","+wheels[2] + ","+ wheels[3] + ")"
    else:
        print("Bad Input, please send SCRIPT or WHEELS command")

    try:
        os.mkdir("received")
    except:
        pass
    try:
        os.remove("received/received.py")
    except:
        pass
    received = open("received/received.py",'w')
    if (coz):
        prepend_module=open("received/cozmo_minibot.py","r")
    else:
        prepend_module=open("received/cup_minibot_prepend.py","r")
    for line in prepend_module:
        received.write(line)
        newline=''
    prepend_module.close()
    if (coz):
        for line in script:
            if line!=('\n') and line!=('\r') and line!=(')'):
                newline+=line
            if line==(")"):
                newline+=', bot)'
                received.write("	"+newline+'\n')
                newline=''
    else:
        received.write(script)
    if (coz):
        received.write('\n'+'cozmo.run_program(cozmo_program)')

    received.close()
    p = spawn_script_process(p)
#connectionSocket.close()

# Defines WHEEL powers depending on button that is pressed
def move_command(b):
    if b == "A":
        runScript("<<<<WHEELS,0,0,0,0>>>>",False)
    if b == "dd":
        runScript("<<<<WHEELS,-100,-100,-100,-100>>>>",False)
    elif b == "dr":
        runScript("<<<<WHEELS,100,-100,100,-100>>>>",False)
    elif b == "dl":
        runScript("<<<<WHEELS,-100,100,-100,100>>>>",False)
    else:
        runScript("<<<<WHEELS,100,100,100,100>>>>",False)

# Reads in xbox button inputs from controller directly attached to RPi
# def xbox():
#    for event in xbox_read.event_stream(deadzone=12000):
#
#        # Convert input event into a string so we can parse it
#        event_triggered = str(event)
#
#        # Extracts the button pressed and value (0 or 1 depending on pressed or unpressed)
#        button = event_triggered[event_triggered.find("(")+1:event_triggered.find(",")]
#        value = event_triggered[event_triggered.find(",")+1:event_triggered.rfind(",")]
#
#        # Button is 1 when it is pressed
#        if value == "1":
#            move_command(button)

def main(p):
    print("Script started")
    # Process Arguments
    cozmo=False
    if (len(sys.argv) > 0):
        # are we a cozmo? TODO: Make cleaner.
        if (str(sys.argv).find("cozmo") != -1):
            print_flush("Becoming a cozmo")
            cozmo=True
    print("setting up tcp")
    serverPort = 10003
    serverSocket = socket.socket(AF_INET, SOCK_STREAM)
    ip = "127.0.0.1"
    print("binding socket")
    serverSocket.bind( (ip, serverPort) )
    serverSocket.listen(1)
    print("waiting for connection")
    connectionSocket, addr = serverSocket.accept()
    print("connection active")
    while True:
        print("Connection accepted")
        command = ""
        while True:
            command += connectionSocket.recv(1024).decode()
            if command.find(">>>>") > 0:
                runScript(command,cozmo)
                command = ""

# Since we are using multiple processes, need to check for main.
if (__name__ == "__main__"):
    #threadxbox = Thread(target = xbox())
    #threadxbox.start()
    beacon = Process(target=udpBeacon)
    beacon.start()
    beacon.join(0.1)
    main(p)
