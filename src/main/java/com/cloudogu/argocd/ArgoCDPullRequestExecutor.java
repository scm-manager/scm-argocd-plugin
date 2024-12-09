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

import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.github.legman.Subscribe;
import com.google.inject.Provider;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.EagerSingleton;
import sonia.scm.HandlerEventType;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.webhook.WebHookService;

import java.util.List;

@Extension
@EagerSingleton
@Slf4j
@Requires("scm-review-plugin")
class ArgoCDPullRequestExecutor extends ArgoCDEventExecutor {

  private final Provider<WebHookService> webHookService;
  private final RepositoryServiceFactory repositoryServiceFactory;
  protected ArgoCDSender sender;

  @Inject
  ArgoCDPullRequestExecutor(Provider<WebHookService> webHookService, RepositoryServiceFactory repositoryServiceFactory) {
    this.webHookService = webHookService;
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.sender = new ArgoCDSender(webHookService.get());
  }

  @Subscribe
  public void handlePullRequest(PullRequestMergedEvent event) {
    handleBasicPullRequest(event, ScmmPullRequestEventPayload.Action.MERGED);
  }

  @Subscribe
  public void handlePullRequest(PullRequestRejectedEvent event) {
    handleBasicPullRequest(event, ScmmPullRequestEventPayload.Action.REJECTED);
  }

  @Subscribe
  public void handlePullRequest(PullRequestEvent event) {
    try {
      if (HandlerEventType.MODIFY.equals(event.getEventType())) {
        handleBasicPullRequest(event, ScmmPullRequestEventPayload.Action.CHANGED);
      } else if (HandlerEventType.CREATE.equals(event.getEventType())) {
        handleBasicPullRequest(event, ScmmPullRequestEventPayload.Action.OPENED);
      }
    } catch (Exception e) {
      log.error("Unexpected error during processing of a PullRequestEvent in ArgoCD plugin", e);
    }
  }

  void handleBasicPullRequest(BasicPullRequestEvent event, ScmmPullRequestEventPayload.Action action) {
    List<ArgoCDWebhook> configurations = webHookService.get().getConfigurations(ArgoCDWebhook.class, event.getRepository());
    configurations.forEach(config -> {
      if (HookImplementation.SCMM.equals(config.getHookImplementation())) {
        try (RepositoryService service = repositoryServiceFactory.create(event.getRepository())) {
          String sourceUrl = findSourceUrl(service, event.getRepository());
          String sourceBranch = event.getPullRequest().getSource();
          String targetBranch = event.getPullRequest().getTarget();
          ScmmPullRequestEventPayload.Repository repository = new ScmmPullRequestEventPayload.Repository(
            sourceUrl, event.getRepository().getNamespace(), event.getRepository().getName()
          );
          sender.sendEvent(config, new ScmmPullRequestEventPayload(repository, sourceBranch, targetBranch, action));
        } catch (Exception e) {
          throw new InternalRepositoryException(event.getRepository(), "Failed to trigger ArgoCD Webhook", e);
        }
      }
    });
  }
}
