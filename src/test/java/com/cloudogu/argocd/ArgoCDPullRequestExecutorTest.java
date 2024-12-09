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

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.google.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.repository.api.ScmProtocol;
import sonia.scm.webhook.WebHookService;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArgoCDPullRequestExecutorTest {
  @Mock
  private WebHookService webHookService;

  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;

  @Mock
  private RepositoryService repositoryService;

  @Mock
  private ArgoCDSender sender;

  private ArgoCDPullRequestExecutor target;

  private final Repository repository = RepositoryTestData.createHeartOfGold();

  @BeforeEach
  void setUp() {
    Provider<WebHookService> webHookServiceProvider = () -> webHookService;
    target = new ArgoCDPullRequestExecutor(webHookServiceProvider, repositoryServiceFactory);
    target.sender = sender;
  }

  void setUpService() {
    when(repositoryServiceFactory.create(any(Repository.class))).thenReturn(repositoryService);

    when(repositoryService.getSupportedProtocols()).thenReturn(Stream.of(
      new ScmProtocol() {
        @Override
        public String getType() {
          return "http";
        }

        @Override
        public String getUrl() {
          return "scm-manager.org";
        }
      }
    ));
  }

  private PullRequestEvent prepareBasicEvent(HandlerEventType eventType) {
    PullRequest pullRequest = new PullRequest();
    pullRequest.setSource("fork");
    pullRequest.setTarget("main");
    return new PullRequestEvent(repository, pullRequest, pullRequest, eventType);
  }

  private void prepareScmmWebHook(ArgoCDWebhook webhook) {
    webhook.setHookImplementation(HookImplementation.SCMM);
    webhook.setUrl("https://scm-manager.org/hitchhiker");
    webhook.setInsecure(false);
    webhook.setSecret("secret");

    when(webHookService.getConfigurations(any(), any())).thenReturn(List.of(webhook));
  }

  @Test
  void shouldNotFireForGitHubHook() {
    ArgoCDWebhook webhook = new ArgoCDWebhook();
    webhook.setHookImplementation(HookImplementation.GITHUB);
    webhook.setUrl("https://scm-manager.org/hitchhiker");
    webhook.setInsecure(false);
    webhook.setSecret("secret");

    when(webHookService.getConfigurations(any(), any())).thenReturn(List.of(webhook));

    PullRequest pullRequest = new PullRequest();
    pullRequest.setSource("fork");
    pullRequest.setTarget("main");
    PullRequestEvent event = new PullRequestEvent(repository, pullRequest, pullRequest, HandlerEventType.CREATE);

    target.handleBasicPullRequest(event, ScmmPullRequestEventPayload.Action.OPENED);

    verify(sender, never()).sendEvent(any(ArgoCDWebhook.class), any(PullRequestEventPayload.class));
  }

  @Test
  void shouldFireForScmmHookAfterOpenedEvent() {
    setUpService();
    ArgoCDWebhook webhook = new ArgoCDWebhook();
    prepareScmmWebHook(webhook);

    PullRequestEvent event = prepareBasicEvent(HandlerEventType.CREATE);

    target.handlePullRequest(event);

    verify(sender).sendEvent(eq(webhook), any(ScmmPullRequestEventPayload.class));
  }

  @Test
  void shouldFireForScmmHookAfterChangedEvent() {
    setUpService();
    ArgoCDWebhook webhook = new ArgoCDWebhook();
    prepareScmmWebHook(webhook);

    PullRequestEvent event = prepareBasicEvent(HandlerEventType.MODIFY);
    target.handlePullRequest(event);

    verify(sender).sendEvent(eq(webhook), any(ScmmPullRequestEventPayload.class));
  }

  @Test
  void shouldFireForScmmHookAfterMergedEvent() {
    setUpService();

    PullRequest pullRequest = new PullRequest();
    pullRequest.setSource("fork");
    pullRequest.setTarget("main");

    ArgoCDWebhook webhook = new ArgoCDWebhook();
    prepareScmmWebHook(webhook);
    PullRequestMergedEvent event = new PullRequestMergedEvent(repository, pullRequest);

    target.handlePullRequest(event);

    verify(sender).sendEvent(eq(webhook), any(ScmmPullRequestEventPayload.class));
  }

  @Test
  void shouldFireForScmmHookAfterRejectedEvent() {
    setUpService();

    PullRequest pullRequest = new PullRequest();
    pullRequest.setSource("fork");
    pullRequest.setTarget("main");

    ArgoCDWebhook webhook = new ArgoCDWebhook();
    prepareScmmWebHook(webhook);
    PullRequestRejectedEvent event = new PullRequestRejectedEvent(repository, pullRequest, PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER);

    target.handlePullRequest(event);

    verify(sender).sendEvent(eq(webhook), any(ScmmPullRequestEventPayload.class));
  }

  @Test
  void shouldSendCorrectScmmPayload() {
    setUpService();
    ArgumentCaptor<ScmmPullRequestEventPayload> captor = ArgumentCaptor.forClass(ScmmPullRequestEventPayload.class);

    PullRequest pullRequest = new PullRequest();
    pullRequest.setSource("fork");
    pullRequest.setTarget("main");

    ArgoCDWebhook webhook = new ArgoCDWebhook();
    prepareScmmWebHook(webhook);
    PullRequestRejectedEvent event = new PullRequestRejectedEvent(repository, pullRequest, PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER);

    target.handlePullRequest(event);

    verify(sender).sendEvent(eq(webhook), captor.capture());

    ScmmPullRequestEventPayload.Repository expectedPayloadRepository =
      new ScmmPullRequestEventPayload.Repository(
        "scm-manager.org",
        repository.getNamespace(),
        repository.getName()
      );

    assertThat(captor.getValue().getRepository()).isEqualTo(expectedPayloadRepository);
  }


}
