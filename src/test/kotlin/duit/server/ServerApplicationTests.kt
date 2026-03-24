package duit.server

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import duit.server.support.TestFirebaseConfig
import duit.server.support.TestRedisConfig
import org.springframework.context.annotation.Import

@Import(TestRedisConfig::class, TestFirebaseConfig::class)
@SpringBootTest
class ServerApplicationTests {

	@Test
	fun contextLoads() {
	}

}
