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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.net.ahc.AdvancedHttpClient;
import sonia.scm.net.ahc.AdvancedHttpRequestWithBody;
import sonia.scm.net.ahc.Content;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Branches;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.repository.api.ScmProtocol;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.stream.Stream;

import static com.cloudogu.argocd.HookImplementation.GITHUB;
import static com.cloudogu.argocd.HookImplementation.SCMM;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArgoCDWebhookExecutorTest {

  @Mock
  private AdvancedHttpClient client;
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

  @Mock
  private Content content;

  private final Repository repository = RepositoryTestData.create42Puzzle();

  @BeforeEach
  void initClient() {
    lenient().when(client.post(any())).thenReturn(request);
    lenient().when(request.getContent()).thenReturn(content);
    when(serviceFactory.create(repository)).thenReturn(service);
    when(service.getBranchesCommand()).thenReturn(branchesCommandBuilder);
  }

  @Test
  void shouldThrowExceptionForMissingHttpProtocol() {
    when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("main"));
    ArgoCDWebhookExecutor executor = createExecutor(new ArgoCDWebhook(SCMM, "https://argo-test.com/webhook", "", false));

    assertThrows(ArgoCDHookExecutionException.class, executor::run);
  }

  @Test
  void shouldThrowExceptionForMissingDefaultBranch() throws IOException {
    when(branchesCommandBuilder.getBranches()).thenReturn(
      new Branches(singletonList(Branch.normalBranch("main", "abc", 0L)))
    );

    when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("main"));
    ArgoCDWebhookExecutor executor = createExecutor(new ArgoCDWebhook(SCMM, "https://argo-test.com/webhook", "", false));

    assertThrows(InternalRepositoryException.class, executor::run);
  }

  @Nested
  class WithHttpUrlAndDefaultBranch {

    @BeforeEach
    void initRepoMocks() throws IOException {
      when(branchesCommandBuilder.getBranches()).thenReturn(
        new Branches(singletonList(Branch.defaultBranch("main", "abc", 0L)))
      );
      when(service.getSupportedProtocols()).thenReturn(Stream.of(new ScmProtocol() {
        @Override
        public String getType() {
          return "http";
        }

        @Override
        public String getUrl() {
          return "https://test.de";
        }
      }));
    }

    @Nested
    class WithScmmImplementation {

      @Test
      void shouldTriggerWebhookWithoutSecretWithModifiedBranches() {
        when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("main"));
        ArgoCDWebhookExecutor executor = createExecutor(new ArgoCDWebhook(SCMM, "https://argo-test.com/webhook", "", false));

        executor.run();

        verify(request).header("X-SCM-PushEvent", "Push");
        verify(request).spanKind("Webhook");
        verify(request).contentType(MediaType.APPLICATION_JSON);
        verify(request).jsonContent(argThat((ScmPushEventPayload p) -> {
          assertThat(p.getHtmlUrl()).isEqualTo("https://test.de");
          assertThat(p.getBranch().isDefaultBranch()).isTrue();
          return true;
        }));
      }

      @Test
      void shouldNotTriggerWebhookWithoutSecretWithDeletedBranches() {
        when(event.getContext().getBranchProvider().getDeletedOrClosed()).thenReturn(singletonList("feature"));
        ArgoCDWebhookExecutor executor = createExecutor(new ArgoCDWebhook(SCMM, "https://argo-test.com/webhook", "", false));

        executor.run();

        verify(request).header("X-SCM-PushEvent", "Push");
        verify(request).spanKind("Webhook");
        verify(request).contentType(MediaType.APPLICATION_JSON);
        verify(request).jsonContent(argThat((ScmPushEventPayload p) -> {
          assertThat(p.getHtmlUrl()).isEqualTo("https://test.de");
          assertThat(p.getBranch().isDefaultBranch()).isFalse();
          return true;
        }));
      }

      @Test
      void shouldTriggerWebhookWithSecret() {
        when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("main"));
        ArgoCDWebhookExecutor executor = createExecutor(new ArgoCDWebhook(SCMM, "https://argo-test.com/webhook", "456", false));

        executor.run();

        verify(request).header("X-SCM-PushEvent", "Push");
        verify(request).header("X-SCM-Signature", "sha1=22c2bbe31bd7e8cea1169f6cbbf89f7935a6116a");
        verify(request).spanKind("Webhook");
        verify(request).contentType(MediaType.APPLICATION_JSON);
        verify(request).jsonContent(argThat((ScmPushEventPayload p) -> {
          assertThat(p.getHtmlUrl()).isEqualTo("https://test.de");
          assertThat(p.getBranch().isDefaultBranch()).isTrue();
          return true;
        }));
      }

      @Test
      void shouldTriggerWebhookWithoutInsecureOption() {
        when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("test"));
        ArgoCDWebhookExecutor executor = createExecutor(new ArgoCDWebhook(SCMM, "https://argo-test.com/webhook", "456", false));

        executor.run();

        verify(request, never()).disableCertificateValidation(true);
        verify(request, never()).disableHostnameValidation(true);
      }

      @Test
      void shouldTriggerWebhookWithInsecureOption() {
        when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("test"));
        ArgoCDWebhookExecutor executor = createExecutor(new ArgoCDWebhook(SCMM, "https://argo-test.com/webhook", "456", true));

        executor.run();

        verify(request).disableCertificateValidation(true);
        verify(request).disableHostnameValidation(true);
      }
    }

    @Nested
    class WithGithubImplementation {

      @Test
      void shouldTriggerWebhookWithoutSecretWithModifiedBranches() {
        when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("main"));
        ArgoCDWebhookExecutor executor = createExecutor(new ArgoCDWebhook(GITHUB, "https://argo-test.com/webhook", "", false));

        executor.run();

        verify(request).header("X-Github-Event", "push");
        verify(request).spanKind("Webhook");
        verify(request).contentType(MediaType.APPLICATION_JSON);
        verify(request).jsonContent(argThat((GitHubPushEventPayloadDto p) -> {
          assertThat(p.getCommits()).isEmpty();
          assertThat(p.getRef()).isEqualTo("refs/heads/main");
          assertThat(p.getRepository().getHtmlUrl()).isEqualTo("https://test.de");
          assertThat(p.getRepository().getDefaultBranch()).isEqualTo("main");
          return true;
        }));
      }

      @Test
      void shouldTriggerWebhookWithSecret() {
        when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("main"));
        ArgoCDWebhookExecutor executor = createExecutor(new ArgoCDWebhook(GITHUB, "https://argo-test.com/webhook", "456", false));

        executor.run();

        verify(request).header("X-Github-Event", "push");
        verify(request).header("X-Hub-Signature", "sha1=22c2bbe31bd7e8cea1169f6cbbf89f7935a6116a");
        verify(request).spanKind("Webhook");
        verify(request).contentType(MediaType.APPLICATION_JSON);
        verify(request).jsonContent(argThat((GitHubPushEventPayloadDto p) -> {
          assertThat(p.getCommits()).isEmpty();
          assertThat(p.getRef()).isEqualTo("refs/heads/main");
          assertThat(p.getRepository().getHtmlUrl()).isEqualTo("https://test.de");
          assertThat(p.getRepository().getDefaultBranch()).isEqualTo("main");
          return true;
        }));
      }
    }
  }

  private ArgoCDWebhookExecutor createExecutor(ArgoCDWebhook webhook) {
    return new ArgoCDWebhookExecutor(client, serviceFactory, webhook, repository, event);
  }
}
