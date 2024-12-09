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

import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.webhook.WebHookExecutor;

import java.io.IOException;

class ArgoCDCommitExecutor extends ArgoCDEventExecutor implements WebHookExecutor {

  private final RepositoryServiceFactory serviceFactory;
  private final ArgoCDWebhook webhook;
  private final Repository repository;
  private final PostReceiveRepositoryHookEvent event;
  private final ArgoCDSender sender;

  public ArgoCDCommitExecutor(ArgoCDSender sender,
                              RepositoryServiceFactory serviceFactory,
                              ArgoCDWebhook webhook,
                              Repository repository,
                              PostReceiveRepositoryHookEvent event) {
    this.sender = sender;
    this.serviceFactory = serviceFactory;
    this.webhook = webhook;
    this.repository = repository;
    this.event = event;
  }

  @Override
  public void run() {
    HookBranchProvider branchProvider = event.getContext().getBranchProvider();
    try (RepositoryService service = serviceFactory.create(repository)) {
      String defaultBranch = findDefaultBranch(service, repository);
      String sourceUrl = findSourceUrl(service, repository);
      switch (webhook.getHookImplementation()) {
        case SCMM:
          branchProvider.getCreatedOrModified().forEach(branch -> sender.sendEvent(
            webhook, new ScmmPushEventPayload(sourceUrl, defaultBranch.equals(branch), branch)));
          branchProvider.getDeletedOrClosed().forEach(branch -> sender.sendEvent(
            webhook, new ScmmPushEventPayload(sourceUrl, defaultBranch.equals(branch), branch)));
          break;
        case GITHUB:
          branchProvider.getCreatedOrModified().forEach(branch -> sender.sendEvent(
            webhook, new GitHubPushEventPayload(sourceUrl, defaultBranch, branch)));
          branchProvider.getDeletedOrClosed().forEach(branch -> sender.sendEvent(
            webhook, new GitHubPushEventPayload(sourceUrl, defaultBranch, branch)));
          break;
      }
    } catch (IOException e) {
      throw new InternalRepositoryException(repository, "Failed to trigger ArgoCD Webhook", e);
    }
  }
}
