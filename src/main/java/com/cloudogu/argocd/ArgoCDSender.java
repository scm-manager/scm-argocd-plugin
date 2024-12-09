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

import com.google.common.base.Strings;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import sonia.scm.webhook.HttpMethod;
import sonia.scm.webhook.WebHookExecutionHeader;
import sonia.scm.webhook.WebHookService;
import sonia.scm.webhook.execution.WebHookExecution;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

class ArgoCDSender {

  private final WebHookService webHookService;

  public ArgoCDSender(WebHookService webHookService) {
    this.webHookService = webHookService;
  }

  void sendEvent(ArgoCDWebhook webhook, PullRequestEventPayload payload) {
    if (webhook.getHookImplementation().equals(HookImplementation.SCMM)) {
      sendEvent(webhook, payload, HookImplementation.EventType.PULL_REQUEST_EVENT);
    } else {
      throw new UnsupportedOperationException("Tried to send a pull request event for a Github key.");
    }
  }

  void sendEvent(ArgoCDWebhook webhook, PushEventPayload payload) {
    sendEvent(webhook, payload, HookImplementation.EventType.PUSH_EVENT);
  }

  private void sendEvent(ArgoCDWebhook webhook, Object payload, HookImplementation.EventType eventType) {
    String url = normalizeUrl(webhook.getUrl()) + "api/webhook";
    List<WebHookExecutionHeader> headerList = new ArrayList<>();
    headerList.add(WebHookExecutionHeader.from(webhook.getHookImplementation().getHeader(eventType)));

    if (!Strings.isNullOrEmpty(webhook.getSecret())) {
      HmacUtils hmacUtils = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, webhook.getSecret());
      WebHookExecutionHeader securityHeader = getWebHookExecutionSecurityHeader(webhook, hmacUtils);
      headerList.add(securityHeader);
    }

    WebHookExecution.WebHookExecutionBuilder executionBuilder = WebHookExecution
      .builder()
      .httpMethod(HttpMethod.POST)
      .url(url)
      .headers(headerList)
      .payload(payload);
    if (webhook.isInsecure()) {
      executionBuilder
        .prepare(b -> {
          b.disableCertificateValidation(true);
          b.disableHostnameValidation(true);
        });
    }
    webHookService.execute(executionBuilder.build());
  }

  protected static WebHookExecutionHeader getWebHookExecutionSecurityHeader(ArgoCDWebhook webhook, HmacUtils hmacUtils) {
    Supplier<String> hmacRunner = () -> {
      String secret = webhook.getSecret();
      String digest = hmacUtils.hmacHex(secret);
      return "sha1=" + digest;
    };
    return new WebHookExecutionHeader(
      webhook.getHookImplementation().getSecurityHeaderKey(), hmacRunner);
  }

  private String normalizeUrl(String url) {
    if (!url.endsWith("/")) {
      return url + "/";
    } else {
      return url;
    }
  }
}
