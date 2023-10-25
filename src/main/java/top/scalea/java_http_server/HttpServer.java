package top.scalea.java_http_server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class HttpServer extends com.sun.net.httpserver.HttpServer {
    private static final byte[] on404 = "<h1>404 Not Found</h1>No context found for request".getBytes();
    public Map<Integer , Map<String , byte[]>> deferror = new ConcurrentHashMap<>();

    {
        for (Integer s : deferrors.keySet()) {
            deferror.put(s, new ConcurrentHashMap<>(deferrors.get(s)));
        }
    }
    private static final Map<Integer , Map<String , byte[]>> deferrors = new HashMap<>();
    static {
        Map<String, byte[]> temp = new HashMap<>();
        temp.put("en-US", "<h1>404 Not Found</h1>No context found for request".getBytes());
        temp.put("zh-CN", "<h1>404 未找到</h1>未找到请求的上下文".getBytes());
        deferrors.put(404, temp);

    }
    private final PrintStream err;
    private final com.sun.net.httpserver.HttpServer hs;
    private final Map<String , List<HttpHandler>> only = new ConcurrentHashMap<>();
    //private final Map<String , List<HttpHandler>> wildcard = new ConcurrentHashMap<>();
    private final Map<String , List<HttpHandler>> regexp = new ConcurrentHashMap<>();

    public HttpServer() throws IOException {
        this(null, 0);
    }
    public HttpServer(InetSocketAddress addr, int backlog) throws IOException {
        this(addr, backlog, System.err);
    }

    public HttpServer(InetSocketAddress addr, int backlog, PrintStream err) throws IOException {
        this.err = err;
        hs = com.sun.net.httpserver.HttpServer.create(addr, backlog);
        hs.createContext("/", new he(this));
    }
    public static HttpServer create() throws IOException {
        return new HttpServer();
    }
    public static HttpServer create(InetSocketAddress addr, int backlog) throws IOException {
        return new HttpServer(addr, backlog);
    }
    public void setIn404(HttpHandler hh)
    {

    }
    private static class he implements HttpHandler{

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
                            e.printStackTrace(hsr.err);
                        }
                    }
                }
                String[] rel = hsr.regexp.keySet().toArray(new String[0]);
                List<HttpHandler>[] rev = hsr.regexp.values().toArray(new List[0]);
                for (int i = 0; i < rel.length; i++) {
                    try {
                        if (path.matches(rel[i])) {
                            for (HttpHandler xh : rev[i]) {
                                in404 = false;
                                try {
                                    xh.handle(exchange);
                                } catch (Exception e) {
                                    e.printStackTrace(hsr.err);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace(hsr.err);
                    }
                }
                if (in404) {
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                    List<String> ls = exchange.getRequestHeaders().get("Accept-Language");
                    String l = "en-US";
                    if (ls != null && !ls.isEmpty()) l = ls.get(0).split(",")[0];
                    Map<String , byte[]> temp = hsr.deferror != null ? hsr.deferror.get(404) : null;
                    byte[] date = temp != null ? temp.get(l) : new byte[0];
                    exchange.sendResponseHeaders(404, date.length);
                    exchange.getResponseBody().write(date);
                    exchange.close();
                }
            }catch (Exception e)
            {
                e.printStackTrace(hsr.err);
                exchange.sendResponseHeaders(500,0);
            }
        }
        he(HttpServer hse)
        {
            this.hsr = hse;
        }
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
        hs.bind(addr, backlog);
    }

    /**
     * Starts this server in a new background thread. The background thread
     * inherits the priority, thread group and context class loader
     * of the caller.
     */
    @Override
    public void start() {
        hs.start();
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
        hs.setExecutor(executor);
    }

    /**
     * Returns this server's {@code Executor} object if one was specified with
     * {@link #setExecutor(Executor)}, or {@code null} if none was specified.
     *
     * @return the {@code Executor} established for this server or {@code null} if not set.
     */
    @Override
    public Executor getExecutor() {
        return hs.getExecutor();
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
        hs.stop(delay);
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
        return hs.getAddress();
    }
}
