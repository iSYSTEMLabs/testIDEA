
from __future__ import print_function

import sys
import os
import re
import time
import threading
import subprocess
from io import IOBase

#import Tkinter
#import tkMessageBox

try:
    import tkinter as uniTkinter
    import tkinter.messagebox as tkMsg
except ImportError as err:
    import Tkinter as uniTkinter
    import tkMessageBox as tkMsg


class TestStatus:
    """
    Data object for reporting status by test functions. Objects of this class 
    are passed from test functions to registered listener, for example GUI.
    GUI then uses this information to show test results and Details dialog.
    """
    
    ST_OK = 1
    ST_ERROR = 2
    ST_FINISHED = 3
    ST_TERMINATED = 4
    ST_CYCLE_FINIFSHED = 5
    
    def __init__(self, interface, configName, status, subtestId, files, percentageExecuted, testCycleNumber = 0):
        self.interface = interface  # name of IConnect interface mainly used 
                                    # during tests, for example 'debug', or 'ide'
        self.configName = configName # configuration name, for example 'HC12'
        self.status = status        # enumeration with values ST_..., see above
        self.subtestId = subtestId  # string, which identifies subtest, shown to
                                    # the user 
        self.files = files          # list of lists with 2 elements - result  
                                    # and expected result file name, for 
                                    # example: [[result.log, expected.log]]
        self.percentageExecuted = percentageExecuted # used for setting 
                                    #  progressbar. Should contain fraction of
                                    # tests executed by current test, for example 0.33
                                    # for 1.3 of complete test
        self.testCycleNumber = testCycleNumber    # used for counting number of test cycles. In
                                    # one cycle all selected tests are executed


#class RunExternal(threading.Thread):
#    """
#    This class runs an external program in a new thread, so that GUI thread
#    does not stop.
#    """
#    def __init__(self, commandLine):
#        threading.Thread.__init__(self)
#        
#        self.commandLine = commandLine
#        
#                
#    def run(self):
#        try:
#            retCode = subprocess.Popen(self.commandLine)
#        except OSError as ex:
#            tkMsg.showerror("Error running external program", 
#                                   self.commandLine + "\n\n" + str(ex))     

    
class Callable:
    """
    This class is used to pass parameters to actions performed by uniTkinter buttons.
    """    
    def __init__(self, func, *args):
        self.func = func             
        self.args = args             
    
     
    def __call__(self):
        apply(self.func, self.args)    


def showDiff(diffProgram, file1, file2):
    # @param diffProgram: path to diff exe, for example: r"e:\apps\KDiff3\kdiff3.exe"
    try:
        commandLine = diffProgram + " " + file1 + " " + file2
        subprocess.Popen(commandLine)
    except OSError as ex:
        tkMsg.showerror("Error running external program", 
                               diffProgram + "\n\n" + str(ex))     


"""
A Python replacement for java.util.Properties class
This is modelled as closely as possible to the Java original.

Created - Anand B Pillai <abpillai@gmail.com>

MK, Jan, 2008:
Source URL: http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/496795
License: Unknown     
"""

class IllegalArgumentException(Exception):

    def __init__(self, lineno, msg):
        self.lineno = lineno
        self.msg = msg

    def __str__(self):
        s='Exception at line number %d => %s' % (self.lineno, self.msg)
        return s


class Properties(object):
    """ A Python replacement for java.util.Properties """
    
    def __init__(self, props=None):

        # Note: We don't take a default properties object
        # as argument yet

        # Dictionary of properties.
        self._props = {}
        # Dictionary of properties with 'pristine' keys
        # This is used for dumping the properties to a file
        # using the 'store' method
        self._origprops = {}

        # Dictionary mapping keys from property
        # dictionary to pristine dictionary
        self._keymap = {}
        
        self.othercharre = re.compile(r'(?<!\\)(\s*\=)|(?<!\\)(\s*\:)')
        self.othercharre2 = re.compile(r'(\s*\=)|(\s*\:)')
        self.bspacere = re.compile(r'\\(?!\s$)')
        
    def __str__(self):
        s='{'
        for key,value in self._props.items():
            s = ''.join((s,key,'=',value,', '))

        s=''.join((s[:-2],'}'))
        return s

    def __parse(self, lines):
        """ Parse a list of lines and create
        an internal property dictionary """

        # Every line in the file must consist of either a comment
        # or a key-value pair. A key-value pair is a line consisting
        # of a key which is a combination of non-white space characters
        # The separator character between key-value pairs is a '=',
        # ':' or a whitespace character not including the newline.
        # If the '=' or ':' characters are found, in the line, even
        # keys containing whitespace chars are allowed.

        # A line with only a key according to the rules above is also
        # fine. In such case, the value is considered as the empty string.
        # In order to include characters '=' or ':' in a key or value,
        # they have to be properly escaped using the backslash character.

        # Some examples of valid key-value pairs:
        #
        # key     value
        # key=value
        # key:value
        # key     value1,value2,value3
        # key     value1,value2,value3 \
        #         value4, value5
        # key
        # This key= this value
        # key = value1 value2 value3
        
        # Any line that starts with a '#' is considerered a comment
        # and skipped. Also any trailing or preceding whitespaces
        # are removed from the key/value.
        
        # This is a line parser. It parses the
        # contents like by line.

        lineno=0
        i = iter(lines)

        for line in i:
            lineno += 1
            line = line.strip()
            # Skip null lines
            if not line: continue
            # Skip lines which are comments
            if line[0] == '#': continue
            # Some flags
            escaped=False
            # Position of first separation char
            sepidx = -1
            # A flag for performing wspace re check
            flag = 0
            # Check for valid space separation
            # First obtain the max index to which we
            # can search.
            m = self.othercharre.search(line)
            if m:
                first, last = m.span()
                start, end = 0, first
                flag = 1
                wspacere = re.compile(r'(?<![\\\=\:])(\s)')        
            else:
                if self.othercharre2.search(line):
                    # Check if either '=' or ':' is present
                    # in the line. If they are then it means
                    # they are preceded by a backslash.
                    
                    # This means, we need to modify the
                    # wspacere a bit, not to look for
                    # : or = characters.
                    wspacere = re.compile(r'(?<![\\])(\s)')        
                start, end = 0, len(line)
                
            m2 = wspacere.search(line, start, end)
            if m2:
                # print ('Space match=>',line)
                # Means we need to split by space.
                first, last = m2.span()
                sepidx = first
            elif m:
                # print ('Other match=>',line)
                # No matching wspace char found, need
                # to split by either '=' or ':'
                first, last = m.span()
                sepidx = last - 1
                # print (line[sepidx])
                
                
            # If the last character is a backslash
            # it has to be preceded by a space in which
            # case the next line is read as part of the
            # same property
            while line[-1] == '\\':
                # Read next line
                nextline = i.next()
                nextline = nextline.strip()
                lineno += 1
                # This line will become part of the value
                line = line[:-1] + nextline

            # Now split to key,value according to separation char
            if sepidx != -1:
                key, value = line[:sepidx], line[sepidx+1:]
            else:
                key,value = line,''

            self.processPair(key, value)
            
    def processPair(self, key, value):
        """ Process a (key, value) pair """

        oldkey = key
        oldvalue = value
        
        # Create key intelligently
        keyparts = self.bspacere.split(key)
        # print (keyparts)

        strippable = False
        lastpart = keyparts[-1]

        if lastpart.find('\\ ') != -1:
            keyparts[-1] = lastpart.replace('\\','')

        # If no backspace is found at the end, but empty
        # space is found, strip it
        elif lastpart and lastpart[-1] == ' ':
            strippable = True

        key = ''.join(keyparts)
        if strippable:
            key = key.strip()
            oldkey = oldkey.strip()
        
        oldvalue = self.unescape(oldvalue)
        value = self.unescape(value)
        
        self._props[key] = value.strip()

        # Check if an entry exists in pristine keys
        if key in self._keymap:
            oldkey = self._keymap.get(key)
            self._origprops[oldkey] = oldvalue.strip()
        else:
            self._origprops[oldkey] = oldvalue.strip()
            # Store entry in keymap
            self._keymap[key] = oldkey
        
    def escape(self, value):

        # Java escapes the '=' and ':' in the value
        # string with backslashes in the store method.
        # So let us do the same.
        newvalue = value.replace(':','\:')
        newvalue = newvalue.replace('=','\=')

        return newvalue

    def unescape(self, value):

        # Reverse of escape
        newvalue = value.replace('\:',':')
        newvalue = newvalue.replace('\=','=')

        return newvalue    
        
    def load(self, stream):
        """ Load properties from an open file stream """
        
        # For the time being only accept file input streams
        
        if not isinstance(stream, IOBase):
            raise TypeError('Argument should be a file object!')
        # Check for the opened mode
        if stream.mode != 'r':
            raise ValueError('Stream should be opened in read-only mode!')

        try:
            lines = stream.readlines()
            self.__parse(lines)
        except IOError as e:
            raise


    def getProperty(self, key, default = None):
        """ Return a property for the given key """
        return self._props.get(key, default)


    def setProperty(self, key, value):
        """ Set the property for the given key """

        if type(key) is str and type(value) is str:
            self.processPair(key, value)
        else:
            raise TypeError('both key and value should be strings!')

    def propertyNames(self):
        """ Return an iterator over all the keys of the property
        dictionary, i.e the names of the properties """

        return self._props.keys()

    def list(self, out=sys.stdout):
        """ Prints a listing of the properties to the
        stream 'out' which defaults to the standard output """

        out.write('-- listing properties --\n')
        for key,value in self._props.items():
            out.write(''.join((key,'=',value,'\n')))

    def store(self, out, header=""):
        """ Write the properties list to the stream 'out' along
        with the optional 'header' """

        if out.mode[0] != 'w':
            raise ValueError('Steam should be opened in write mode!')

        try:
            out.write(''.join(('#',header,'\n')))
            # Write timestamp
            tstamp = time.strftime('%a %b %d %H:%M:%S %Z %Y', time.localtime())
            out.write(''.join(('#',tstamp,'\n')))
            # Write properties from the pristine dictionary
            for prop, val in self._origprops.items():
                out.write(''.join((prop,'=',self.escape(val),'\n')))
                
            out.close()
        except IOError as e:
            raise

    def getPropertyDict(self):
        return self._props

    def __getitem__(self, name):
        """ To support direct dictionary like access """

        return self.getProperty(name)

    def __setitem__(self, name, value):
        """ To support direct dictionary like access """

        self.setProperty(name, value)
        
    def __getattr__(self, name):
        """ For attributes not found in self, redirect
        to the properties dictionary """

        try:
            return self.__dict__[name]
        except KeyError:
            if hasattr(self._props,name):
                return getattr(self._props, name)


# GUIUtils
    
def createSeparator(master, orientation, col_, row_, span):
    """
    Creates horizontal or vertical separator bar.
    
    @param master parent of the separator (frame, where it should appear) 
    @param orientation 'H' for horizontal separator, 'V' for vertical separator
    @param col_ colum number in grid layout, where the separator should be located
    @param row_ colum number in grid layout, where the separator should be located
    @param span rowspan value for vertical and columnspan value for horizontal
                separator  
    """
    frame = uniTkinter.Frame(master, borderwidth = 3, relief = uniTkinter.RAISED)
    if orientation == 'H':
        frame.grid(column = col_, 
                   row = row_, 
                   columnspan = span, 
                   sticky = uniTkinter.E + uniTkinter.W, 
                   ipady = 1,
                   padx = 3,
                   pady = 5)
        
    elif orientation == 'V':
        frame.grid(column = col_, 
                   row = row_, 
                   rowspan = span, 
                   sticky = uniTkinter.N + uniTkinter.S, 
                   ipadx = 1,
                   padx = 5,
                   pady = 3)
    else:
        raise Exception("Invalid orientation for separator, should be 'H' or "\
                        "'V', but is: ", orientation)


class DialogBase(uniTkinter.Toplevel):
    """
    This class should be inherited by class, which implement dialog windows.
    It implements functionality common to most dialogs. 
    """

    def __init__(self, master, title = None):

        uniTkinter.Toplevel.__init__(self, master)
        
        if title:
            self.title(title)

        self.master = master
        self.isOK = False  # set this one to True if the user pressed OK button

        # associate this dialog with a master window
        self.transient(master)

        mainFrame = uniTkinter.Frame(self)
        self.focusedWidget = self.createMainPanel(mainFrame)

        if not self.focusedWidget:
            self.focusedWidget = self

        mainFrame.grid(column = 0, row = 0, padx = 5 , pady = 5)

        createSeparator(self, 'H', 0, 1, 1)

        self.createButtonPanel()

        self.grab_set()  # make the dialog modal

        self.protocol("WM_DELETE_WINDOW", self.cancelPressed)

        self.geometry("+%d+%d" % (master.winfo_rootx() + 35,
                                  master.winfo_rooty() + 35))

        self.focusedWidget.focus_set()

        self.wait_window(self)


    def createButtonPanel(self):
        # adds OK and Cancel buttons
        # override, if other set of buttons is required
        
        frame = uniTkinter.Frame(self)

        btn = uniTkinter.Button(frame, text = "OK", width = 10, 
                             command = self.okPressed, default = uniTkinter.ACTIVE)
        
        btn.pack(side = uniTkinter.LEFT, padx = 5, pady = 5)
        
        btn = uniTkinter.Button(frame, text = "Cancel", width = 10, 
                             command = self.cancelPressed)
        
        btn.pack(side = uniTkinter.LEFT, padx = 5, pady = 5)

        self.bind("<Return>", self.okPressed)
        self.bind("<Escape>", self.cancelPressed)

        frame.grid(row = 2)


    def okPressed(self, event = None):

        if not self.validate():
            self.focusedWidget.focus_set() # keep focus on a dialog
            return

        self.withdraw()
        self.update_idletasks()

        self.isOK = True
        self.processData()

        self.cancelPressed()


    def isOKPressed(self):
        # This method should be called by application, to see, if the data is
        # valid and the user pressed OK button
        return self.isOK

    
    def cancelPressed(self, event = None):
        # put focus back to the master window
        self.master.focus_set()
        self.destroy()

    
    # methods to be overridden follow
     
    def createMainPanel(self, master):
        # Creates dialog body. If there is a widget, which should have initial 
        # focus, return it. Otherwise omit return statement. 
        # override
        pass


    def validate(self):
        # Returns True, if data entered by the user is valid, False otherwise.
        # override
        return True 


    def processData(self):
        # Called only when data is valid and user pressed OK button. Usually 
        # store data to derived class's attributes, so that application can
        # use them
        # override
        pass 
    