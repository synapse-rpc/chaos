package rpc.synapse.test;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.AMQP;
import rpc.synapse.chaos.BaseCallback;

import java.util.Hashtable;

public class EventTest extends BaseCallback {
    @Override
    public Hashtable<String, String> regAlias() {
        Hashtable<String, String> alias = new Hashtable<>();
        alias.put("dotnet.test", "test");
        alias.put("golang.test", "test");
        alias.put("python.test", "test");
        alias.put("php.test", "test");
        alias.put("ruby.test", "test");
        alias.put("java.test", "test");
        return alias;
    }

    public boolean test(JSONObject body, AMQP.BasicProperties props) {
        System.out.printf("**收到EVENT: %s@%s %s\n", props.getType(), props.getReplyTo(), body.toJSONString());
        return true;
    }
}
