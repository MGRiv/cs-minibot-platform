from socket import *
serverPort = 3001
serverSocket= socket(AF_INET, SOCK_STREAM)
serverSocket.bind( ('', serverPort) )
serverSocket.listen(1)

print('Running')
while True:
	connectionSocket, addr = serverSocket.accept()
	message = connectionSocket.recv(1024).decode()
	received=open("received.py",'w')
	cozmo_module=open("cozmo_minibot.py","r")
	for line in cozmo_module:
		received.write(line)
	newline=''
	for line in message:
		if line!=('\n') and line!=('\r') and line!=("	") and line!=('(') and line!=(')'):
			newline+=line
		if line==("("):
			newline+=line
		if line ==(")"):
			newline+=",robot )"
			received.write("    "+newline+'\n'+ '    time.sleep(2)'+'\n')
			newline=''

	received.write('\n'+'cozmo.run_program(cozmo_program)')
	received.close()
	import received
	connectionSocket.close()
