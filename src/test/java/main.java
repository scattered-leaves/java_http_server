import top.scalea.java_http_server.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class main {
    public static void main(String[] args) throws IOException {
        HttpServer.create(new InetSocketAddress("127.0.0.1", 11453), 0).start();
    }
}
