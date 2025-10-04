package duit.server

import duit.server.config.TestJooqConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestJooqConfig::class)
class ServerApplicationTests {

	@Test
	fun contextLoads() {
	}

}
