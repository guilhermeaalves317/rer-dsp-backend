package br.car.dsp.dto;

import java.util.List;

/**
 * About page configuration (banner title and tabbed markdown content).
 * Source of truth: an external index JSON file plus markdown files, same
 * external-config convention as the installation config.
 */
public record AboutConfigResponse(
		boolean enabled,
		String bannerTitle,
		List<AboutTabResponse> tabs
) {

	public AboutConfigResponse {
		tabs = tabs == null ? List.of() : tabs;
	}
}
