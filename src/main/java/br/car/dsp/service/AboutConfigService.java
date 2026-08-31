package br.car.dsp.service;

import br.car.dsp.config.AboutConfigProperties;
import br.car.dsp.dto.AboutConfigResponse;
import br.car.dsp.dto.AboutTabResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the About page configuration from an external index JSON file, resolving
 * each tab's markdown content from files inside the configured content directory.
 * Mirrors {@link InstallationConfigService}: loaded once, cached in memory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AboutConfigService {

	private static final AboutConfigResponse DISABLED = new AboutConfigResponse(false, null, List.of());

	private final AboutConfigProperties properties;
	private final ObjectMapper objectMapper;
	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	private volatile AboutConfigResponse cached;

	public AboutConfigResponse getAboutConfig() {
		AboutConfigResponse current = cached;
		if (current != null) {
			return current;
		}
		synchronized (this) {
			if (cached == null) {
				cached = loadFromFile();
			}
			return cached;
		}
	}

	private AboutConfigResponse loadFromFile() {
		String location = properties.getConfigFile();
		if (!exists(location)) {
			log.warn("About config not found at {}. About page disabled.", location);
			return DISABLED;
		}

		AboutIndex index;
		try (InputStream input = openStream(location)) {
			index = objectMapper.readValue(input, AboutIndex.class);
		} catch (IOException ex) {
			log.error("Failed to load about config from {}", location, ex);
			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to load about config",
					ex
			);
		}

		if (!index.enabled()) {
			log.warn("About config at {} has enabled=false. About page disabled.", location);
			return DISABLED;
		}

		List<AboutTabResponse> tabs = new ArrayList<>();
		if (index.tabs() != null) {
			for (AboutIndexTab tab : index.tabs()) {
				String content = readTabContent(tab);
				tabs.add(new AboutTabResponse(tab.id(), tab.label(), content));
			}
		}

		return new AboutConfigResponse(true, index.bannerTitle(), tabs);
	}

	private String readTabContent(AboutIndexTab tab) {
		String contentDir = properties.getContentDir();
		String location = joinContentPath(contentDir, tab.file());
		try (InputStream input = openStream(location)) {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			log.error("Failed to load about tab content from {}", location, ex);
			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to load about config",
					ex
			);
		}
	}

	private String joinContentPath(String contentDir, String file) {
		if (contentDir.endsWith("/")) {
			return contentDir + file;
		}
		return contentDir + "/" + file;
	}

	private boolean exists(String location) {
		if (location.startsWith("classpath:") || location.startsWith("file:")) {
			return resourceLoader.getResource(location).exists();
		}
		return Files.exists(Path.of(location));
	}

	private InputStream openStream(String location) throws IOException {
		if (location.startsWith("classpath:") || location.startsWith("file:")) {
			Resource resource = resourceLoader.getResource(location);
			if (!resource.exists()) {
				throw new IOException("About config not found: " + location);
			}
			return resource.getInputStream();
		}

		Path path = Path.of(location);
		if (!Files.exists(path)) {
			throw new IOException("About config not found: " + path.toAbsolutePath());
		}
		return Files.newInputStream(path);
	}

	/**
	 * Raw shape of the index JSON, before markdown content is resolved.
	 */
	private record AboutIndex(
			boolean enabled,
			String bannerTitle,
			List<AboutIndexTab> tabs
	) {
	}

	private record AboutIndexTab(
			String id,
			String label,
			String file
	) {
	}
}
