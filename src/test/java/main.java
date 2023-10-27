import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import top.scalea.java_http_server.HttpServer;
import top.scalea.java_http_server.Type;

import java.io.IOException;
import java.net.InetSocketAddress;

public class main {
    public static void main(String[] args) throws IOException {
        System.out.println("sb".matches("sb+"));
        HttpServer hs = HttpServer.create(new InetSocketAddress("127.0.0.1", 11453), 0);
        hs.start();
        hs.createContext("/hs+", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.sendResponseHeaders(200,0);
                exchange.getResponseBody().write(exchange.getRequestHeaders().toString().getBytes());
            }
        }, Type.REGEXP);

    }
}
