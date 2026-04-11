# my_script.py
import sys
import io

_stdin_stream = None
_stdout_stream = None
_stderr_stream = None

class _JavaStreamProxy:
    def __init__(self, java_stream, mode):
        self._java_stream = java_stream
        self._mode = mode
    def write(self, s):
        if 'w' not in self._mode:
            raise io.UnsupportedOperation("not writable")
        self._java_stream.write(s)
    def read(self, size=-1):
        if 'r' not in self._mode:
            raise io.UnsupportedOperation("not readable")
        if size == -1:
            raise io.UnsupportedOperation("read(-1) not supported")
        data = bytearray()
        for _ in range(size):
            b = self._java_stream.read()
            if b == -1:
                break
            data.append(b)
        return bytes(data)
    def readline(self, size=-1):
        if 'r' not in self._mode:
            raise io.UnsupportedOperation("not readable")
        return self._java_stream.readline()
    def flush(self):
        if 'w' not in self._mode:
            raise io.UnsupportedOperation("not writable")
        self._java_stream.flush()
    def close(self):
        pass
    def closed(self):
        return self._java_stream.closed()
    def readable(self):
        return 'r' in self._mode
    def writable(self):
        return 'w' in self._mode
    def seekable(self):
        return False
    def fileno(self):
        raise io.UnsupportedOperation("file not supported")
    def isatty(self):
        return False

def set_streams(stdin_java_stream, stdout_java_stream, stderr_java_stream):
    global _stdin_stream, _stdout_stream, _stderr_stream
    _stdin_stream = _JavaStreamProxy(stdin_java_stream, "r")
    _stdout_stream = _JavaStreamProxy(stdout_java_stream, "w")
    _stderr_stream = _JavaStreamProxy(stderr_java_stream, "w")
    sys.stdin = _stdin_stream
    sys.stdout = _stdout_stream
    sys.stderr = _stderr_stream
    if sys.stdout is not None and hasattr(sys.stdout, 'flush'):
        sys.stdout.flush()
    if sys.stderr is not None and hasattr(sys.stderr, 'flush'):
        sys.stderr.flush()

def capture_output_exec(user_code):
    original_stdout = sys.stdout
    original_stderr = sys.stderr
    captured_output = io.StringIO()
    sys.stdout = captured_output
    sys.stderr = captured_output
    try:
        locals_dict = {}
        exec(user_code, globals(), locals_dict)
        hex_value = locals_dict.get('hex', None)
        print(f"HEX value returned: {hex_value}")
        return hex_value, captured_output.getvalue()
    except SyntaxError as e:
        print(f"Syntax error: {e}\nProblematic code:\n{user_code}", file=sys.stderr)
        return None, captured_output.getvalue()
    except Exception as e:
        print(f"Execution error: {e}\nProblematic code:\n{user_code}", file=sys.stderr)
        return None, captured_output.getvalue()
    finally:
        sys.stdout = original_stdout
        sys.stderr = original_stderr
        try:
            if sys.stdout is not None and hasattr(sys.stdout, 'flush'):
                sys.stdout.flush()
            if sys.stderr is not None and hasattr(sys.stderr, 'flush'):
                sys.stderr.flush()
        except Exception as flush_error:
            print(f"Flush stream error: {flush_error}", file=sys.stderr)

def execute_user_code(user_code):
    hex_value, output = capture_output_exec(user_code)
    print(f"execute_user_code: Returning HEX:{hex_value}, output={output}")
    return [hex_value, output]