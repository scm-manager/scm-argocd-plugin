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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.net.ahc.AdvancedHttpRequestWithBody;
import sonia.scm.net.ahc.Content;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Branches;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.Person;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.repository.api.ScmProtocol;
import sonia.scm.webhook.WebHookService;

import java.io.IOException;
import java.util.stream.Stream;

import static com.cloudogu.argocd.HookImplementation.GITHUB;
import static com.cloudogu.argocd.HookImplementation.SCMM;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArgoCDCommitExecutorTest {

  @Mock
  private WebHookService webHookService;
  @Mock
  private RepositoryServiceFactory serviceFactory;
  @Mock
  private RepositoryService service;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private BranchesCommandBuilder branchesCommandBuilder;

  @Mock(answer = Answers.RETURNS_SELF)
  private AdvancedHttpRequestWithBody request;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private PostReceiveRepositoryHookEvent event;

  private ArgoCDSender sender;

  @Mock
  private Content content;

  private final Repository repository = RepositoryTestData.create42Puzzle();

  @BeforeEach
  void initClient() {
    sender = spy(new ArgoCDSender(webHookService));
    lenient().when(request.getContent()).thenReturn(content);

    when(serviceFactory.create(repository)).thenReturn(service);
    when(service.getBranchesCommand()).thenReturn(branchesCommandBuilder);
  }

  private ArgoCDCommitExecutor createExecutor(ArgoCDWebhook webhook) {
    return new ArgoCDCommitExecutor(sender, serviceFactory, webhook, repository, event);
  }


  @Test
  void shouldThrowExceptionForMissingHttpProtocol() {
    when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("main"));
    when(service.getSupportedProtocols()).thenReturn(Stream.of());

    ArgoCDCommitExecutor executor = createExecutor(new ArgoCDWebhook(SCMM, "https://argo-test.com", "", false));

    assertThrows(ArgoCDHookExecutionException.class, executor::run);
  }

  @Test
  void shouldThrowExceptionForMissingDefaultBranch() throws IOException {
    when(branchesCommandBuilder.getBranches()).thenReturn(
      new Branches(singletonList(Branch.normalBranch("main", "abc", 0L, new Person())))
    );

    when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("main"));
    ArgoCDCommitExecutor executor = createExecutor(new ArgoCDWebhook(SCMM, "https://argo-test.com/webhook", "", false));

    assertThrows(InternalRepositoryException.class, executor::run);
  }

  @Nested
  class SendEventCheck {
    @BeforeEach
    void initRepoMocks() throws IOException {
      when(branchesCommandBuilder.getBranches()).thenReturn(
        new Branches(singletonList(Branch.defaultBranch("main", "abc", 0L, new Person())))
      );
      when(service.getSupportedProtocols()).thenReturn(Stream.of(new ScmProtocol() {
        @Override
        public String getType() {
          return "http";
        }

        @Override
        public String getUrl() {
          return "http://scm-manager.org/scm/somenamespace/repository";
        }
      }));
    }

    @Test
    void shouldTriggerSCMMWebhookWithModifiedBranches() {
      ArgumentCaptor<ScmmPushEventPayload> payloadCaptor = ArgumentCaptor.forClass(ScmmPushEventPayload.class);
      ArgoCDWebhook webhook = new ArgoCDWebhook(SCMM, "https://argo-test.com/webhook", "", false);

      when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("main"));
      ArgoCDCommitExecutor executor = createExecutor(webhook);

      executor.run();

      verify(sender).sendEvent(eq(webhook), payloadCaptor.capture());

      ScmmPushEventPayload payload = payloadCaptor.getValue();

      assertEquals(new ScmmPushEventPayload.WebhookBranch(true, "main"), payload.getBranch());
      assertEquals("http://scm-manager.org/scm/somenamespace/repository", payload.getRepository().getSourceUrl());
    }

    @Test
    void shouldTriggerSCMMWebhookWithDeletedBranches() {
      ArgumentCaptor<ScmmPushEventPayload> payloadCaptor = ArgumentCaptor.forClass(ScmmPushEventPayload.class);
      ArgoCDWebhook webhook = new ArgoCDWebhook(SCMM, "https://argo-test.com/webhook", "", false);

      when(event.getContext().getBranchProvider().getDeletedOrClosed()).thenReturn(singletonList("feature"));
      ArgoCDCommitExecutor executor = createExecutor(webhook);

      executor.run();

      verify(sender).sendEvent(eq(webhook), payloadCaptor.capture());

      ScmmPushEventPayload payload = payloadCaptor.getValue();

      assertEquals(new ScmmPushEventPayload.WebhookBranch(false, "feature"), payload.getBranch());
      assertEquals("http://scm-manager.org/scm/somenamespace/repository", payload.getRepository().getSourceUrl());
    }

    @Test
    void shouldTriggerGitHubWebhookWithModifiedBranches() {
      ArgumentCaptor<GitHubPushEventPayload> payloadCaptor = ArgumentCaptor.forClass(GitHubPushEventPayload.class);
      ArgoCDWebhook webhook = new ArgoCDWebhook(GITHUB, "https://argo-test.com/webhook", "", false);

      when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("main"));
      ArgoCDCommitExecutor executor = createExecutor(webhook);

      executor.run();

      verify(sender).sendEvent(eq(webhook), payloadCaptor.capture());

      GitHubPushEventPayload payload = payloadCaptor.getValue();

      assertEquals(new GitHubPushEventPayload("http://scm-manager.org/scm/somenamespace/repository", "main", "main"), payload);
    }

    @Test
    void shouldTriggerGitHubWebhookWithDeletedBranches() {
      ArgumentCaptor<GitHubPushEventPayload> payloadCaptor = ArgumentCaptor.forClass(GitHubPushEventPayload.class);
      ArgoCDWebhook webhook = new ArgoCDWebhook(GITHUB, "https://argo-test.com/webhook", "", false);

      when(event.getContext().getBranchProvider().getDeletedOrClosed()).thenReturn(singletonList("feature"));
      ArgoCDCommitExecutor executor = createExecutor(webhook);

      executor.run();

      verify(sender).sendEvent(eq(webhook), payloadCaptor.capture());

      GitHubPushEventPayload payload = payloadCaptor.getValue();

      assertEquals(new GitHubPushEventPayload("http://scm-manager.org/scm/somenamespace/repository", "main", "feature"), payload);
    }
  }
}
