package top.scalea.java_http_server;

import com.sun.net.httpserver.HttpHandler;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HSInfo {
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
    protected PrintStream err;

    protected HSInfo(PrintStream errstrem)
    {
        err = errstrem;
    }
    protected HSInfo()
    {
        this(System.err);
    }
    public void setErrStrem(PrintStream errStrem)
    {
        err = errStrem;
    }

    public PrintStream getErrstrem() {
        return err;
    }

    public void setmesg(int code, String language, String date)
    {
        if (deferror.containsKey(code)) deferror.get(code).put(language, date.getBytes());
        else deferror.put(code, new ConcurrentHashMap<>() {{
            put(language, date.getBytes());
        }});
    }




}
