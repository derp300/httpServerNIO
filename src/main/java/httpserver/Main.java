package httpserver;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static Map<String, String> config;
    static {
        config = new HashMap<>();
        config.put("port", "9090");
        config.put("rootDir", "site");
        config.put("cache", "*");
    }

    public static void main(String[] args) {
        if(args.length == 1) {
            readConfig(args[0]);
        } else {
            readConfig("config.json");
        }
        
        httpserver.FileReader.init(config.get("rootDir"), config.get("cache"));

        try {
            HttpServer server = new HttpServer(Integer.parseInt(config.get("port")));
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readConfig(String configPath) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject object = (JSONObject) parser.parse(new FileReader(configPath));
            String value;
            for(Map.Entry<String, String> item: config.entrySet()) {
                value = (String) object.get(item.getKey());
                if(value != null)
                    config.put((String) item.getKey(), value);
            }
        }
        catch (IOException | ParseException e) {
            System.out.println(e);
        }
        
        printConfig();

    }

    private static void printConfig() {
        for(Map.Entry<String, String> item: config.entrySet()) {
            System.out.println(item.getKey() + ": " + item.getValue());
        }
    }

}
