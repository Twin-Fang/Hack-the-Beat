package kr.suhsaechan.hackthebeat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// 운영 DB 없이 컨텍스트가 뜨는지만 확인한다 (H2 인메모리)
@SpringBootTest
@ActiveProfiles("test")
class HackTheBeatApplicationTests {

    @Test
    void contextLoads() {
    }
}
