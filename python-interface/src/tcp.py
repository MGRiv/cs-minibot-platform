from socket import *
import multiprocessing, time, signal, os, sys
from multiprocessing import Process

# Initialize our process
def run_script():
    from received import received

def spawn_script_process(p):
    if (p.is_alive()):
        p.terminate()
    time.sleep(0.1)
    p = Process(target=run_script)
    p.start()
    p.join(0.1)
    return p

def main(p):
    serverPort = 10000
    serverSocket= socket(AF_INET, SOCK_STREAM)
    ip = "127.0.0.1"
    serverSocket.bind( (ip, serverPort) )
    serverSocket.listen(1)
    connectionSocket, addr = serverSocket.accept()
    while True:
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

        try:
            os.mkdir("received")
        except:
            pass
        try:
            os.remove("received/received.py")
        except:
            pass
        received = open("received/received.py",'w')
        received.write(script)
        received.close()
        p = spawn_script_process(p)
        #connectionSocket.close()
# Name the process spawned.
print(__name__)
sys.stdout.flush()
if (__name__ == "__main__"):
    p = multiprocessing.Process(target=time.sleep, args=(1000,))
    main(p)
