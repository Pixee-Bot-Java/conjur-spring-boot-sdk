package com.cyberark.conjur.springboot.core.env;

import com.cyberark.conjur.sdk.ApiException;
import com.cyberark.conjur.sdk.endpoint.SecretsApi;
import com.cyberark.conjur.springboot.annotations.ConjurPropertySource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author bnasslahsen
 */
@SpringBootTest
public class ConjurPropertySourceTest {


	@MockBean
	private SecretsApi secretsApi;

	@Test
	public void testGetSecretCallsCount() throws ApiException {
		// Verify the number of times the method was called
		verify(secretsApi, times(3)).getSecret(any(), any(),
				any());
	}

	@SpringBootApplication
	static class ConjurPropertySourceTestApp {

		@ConjurPropertySource("db/")
		@Configuration
		class ConjurPropertySourceConfiguration {

			@Value("${dbpassWord}")
			private byte[] dbpassWord;

			@Value("${dbuserName}")
			private byte[] dbuserName;

			@Value("${key}")
			private byte[] key;
		}
	}

}
