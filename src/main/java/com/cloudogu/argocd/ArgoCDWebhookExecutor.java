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

import com.google.common.base.Strings;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import sonia.scm.net.ahc.AdvancedHttpClient;
import sonia.scm.net.ahc.AdvancedHttpRequestWithBody;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.webhook.WebHookExecutor;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ArgoCDWebhookExecutor implements WebHookExecutor {

  private final AdvancedHttpClient client;
  private final ArgoCDWebhookPayloadGenerator payloader;
  private final ArgoCDWebhook webhook;
  private final Repository repository;
  private final PostReceiveRepositoryHookEvent event;

  public ArgoCDWebhookExecutor(AdvancedHttpClient client, ArgoCDWebhookPayloadGenerator payloader, ArgoCDWebhook webhook, Repository repository, PostReceiveRepositoryHookEvent event) {
    this.client = client;
    this.payloader = payloader;
    this.webhook = webhook;
    this.repository = repository;
    this.event = event;
  }

  @Override
  public void run() {
    HookBranchProvider branchProvider = event.getContext().getBranchProvider();
    branchProvider.getCreatedOrModified().forEach(this::sendEvent);
    branchProvider.getDeletedOrClosed().forEach(this::sendEvent);
  }

  private void sendEvent(String branch) {
    try {
      GitHubPushEventPayloadDto payload = payloader.createPayload(repository, branch);
      AdvancedHttpRequestWithBody request = client.post(webhook.getUrl())
        //TODO Remove after testing
        .disableCertificateValidation(true).disableHostnameValidation(true)
        .header("X-Github-Event", "push")
        .spanKind("Webhook")
        .contentType(MediaType.APPLICATION_JSON)
        .jsonContent(payload);

      if (!Strings.isNullOrEmpty(webhook.getSecret())) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
          request.getContent().process(baos);
          String digest = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, webhook.getSecret()).hmacHex(baos.toByteArray());
          request.header("X-Hub-Signature", "sha1=" + digest);
        }
      }

      request
        .request();

    } catch (IOException e) {
      throw new ArgoCDHookExecutionException(
        "Could not execute ArgoCD webhook",
        e
      );
    }
  }
}
