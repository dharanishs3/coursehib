#!/usr/bin/env python3
"""
Tiny local server for the CourseHub test page.
  - serves index.html at http://localhost:5173
  - forwards every /api/... request to the Spring Boot app (default http://localhost:8080)
Because the page and the API then share one address, the browser needs no CORS setup.

Run:   python3 test-ui/server.py
Stop:  Ctrl+C
"""
import http.client
import os
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

BACKEND = os.environ.get("BACKEND", "http://localhost:8080")
PORT = int(os.environ.get("PORT", "5173"))
HERE = os.path.dirname(os.path.abspath(__file__))
HOP_BY_HOP = {"connection", "keep-alive", "transfer-encoding", "te", "trailer", "upgrade", "proxy-authenticate",
              "proxy-authorization", "host", "content-length"}


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        if self.path.startswith("/api/"):
            return self.proxy()
        if self.path in ("/", "/index.html"):
            with open(os.path.join(HERE, "index.html"), "rb") as f:
                body = f.read()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        self.send_error(404)

    do_POST = do_PUT = do_DELETE = do_PATCH = do_OPTIONS = do_HEAD = proxy = None

    def proxy(self):
        target = urlparse(BACKEND)
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length else None
        headers = {k: v for k, v in self.headers.items() if k.lower() not in HOP_BY_HOP}
        conn = http.client.HTTPConnection(target.hostname, target.port or 80, timeout=60)
        try:
            conn.request(self.command, self.path, body=body, headers=headers)
            resp = conn.getresponse()
            data = resp.read()
        except Exception as ex:  # backend not running
            msg = f"Cannot reach the backend at {BACKEND}: {ex}".encode()
            self.send_response(502)
            self.send_header("Content-Type", "text/plain")
            self.send_header("Content-Length", str(len(msg)))
            self.end_headers()
            self.wfile.write(msg)
            return
        finally:
            conn.close()
        self.send_response(resp.status)
        for k, v in resp.getheaders():
            if k.lower() not in HOP_BY_HOP:
                self.send_header(k, v)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(data)

    def log_message(self, fmt, *args):
        sys.stderr.write("%s %s\n" % (self.command, self.path))


for name in ("do_POST", "do_PUT", "do_DELETE", "do_PATCH", "do_OPTIONS", "do_HEAD"):
    setattr(Handler, name, Handler.proxy)

if __name__ == "__main__":
    print(f"Test page: http://localhost:{PORT}   (API forwarded to {BACKEND})")
    ThreadingHTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
