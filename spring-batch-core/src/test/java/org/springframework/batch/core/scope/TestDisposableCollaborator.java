package org.springframework.batch.core.scope;

import org.springframework.beans.factory.DisposableBean;

@SuppressWarnings("serial")
public class TestDisposableCollaborator extends TestCollaborator implements DisposableBean {

	public static volatile String message = "none";

	@Override
	public void destroy() throws Exception {
		message = (message.equals("none") ? "" : message + ",") + getName() + ":destroyed";
	}

}
