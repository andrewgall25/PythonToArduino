import requests
import json

def send_python_to_server(py_code):
    url = "http://192.168.1.103:5000/build"
    print(f"Sending POST request to{url}")
    print(f"Python code sent:\n{py_code[:100]}...")
    try:
        resp = requests.post(url, json={"code": py_code}, timeout=10)
        print(f"Server response: status={resp.status_code}, text={resp.text}")
        if resp.status_code == 200:
            try:
                data = resp.json()
                if data.get("success"):
                    print(f"HEX received: {data['hex'][:100]}...")
                    return data["hex"]
                else:
                    print(f"Build error: {data.get('error', 'Unknown error')}")
                    return None
            except json.JSONDecodeError as e:
                print(f"JSON decoding error: {e}")
                return None
        else:
            print(f"Server communication error: {resp.status_code} - {resp.text}")
            return None
    except requests.exceptions.ConnectionError as e:
        print(f"Error connecting to the server: {e}")
        return None
    except Exception as e:
        print(f"Exception during build: {e}")
        return None