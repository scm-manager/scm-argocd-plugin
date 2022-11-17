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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.net.ahc.AdvancedHttpClient;
import sonia.scm.net.ahc.AdvancedHttpRequestWithBody;
import sonia.scm.net.ahc.Content;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import javax.ws.rs.core.MediaType;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArgoCDWebhookExecutorTest {

  @Mock
  private AdvancedHttpClient client;

  @Mock(answer = Answers.RETURNS_SELF)
  private AdvancedHttpRequestWithBody request;

  @Mock
  private ArgoCDWebhookPayloadGenerator payloader;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private PostReceiveRepositoryHookEvent event;

  @Mock
  private Content content;
  private final GitHubPushEventPayloadDto payload = new GitHubPushEventPayloadDto(new GitHubRepository("test.de", "main"));

  private final Repository repository = RepositoryTestData.create42Puzzle();

  @BeforeEach
  void initClient() {
    when(client.post(any())).thenReturn(request);
    when(payloader.createPayload(repository, "main")).thenReturn(payload);
    lenient().when(request.getContent()).thenReturn(content);
  }

  @Test
  void shouldTriggerWebhookWithoutSecretWithModifiedBranches() {
    when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("main"));
    ArgoCDWebhookExecutor executor = createExecutor(new ArgoCDWebhook("https://argo-test.com/webhook", ""));

    executor.run();

    verify(request).header("X-Github-Event", "push");
    verify(request).spanKind("Webhook");
    verify(request).contentType(MediaType.APPLICATION_JSON);
    verify(request).jsonContent(payload);
  }

  @Test
  void shouldTriggerWebhookWithoutSecretWithDeletedBranches() {
    when(event.getContext().getBranchProvider().getDeletedOrClosed()).thenReturn(singletonList("main"));
    ArgoCDWebhookExecutor executor = createExecutor(new ArgoCDWebhook("https://argo-test.com/webhook", ""));

    executor.run();

    verify(request).header("X-Github-Event", "push");
    verify(request).spanKind("Webhook");
    verify(request).contentType(MediaType.APPLICATION_JSON);
    verify(request).jsonContent(payload);
  }

  @Test
  void shouldTriggerWebhookWithSecret() {
    when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(singletonList("main"));
    ArgoCDWebhookExecutor executor = createExecutor(new ArgoCDWebhook("https://argo-test.com/webhook", "456"));

    executor.run();

    verify(request).header("X-Github-Event", "push");
    verify(request).header("X-Hub-Signature", "sha1=22c2bbe31bd7e8cea1169f6cbbf89f7935a6116a");
    verify(request).spanKind("Webhook");
    verify(request).contentType(MediaType.APPLICATION_JSON);
    verify(request).jsonContent(payload);
  }

  private ArgoCDWebhookExecutor createExecutor(ArgoCDWebhook webhook) {
    return new ArgoCDWebhookExecutor(client, payloader, webhook, repository, event);
  }
}
