import json, time
import MiniBotFramework
import MiniBotScripts.base

# Constants
CONFIG_LOCATION = "MiniBotConfig/config.json"
bot = None

def main():
    print("Initializing MiniBot Software")
    # Load config
    config_file = open(CONFIG_LOCATION)
    config = json.loads(config_file.read())
    bot = MiniBotScripts.base.MiniBot.MiniBot(config)

    # Initialize TCP
    tcpInstance = None
    if config["acceptTcp"]:
        tcpInstance = MiniBotFramework.Communication.TCP.TCP()

    # Initialize UDP broadcast
    if config["discoverable"]:
        # TODO
        pass

    # If startup script specified, run it
    if config["startupScript"] != "":
        # TODO
        pass

    print("Entering main loop")
    # Main loop
    while True:
        # Poll TCP Connection
        tcpCmd = tcpInstance.get_command()
        if tcpCmd != "":
            parse_command(tcpCmd)
        # Check on the main code
        time.sleep(0.01)

def parse_command(cmd):
    comma = cmd.find(",")
    end = cmd.find(">>>>")
    key = cmd[4:comma]
    value = cmd[comma+1:end]
    if key == "GPIO":
        # A;B;C;D
        values = value.split(";")
        print(values)
    else:
        print("Unknown key: " + key)

if (__name__ == "__main__"):
    main()
