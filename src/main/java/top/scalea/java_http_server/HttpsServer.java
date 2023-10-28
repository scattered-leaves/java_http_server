package top.scalea.java_http_server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import org.apache.commons.collections4.multimap.AbstractListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.io.IOException;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class HttpsServer extends HttpServer {
    public final HSInfo hsinfo;
    private final com.sun.net.httpserver.HttpsServer hss;
    final AbstractListValuedMap<String , HttpHandler> only = new ArrayListValuedHashMap<>();
    //private final Map<String , List<HttpHandler>> wildcard = new ConcurrentHashMap<>();
    final AbstractListValuedMap<String , HttpHandler> regexp = new ArrayListValuedHashMap<>();

    public HttpsServer() throws IOException {
        this(null, 0);
    }
    public HttpsServer(InetSocketAddress addr, int backlog) throws IOException {
        this(addr, backlog, System.err);
    }

    public HttpsServer(InetSocketAddress addr, int backlog, PrintStream err) throws IOException {
        hsinfo = new HSInfo(err);
        hss = com.sun.net.httpserver.HttpsServer.create(addr, backlog);
        hss.createContext("/", new handle(this));
    }
    public static HttpsServer create() throws IOException {
        return new HttpsServer();
    }
    public static HttpsServer create(InetSocketAddress addr, int backlog) throws IOException {
        return new HttpsServer(addr, backlog);
    }

    static class handle implements HttpHandler {

        private final HttpsServer hsr;
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
        handle(HttpsServer hse)
        {
            this.hsr = hse;
        }
    }
    /**
     * Sets this server's {@link HttpsConfigurator} object.
     *
     * @param config the {@code HttpsConfigurator} to set
     * @throws NullPointerException if config is {@code null}
     */
    public void setHttpsConfigurator(HttpsConfigurator config) {
        hss.setHttpsConfigurator(config);
    }

    /**
     * Gets this server's {@link HttpsConfigurator} object, if it has been set.
     *
     * @return the {@code HttpsConfigurator} for this server, or {@code null} if
     * not set
     */
    public HttpsConfigurator getHttpsConfigurator() {
        return hss.getHttpsConfigurator();
    }


    /**
     * Binds a currently unbound {@code HttpServer} to the given address and
     * port number. A maximum backlog can also be specified. This is the maximum
     * number of queued incoming connections to allow on the listening socket.
     * Queued TCP connections exceeding this limit may be rejected by the TCP
     * implementation.
     *
     * @param addr    the address to listen on
     * @param backlog the socket backlog. If this value is less than or equal to
     *                zero, then a system default value is used
     * @throws BindException        if the server cannot bind to the requested address
     *                              or if the server is already bound
     * @throws NullPointerException if addr is {@code null}
     */
    @Override
    public void bind(InetSocketAddress addr, int backlog) throws IOException {
        hss.bind(addr, backlog);
    }

    /**
     * Starts this server in a new background thread. The background thread
     * inherits the priority, thread group and context class loader
     * of the caller.
     */
    @Override
    public void start() {
        hss.start();
    }

    /**
     * Sets this server's {@link Executor} object. An
     * {@code Executor} must be established before {@link #start()} is called.
     * All HTTP requests are handled in tasks given to the executor.
     * If this method is not called (before {@link #start()}) or if it is called
     * with a {@code null Executor}, then a default implementation is used,
     * which uses the thread which was created by the {@link #start()} method.
     *
     * @param executor the {@code Executor} to set, or {@code null} for  default
     *                 implementation
     * @throws IllegalStateException if the server is already started
     */
    @Override
    public void setExecutor(Executor executor) {
        hss.setExecutor(executor);
    }

    /**
     * Returns this server's {@code Executor} object if one was specified with
     * {@link #setExecutor(Executor)}, or {@code null} if none was specified.
     *
     * @return the {@code Executor} established for this server or {@code null} if not set.
     */
    @Override
    public Executor getExecutor() {
        return hss.getExecutor();
    }

    /**
     * Stops this server by closing the listening socket and disallowing
     * any new exchanges from being processed. The method will then block
     * until all current exchange handlers have completed or else when
     * approximately <i>delay</i> seconds have elapsed (whichever happens
     * sooner). Then, all open TCP connections are closed, the background
     * thread created by {@link #start()} exits, and the method returns.
     * Once stopped, a {@code HttpServer} cannot be re-used.
     *
     * @param delay the maximum time in seconds to wait until exchanges have finished
     * @throws IllegalArgumentException if delay is less than zero
     */
    @Override
    public void stop(int delay) {
        hss.stop(delay);
    }

    /**
     * Creates a {@code HttpContext}. A  {@code HttpContext} represents a mapping
     * from a URI path to a exchange handler on this  {@code HttpServer}. Once
     * created, all requests received by the server for the path will be handled
     * by calling the given handler object. The context is identified by the
     * path, and can later be removed from the server using this with the
     * {@link #removeContext(String)} method.
     *
     * <p> The path specifies the root URI path for this context. The first
     * character of path must be '/'.
     *
     * <p>The class overview describes how incoming request URIs are
     * <a href="#mapping_description">mapped</a> to HttpContext instances.
     *
     * @param path    the root URI path to associate the context with
     * @param handler the handler to invoke for incoming requests
     * @return an instance of {@code HttpContext}
     * @throws IllegalArgumentException if path is invalid, or if a context
     *                                  already exists for this path
     * @throws NullPointerException     if either path, or handler are {@code null}
     * @apiNote The path should generally, but is not required to, end with '/'.
     * If the path does not end with '/', eg such as with {@code "/foo"} then
     * this would match requests with a path of {@code "/foobar"} or
     * {@code "/foo/bar"}.
     */
    @Override
    public HttpContext createContext(String path, HttpHandler handler) {
        return createContext(path, handler, Type.ONLY);
    }
    public HttpContext createContext(String path, HttpHandler handler, Type type) {
        switch (type){
            case ONLY :
                only.put(path, handler);
            case REGEXP:
                regexp.put(path, handler);
        }
        return null;
    }

    /**
     * Creates a HttpContext without initially specifying a handler. The handler
     * must later be specified using {@link HttpContext#setHandler(HttpHandler)}.
     * A {@code HttpContext} represents a mapping from a URI path to an exchange
     * handler on this {@code HttpServer}. Once created, and when the handler has
     * been set, all requests received by the server for the path will be handled
     * by calling the handler object. The context is identified by the path, and
     * can later be removed from the server using this with the
     * {@link #removeContext(String)} method.
     *
     * <p>The path specifies the root URI path for this context. The first character of path must be
     * '/'.
     *
     * <p>The class overview describes how incoming request URIs are
     * <a href="#mapping_description">mapped</a> to {@code HttpContext} instances.
     *
     * @param path the root URI path to associate the context with
     * @return an instance of {@code HttpContext}
     * @throws IllegalArgumentException if path is invalid, or if a context
     *                                  already exists for this path
     * @throws NullPointerException     if path is {@code null}
     * @apiNote The path should generally, but is not required to, end with '/'.
     * If the path does not end with '/', eg such as with {@code "/foo"} then
     * this would match requests with a path of {@code "/foobar"} or
     * {@code "/foo/bar"}.
     */
    @Override
    public HttpContext createContext(String path) {
        return createContext(path, null);
    }

    /**
     * Removes the context identified by the given path from the server.
     * Removing a context does not affect exchanges currently being processed
     * but prevents new ones from being accepted.
     *
     * @param path the path of the handler to remove
     * @throws IllegalArgumentException if no handler corresponding to this
     *                                  path exists.
     * @throws NullPointerException     if path is {@code null}
     */
    @Override
    public void removeContext(String path) throws IllegalArgumentException {

    }

    /**
     * Removes the given context from the server.
     * Removing a context does not affect exchanges currently being processed
     * but prevents new ones from being accepted.
     *
     * @param context the context to remove
     * @throws NullPointerException if context is {@code null}
     */
    @Override
    public void removeContext(HttpContext context) {

    }

    /**
     * Returns the address this server is listening on
     *
     * @return the {@code InetSocketAddress} the server is listening on
     */
    @Override
    public InetSocketAddress getAddress() {
        return hss.getAddress();
    }
}
