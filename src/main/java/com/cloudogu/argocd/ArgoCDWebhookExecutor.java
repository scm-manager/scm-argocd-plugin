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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import sonia.scm.net.ahc.AdvancedHttpClient;
import sonia.scm.net.ahc.AdvancedHttpRequestWithBody;
import sonia.scm.repository.Repository;
import sonia.scm.webhook.WebHookExecutor;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

public class ArgoCDWebhookExecutor implements WebHookExecutor {

  private final AdvancedHttpClient client;
  private final ArgoCDWebhookPayloader payloader;
  private final ArgoCDWebhook webhook;
  private final Repository repository;

  public ArgoCDWebhookExecutor(AdvancedHttpClient client, ArgoCDWebhookPayloader payloader, ArgoCDWebhook webhook, Repository repository) {
    this.client = client;
    this.payloader = payloader;
    this.webhook = webhook;
    this.repository = repository;
  }

  @Override
  public void run() {
    try {
      GitHubPushEventPayloadDto payload = payloader.createPayload(repository);
      AdvancedHttpRequestWithBody request = client.post(webhook.getUrl())
        .header("X-Github-Event", "push");

      if (!Strings.isNullOrEmpty(webhook.getSecret())) {
          request.header("X-Hub-Signature", "sha1=" + hmacSha1(webhook.getSecret(), payload));
        }

        request.spanKind("Webhook")
        .contentType(MediaType.APPLICATION_JSON)
        .jsonContent(payload)
        .request()
        .getStatus();

    } catch (IOException e) {
      throw new ArgoCDHookExecutionException(
        entity(Repository.class, repository.getNamespaceAndName().toString()).build(),
        "Could not execute ArgoCD webhook",
        e
      );
    }
  }

  public static String hmacSha1(String key, GitHubPushEventPayloadDto value) throws JsonProcessingException {
    String json = new ObjectMapper().writeValueAsString(value);
    return new HmacUtils(HmacAlgorithms.HMAC_SHA_1, key).hmacHex(json.getBytes());
  }
}
