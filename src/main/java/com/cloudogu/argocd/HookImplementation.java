/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.argocd;

import sonia.scm.net.ahc.BaseHttpRequest;

enum HookImplementation {
  SCMM,
  GITHUB;

  void setHeader(BaseHttpRequest<?> request) {
    if (this == SCMM) {
      request.header("X-SCM-PushEvent", "Push");
    } else {
      request.header("X-Github-Event", "push");
    }
  }

  public void setSecurityHeader(BaseHttpRequest<?> request, String digest) {
    if (this == SCMM) {
      request.header("X-SCM-Signature", "sha1=" + digest);
    } else {
      request.header("X-Hub-Signature", "sha1=" + digest);
    }
  }
}
