/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cloudogu.argocd;

import sonia.scm.ContextEntry;
import sonia.scm.repository.Branch;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.repository.api.ScmProtocol;

import javax.inject.Inject;
import java.io.IOException;
import java.util.stream.Collectors;

public class ArgoCDWebhookPayloader {

  private final RepositoryServiceFactory serviceFactory;

  @Inject
  public ArgoCDWebhookPayloader(RepositoryServiceFactory serviceFactory) {
    this.serviceFactory = serviceFactory;
  }

  public GitHubPushEventPayloadDto createPayload(Repository repository) {
    try (RepositoryService service = serviceFactory.create(repository)) {
      if (!service.isSupported(Command.BRANCHES)) {
        throw new InternalRepositoryException(repository, "Repository does not support this type of webhook");
      }
      ScmProtocol scmProtocol = service.getSupportedProtocols().collect(Collectors.toList()).get(0);
      String defaultBranch = service.getBranchesCommand().getBranches().getBranches().stream()
        .filter(Branch::isDefaultBranch)
        .findFirst().map(Branch::getName)
        .orElseThrow(() -> new InternalRepositoryException(repository, "Could not find default branch"));
      return new GitHubPushEventPayloadDto(new GitHubRepository(scmProtocol.getUrl(), defaultBranch));
    } catch (IOException e) {
      throw new ArgoCDHookExecutionException(ContextEntry.ContextBuilder.entity(repository).build(), "Failed to create webhook payload", e);
    }
  }
}
