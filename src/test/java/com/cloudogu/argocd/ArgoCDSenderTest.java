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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.net.ahc.AdvancedHttpRequest;
import sonia.scm.webhook.WebHookExecutionHeader;
import sonia.scm.webhook.WebHookHeaderExecutionException;
import sonia.scm.webhook.WebHookService;
import sonia.scm.webhook.execution.WebHookExecution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cloudogu.argocd.HookImplementation.GITHUB;
import static com.cloudogu.argocd.HookImplementation.SCMM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ArgoCDSenderTest {

  @Mock
  WebHookService webHookService;

  @Captor
  ArgumentCaptor<WebHookExecution> argumentCaptor;

  @InjectMocks
  ArgoCDSender sender;

  private static final String SECRET_VALUE = "someSecret";

  void setUpScmmWebHook(ArgoCDWebhook webHook, String url) {
    webHook.setHookImplementation(SCMM);
    webHook.setUrl(url);
    webHook.setInsecure(false);
    webHook.setSecret(SECRET_VALUE);
  }

  void setUpGithubWebHook(ArgoCDWebhook webHook, String url) {
    webHook.setHookImplementation(GITHUB);
    webHook.setUrl(url);
    webHook.setInsecure(false);
    webHook.setSecret(SECRET_VALUE);
  }

  @Nested
  class UrlCheck {

    @Test
    void shouldSendWebHookWithCorrectURLEndingWithoutSlash() {
      String exampleUrl = "https://scm-manager.com/argocd";
      String sourceUrl = "https://scm-manager.com/scm/";
      ArgoCDWebhook webHook = new ArgoCDWebhook();
      setUpScmmWebHook(webHook, exampleUrl);
      ScmmPushEventPayload payload = new ScmmPushEventPayload(sourceUrl, true, "branch123");

      sender.sendEvent(webHook, payload);
      verify(webHookService).execute(argumentCaptor.capture());

      String expectedUrl = "https://scm-manager.com/argocd/api/webhook";
      assertEquals(expectedUrl, argumentCaptor.getValue().getUrl());

    }


    @Test
    void shouldSendWebHookWithCorrectURLEndingWithSlash() {
      String exampleUrl = "https://scm-manager.com/argocd/";
      String sourceUrl = "https://scm-manager.com/scm/";
      ArgoCDWebhook webHook = new ArgoCDWebhook();
      setUpScmmWebHook(webHook, exampleUrl);
      ScmmPushEventPayload payload = new ScmmPushEventPayload(sourceUrl, true, "branch123");

      sender.sendEvent(webHook, payload);
      verify(webHookService).execute(argumentCaptor.capture());

      String expectedUrl = "https://scm-manager.com/argocd/api/webhook";
      assertEquals(expectedUrl, argumentCaptor.getValue().getUrl());
    }
  }

  @Test
  void shouldGetCorrectPayload() {
    String exampleUrl = "https://scm-manager.com/argocd/";
    String sourceUrl = "https://scm-manager.com/scm/";
    ArgoCDWebhook webHook = new ArgoCDWebhook();
    setUpScmmWebHook(webHook, exampleUrl);

    ScmmPushEventPayload payload = new ScmmPushEventPayload(sourceUrl, true, "branch123");

    sender.sendEvent(webHook, payload);
    verify(webHookService).execute(argumentCaptor.capture());

    assertEquals(payload, argumentCaptor.getValue().getPayload());
  }

  @Test
  void shouldAssignCorrectEventTypeForScmmPush() {
    String exampleUrl = "https://scm-manager.com/argocd/";
    String sourceUrl = "https://scm-manager.com/scm/";
    ArgoCDWebhook webHook = new ArgoCDWebhook();
    setUpScmmWebHook(webHook, exampleUrl);

    ScmmPushEventPayload payload = new ScmmPushEventPayload(sourceUrl, true, "branch123");

    sender.sendEvent(webHook, payload);
    verify(webHookService).execute(argumentCaptor.capture());

    WebHookExecutionHeader execHeader = argumentCaptor.getValue().getHeaders()
      .stream().filter(w -> w.getKey().equals("X-SCM-Event")).toList().get(0);

    assertEquals("Push", execHeader.getValue());
  }

  @Test
  void shouldAssignCorrectEventTypeForGithubPush() {
    String exampleUrl = "https://scm-manager.com/argocd/";
    String sourceUrl = "https://scm-manager.com/scm/";
    ArgoCDWebhook webHook = new ArgoCDWebhook();
    setUpGithubWebHook(webHook, exampleUrl);

    GitHubPushEventPayload payload = new GitHubPushEventPayload(sourceUrl, "main", "branch123");

    sender.sendEvent(webHook, payload);
    verify(webHookService).execute(argumentCaptor.capture());

    WebHookExecutionHeader execHeader = argumentCaptor.getValue().getHeaders()
      .stream().filter(w -> w.getKey().equals("X-Github-Event")).toList().get(0);

    assertEquals("Push", execHeader.getValue());
  }

  @Test
  void shouldAssignCorrectEventTypeForPullRequest() {
    String exampleUrl = "https://scm-manager.com/argocd/";
    ScmmPullRequestEventPayload.Repository repository = new ScmmPullRequestEventPayload.Repository(
      "https://scm-manager.com/scm/", "namespace", "name"
    );
    ArgoCDWebhook webHook = new ArgoCDWebhook();
    setUpScmmWebHook(webHook, exampleUrl);

    ScmmPullRequestEventPayload payload =
      new ScmmPullRequestEventPayload(repository, "main", "branch123", ScmmPullRequestEventPayload.Action.MERGED);

    sender.sendEvent(webHook, payload);
    verify(webHookService).execute(argumentCaptor.capture());

    WebHookExecutionHeader execHeader = argumentCaptor.getValue().getHeaders()
      .stream().filter(w -> w.getKey().equals("X-SCM-Event")).toList().get(0);

    try {
      assertEquals("PullRequest", execHeader.getValue());
    } catch (WebHookHeaderExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void shouldSetInsecureFlagIfRequested() {
    String exampleUrl = "https://scm-manager.com/argocd/";
    String sourceUrl = "https://scm-manager.com/scm/";
    ArgoCDWebhook webHook = new ArgoCDWebhook();
    setUpScmmWebHook(webHook, exampleUrl);
    webHook.setInsecure(true);

    ScmmPushEventPayload payload = new ScmmPushEventPayload(sourceUrl, true, "branch123");

    sender.sendEvent(webHook, payload);
    verify(webHookService).execute(argumentCaptor.capture());

    WebHookExecution webHookExecution = argumentCaptor.getValue();
    AdvancedHttpRequest request = mock(AdvancedHttpRequest.class);
    webHookExecution.getPrepare().accept(request);

    verify(request).disableCertificateValidation(true);
    verify(request).disableHostnameValidation(true);
  }

  @Test
  void shouldNotSetInsecureFlagIfNotRequested() {
    String exampleUrl = "https://scm-manager.com/argocd/";
    String sourceUrl = "https://scm-manager.com/scm/";
    ArgoCDWebhook webHook = new ArgoCDWebhook();
    setUpScmmWebHook(webHook, exampleUrl);
    webHook.setInsecure(false);

    ScmmPushEventPayload payload = new ScmmPushEventPayload(sourceUrl, true, "branch123");

    sender.sendEvent(webHook, payload);

    // We implicitly expect the WebHookExecution to not be Preparable.
    // At the time this test was written, Preparable WebHookExecutions were only used for security inactivation.
    verify(webHookService).execute(argumentCaptor.capture());
  }

  @Nested
  class Secret {
    private Map<String, String> getHeaders(List<WebHookExecutionHeader> headers) {
      Map<String, String> map = new HashMap<>();
      headers.forEach(header -> {
        try {
          map.put(header.getKey(), header.getValue());
        } catch (WebHookHeaderExecutionException e) {
          log.warn("Webhook with key " + header.getKey() + " ignored due to header exception.");
        }
      });
      return map;
    }

    @Test
    void shouldBeAbleToGenerateHeaderWithHmacApplied() {
      HmacUtils hmacUtils = mock(HmacUtils.class);
      when(hmacUtils.hmacHex(any(String.class))).thenReturn("H#SHED_VALUE");
      String exampleUrl = "https://scm-manager.com/argocd/";
      ArgoCDWebhook webHook = new ArgoCDWebhook();
      setUpScmmWebHook(webHook, exampleUrl);

      WebHookExecutionHeader secretHeader = ArgoCDSender.getWebHookExecutionSecurityHeader(webHook, hmacUtils);
      String hashedValue = null;
      try {
        hashedValue = secretHeader.getValue();
      } catch (WebHookHeaderExecutionException e) {
        fail("WebHook header exception thrown. Please check test structure.");
      }

      verify(hmacUtils).hmacHex(SECRET_VALUE);
      assertEquals(webHook.getHookImplementation().getSecurityHeaderKey(), secretHeader.getKey());
      assertEquals("sha1=H#SHED_VALUE", hashedValue);
    }

    @Test
    void shouldTriggerSCMMWebhookWithSecret() {
      ArgoCDWebhook webHook = new ArgoCDWebhook(SCMM, "https://argo-test.com/webhook", "456", false);
      setUpScmmWebHook(webHook, "https://argo-test.com/webhook");

      String sourceUrl = "https://scm-manager.com/scm/";
      ScmmPushEventPayload payload = new ScmmPushEventPayload(sourceUrl, true, "main");

      sender.sendEvent(webHook, payload);

      verify(webHookService).execute(argumentCaptor.capture());

      WebHookExecution webHookExecution = argumentCaptor.getValue();

      Map<String, String> headers = getHeaders(webHookExecution.getHeaders());

      assertThat(headers).containsEntry("X-SCM-Signature", "sha1=3e7469b1596c78380a1fe7842f44202600f7f4ac");
      assertThat(headers).containsEntry("X-SCM-Event", "Push");
    }

    @Test
    void shouldTriggerGitHubWebhookWithSecret() {
      ArgoCDWebhook webHook = new ArgoCDWebhook(GITHUB, "https://argo-test.com/webhook", "456", false);
      setUpGithubWebHook(webHook, "https://argo-test.com/webhook");

      String sourceUrl = "https://scm-manager.com/scm/";
      GitHubPushEventPayload payload = new GitHubPushEventPayload(sourceUrl, "main", "main");

      sender.sendEvent(webHook, payload);

      verify(webHookService).execute(argumentCaptor.capture());

      WebHookExecution webHookExecution = argumentCaptor.getValue();

      Map<String, String> headers = getHeaders(webHookExecution.getHeaders());

      assertThat(headers).containsEntry("X-Hub-Signature", "sha1=3e7469b1596c78380a1fe7842f44202600f7f4ac");
      assertThat(headers).containsEntry("X-Github-Event", "Push");
    }
  }
}
