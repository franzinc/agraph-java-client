package miniclient;

public class RowReader {

}
/*****
class RowReader:
    def __init__(self, callback):
        self.hasNames = None
        self.names = None
        self.skipNextBracket = False
        self.callback = callback
        self.backlog = None

    def process(self, string):
        if self.hasNames is None: self.hasNames = string[0] == "{"
        ln = len(string)
        if self.backlog: string = self.backlog + string
        pos = [0]

        def useArray(arr):
            if self.hasNames:
                if self.names:
                    self.callback(arr, self.names)
                else:
                    self.names = arr
                    self.skipNextBracket = True
            else:
                self.callback(arr, None)

        def takeArrayAt(start):
            scanned = start + 1
            while True:
                end = string.find("]", scanned)
                if end == -1: return False
                try:
                    useArray(cjson.decode(string[start : end + 1].decode("utf-8")))
                    pos[0] = end + 1
                    return True
                except cjson.DecodeError:
                    scanned = end + 1

        while True:
            start = string.find("[", pos[0])
            if self.skipNextBracket:
                self.skipNextBracket = False
                pos[0] = start + 1
            elif start == -1 or not takeArrayAt(start):
                break

        if pos[0] == 0:
            self.backlog = string
            return ln
        else:
            self.backlog = None
            return pos[0]
****/