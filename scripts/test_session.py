import socket
import json
import time
import threading
import sys

def udp_listener(port, stop_event):
    """
    Listens for UDP notifications from the server.
    """
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Bind to all interfaces on the specified port
        sock.bind(('0.0.0.0', port))
    except Exception as e:
        print(f"\n[UDP Error] Failed to bind to port {port}: {e}")
        return

    sock.settimeout(1.0)
    print(f"\n[UDP Status] Listener started on port {port}")
    
    while not stop_event.is_set():
        try:
            data, addr = sock.recvfrom(4096)
            print(f"\n[UDP Notification from {addr}]:")
            try:
                # Attempt to parse and pretty-print JSON
                json_data = json.loads(data.decode('utf-8'))
                print(json.dumps(json_data, indent=2))
            except:
                # Fallback to raw string if not JSON
                print(data.decode('utf-8'))
        except socket.timeout:
            continue
        except Exception as e:
            print(f"\n[UDP Error]: {e}")
            break
            
    sock.close()
    print("\n[UDP Status] Listener closed.")

def send_request(s, request):
    """
    Sends a JSON request over TCP and waits for a single newline-terminated response.
    """
    # Each message must be terminated by a newline
    message = json.dumps(request) + '\n'
    try:
        s.sendall(message.encode('utf-8'))
        print(f"\n[TCP Sent]: {message.strip()}")
    except Exception as e:
        print(f"\n[TCP Error] Failed to send: {e}")
        return None
    
    # Read the response byte-by-byte until a newline is found
    response_data = b""
    while True:
        try:
            chunk = s.recv(1)
            if not chunk:
                # Connection closed by peer
                print("\n[TCP Status] Connection closed by server.")
                break
            if chunk == b'\n':
                break
            response_data += chunk
        except socket.timeout:
            print("\n[TCP Error] Response timeout.")
            break
        except Exception as e:
            print(f"\n[TCP Error] Read error: {e}")
            break
    
    if not response_data:
        return None
        
    response_str = response_data.decode('utf-8')
    print(f"[TCP Received]: {response_str}")
    
    try:
        return json.loads(response_str)
    except json.JSONDecodeError:
        print("[TCP Error] Failed to decode JSON response")
        return response_str

def main():
    host = 'localhost'
    tcp_port = 8080
    # Arbitrary port for UDP notifications. 
    # Ensure this port is not blocked and doesn't conflict.
    udp_port = 9999 
    
    # Event to stop the UDP listener thread later
    stop_event = threading.Event()
    
    # Start UDP listener in a separate thread
    listener_thread = threading.Thread(target=udp_listener, args=(udp_port, stop_event))
    listener_thread.daemon = True
    listener_thread.start()
    
    # Brief delay to allow UDP listener to bind
    time.sleep(0.5)
    
    try:
        # Create TCP connection
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(5.0) # 5 second timeout for TCP operations
        
        print(f"Connecting to TCP server at {host}:{tcp_port}...")
        s.connect((host, tcp_port))
        print(f"Connected.")
        
        # Use a unique username for each test run using timestamp
        username = f"testuser_{int(time.time())}"
        password = "testpassword123"
        
        # 1. Registration Request
        # Protocol: 'register' operation uses 'name' for the username
        register_req = {
            "operation": "register",
            "name": username,
            "psw": password
        }
        print("\n--- 1. Sending Registration ---")
        send_request(s, register_req)
        
        # 2. Login Request
        # Protocol: 'login' operation uses 'username' and includes 'udpPort'
        login_req = {
            "operation": "login",
            "username": username,
            "psw": password,
            "udpPort": udp_port
        }
        print("\n--- 2. Sending Login ---")
        send_request(s, login_req)
        
        # 3. Get Puzzle Info Request
        # Protocol: GET_PUZZLE_INFO operation name is 'requestGameInfo'
        puzzle_req = {
            "operation": "requestGameInfo"
        }
        print("\n--- 3. Sending Get Puzzle Info ---")
        send_request(s, puzzle_req)
        
        # 4. Wait for notifications
        wait_seconds = 16
        print(f"\n--- 4. Waiting {wait_seconds} seconds for UDP notifications ---")
        print("(If a game ends or a new one starts during this window, you'll see a notification)")
        
        # Wait while checking if the thread is still alive
        for _ in range(wait_seconds):
            if not listener_thread.is_alive():
                print("[!] UDP Listener thread died unexpectedly.")
                break
            time.sleep(1)
        
        # Cleanup
        print("\n--- Cleanup ---")
        s.close()
        print("TCP Connection closed.")
        
    except ConnectionRefusedError:
        print(f"\n[Error] Connection refused. Is the server running on port {tcp_port}?")
    except socket.timeout:
        print("\n[Error] Socket operation timed out.")
    except Exception as e:
        print(f"\n[Error] {e}")
    finally:
        stop_event.set()
        # Give the listener a moment to exit
        time.sleep(1)
        print("Test script finished.")

if __name__ == "__main__":
    main()
