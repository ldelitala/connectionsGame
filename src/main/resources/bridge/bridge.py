import asyncio
import json
import socket
import websockets
import http.server
import socketserver
import threading
import os

# --- Configuration ---
JAVA_TCP_PORT = 8080
WEB_PORT = 8000
WS_PORT = 8001

# --- HTTP Server for Frontend ---
def start_http_server():
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    Handler = http.server.SimpleHTTPRequestHandler
    with socketserver.TCPServer(("", WEB_PORT), Handler) as httpd:
        print(f"Web UI served at http://localhost:{WEB_PORT}")
        httpd.serve_forever()

# --- WebSocket to TCP Bridge ---
async def handle_client(websocket):
    print(f"New Web client connected.")
    
    # Connect to Java TCP Server
    try:
        reader, writer = await asyncio.open_connection('localhost', JAVA_TCP_PORT)
    except ConnectionRefusedError:
        await websocket.send(json.dumps({"status": "error", "message": "Cannot connect to Game Server."}))
        return

    # Dedicated UDP listener for this client
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    udp_socket.bind(('localhost', 0))
    udp_port = udp_socket.getsockname()[1]
    udp_socket.setblocking(False)

    async def listen_udp():
        loop = asyncio.get_running_loop()
        while True:
            try:
                data, addr = await loop.sock_recvfrom(udp_socket, 4096)
                # Forward UDP notification to Web client
                msg = data.decode('utf-8')
                await websocket.send(msg)
            except Exception as e:
                print(f"UDP Error: {e}")
                break

    udp_task = asyncio.create_task(listen_udp())

    async def listen_tcp():
        while True:
            try:
                data = await reader.readuntil(b'\n')
                if not data:
                    break
                msg = data.decode('utf-8').strip()
                print(f"JAVA -> WEB: {msg}")
                await websocket.send(msg)
            except asyncio.IncompleteReadError:
                break
            except Exception as e:
                print(f"TCP Read Error: {e}")
                break

    tcp_task = asyncio.create_task(listen_tcp())

    # Intercept login/register to append UDP port, then forward to TCP
    try:
        async for message in websocket:
            print(f"WEB -> JAVA: {message}")
            try:
                req = json.loads(message)
                # Auto-inject UDP port for session tracking
                if req.get("operation") in ["login", "register", "updateCredentials"]:
                    req["udpPort"] = udp_port
                
                payload = json.dumps(req) + "\n"
                writer.write(payload.encode('utf-8'))
                await writer.drain()
            except json.JSONDecodeError:
                pass
    except websockets.exceptions.ConnectionClosed:
        print("Web client disconnected.")
    finally:
        udp_task.cancel()
        tcp_task.cancel()
        writer.close()
        await writer.wait_closed()
        udp_socket.close()

async def main():
    # Start HTTP server in a separate thread
    threading.Thread(target=start_http_server, daemon=True).start()
    
    # Start WebSocket server
    print(f"WebSocket Bridge running on ws://localhost:{WS_PORT}")
    async with websockets.serve(handle_client, "localhost", WS_PORT):
        await asyncio.Future()  # run forever

if __name__ == "__main__":
    asyncio.run(main())
