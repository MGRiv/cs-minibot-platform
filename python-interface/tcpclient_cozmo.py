from socket import *

serverName = 'localhost'
serverPort = 3001
serverSocket= socket(AF_INET, SOCK_STREAM)
serverSocket.connect( (serverName, serverPort) )
script=open('testcommand2.py','rb')
serverSocket.send(script.read())
wow = serverSocket.recv(1024)
serverSocket.close()
