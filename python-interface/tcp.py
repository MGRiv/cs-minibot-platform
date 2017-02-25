from socket import *
import os
serverPort = 10000
serverSocket= socket(AF_INET, SOCK_STREAM)
ip = "127.0.0.1"
serverSocket.bind( (ip, serverPort) )
serverSocket.listen(1)
connectionSocket, addr = serverSocket.accept()
command = ""
while True:
    command += connectionSocket.recv(1024).decode()
    if command.find(">>>>") > 0:
        break

script = ""
if command.find("SCRIPT") > 0:
    begin = command.find(",") + 1
    end = command.find(">")
    script = command[begin:end]

elif command.find("WHEELS") > 0:
    command = command[command.find(",")+1:command.find(">")]
    wheels = []
    for i in range(4):
        index = command.find(",")
        if index == -1:
            val = str(0)
        else:
            val = command[:command.find(",")]
            command = command[command.find(",")+1:]
        wheels.append(val)

    script = "set_wheel_power(" + wheels[0] + ","+wheels[1]+","+wheels[2] + ","+ wheels[3] + ")"
else:
    print("Bad Input, please send SCRIPT or WHEELS command")

received = open("received.py",'w')
received.write(script)
received.close()
import received
connectionSocket.close()

execfile('./received.py')
