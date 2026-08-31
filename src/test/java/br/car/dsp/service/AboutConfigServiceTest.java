package br.car.dsp.service;

import br.car.dsp.config.AboutConfigProperties;
import br.car.dsp.dto.AboutConfigResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AboutConfigServiceTest {

	@Test
	void getAboutConfig_ShouldLoadBannerTitleTabsAndMarkdownContentFromTestFixture() {
		AboutConfigProperties properties = new AboutConfigProperties();
		properties.setConfigFile("classpath:about-config-test.json");
		properties.setContentDir("classpath:about/");

		AboutConfigService service = new AboutConfigService(properties, new ObjectMapper());

		AboutConfigResponse config = service.getAboutConfig();

		assertNotNull(config);
		assertTrue(config.enabled());
		assertEquals("About Test", config.bannerTitle());
		assertEquals(2, config.tabs().size());

		var overview = config.tabs().getFirst();
		assertEquals("overview", overview.id());
		assertEquals("Overview", overview.label());
		assertTrue(overview.content().contains("This is the overview content."));

		var license = config.tabs().get(1);
		assertEquals("license", license.id());
		assertTrue(license.content().contains("Licensed under GPL-3.0."));
	}

	@Test
	void getAboutConfig_ShouldCacheResultAcrossCalls() {
		AboutConfigProperties properties = new AboutConfigProperties();
		properties.setConfigFile("classpath:about-config-test.json");
		properties.setContentDir("classpath:about/");

		AboutConfigService service = new AboutConfigService(properties, new ObjectMapper());

		AboutConfigResponse first = service.getAboutConfig();
		AboutConfigResponse second = service.getAboutConfig();

		assertEquals(first, second);
	}

	@Test
	void getAboutConfig_ShouldReturnDisabledWhenIndexHasEnabledFalse() {
		AboutConfigProperties properties = new AboutConfigProperties();
		properties.setConfigFile("classpath:about-config-disabled-test.json");
		properties.setContentDir("classpath:about/");

		AboutConfigService service = new AboutConfigService(properties, new ObjectMapper());

		AboutConfigResponse config = service.getAboutConfig();

		assertFalse(config.enabled());
		assertTrue(config.tabs().isEmpty());
	}

	@Test
	void getAboutConfig_ShouldReturnDisabledWhenClasspathConfigFileIsMissing() {
		AboutConfigProperties properties = new AboutConfigProperties();
		properties.setConfigFile("classpath:does-not-exist-about-config.json");
		properties.setContentDir("classpath:about/");

		AboutConfigService service = new AboutConfigService(properties, new ObjectMapper());

		AboutConfigResponse config = service.getAboutConfig();

		assertFalse(config.enabled());
		assertNull(config.bannerTitle());
		assertTrue(config.tabs().isEmpty());
	}

	@Test
	void getAboutConfig_ShouldReturnDisabledWhenFilesystemConfigFileIsMissing() {
		AboutConfigProperties properties = new AboutConfigProperties();
		properties.setConfigFile("/tmp/does-not-exist-about-config.json");
		properties.setContentDir("classpath:about/");

		AboutConfigService service = new AboutConfigService(properties, new ObjectMapper());

		AboutConfigResponse config = service.getAboutConfig();

		assertFalse(config.enabled());
		assertTrue(config.tabs().isEmpty());
	}

	@Test
	void getAboutConfig_ShouldFailWhenReferencedMarkdownFileIsMissing() {
		AboutConfigProperties properties = new AboutConfigProperties();
		properties.setConfigFile("classpath:about-config-missing-file-test.json");
		properties.setContentDir("classpath:about/");

		AboutConfigService service = new AboutConfigService(properties, new ObjectMapper());

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				service::getAboutConfig
		);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
		assertEquals("Failed to load about config", exception.getReason());
		assertTrue(exception.getCause().getMessage().contains("does-not-exist.md"));
	}

	@Test
	void getAboutConfig_ShouldFailWhenIndexJsonIsMalformed() throws Exception {
		Path temp = Files.createTempFile("about-config-malformed", ".json");
		Files.writeString(temp, "{ not valid json ");

		AboutConfigProperties properties = new AboutConfigProperties();
		properties.setConfigFile(temp.toAbsolutePath().toString());
		properties.setContentDir("classpath:about/");

		AboutConfigService service = new AboutConfigService(properties, new ObjectMapper());

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				service::getAboutConfig
		);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
		assertEquals("Failed to load about config", exception.getReason());
	}
}
