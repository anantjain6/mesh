package com.gentics.mesh.core.project;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;

public class ProjectVerticleETagTest extends AbstractETagTest {

	@Autowired
	private ProjectVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	@Override
	public void testReadMultiple() {
		try (NoTx noTx = db.noTx()) {
			MeshResponse<ProjectListResponse> response = getClient().findProjects().invoke();
			latchFor(response);
			String etag = response.getResponse().getHeader(ETAG);
			assertNotNull(etag);

			expect304(getClient().findProjects(), etag);
			expectNo304(getClient().findProjects(new PagingParameters().setPage(2)), etag);
		}
	}

	@Test
	@Override
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			MeshResponse<ProjectResponse> response = getClient().findProjectByUuid(project.getUuid()).invoke();
			latchFor(response);
			String etag = project.getETag(getMockedInternalActionContext());
			assertEquals(etag, response.getResponse().getHeader(ETAG));

			// Check whether 304 is returned for correct etag
			MeshRequest<ProjectResponse> request = getClient().findProjectByUuid(project.getUuid());
			assertEquals(etag, expect304(request, etag));

			// The node has no node reference and thus expanding will not affect the etag
			assertEquals(etag, expect304(getClient().findProjectByUuid(project.getUuid(), new NodeParameters().setExpandAll(true)), etag));

			// Assert that adding bogus query parameters will not affect the etag
			expect304(getClient().findProjectByUuid(project.getUuid(), new NodeParameters().setExpandAll(false)), etag);
			expect304(getClient().findProjectByUuid(project.getUuid(), new NodeParameters().setExpandAll(true)), etag);
		}

	}

}
