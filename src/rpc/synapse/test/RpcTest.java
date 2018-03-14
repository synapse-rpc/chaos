package rpc.synapse.test;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.AMQP;
import rpc.synapse.chaos.BaseCallback;

import java.util.Hashtable;

public class RpcTest extends BaseCallback {
    @Override
    public Hashtable<String, String> regAlias() {
        Hashtable<String, String> alias = new Hashtable<>();
        alias.put("test", "test");
        return alias;
    }

    public JSONObject test(JSONObject body, AMQP.BasicProperties props) {
        System.out.printf("**RPC请求: %s@%s %s\n", props.getType(), props.getReplyTo(), body.toJSONString());
        JSONObject ret = new JSONObject();
        ret.put("from", "java");
        ret.put("m", body.get("msg"));
        ret.put("number", 5233);
        return ret;
    }
}
