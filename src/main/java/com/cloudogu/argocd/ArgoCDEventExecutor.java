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

import sonia.scm.repository.Branch;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.ScmProtocol;

import java.io.IOException;

class ArgoCDEventExecutor {

  protected String findSourceUrl(RepositoryService service, Repository repository) {
    return service.getSupportedProtocols()
      .filter(p -> "http".equals(p.getType()))
      .findFirst()
      .map(ScmProtocol::getUrl)
      .orElseThrow(() -> new ArgoCDHookExecutionException("Http protocol not found for repository " + repository));
  }

  protected String findDefaultBranch(RepositoryService service, Repository repository) throws IOException {
    return service.getBranchesCommand().getBranches().getBranches().stream()
      .filter(Branch::isDefaultBranch)
      .findFirst()
      .map(Branch::getName)
      .orElseThrow(() -> new InternalRepositoryException(repository, "Could not find default branch"));
  }
}
