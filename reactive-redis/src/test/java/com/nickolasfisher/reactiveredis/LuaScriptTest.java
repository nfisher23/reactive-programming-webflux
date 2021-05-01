package com.nickolasfisher.reactiveredis;

import com.github.dockerjava.zerodep.shaded.org.apache.commons.codec.binary.Hex;
import io.lettuce.core.ScriptOutputType;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LuaScriptTest extends BaseSetupAndTeardownRedis {

    public static final String SAMPLE_LUA_SCRIPT = "return redis.call('set',KEYS[1],ARGV[1],'ex',ARGV[2])";

    @Test
    public void executeLuaScript() {
        String script = SAMPLE_LUA_SCRIPT;

        StepVerifier.create(redisReactiveCommands.eval(script, ScriptOutputType.BOOLEAN,
                // keys as an array
                Arrays.asList("foo1").toArray(new String[0]),
                // other arguments
                "bar1", "10"
                )
        )
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.get("foo1"))
                .expectNext("bar1")
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.ttl("foo1"))
                .expectNextMatches(ttl -> 7 < ttl && 11 > ttl)
                .verifyComplete();
    }

    @Test
    public void scriptLoadFromResponse() {
        String shaOfScript = redisReactiveCommands.scriptLoad(SAMPLE_LUA_SCRIPT).block();

        StepVerifier.create(redisReactiveCommands.evalsha(
                shaOfScript,
                ScriptOutputType.BOOLEAN,
                // keys as an array
                Arrays.asList("foo1").toArray(new String[0]),
                // other arguments
                "bar1", "10")
        )
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void scriptLoadFromDigest() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digestAsBytes = md.digest(SAMPLE_LUA_SCRIPT.getBytes(StandardCharsets.UTF_8));
        String hexadecimalStringOfScriptSha1 = Hex.encodeHexString(digestAsBytes);
        String hexStringFromRedis = redisReactiveCommands.scriptLoad(SAMPLE_LUA_SCRIPT).block();

        // they're the same
        assertEquals(hexadecimalStringOfScriptSha1, hexStringFromRedis);

        StepVerifier.create(redisReactiveCommands.evalsha(
                hexadecimalStringOfScriptSha1,
                ScriptOutputType.BOOLEAN,
                // keys as an array
                Arrays.asList("foo1").toArray(new String[0]),
                // other arguments
                "bar1", "10")
        )
                .expectNext(true)
                .verifyComplete();
    }
}
