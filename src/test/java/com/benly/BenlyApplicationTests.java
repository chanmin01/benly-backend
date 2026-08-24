package com.benly;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(properties = {
        "claude.api-key=dummy-key",
        "claude.api-url=https://api.anthropic.com/v1/messages",
        "claude.model=claude-sonnet-4-6",
        "openai.api-key=dummy-key",
        "openai.whisper.api-url=https://api.openai.com/v1/audio/transcriptions",
        "openai.whisper.model=whisper-1"
})
class BenlyApplicationTests {

	@Test
	void contextLoads() {
	}

}
