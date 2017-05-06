import Note_Library
from pygame_play_tone import Sound as sd

def happySong():
    sd.Note(16.35).play(0)
    bot.wait(1)
    sd.Note(20.6).play(0)
    bot.wait(1)
    sd.Note(24.5).play(0)
    bot.wait(2)
    # sd.Note(Note_Library.NOTE_C0).play(0)
    # bot.wait(1)
    # sd.Note(Note_Library.NOTE_E0).play(0)
    # bot.wait(1)
    # sd.Note(Note_Library.NOTE_G0).play(0)
    # bot.wait(2)

def showHappy(bot):
    happySong()
    bot.move_forward(25)
    bot.wait(3)
    bot.stop()
    # bot.wait(1)
    bot.move_backward(25)
    bot.wait(3)
    bot.stop()
    # bot.wait(1)
    bot.turn_clockwise(25)
    bot.wait(5)
    bot.stop()
    # bot.wait(1)
    bot.turn_counter_clockwise(25)
    bot.wait(5)
    bot.stop()
    # bot.wait(1)

def showSad(bot):
    pass

def showSurprise(bot):
    pass

def showAnger(bot):
    pass

def run(bot):
    bot.move_forward(20)
    bot.wait(3)
    bot.stop()
    # bot.wait(3)
    print("Done FD")
    bot.move_backward(20)
    bot.wait(3)
    bot.stop()
    # bot.wait(3)
    print("Done BK")

    test_emotion = 0

    if test_emotion == 0:
        print("Happy")
        showHappy(bot)
    elif test_emotion == 1:
        print("Sad")
        showSad(bot)
    elif test_emotion == 2:
        print("Surprise")
        showSurprise(bot)
    elif test_emotion == 3:
        print("Anger")
        showAnger(bot)

    bot.stop()
