# my_script.py
import sys
import io
import time  # Fondamentale per far funzionare lo sleep

_stdin_stream = None
_stdout_stream = None
_stderr_stream = None
_check_connection_func = None
_is_running = True

ON = True
OFF = False

def stop_execution():
    global _is_running
    _is_running = False

def is_running():
    global _is_running
    return _is_running

def set_connection_checker(func):
    global _check_connection_func
    _check_connection_func = func

# Funzione che l'utente userà nell'app
def turn(pin, state):
    # CONTROLLO CONNESSIONE
    if _check_connection_func is not None:
        if not _check_connection_func():
            # Stampiamo un errore su stderr (così appare in rosso o evidenziato nell'app)
            sys.stderr.write("ERROR: Arduino disconnected! Cannot send the command.\n")
            return # Esci senza inviare il comando

    val = 1 if state == "ON" or state is True else 0
    cmd = f"SET:{pin}:{val}\n"
    sys.stdout.write(cmd)
    sys.stdout.flush()

# Aggiorna exec_globals includendo eventualmente il checker se necessario
exec_globals = {
    "time": time,
    "delay": lambda ms: time.sleep(ms / 1000.0),
    "turn": turn,
    "ON": ON,
    "OFF": OFF,
    "print": print,
    "is_running": is_running,
    "stop": stop_execution
}


# ... resto del file (set_streams, execute_live_code) ...
class _JavaStreamProxy:
    def __init__(self, java_stream, mode):
        self._java_stream = java_stream
        self._mode = mode

    def write(self, s):
        if 'w' not in self._mode:
            raise io.UnsupportedOperation("not writable")
        self._java_stream.write(s)
        # TRUCCO PER IL REAL-TIME: Forza il flush ad ogni scrittura
        self.flush()

    def flush(self):
        if 'w' not in self._mode:
            raise io.UnsupportedOperation("not writable")
        self._java_stream.flush()

    # ... gli altri metodi (read, readline, ecc.) restano uguali ...
    def read(self, size=-1):
        if 'r' not in self._mode: raise io.UnsupportedOperation("not readable")
        if size == -1: raise io.UnsupportedOperation("read(-1) not supported")
        data = bytearray()
        for _ in range(size):
            b = self._java_stream.read()
            if b == -1: break
            data.append(b)
        return bytes(data)

    def readline(self, size=-1):
        if 'r' not in self._mode: raise io.UnsupportedOperation("not readable")
        return self._java_stream.readline()

    def close(self): pass
    def closed(self): return self._java_stream.closed()
    def readable(self): return 'r' in self._mode
    def writable(self): return 'w' in self._mode
    def seekable(self): return False
    def fileno(self): raise io.UnsupportedOperation("file not supported")
    def isatty(self): return False

def set_streams(stdin_java_stream, stdout_java_stream, stderr_java_stream):
    global _stdin_stream, _stdout_stream, _stderr_stream
    _stdin_stream = _JavaStreamProxy(stdin_java_stream, "r")
    _stdout_stream = _JavaStreamProxy(stdout_java_stream, "w")
    _stderr_stream = _JavaStreamProxy(stderr_java_stream, "w")
    sys.stdin = _stdin_stream
    sys.stdout = _stdout_stream
    sys.stderr = _stderr_stream

def execute_live_code(user_code):
    global _is_running
    _is_running = True  # Riabilita l'esecuzione
    try:
        # Passiamo exec_globals due volte: come globals e come locals
        # Questo risolve molti problemi di visibilità delle variabili nei loop
        exec(user_code, exec_globals, exec_globals)
        return None
    except Exception as e:
        sys.stderr.write(f"Python Error: {str(e)}\n")
        return str(e)

# Mantieni capture_output_exec solo se ti serve per altre funzioni specifiche
# ma per l'app principale usa execute_live_code