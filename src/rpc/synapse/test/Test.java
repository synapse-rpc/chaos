package rpc.synapse.test;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.cli.*;
import rpc.synapse.chaos.Synapse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Test {
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("h", "help", false, "Show Help");
        options.addRequiredOption(null, "host", true, "RabbitMQ Host");
        options.addOption(null, "port", true, "RabbitMQ Port");
        options.addRequiredOption("u", "user", true, "RabbitMQ Username");
        options.addRequiredOption("p", "pass", true, "RabbitMQ Password");
        options.addOption(null, "vhost", true, "RabbitMQ Virtual Host");
        options.addRequiredOption(null, "sys_name", true, "system name");
        options.addOption("d", "debug", false, "Debug Mode");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("h")) {
            String formatStr = "must need theses parameters";
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp(formatStr, "", options, "");
            return;
        }
        Synapse app = new Synapse();
        if (cmd.hasOption("debug")) {
            app.debug = true;
        }
        if (cmd.hasOption("host")) {
            app.mqHost = cmd.getOptionValue("host");
        }
        if (cmd.hasOption("port")) {
            app.mqPort = Integer.parseInt(cmd.getOptionValue("port"));
        }
        if (cmd.hasOption("user")) {
            app.mqUser = cmd.getOptionValue("user");
        }
        if (cmd.hasOption("pass")) {
            app.mqPass = cmd.getOptionValue("pass");
        }
        if (cmd.hasOption("vhost")) {
            app.mqVHost = cmd.getOptionValue("vhost");
        }
        if (cmd.hasOption("sys_name")) {
            app.sysName = cmd.getOptionValue("sys_name");
        }
        app.appName = "java";
        app.eventCallback = new EventTest();
        app.rpcCallback = new RpcTest();
        app.serve();
        String[] inputs;
        JSONObject body;
        JSONObject res;
        showHelp();
        while (true) {
            body = new JSONObject();
            String str = readDataFromConsole("input >> ");
            inputs = str.split(" ");
            switch (inputs[0]) {
                case "event":
                    if (inputs.length != 3) {
                        showHelp();
                        continue;
                    }
                    body.put("msg", inputs[2]);
                    app.sendEvent(inputs[1], body);
                    break;
                case "rpc":
                    if (inputs.length != 4) {
                        showHelp();
                        continue;
                    }
                    body.put("msg", inputs[2]);
                    res = app.sendRpc(inputs[1], inputs[2], body);
                    System.out.println(res.toJSONString());
                    break;
                default:
                    showHelp();
            }
        }
    }

    private static void showHelp() {
        System.out.println("----------------------------------------------");
        System.out.println("|   event usage:                             |");
        System.out.println("|     > event [event] [msg]                  |");
        System.out.println("|   rpc usage:                               |");
        System.out.println("|     > rpc [app] [method] [msg]             |");
        System.out.println("----------------------------------------------");
    }

    private static String readDataFromConsole(String prompt) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String str = null;
        try {
            System.out.print(prompt);
            str = br.readLine();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }
}
