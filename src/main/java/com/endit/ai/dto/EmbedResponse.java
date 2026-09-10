/**
 * 파이썬 /embed 가 돌려주는 좌표 묶음
 */
package com.endit.ai.dto;

import java.util.List;

public record EmbedResponse(
		List<double[]> vectors,
		String model,
		int dims) {
}
