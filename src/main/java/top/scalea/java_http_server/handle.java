package top.scalea.java_http_server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

class handle implements HttpHandler {

    private final HttpServer hsr;
    /**
     * Handle the given request and generate an appropriate response.
     * See {@link HttpExchange} for a description of the steps
     * involved in handling an exchange.
     *
     * @param exchange the exchange containing the request from the
     *                 client and used to send the response
     * @throws NullPointerException if exchange is {@code null}
     * @throws IOException          if an I/O error occurs
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            boolean in404 = true;
            String path = exchange.getRequestURI().getPath();
            if (hsr.only.containsKey(path)) {
                List<HttpHandler> h = hsr.only.get(path);
                for (HttpHandler xh : h) {
                    in404 = false;
                    try {
                        xh.handle(exchange);
                    } catch (Exception e) {
                        e.printStackTrace(hsr.hsinfo.err);
                    }
                }
            }
            String[] rel = hsr.regexp.keySet().toArray(new String[0]);
            HttpHandler[] rev = hsr.regexp.values().toArray(new HttpHandler[0]);
            for (int i = 0; i < rel.length; i++) {
                try {
                    if (path.matches(rel[i])) {
                        for (HttpHandler xh : rev) {
                            in404 = false;
                            try {
                                xh.handle(exchange);
                            } catch (Exception e) {
                                e.printStackTrace(hsr.hsinfo.err);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace(hsr.hsinfo.err);
                }
            }
            if (in404) {
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                List<String> ls = exchange.getRequestHeaders().get("Accept-Language");
                String l = "en-US";
                if (ls != null && !ls.isEmpty()) l = ls.get(0).split(",")[0];
                Map<String , byte[]> temp = hsr.hsinfo.deferror != null ? hsr.hsinfo.deferror.get(404) : null;
                byte[] date = temp != null ? temp.get(l) : new byte[0];
                exchange.sendResponseHeaders(404, date.length);
                exchange.getResponseBody().write(date);
                exchange.close();
            }
        }catch (Exception e)
        {
            e.printStackTrace(hsr.hsinfo.err);
            exchange.sendResponseHeaders(500,0);
        }finally {
            exchange.close();
        }
    }
    handle(HttpServer hse)
    {
        this.hsr = hse;
    }
}