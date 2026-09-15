/**
 * 파이썬 /embed 가 돌려주는 좌표 묶음 (수업 05 ChatResponseVO 형태)
 */
package com.endit.domain;

import java.util.List;

import com.endit.cmn.DTO;

public class EmbedResponseVO extends DTO {

	private List<double[]> vectors;
	private String model;
	private int dims;

	public EmbedResponseVO() {
		super();
	}

	public List<double[]> getVectors() { return vectors; }
	public void setVectors(List<double[]> vectors) { this.vectors = vectors; }

	public String getModel() { return model; }
	public void setModel(String model) { this.model = model; }

	public int getDims() { return dims; }
	public void setDims(int dims) { this.dims = dims; }

	@Override
	public String toString() {
		return "EmbedResponseVO [vectors=" + (null == vectors ? 0 : vectors.size())
				+ "건, model=" + model + ", dims=" + dims + "]";
	}
}
