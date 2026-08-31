package br.car.dsp.controller;

import br.car.dsp.dto.AboutConfigResponse;
import br.car.dsp.dto.AboutTabResponse;
import br.car.dsp.service.AboutConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AboutControllerTest {

	@Mock
	private AboutConfigService aboutConfigService;

	@InjectMocks
	private AboutController aboutController;

	@Test
	void getAboutConfig_ShouldDelegateToService() {
		AboutConfigResponse expected = new AboutConfigResponse(
				true,
				"About",
				List.of(new AboutTabResponse("overview", "Overview", "# Overview\n"))
		);
		when(aboutConfigService.getAboutConfig()).thenReturn(expected);

		AboutConfigResponse result = aboutController.getAboutConfig();

		assertEquals(expected, result);
		assertEquals("About", result.bannerTitle());
		assertEquals(1, result.tabs().size());
		assertEquals("overview", result.tabs().getFirst().id());
		verify(aboutConfigService).getAboutConfig();
	}
}
