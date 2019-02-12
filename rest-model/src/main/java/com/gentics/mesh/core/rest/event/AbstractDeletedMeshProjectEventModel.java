package com.gentics.mesh.core.rest.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.project.ProjectReference;

public class AbstractDeletedMeshProjectEventModel extends DeletedMeshEventModel implements ProjectEvent {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the project to which the element belonged.")
	private ProjectReference project;

	@Override
	public ProjectReference getProject() {
		return project;
	}

	@Override
	public void setProject(ProjectReference project) {
		this.project = project;
	}
}
